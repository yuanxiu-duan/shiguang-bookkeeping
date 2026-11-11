<p align="center">
  <img src="jz-app/static/app-icon.jpg" width="120" alt="拾光账本" style="border-radius: 24px" />
</p>

<h1 align="center">拾光账本 · MoneyKeeper</h1>

<p align="center">记录每一刻的光阴 —— 个人与家庭的轻量级多端记账应用</p>

<p align="center">
  <img alt="JDK" src="https://img.shields.io/badge/JDK-21-orange" />
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen" />
  <img alt="Vue" src="https://img.shields.io/badge/Vue-3-42b883" />
  <img alt="uni-app" src="https://img.shields.io/badge/uni--app-Vue2-2b9939" />
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL-8.0-4479A1" />
  <img alt="License" src="https://img.shields.io/badge/License-MIT-yellow" />
</p>

<p align="center">
  <a href="https://gitee.com/dayuanxiu/shiguang-bookkeeping">Gitee</a> ·
  <a href="https://github.com/yuanxiu-duan/shiguang-bookkeeping">GitHub</a> ·
  <a href="CHANGELOG.md">更新日志</a> ·
  <a href="docs/">项目文档</a>
</p>

---

## 这是什么

拾光账本是一套**开箱即用的全栈记账系统**：后端 Spring Boot + MySQL，Web 管理端 Vue 3 + Element Plus，移动端 uni-app（一套代码同时出 H5 / 微信小程序 / Android / iOS）。

它不是「记账 Demo」，而是把记账这件事从白天到夜晚会用到的细节都做了：共享账本、离线记账、智能分类、预算预警、Excel 导入导出、APP 内更新、接口级加密……适合个人自建，也适合拿来做全栈项目的学习样本。

> 名字取自「拾光」——记录每一刻的光阴。

## 功能亮点

- **五端覆盖，一套业务代码**：Web 管理端负责批量补录、统计与后台；uni-app 移动端负责日常随手记。
- **共享账本**：邀请家人 / 伴侣一起记账，支持邀请、接受、拒绝、移除、退出与备注别名，权限按账本归属校验。
- **离线记账**：App 端断网也能记，联网后自动同步，不会因为地铁里没信号而丢一笔。
- **智能分类**：按关键词自动推荐分类，常用消费少点几下。
- **预算与预警**：按月、按分类设置预算，超阈值自动生成预警并支持未读数提醒。
- **统计报表**：月 / 年收支、趋势曲线、分类占比，移动端与 Web 端都有图表。
- **导入导出**：Excel 同步导出、异步导出（完成后邮件通知）、导入自动匹配分类。
- **接口级加密**：RSA + AES 混合加密，前端无感，抓包也看不到明文业务数据。
- **APP 发布后台**：Web 后台管理 OSS 配置与 APP 版本发布，客户端自动检查更新、支持强制更新。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.2 · JDK 21 · Spring Security + JWT · Spring Data JPA (Hibernate) · Flyway · MySQL 8.0 · AWS S3 SDK 2.25 · EasyExcel · spring-boot-starter-mail |
| Web 管理端 | Vue 3（`<script setup>`）· Element Plus · Vite 5 · Pinia · Vue Router 4 · Axios · ECharts · dayjs |
| 移动端 | uni-app（Vue 2 Options API）· H5 / 微信小程序 / Android / iOS · uCharts · 离线队列 |
| 对象存储 | 兼容 S3 协议：阿里云 OSS / 腾讯云 COS / 七牛 / MinIO / Cloudflare R2 / AWS S3 |

## 项目结构

```
shiguang-bookkeeping/
├── jz-server/            # Spring Boot 后端（默认端口 8070）
│   └── src/main/java/com/moneykeeper/
│       ├── controller/   # 9 个控制器
│       ├── service/      # 15 个业务服务
│       ├── repository/   # 13 个 JPA Repository
│       ├── entity/       # 13 个实体类
│       ├── dto/          # 请求 / 响应对象
│       ├── config/       # 安全、加密、异常处理、JWT
│       ├── filter/       # CryptoFilter：请求体加解密
│       └── storage/      # S3 兼容对象存储（上传 / 预签名直传）
├── jz-web/               # Vue 3 + Element Plus 管理端（11 个路由页面）
├── jz-app/               # uni-app 跨端（12 个页面）
├── docs/                 # 需求 / 架构 / 数据库 / 接口 / 部署 / 测试文档
├── scripts/              # 打包与版本号脚本（PowerShell）
├── docker-compose.yml    # MySQL + 后端编排
└── start-server.bat      # Windows 一键启动后端
```

## 快速开始

### 环境要求

| 依赖 | 版本 |
|---|---|
| JDK | 21+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| HBuilderX | 可选，用于 App / 小程序打包 |

### 1. 建库

```sql
CREATE DATABASE IF NOT EXISTS moneykeeper
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 2. 启动后端

```bash
cd jz-server
mvn spring-boot:run -Dspring.profiles.active=local
```

也可以直接双击 `start-server.bat`。启动后访问 `http://localhost:8070/api/auth/public-key`，能拿到公钥就说明起来了。

> 首次启动会自动建表（Flyway）、写入默认分类，并初始化管理员账号：
> 用户名 `admin`，密码取配置项 `app.admin.password`（默认 `change-me-admin-password`）。
> **部署到公网前务必改掉。**

