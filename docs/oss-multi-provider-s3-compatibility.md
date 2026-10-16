# 为什么 AWS S3 SDK 不能「开箱即用」兼容所有 OSS？—— 多提供商对象存储踩坑实录

> 当你以为"S3 兼容"就是一句 `new S3Client()` 搞定一切，现实会给你一记响亮的 403。

---

## 背景：一个记账应用的"野心"

最近给「拾光账本」加了一个 APP 版本分发功能，需要上传 APK 到对象存储。考虑到用户可能使用不同的云服务商，我决定支持**多提供商 OSS**：

- ☁️ **AWS S3**（全球标准）
- 🇨🇳 **阿里云 OSS**
- 🇨🇳 **腾讯云 COS**
- 🇨🇳 **七牛云 Kodo**
- 🌐 **Cloudflare R2**（零出站流量费）

技术栈是 **Spring Boot 3.2 + AWS SDK for Java v2**。

直觉上，既然大家都说自己是 "S3 兼容"，那我只需换 `endpoint` 和 `credentials` 就行了吧？

```java
// 天真的想法
S3Client.builder()
    .region(Region.of("us-east-1"))
    .endpointOverride(URI.create("https://cos.ap-guangzhou.myqcloud.com"))
    .credentialsProvider(...)
    .build();
```

**然后我花了整整一个下午调试 403 错误。**

---

## 一、S3 "标准" 的幻象

AWS S3 的 API 协议确实成了事实上的行业标准，但各厂商的"兼容"程度差异巨大，主要体现在三个方面：

| 差异维度 | AWS S3 原生 | 兼容实现 |
|---------|------------|---------|
| **签名算法** | AWS Signature V4 | 各有变种 |
| **Region 处理** | 必填，影响签名 | 部分厂商用 auto/固定值 |
| **Path Style** | Virtual-hosted（默认） | 部分只支持 Path-style |
| **Endpoint 格式** | `s3.us-east-1.amazonaws.com` | 各家完全不同 |

核心矛盾在于：**AWS SDK 是为 AWS 设计的，它的默认行为基于 AWS 的规范。当你接到其他厂商时，SDK 仍然用 AWS 的"思维方式"签名请求，自然会被拒绝。**

---

## 二、五个提供商，五种配置

### 2.1 AWS S3 —— 最标准，也最省心

```java
Region region = Region.of("us-east-1");  // 真实 region
// endpoint 不需要 override，SDK 自动处理
// forcePathStyle = false（默认，使用 virtual-hosted style）
```

AWS S3 是 SDK 的"亲儿子"，用默认配置就行。

### 2.2 阿里云 OSS —— 需要 forcePathStyle

```java
Region region = Region.of("cn-hangzhou");
URI endpoint = URI.create("https://oss-cn-hangzhou.aliyuncs.com");
// 关键：阿里云 OSS 不支持 virtual-hosted，必须 path-style
boolean forcePathStyle = true;
```

阿里云 OSS 对 Signature V4 的支持较好，但 **bucket 访问方式必须用 path-style**（`/bucket-name/key` 而不是 `bucket-name.endpoint/key`）。

### 2.3 腾讯云 COS —— 类似阿里云

```java
Region region = Region.of("ap-guangzhou");
URI endpoint = URI.create("https://cos.ap-guangzhou.myqcloud.com");
boolean forcePathStyle = true;
```

和阿里云类似，腾讯云 COS 也需要 path-style 访问。

### 2.4 七牛云 Kodo —— 自有 region 体系

```java
Region region = Region.of("z0");  // 七牛用 z0/z1/z2/na0 等
URI endpoint = URI.create("https://s3-cn-east-1.qiniucs.com");
boolean forcePathStyle = true;
```

七牛的 region 代码和 AWS 完全不同（`z0` 代表华东），但 S3 兼容层接受它。

### 2.5 Cloudflare R2 —— 最"惊喜"的一个

```java
Region region = Region.of("auto");  // ⚠️ 必须是 "auto"！
URI endpoint = URI.create("https://<account-id>.r2.cloudflarestorage.com");
boolean forcePathStyle = true;
```

**这里就是整个调试过程的核心！**

---

## 三、Cloudflare R2 的 403 调试之旅

### 第一回合：用 `us-east-1`

```
连接失败: The request signature we calculated does not match 
the signature you provided. (Status Code: 403)
```

R2 是 Cloudflare 的全球对象存储，没有传统意义上的 "region" 概念。用 `us-east-1` 签名 → SDK 计算出的签名带了 region 信息 → R2 不认 → **403 SignatureDoesNotMatch**。

