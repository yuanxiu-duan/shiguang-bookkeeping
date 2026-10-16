# EdgeOne CDN 大文件上传 524 超时排查与解决

![EdgeOne 524 超时主图](./images/edgeone-524-cover.svg)

> 一次"响应解析失败"的线上事故，根源竟在 CDN 超时。本文记录完整排查过程与三种解决方案。

## 一、现象

某次线上发布 APP 新版本，管理员在 Web 后台上传 APK 文件（约 15MB）时，浏览器报错：

```
响应解析失败
```

控制台看到 `POST /api/admin/app-releases/upload-file` 返回了 **HTTP 524**，响应体为空，导致前端 `JSON.parse("")` 抛出异常。

## 二、排查过程

### 1. 本地测试：后端没问题

用 Node 脚本直接请求本地后端（绕过 CDN 和 Nginx）：

| 文件大小 | 耗时 | HTTP | 结果 |
|---------|------|------|------|
| 1 KB | 1.7s | 200 | 成功 |
| 1 MB | 2.9s | 200 | 成功 |
| 14.74 MB（APK） | 16.7s | 200 | 成功 |

本地直接上传 15MB APK 完全正常，后端代码、OSS 配置都没问题。

### 2. 线上测试：超时出现在 CDN 层

用同样脚本请求线上接口 `https://your-domain.com/api/admin/app-releases/upload-file`：

| 文件大小 | 耗时 | HTTP | 结果 |
|---------|------|------|------|
| 1 KB | 1.7s | 200 | 成功 |
| 1 MB | 2.9s | 200 | 成功 |
| 14.74 MB（APK） | 16.7s | **524** | **失败** |

关键证据在响应头：

```
HTTP/1.1 524 Receive timeout from origin
server: TencentEdgeOne
content-length: 0
```

**`server: TencentEdgeOne`** —— 响应不是 Nginx 返回的，也不是后端返回的，而是腾讯云 EdgeOne CDN 直接返回的 524。

### 3. 为什么是 524？

524 是 Cloudflare 和 EdgeOne 特有的状态码，含义是"源站已建立连接但响应超时"。

EdgeOne 作为反向代理，请求链路如下：

```
浏览器 → EdgeOne CDN → Nginx → 后端 (Spring Boot) → OSS (Cloudflare R2)
```

15MB 文件上传链路：
1. **浏览器 → EdgeOne**：上传文件流（用户上行带宽）
2. **EdgeOne → Nginx**：转发文件流
3. **Nginx → 后端**：转发文件流
4. **后端 → OSS**：再上传到 OSS（出网到 Cloudflare R2）

整个链路是**串行**的，后端必须**接收完整文件**后才能开始上传到 OSS。15MB 文件经过三跳 + OSS 上传，总耗时 16.7 秒。

而 EdgeOne 对源站的默认响应超时是 **15 秒**，超过就掐断连接返回 524，响应体为空。

Nginx 配置 `proxy_read_timeout 180s` 完全没问题，但 **EdgeOne 在 Nginx 之前就掐断了**，Nginx 配置再大也没用。

### 4. 关键陷阱：改错域名

排查时一度怀疑是 CDN 超时，但改了 CDN 超时后仍失败。原因是：

- `oss.your-domain.com` 是 OSS 公开下载域名
- `your-domain.com` 才是主站域名，**上传请求走的是这个域名**

改错域名，问题依旧。

## 三、解决方案

### 方案 1：调大 EdgeOne 回源超时（最简单，治标）

在 EdgeOne 控制台 → 站点加速 → 找到 **your-domain.com** 域名 → 规则引擎 → 修改"回源超时时间"：

- HTTP 应答超时：**150 秒**（或更大）

**优点**：不改代码，立即生效。
**缺点**：治标不治本。文件更大时（如 50MB+）仍可能超时，且 CDN 超时不宜设过大，否则真异常时用户等待时间过长。

> ⚠️ 注意：修改时一定要确认改的是**主站域名**（your-domain.com），而不是 OSS 下载域名（oss.your-domain.com）。

### 方案 2：预签名 URL 直传 OSS（推荐，治本）

让前端直接 PUT 文件到 OSS，完全绕过 CDN 和后端：

```
浏览器 → OSS (Cloudflare R2)   ← 直传，不经过 EdgeOne
后端 ← 浏览器（请求预签名URL）   ← 小请求，毫秒级
```

#### 后端实现（Spring Boot + AWS SDK v2）