### 3. 启动 Web 管理端

```bash
cd jz-web
npm install
npm run dev
```

访问 `http://localhost:6173`。

### 4. 启动移动端（H5 预览）

```bash
cd jz-app
npm install
npm run dev:h5
```

打包成 App / 小程序请用 HBuilderX 打开 `jz-app` 目录。

## APP 打包与发布

版本号的唯一来源是 `jz-app/manifest.json` 里的 `versionName` + `versionCode`，规则：

```
versionCode = major * 100 + minor * 10 + patch
例：1.1.7 → 117
```

每次发版按顺序走四步：

1. **升版本号**：`scripts/bump-version.ps1` 会自动补丁号 +1 并同步 `versionCode`，预览可用 `-DryRun`。
2. **云打包**：HBuilderX → 发行 → 原生 App 云打包，产出 `.apk` / `.ipa`。
3. **后台发布**：Web 管理端「后台管理 → APP 发布管理」，填版本号与更新说明后上传安装包。
   - 安装包由**浏览器直传对象存储**（后端签发 30 分钟有效的预签名 PUT 地址，密钥不出服务端），不占用服务器带宽。
   - 新接口：`POST /api/admin/app-releases/presign`。
4. **客户端自检**：App 启动时调用 `GET /api/admin/app-releases/check-update`，先比语义版本、再比 `versionCode`，支持强制更新。

## 接口与加密

- 所有接口前缀 `/api`，需登录的接口带 `Authorization: Bearer <token>`。
- 业务请求/响应默认走 `CryptoFilter` 加解密（RSA 交换 AES 密钥 + AES 加密 Body），前端封装已自动处理；
  只有文件上传、版本检查等少数接口在白名单里走明文。

```json
{ "code": 200, "msg": "success", "data": {} }
```

| code | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 参数错误 / 业务异常 |
| 401 | 未登录或 Token 过期 |
| 403 | 无权限 |
| 500 | 服务器内部错误 |

## 核心接口一览

| 模块 | 路径前缀 | 接口数 |
|---|---|---|
| 认证 | `/api/auth/**` | 9 |
| 账本 | `/api/account-books/**` | 12 |
| 交易 | `/api/transactions/**` | 12 |
| 分类 | `/api/categories/**` | 5 |
| 预算 | `/api/budgets/**` | 5 |
| 预算预警 | `/api/budgets/alerts/**` | 4 |
| 统计 | `/api/statistics/**` | 5 |
| 标签 | `/api/tags/**` | 4 |
| 后台管理 | `/api/admin/**` | 16 |
| **合计** | | **72** |

完整字段与示例见 [API.md](API.md) 与 [docs/API_SPECIFICATION.md](docs/API_SPECIFICATION.md)。

## 文档索引

| 文档 | 内容 |
|---|---|
| [产品需求文档](docs/PRD_PRODUCT_REQUIREMENTS.md) | 角色、场景、功能边界 |
| [系统架构设计](docs/SYSTEM_ARCHITECTURE.md) | 分层、认证、加密链路、部署形态 |
| [数据库设计](docs/DATABASE_DESIGN.md) | 表结构与索引说明 |
| [接口文档](docs/API_SPECIFICATION.md) | 72 个接口的请求 / 响应 |
| [部署运维手册](docs/DEPLOYMENT_GUIDE.md) | 服务器、Nginx、容器化部署 |
| [测试用例](docs/TEST_CASES.md) | 功能与异常用例清单 |
| [用户手册](docs/USER_MANUAL.md) | 面向最终用户的使用说明 |
| [多 OSS 兼容踩坑](docs/oss-multi-provider-s3-compatibility.md) | 为什么 S3 SDK 不能开箱即用兼容所有厂商 |
| [EdgeOne 大文件 524 排查](docs/BLOG_EDGEONE_524_UPLOAD_TIMEOUT.md) | CDN 上传超时的定位与解决 |
| [项目结构参考](STRUCTURE.md) | 目录级说明 |

## 常见问题

**Q：后端端口被占用怎么办？**
改 `jz-server/src/main/resources/application*.yml` 的 `server.port`，同步改 Web 端 Vite 代理即可。

**Q：前端请求报 401 / 解密失败？**
先确认后端 `/api/auth/public-key` 可访问；再确认前后端使用的密钥对一致（后端启动日志会打印指纹）。

**Q：可以只部署 Web 端不部署 App 吗？**
可以，三端相互独立，Web 端缺失后端只是无法登录。

**Q：对象存储必须用云厂商吗？**
不用，MinIO 也能跑，后台管理里填 endpoint / accessKey / bucket 即可。

**Q：为什么移动端用 Vue 2？**
uni-app 的 Vue 3 支持尚不稳定，项目选择保守的 Options API，兼容性优先。

## 参与贡献

1. Fork 本仓库并新建分支：`feat/xxx`、`fix/xxx`
2. 提交前跑通：后端 `mvn -DskipTests package`，Web `npm run build`，App `npm run build:h5`
3. 提交 PR，说明改动动机与验证方式

涉及数据库结构变更请附 Flyway 迁移脚本；新增接口请同步更新 `API.md` 与 `CHANGELOG.md`。

## 许可证

[MIT](LICENSE) © 2026 yuanxiu-duan