### 第二回合：换成 `auto`

AWS SDK v2 支持 `Region.of("auto")`，但需要**同时禁用 S3Configuration 的 path-style 自动检测**，否则会产生配置冲突：

```java
// ❌ 错误做法：S3Configuration 和 forcePathStyle 同时设置会冲突
S3Configuration s3Config = S3Configuration.builder()
    .pathStyleAccessEnabled(true)
    .build();

builder.serviceConfiguration(s3Config)
       .forcePathStyle(true);  // 冲突！
```

SDK 会抛出 `java.lang.IllegalStateException`，因为两个配置项控制同一个行为。

```java
// ✅ 正确做法：只用 forcePathStyle
S3Client.builder()
    .region(Region.of("auto"))
    .endpointOverride(URI.create("https://xxx.r2.cloudflarestorage.com"))
    .forcePathStyle(true)   // 只用 builder 级别的配置
    .credentialsProvider(...)
    .build();
```

### 第三回合：又爆 403，但错误不一样了

```
连接失败: null (Service: S3, Status Code: 403, Request ID: null)
```

这次错误信息是 `null`，说明请求到达了 R2 但签名仍不对。**最终发现根因完全不是代码问题**——

### 根因：数据库字段填串了！

数据库 `oss_configs` 表里：

| 字段 | 存储的值 | 应该是 |
|------|---------|--------|
| `bucket_name` | `<secret-key>`（Secret Key！） | `<bucket-name>` |
| `secret_key` | `<bucket-name>`（Bucket 名！） | `<secret-key>` |

**Bucket 和 Secret Key 两个字段填反了。** SDK 用 Bucket 名当作 Secret Key 去签名 → R2 验签失败 → 403。

> 💡 **教训**：调试时不要只盯着代码，先验证一下**配置数据本身是否正确**。

---

## 四、最终的统一架构

```java
private S3Client buildClient(OssConfig config) {
    // 1. 防呆：自动 trim 空格和末尾斜杠
    String accessKey = config.getAccessKey().trim();
    String secretKey = config.getSecretKey().trim();
    String endpoint = config.getEndpoint().trim();
    while (endpoint.endsWith("/")) {
        endpoint = endpoint.substring(0, endpoint.length() - 1);
    }

    // 2. 根据提供商决定 region
    String region = switch (config.getProvider()) {
        case "CLOUDFLARE" -> "auto";           // R2 签名必须用 auto
        case "ALIYUN"     -> "cn-hangzhou";    // 可自定义
        case "TENCENT"    -> "ap-guangzhou";   // 可自定义
        case "QINIU"      -> "z0";             // 华东
        default           -> "us-east-1";
    };

    // 3. 统一构建
    return S3Client.builder()
        .region(Region.of(region))
        .endpointOverride(URI.create(endpoint))
        .forcePathStyle(true)   // 兼容性最好
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            )
        )
        .build();
}
```

---

## 五、总结：为什么不能「一个配置走天下」

| 原因 | 说明 |
|------|------|
| **签名算法差异** | 虽然都叫 Signature V4，但 region/service 等参数生成规则不同 |
| **Region 语义不同** | AWS 把 region 当数据中心位置；R2 是全球化的一层，没有 region；七牛用自定义代码 |
| **Endpoint 格式各异** | 每家都有自己的域名格式和路由规则 |
| **Path Style 支持度** | AWS 默认 Virtual-hosted；国内厂商大多只支持 Path-style |
| **SDK 的"惯性"** | AWS SDK 默认按 AWS 的规则签名预处理，不会为第三方"让步" |

**一句话总结：**

> "S3 兼容"就像"USB-C 兼容"——插头一样，但传输协议、充电功率、视频输出支持度各不相同。能插进去不代表能正常工作。

---

## 六、给后来者的建议

1. **测试先行**：写一个独立测试程序，依次验证各提供商的配置组合，比反复改 Web 表单效率高 10 倍
2. **加防呆设计**：所有从数据库/用户输入读取的配置值，一律 `trim()` + 格式校验
3. **失败信息完整化**：测试连接失败时返回 `provider + endpoint + region + bucket` 的完整上下文，便于定位
4. **别迷信官网文档**：各厂商的 S3 兼容文档往往滞后于 SDK 版本，实际行为以测试为准

---

*作者: MoneyKeeper Team | 日期: 2026-06-15 | 项目: 拾光账本 (MoneyKeeper)*
