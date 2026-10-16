# 拾光账本 (moneykeeper)

Spring Boot 3.2 + JDK 21 + uni-app (Vue 2) + Vue 3 全栈记账应用。

## Commands

- 启动后端: 双击 `start-server.bat`
- 后端调试: `mvn spring-boot:run`
- 后端打包: `mvn clean package -DskipTests`
- uni-app 开发: 在 `jz-app/` 目录运行 `npm run dev:h5`
- uni-app 构建: 在 `jz-app/` 目录运行 `npm run build`
- Web 端开发: 在 `jz-web/` 目录运行 `npm run dev`

## Architecture

完整目录结构见 [STRUCTURE.md](STRUCTURE.md)。

```
jz-server/          # Spring Boot 后端 (port 8070, 9个Controller, 15个Service)
jz-app/             # uni-app 跨端 (Vue 2, 12个页面)
jz-web/             # Vue 3 + Element Plus 管理端 (11个路由页面)
```

## Dos

- 后端新增 API 时，统一放在对应 Controller，挂 `/api/` 路径
- 验证使用 `jakarta.validation` 的 `@Valid` + DTO 注解
- 事务操作加 `@Transactional`
- 前端 uni-app 使用 Vue 2 Options API（`export default { data()..., methods:... }`）
- 前端 Web 端使用 `<script setup>` + Element Plus 组件
- 弹窗复用 `picker-mask/picker-content` 结构
- 所有 API 调用在 `jz-app/api/index.js` 中统一维护，**必须使用 `request()` 函数**，不要直接调用 `uni.request()`；CryptoFilter 已排除的公开接口（如 check-update）同样适用——`request()` 对无 `encData` 的响应自动跳过解密
- 新增接口默认必须经过 CryptoFilter 加解密；除文件下载/上传、模板下载、APP更新检查、三方回调等确需明文或流式处理的接口外，不要加入排除列表；确需排除时必须使用精确路径匹配，避免 `/export` 误排除 `/export-tasks`
- 错误抛出使用 `IllegalArgumentException`（由 `GlobalExceptionHandler` 统一处理返回 `Result.error`）
- 需要认证的 Controller 方法用 `@AuthenticationPrincipal User user` 获取当前用户
- 新增 API 在 `CHANGELOG.md` 末尾记录

## Don'ts

- 不要修改 `SecurityConfig.java` 的白名单路径，除非确实需要放行
- 不要在 Controller 里写业务逻辑，都放到 Service 层
- 不要用 MyBatis，项目使用 Spring Data JPA
- 不要硬编码密钥/密码，都在 `application.yml` 配置
- 不要在 uni-app 中使用 `<script setup>`，使用 Vue 2 Options API
- 不要添加新的重量级依赖未经验证
- 不要重写/重构整个文件，除非明确要求
- 不要在 `jz-app/api/index.js` 中直接使用 `uni.request()`，必须走 `request()` 统一封装（即使公开接口也一样，CryptoFilter 排除路径的请求加密无害，响应无 `encData` 则自动跳过解密）

## Data Model 要点

- `User`: username(唯一), password(BCrypt), email, nickname(显示优先)
- `VerificationCode`: code(6位数字), type(REGISTER/LOGIN/UPDATE_EMAIL/RESET_PASSWORD), expiredAt(5min)
- 共享账本 `AccountBook`: name/description 只读
- `Transaction`: 仅创建者或账本 OWNER 可编辑/删除

## API 路径速查

完整接口清单（含 DTO）见 [API.md](API.md)。

```
/api/auth/**               # 认证(登录/注册/验证码/用户设置) — 白名单无需认证
/api/account-books/**      # 账本CRUD + 成员管理
/api/transactions/**       # 交易记录CRUD + 导入导出
/api/transactions/search    # 复合筛选流水(GET): startDate/endDate/type/categoryIds(逗号)/account/minAmount/maxAmount
/api/categories/**         # 分类管理
/api/budgets/**            # 预算管理
/api/budgets/alerts/**     # 预算预警
/api/statistics/**         # 统计报表
/api/tags/**               # 标签管理
```

## 环境

- JDK: JDK 21+
- Maven: Maven 3.8+
- 数据库: MySQL (配置在 `application-local.yml`)
- Node 管理: nvm-windows

## APP 版本更新规范

### 版本号配置

版本号唯一源头：`jz-app/manifest.json`
- `versionName`：语义版本号（如 1.0.4）
- `versionCode`：整数，递增规则 `major * 100 + minor * 10 + patch`，增大即可

两个字段必须同步修改。

### 发布新版流程

1. **修改 manifest.json** 中的 `versionName` 和 `versionCode`
3. **HBuilderX 打包**：发行 → 原生App-云打包，生成 APK/IPA
4. **Admin 端发布更新**：登录 jz-web 管理端 → AdminManage → APP发布管理 → 新建记录，填写与 manifest.json 一致的 versionName 和更大的 versionCode，上传 APK 文件并发布
5. **用户端检测更新**：APP 启动时自动静默检查（`jz-app/utils/upgrade.js`），或"我的"页手动点击"检查更新"
   - 版本比较优先语义版本号（versionName），versionCode 作为辅助
   - Android：下载 APK 后调用系统安装器
   - iOS：跳转下载链接至浏览器

### 注意事项

- `manifest.json` 是版本号唯一来源，不要在其他地方维护版本号
- Admin 发布的 `AppRelease.versionCode` 必须大于当前 APP 的 `versionCode`，否则 APP 检测不到更新
- 如果 OSS 存储中已有相同版本号的旧 APK，需先删除或替换，否则用户下载到的可能是旧包