```java
// S3StorageService 新增方法
public PresignResult presignUpload(String directory, String fileName, String contentType) {
    OssConfig config = getActiveConfig();
    String key = buildKey(directory, fileName);
    String fullKey = (config.getPrefix() != null && !config.getPrefix().isBlank())
            ? config.getPrefix() + "/" + key : key;

    S3Presigner presigner = buildPresigner(config);
    PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(config.getBucketName())
            .key(fullKey)
            .contentType(contentType)
            .build();

    PresignedPutObjectRequest presigned = presigner.presignPutObject(builder -> builder
            .putObjectRequest(putRequest)
            .signatureDuration(Duration.ofMinutes(10))
            .build());

    String downloadUrl = buildPublicUrl(config, fullKey);
    return new PresignResult(presigned.url().toString(), key, fullKey, downloadUrl);
}

private S3Presigner buildPresigner(OssConfig config) {
    // 与 buildClient 类似，改用 S3Presigner.builder()
    // ...省略，关键是 region/credentials/endpointOverride 与 S3Client 一致
}
```

#### Controller 新增接口

```java
@PostMapping("/app-releases/presign-upload")
public Result<PresignResponse> presignUpload(@RequestBody PresignRequest req) {
    PresignResult result = storageService.presignUpload("app/android", req.getFileName(), req.getContentType());
    return Result.success(new PresignResponse(result.uploadUrl(), result.fileKey(), result.downloadUrl()));
}
```

#### 前端实现（Vue 3 + Element Plus）

```vue
<script setup>
async function handleUpload(file) {
  // 1. 向后端请求预签名 URL（小请求，毫秒级）
  const { uploadUrl, fileKey, downloadUrl } = await adminApi.presignUpload({
    fileName: file.name,
    contentType: 'application/vnd.android.package-archive'
  })

  // 2. 直接 PUT 文件到 OSS（不经过后端和 CDN）
  const res = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/vnd.android.package-archive' },
    body: file.raw
  })
  if (!res.ok) throw new Error('文件上传失败: ' + res.status)

  // 3. 用返回的 downloadUrl 创建发布记录
  await adminApi.createRelease({
    downloadUrl,
    fileKey,
    fileSize: file.size,
    fileName: file.name,
    // ...其他字段
  })
}
</script>
```

**优点**：
- 完全绕过 CDN，**永不超时**
- 上传速度更快（少一跳）
- 后端不承担文件流，资源占用降低
- 支持超大文件（可扩展为分片上传）

**缺点**：
- 需要改后端 + 前端代码
- OSS 桶需配置 CORS 允许浏览器直接 PUT（R2 默认支持）

#### Cloudflare R2 CORS 配置

R2 桶 → Settings → CORS Policy：

```json
[{
  "AllowedOrigins": ["https://your-domain.com"],
  "AllowedMethods": ["PUT"],
  "AllowedHeaders": ["Content-Type"],
  "MaxAgeSeconds": 3600
}]
```

### 方案 3：分片上传（适合超大文件）

如果文件超过 100MB（如视频），可走 S3 Multipart Upload。本项目 APK 通常 10~30MB，**方案 2 已足够**，方案 3 增加复杂度，不推荐。

## 四、最终选择

本项目 APK 约 15MB，且不频繁发布（每月 1~2 次），选择**方案 1（调大 EdgeOne 超时到 150s）**最经济：

- 不改代码
- 立即生效
- 15MB 在 150s 内必传完

> 调整后实际验证上传 14.74MB APK 成功，耗时约 17 秒。

## 五、总结与最佳实践

### 排查 CDN 超时问题的方法

1. **看响应头 `server` 字段**：确认是 CDN（EdgeOne/Cloudflare）还是 Nginx 返回的错误。
2. **本地直连后端测试**：排除后端代码问题。
3. **线上直连测试**：定位是 CDN 层还是源站层问题。
4. **不同文件大小对比**：判断是否是超时阈值问题。

### 架构建议

| 场景 | 推荐方案 |
|------|---------|
| 文件 < 10MB，偶尔上传 | 调大 CDN 超时（方案 1） |
| 文件 10~100MB，频繁上传 | 预签名 URL 直传（方案 2） |
| 文件 > 100MB | 分片上传（方案 3） |
| 用户上传头像等小文件 | 后端代理上传即可，无需特殊处理 |

### 通用经验

1. **CDN 在 Nginx 之前**：Nginx 的 `proxy_read_timeout` 对 CDN 超时无效，必须改 CDN 侧配置。
2. **改配置要改对域名**：上传域名和下载域名可能不同，排查时务必区分。
3. **524 ≠ 后端错误**：看到 524 不要去查后端日志，后端可能根本没收到完整请求。
4. **响应体为空 ≠ 网络断开**：CDN 主动掐断连接时会返回 524 + 空响应体，前端 `JSON.parse` 会失败，需要兜底处理。

### 前端兜底处理

无论用哪种方案，前端都应对空响应做兜底：

```js
const text = await res.text()
if (!text) {
  throw new Error('服务器响应超时（524），请稍后重试或检查网络')
}
const json = JSON.parse(text)
```

避免"响应解析失败"这种对用户毫无意义的报错。

---

**环境信息**：腾讯云 EdgeOne + Nginx + Spring Boot 3.2 + Cloudflare R2 + Vue 3

**事件日期**：2026-06-25
