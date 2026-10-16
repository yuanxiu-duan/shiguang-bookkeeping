# 拾光账本 系统架构设计文档

| 项目 | 内容 |
|------|------|
| 文档版本 | V1.1 |
| 编写日期 | 2026-07-01 |
| 适用版本 | App v1.0.4 / Web v1.0 |

---

## 1. 系统概述

拾光账本采用**前后端分离 + 多端覆盖**的架构，由三个独立工程组成：

| 工程 | 技术栈 | 端口 | 部署形态 |
|------|--------|------|----------|
| jz-server | Spring Boot 3.2 + JDK 21 | 8070 | 独立服务/Docker |
| jz-app | uni-app (Vue 2) | - | H5/小程序/App |
| jz-web | Vue 3 + Element Plus | 6173（dev）/ 80（nginx） | 静态资源 + Nginx |

---

## 2. 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                          客户端层                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │  uni-app    │  │  uni-app     │  │  uni-app             │  │
│  │  H5 浏览器   │  │  微信小程序  │  │  iOS / Android App  │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘  │
│         │                 │                     │              │
│         └─────────────────┴─────────────────────┘              │
│                            │                                     │
│                   ┌────────▼────────┐                           │
│                   │  jz-web         │                           │
│                   │  Vue 3 管理端   │                           │
│                   └────────┬────────┘                           │
└────────────────────────────┼────────────────────────────────────┘
                             │ HTTPS
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                       接入层（Nginx）                           │
│  - 静态资源托管（jz-web dist）                                   │
│  - 反向代理 /api/* → jz-server:8070                            │
│  - Gzip 压缩、缓存控制                                          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    应用层（jz-server）                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Filter 层                                               │   │
│  │  - CryptoFilter（RSA+AES 加解密）                        │   │
│  │  - JwtAuthenticationFilter（JWT 认证）                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Controller 层（9 个）                                    │   │
│  │  AuthController / AccountBookController /                │   │
│  │  TransactionController / CategoryController /          │   │
│  │  BudgetController / BudgetAlertController /            │   │
│  │  StatisticsController / TagController / AdminController│   │
│  └─────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Service 层（15 个）                                     │   │
│  │  AuthService / EmailService / AccountBookService /       │   │
│  │  AccountBookMemberService / TransactionService /        │   │
│  │  CategoryService / BudgetService / BudgetAlertService / │   │
│  │  StatisticsService / TagService / ClassificationService│   │
│  │  ExcelService / TransactionExportTaskService /         │   │
│  │  AppReleaseService / OssConfigService                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Repository 层（13 个 JPA Repository）                  │   │
│  └─────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌───────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  MySQL 8.0    │  │  SMTP 邮件服务    │  │  OSS 对象存储    │
│  业务主库      │  │  smtp.qq.com:587 │  │  阿里云/腾讯/    │
│               │  │                  │  │  七牛/MinIO/AWS  │
└───────────────┘  └──────────────────┘  └──────────────────┘
```

---

## 3. 技术架构

### 3.1 后端技术栈（jz-server）

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 语言 | Java | 21 (LTS) | 主开发语言 |
| 框架 | Spring Boot | 3.2.0 | 应用框架 |
| 安全 | Spring Security | 6.x | 认证授权 |
| 持久层 | Spring Data JPA | 3.2 | ORM |
| 数据库 | MySQL | 8.0+ | 关系型数据库 |
| 数据库版本管理 | Flyway | 内置 | 数据库迁移 |
| Excel 处理 | EasyExcel | 3.3.3 | 导入导出 |
| 对象存储 | AWS S3 SDK | 2.25.0 | 兼容多 OSS 提供商 |
| JWT | jjwt | 0.12.3 | Token 生成与校验 |
| 邮件 | spring-boot-starter-mail | 内置 | 验证码发送 |
| 校验 | jakarta.validation | 内置 | 参数校验 |
| 工具 | Lombok | 内置 | 简化代码 |

### 3.2 uni-app 端技术栈（jz-app）

| 类别 | 技术 | 用途 |
|------|------|------|
| 框架 | uni-app (Vue 2 Options API) | 跨端框架 |
| 构建 | Vite | 构建工具 |
| HTTP | uni.request 封装 | 网络请求 |
| 加密 | crypto-js | 前端 AES 加密 |
| 日期 | dayjs | 日期处理 |
| 图表 | uCharts | 统计图表 |
| 离线 | 自研 offlineSync.js | 离线记账同步 |

### 3.3 Web 管理端技术栈（jz-web）

| 类别 | 技术 | 用途 |
|------|------|------|
| 框架 | Vue 3 | 前端框架 |
| 语法 | `<script setup>` | 组合式 API |
| UI | Element Plus | 组件库 |
| 构建 | Vite | 构建工具 |
| 路由 | Vue Router 4 | 路由管理 |
| 状态 | Pinia | 状态管理 |
| HTTP | Axios | 网络请求 |
| 加密 | crypto-js + jsencrypt | 前端加解密 |

---

## 4. 模块划分

### 4.1 后端模块结构

```
com.moneykeeper
├── MoneyKeeperApplication.java        # 启动入口
├── config/                            # 配置层
│   ├── SecurityConfig                 # Spring Security 配置
│   ├── JwtAuthenticationFilter        # JWT 过滤器
│   ├── CryptoConfig                   # 加解密配置
│   ├── GlobalExceptionHandler          # 全局异常处理
│   ├── JacksonConfig                  # JSON 序列化配置
│   ├── ClassificationRulesConfig      # 智能分类规则配置
│   └── AdminInitializer               # 管理员初始化
├── controller/                        # 控制器层（9 个）
├── service/                           # 业务服务层（15 个）
├── repository/                        # 数据访问层（13 个）
├── entity/                            # JPA 实体（13 个）
├── dto/                               # 数据传输对象（20+ 个）
├── filter/                            # 过滤器
│   └── CryptoFilter                   # 加解密过滤器
├── util/                              # 工具类
│   ├── JwtUtil                        # JWT 工具
│   ├── AesUtil                        # AES 加解密
│   └── RsaUtil                        # RSA 加解密
└── storage/
    └── S3StorageService               # S3 兼容存储服务
```

### 4.2 分层架构

```
┌─────────────────────────────────────┐
│  Controller 层                       │  接收请求、参数校验、返回响应
│  - @RestController                   │  - 不写业务逻辑
│  - @Valid 校验                       │  - 异常抛 IllegalArgumentException
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│  Service 层                          │  业务逻辑、事务控制
│  - @Service                          │  - @Transactional 事务
│  - 调用 Repository + 其他 Service    │  - 业务规则校验
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│  Repository 层                       │  数据访问
│  - JpaRepository                     │  - 自定义查询方法
│  - @Query                           │  - 分页支持
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│  Entity 层                           │  数据模型
│  - @Entity / @Table                  │  - 关系映射
│  - @Column / @Index                 │  - 索引定义
└─────────────────────────────────────┘
```

### 4.3 前端模块结构

**jz-app**：
```
jz-app/
├── api/index.js              # API 统一封装（必须用 request()）
├── utils/
│   ├── request.js            # HTTP 请求封装（JWT + 加解密 + 401 处理）
│   ├── crypto.js             # 前端加解密
│   ├── storage.js            # 本地存储
│   ├── config.js             # 环境配置
│   ├── dayjs.js              # 日期工具
│   ├── offlineSync.js        # 离线同步
│   └── upgrade.js            # APP 升级
├── components/               # 公共组件
└── pages/                    # 13 个页面
```

**jz-web**：
```
jz-web/src/
├── api/                      # 按模块拆分 API
├── router/                   # 路由配置
├── stores/                   # Pinia 状态管理
├── utils/
│   ├── request.js            # Axios 封装
│   └── crypto.js             # 加解密
└── views/                    # 9 个页面
```

---

## 5. 安全架构

### 5.1 认证流程

```
用户登录
   │
   ▼
[AuthController.login]
   │
   ▼
[AuthService.login]
   │  1. 校验用户名/邮箱+密码或验证码
   │  2. BCrypt 密码比对
   │  3. 生成 JWT Token（24h 有效）
   ▼
返回 { token, user }
   │
   ▼
前端存储 token → 后续请求 Header: Authorization: Bearer <token>
   │
   ▼
[JwtAuthenticationFilter]
   │  1. 从 Header 提取 token
   │  2. JwtUtil.validateToken 校验
   │  3. JwtUtil.getUsernameFromToken 获取用户名
   │  4. 查询 User → 构建 Authentication
   │  5. SecurityContextHolder 设置认证上下文
   ▼
[Controller @AuthenticationPrincipal User user]
```

### 5.2 接口加密（CryptoFilter）

**加密算法组合**：RSA-2048 + AES-256-CBC

**加密流程**：
1. 前端 GET `/api/auth/public-key` 获取 RSA 公钥
2. 前端生成随机 AES 密钥
3. 用 RSA 公钥加密 AES 密钥 → `encKey`
4. 用 AES 密钥加密请求体 → `encData`
5. 发送 `{ encKey, encData }` 到后端
6. 后端 CryptoFilter 用 RSA 私钥解密得到 AES 密钥
7. 用 AES 密钥解密得到原始请求体
8. 业务处理
9. 响应体用 AES 密钥加密 → `{ encData }`
10. 前端用 AES 密钥解密得到原始响应

**排除路径**（精确匹配）：
- `/api/auth/public-key` — 公钥接口
- `/api/transactions/template` — 模板下载
- `/api/transactions/export` — 同步导出
- `/api/transactions/import` — 文件上传
- `/api/admin/app-releases/upload-file` — APP 文件上传
- `/api/admin/app-releases/check-update` — 更新检查

### 5.3 权限控制

| 路径模式 | 权限 |
|----------|------|
| `/api/auth/**` | 公开（无需认证） |
| `/api/transactions/template` | 公开 |
| `/api/admin/app-releases/check-update` | 公开 |
| `/api/admin/**` | ROLE_ADMIN |
| 其他 `/api/**` | 已认证用户 |
| OPTIONS `/**` | 公开（CORS 预检） |

### 5.4 数据安全

| 项目 | 实现方式 |
|------|----------|
| 密码存储 | BCrypt（cost=10） |
| Token | JWT HS256，24h 有效 |
| 接口加密 | RSA+AES 混合加密 |
| CORS | 白名单配置（app.cors.allowed-origins） |
| CSRF | 禁用（无状态 API） |
| Session | STATELESS |
| SQL 注入 | JPA 参数化查询 |
| XSS | 前端框架自动转义 |
| 文件上传 | 限制 500MB |

---

## 6. 关键业务流程

### 6.1 异步导出任务流程

```
用户提交导出任务
   │
   ▼
[POST /api/transactions/export-tasks]
   │
   ▼
[TransactionExportTaskService.submit]
   │  1. 创建任务记录（status=PENDING）
   │  2. 返回任务 ID
   ▼
定时任务（每 60 秒执行一次）
   │
   ▼
[processNextTask]
   │  1. 查询最早的 PENDING 任务
   │  2. status=PROCESSING, startedAt=now
   │  3. 调用 ExcelService 生成 Excel
   │  4. 文件 > 10MB → 压缩 zip
   │  5. status=SUCCESS, completedAt=now
   │  6. 邮件发送附件
   │  7. emailedAt=now
   │  失败：status=FAILED, errorMessage
   ▼
用户查询任务状态
   │
   ▼
[GET /api/transactions/export-tasks]
   返回最近任务列表
```

### 6.2 APP 升级流程

```
App 启动（onLaunch/onShow，延迟 3 秒）
   │
   ▼
[upgrade.js.checkUpdate]
   │  传入 platform, versionCode, versionName
   ▼
[GET /api/admin/app-releases/check-update]
   │
   ▼
[AppReleaseService.checkUpdate]
   │  1. 查询已发布的最新版本
   │  2. 优先比较 versionName 语义版本
   │  3. versionCode 作为辅助
   │  4. 返回 AppUpdateResponse
   ▼
有新版本？
   ├── 是 + 强制更新 → 弹不可取消对话框
   │       Android: 下载 APK → 进度条 → 系统安装器
   │       iOS: 跳转浏览器下载链接
   └── 是 + 可选更新 → 弹可取消对话框
   └── 否 → 静默
```

### 6.3 共享账本邀请流程

```
OWNER 发起邀请
   │
   ▼
[POST /api/account-books/{id}/members/invite]
   │  body: { username }
   ▼
[AccountBookMemberService.invite]
   │  1. 校验操作者是 OWNER
   │  2. 查询被邀请用户
   │  3. 校验未重复邀请
   │  4. 创建成员记录 status=PENDING
   ▼
被邀请人查看待处理邀请
   │
   ▼
[GET /api/account-books/invitations]
   ▼
被邀请人处理
   ├── [PUT /api/account-books/{id}/members/{memberId}/accept] → status=ACCEPTED
   └── [PUT /api/account-books/{id}/members/{memberId}/reject] → status=REJECTED
```

---

## 7. 部署架构

### 7.1 单机部署（开发/测试）

```
┌─────────────────────────────────────┐
│  开发机                              │
│  - MySQL 8.0 (3306)                 │
│  - jz-server (8070)                 │
│  - jz-web dev server (6173)         │
│  - jz-app H5 dev server             │
└─────────────────────────────────────┘
```

### 7.2 Docker 部署（生产）

```
┌─────────────────────────────────────┐
│  Docker Host                        │
│  ┌─────────────────────────────┐    │
│  │ moneykeeper-mysql           │    │
│  │ image: mysql:8.0            │    │
│  │ port: 3307:3306             │    │
│  │ volume: mysql_data          │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ jz-server                   │    │
│  │ build: ./jz-server          │    │
│  │ port: 8070:8070             │    │
│  │ depends: mysql              │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

### 7.3 生产部署（推荐）

```
┌─────────────────────────────────────┐
│  Nginx 反向代理 (80/443)            │
│  - 静态资源：jz-web dist            │
│  - API 代理：/api/* → backend:8070 │
└─────────────────┬───────────────────┘
                  │
   ┌──────────────┴──────────────┐
   ▼                             ▼
┌─────────────┐           ┌─────────────┐
│ jz-server   │           │  MySQL      │
│ :8070       │           │  :3306      │
└─────────────┘           └─────────────┘
```

---

## 8. 配置管理

### 8.1 后端配置文件

| 文件 | 用途 | 激活方式 |
|------|------|----------|
| application.yml | 主配置（公共） | 默认 |
| application-local.yml | 本地开发 | `spring.profiles.active=local` |
| application-dev.yml | 开发环境 | `spring.profiles.active=dev` |
| application-prod.yml | 生产环境 | `spring.profiles.active=prod` |

### 8.2 关键配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| server.port | 服务端口 | 8070 |
| spring.jpa.hibernate.ddl-auto | DDL 模式 | update |
| app.jwt.secret | JWT 密钥 | - |
| app.jwt.expiration | JWT 有效期 | 86400000 (24h) |
| app.export.output-dir | 导出目录 | exports |
| app.export.schedule-delay-ms | 导出任务间隔 | 60000 (60s) |
| app.export.max-tasks-per-run | 单次处理任务数 | 1 |
| app.export.zip-threshold-bytes | zip 压缩阈值 | 10485760 (10MB) |
| app.cors.allowed-origins | CORS 白名单 | - |
| app.admin.username | 初始管理员账号 | admin |

### 8.3 前端环境配置

**jz-app** (`utils/config.js`)：
- API 地址配置（dev/prod）

**jz-web**：
- `.env.development`：开发环境变量
- `.env.production`：生产环境变量

---

## 9. 监控与日志

### 9.1 日志
- 后端：Spring Boot 默认日志（Console）
- 前端：浏览器 Console / 小程序日志

### 9.2 异常处理
- 全局异常处理器：`GlobalExceptionHandler`
- 业务异常：`IllegalArgumentException` → `Result.error`
- 认证异常：401 JSON 响应 `{ code: 401, msg: "未登录或登录已过期" }`

### 9.3 健康检查
- Spring Boot Actuator（如需启用，需添加依赖）
- Docker healthcheck：mysqladmin ping

---

## 10. 扩展性设计

### 10.1 横向扩展
- 后端无状态（JWT），可直接横向扩展
- MySQL 可主从分离

### 10.2 模块扩展
- 新增 Controller 挂 `/api/` 路径
- 新增 Service 自动注入
- 新增 Entity 自动建表（ddl-auto=update）

### 10.3 多端扩展
- uni-app 支持新增平台（如百度/支付宝小程序）
- Web 端可独立扩展管理功能

---

## 11. 相关文档
- [产品需求文档](./PRD_PRODUCT_REQUIREMENTS.md)
- [数据库设计](./DATABASE_DESIGN.md)
- [接口文档](./API_SPECIFICATION.md)
- [部署运维手册](./DEPLOYMENT_GUIDE.md)
