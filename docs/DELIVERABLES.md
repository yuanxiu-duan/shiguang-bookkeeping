# 拾光账本 项目交付清单

| 项目 | 内容 |
|------|------|
| 项目名称 | 拾光账本（MoneyKeeper） |
| 交付日期 | 2026-07-01 |
| 当前版本 | App v1.0.4 / Web v1.0 / Server v1.0 |
| 交付方 | - |
| 接收方 | - |

---

## 1. 项目概述

拾光账本是一款面向个人与家庭的轻量级多端记账应用，采用 Spring Boot 3.2 + JDK 21 + uni-app (Vue 2) + Vue 3 全栈技术架构，支持 iOS、Android、H5、微信小程序、Web 管理端五端覆盖。

**核心功能**：用户认证、账本管理、共享账本、交易记录、智能分类、预算管理、预算预警、统计分析、标签管理、数据导入导出、APP 升级、后台管理。

---

## 2. 交付物总览

### 2.1 源代码

| 序号 | 工程 | 路径 | 技术栈 | 说明 |
|------|------|------|--------|------|
| 1 | 后端服务 | `jz-server/` | Spring Boot 3.2 + JDK 21 | 9 个 Controller、15 个 Service、13 个 Entity |
| 2 | uni-app 跨端 | `jz-app/` | uni-app (Vue 2) | 12 个页面，支持 H5/小程序/App |
| 3 | Web 管理端 | `jz-web/` | Vue 3 + Element Plus | 11 个路由页面 |

### 2.2 项目文档

| 序号 | 文档名称 | 路径 | 用途 |
|------|----------|------|------|
| 1 | 项目说明 | `README.md` | 项目简介 |
| 2 | AI 行为指令 | `AGENTS.md` | 开发规范与约束 |
| 3 | 项目结构 | `STRUCTURE.md` | 目录结构说明 |
| 4 | API 速查 | `API.md` | 接口快速参考 |

### 2.3 交付文档（docs/）

| 序号 | 文档名称 | 文件名 | 用途 |
|------|----------|--------|------|
| 1 | 产品需求文档 | `PRD_PRODUCT_REQUIREMENTS.md` | 产品定位、功能清单、用户角色、产品原型说明 |
| 2 | 数据库设计文档 | `DATABASE_DESIGN.md` | 表结构、字段说明、ER 关系、索引设计 |
| 3 | 系统架构设计文档 | `SYSTEM_ARCHITECTURE.md` | 技术架构、模块划分、安全设计、部署架构 |
| 4 | 接口文档 | `API_SPECIFICATION.md` | 72 个 API 详细说明、请求/响应示例 |
| 5 | 测试用例文档 | `TEST_CASES.md` | 93 个测试用例，覆盖功能/接口/安全/兼容性 |
| 6 | 部署运维手册 | `DEPLOYMENT_GUIDE.md` | 环境要求、部署步骤、配置说明、运维操作 |
| 7 | 用户使用手册 | `USER_MANUAL.md` | 终端用户操作指南、常见问题 |
| 8 | 项目交付清单 | `DELIVERABLES.md` | 本文档 |
| 9 | OSS 多提供商兼容说明 | `oss-multi-provider-s3-compatibility.md` | S3 兼容性说明 |
| 10 | 流水编辑与添加人验证 | `流水编辑与添加人验证.md` | 业务规则说明 |

### 2.4 数据库脚本

| 序号 | 脚本 | 路径 | 用途 |
|------|------|------|------|
| 1 | 初始化脚本 | `jz-server/src/main/resources/db/init.sql` | 建库建表 + 默认分类 |
| 2 | Flyway V100 | `jz-server/src/main/resources/db/migration/V100__extend_verification_code_type.sql` | 扩展验证码类型字段 |
| 3 | Flyway V200 | `jz-server/src/main/resources/db/migration/V200__add_admin_and_oss_config.sql` | 新增用户角色、OSS 配置、APP 发布表 |
| 4 | Flyway V300 | `jz-server/src/main/resources/db/migration/V300__create_transaction_export_tasks.sql` | 新增导出任务表 |

### 2.5 配置文件

| 序号 | 文件 | 路径 | 用途 |
|------|------|------|------|
| 1 | 主配置 | `jz-server/src/main/resources/application.yml` | 公共配置 |
| 2 | 本地开发 | `jz-server/src/main/resources/application-local.yml` | 本地数据库 |
| 3 | 开发环境 | `jz-server/src/main/resources/application-dev.yml` | 开发环境 |
| 4 | 生产环境 | `jz-server/src/main/resources/application-prod.yml` | 生产环境 |
| 5 | Web 环境变量 | `jz-web/.env.development` / `.env.production` | 前端环境变量 |
| 6 | App 配置 | `jz-app/utils/config.js` | API 地址配置 |
| 7 | Nginx 配置 | `jz-web/nginx.conf` | 反向代理配置 |

### 2.6 部署相关

| 序号 | 文件 | 路径 | 用途 |
|------|------|------|------|
| 1 | Dockerfile | `jz-server/Dockerfile` | 后端容器镜像构建 |
| 2 | Docker Compose | `docker-compose.yml` | 一键部署 MySQL + 后端 |
| 3 | 启动脚本 | `start-server.bat` | Windows 后端启动脚本 |
| 4 | uni-app 配置 | `jz-app/manifest.json` | App 打包配置 |
| 5 | uni-app 页面路由 | `jz-app/pages.json` | 页面路由配置 |

### 2.7 构建产物（如已构建）

| 序号 | 产物 | 路径 | 说明 |
|------|------|------|------|
| 1 | 后端 jar | `jz-server/target/jz-server-1.0.0.jar` | Maven 打包产物 |
| 2 | Web dist | `jz-web/dist/` | Vite 构建产物 |
| 3 | H5 dist | `jz-app/dist/build/h5/` | uni-app H5 构建 |
| 4 | Android APK | `jz-app/unpackage/release/apk/` | App 打包产物 |

---

## 3. 功能清单

### 3.1 已交付功能

| 模块 | 功能点 | 状态 |
|------|--------|------|
| 用户认证 | 注册、登录（密码/验证码）、找回密码 | 已完成 |
| 用户中心 | 修改昵称、邮箱、密码 | 已完成 |
| 账本管理 | 创建、编辑、删除、默认账本 | 已完成 |
| 共享账本 | 邀请、接受、拒绝、移除、退出、别名 | 已完成 |
| 交易记录 | 记一笔、编辑、删除、筛选、分页 | 已完成 |
| 智能分类 | 关键词匹配自动推荐 | 已完成 |
| 分类管理 | 默认分类、自定义分类 CRUD | 已完成 |
| 标签管理 | 创建、编辑、删除、多标签 | 已完成 |
| 预算管理 | 月度分类预算、阈值告警 | 已完成 |
| 预算预警 | 告警生成、已读标记、未读数 | 已完成 |
| 统计报表 | 月统计、年统计、曲线、趋势 | 已完成 |
| 数据导入 | Excel 批量导入 | 已完成 |
| 数据导出 | 同步导出、异步导出、邮件发送 | 已完成 |
| APP 升级 | 版本检测、强制更新、下载安装 | 已完成 |
| 后台管理 | OSS 配置、APP 发布管理 | 已完成 |
| 离线记账 | App 端离线记账 + 自动同步 | 已完成 |
| 接口加密 | RSA + AES 混合加密 | 已完成 |

### 3.2 接口统计

| 模块 | 接口数 |
|------|--------|
| 认证 | 9 |
| 账本 | 12 |
| 交易 | 12 |
| 分类 | 5 |
| 预算 | 5 |
| 预算预警 | 4 |
| 统计 | 5 |
| 标签 | 4 |
| 后台管理 | 16 |
| **合计** | **72** |

### 3.3 页面统计

| 端 | 页面数 |
|----|--------|
| uni-app | 12 |
| Web 管理端 | 11 |
| **合计** | **23** |

---

## 4. 技术栈清单

### 4.1 后端

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 (LTS) |
| 框架 | Spring Boot | 3.2.0 |
| 安全 | Spring Security | 6.x |
| 持久层 | Spring Data JPA | 3.2 |
| 数据库 | MySQL | 8.0+ |
| 数据库版本管理 | Flyway | 内置 |
| Excel 处理 | EasyExcel | 3.3.3 |
| 对象存储 | AWS S3 SDK | 2.25.0 |
| JWT | jjwt | 0.12.3 |
| 邮件 | spring-boot-starter-mail | 内置 |
| 校验 | jakarta.validation | 内置 |
| 工具 | Lombok | 内置 |

### 4.2 uni-app 端

| 类别 | 技术 |
|------|------|
| 框架 | uni-app (Vue 2 Options API) |
| 构建 | Vite 5 |
| 加密 | crypto-js + jsencrypt |
| 日期 | dayjs |

### 4.3 Web 管理端

| 类别 | 技术 |
|------|------|
| 框架 | Vue 3 |
| UI | Element Plus |
| 构建 | Vite 5 |
| 路由 | Vue Router 4 |
| 状态 | Pinia |
| HTTP | Axios |
| 图表 | ECharts |
| 加密 | crypto-js + jsencrypt |

---

## 5. 数据库清单

| 表名 | 中文名 | 记录数级别 |
|------|--------|------------|
| users | 用户表 | 千级 |
| verification_codes | 验证码表 | 万级 |
| account_books | 账本表 | 千级 |
| account_book_members | 账本成员表 | 千级 |
| categories | 分类表 | 百级 |
| transactions | 交易记录表 | 百万级 |
| transaction_tags | 交易标签关联表 | 百万级 |
| tags | 标签表 | 百级 |
| budgets | 预算表 | 千级 |
| budget_alerts | 预算预警表 | 万级 |
| oss_config | OSS 配置表 | 十级 |
| app_release | APP 发布记录表 | 十级 |
| transaction_export_tasks | 交易导出任务表 | 千级 |

**合计**：13 张表

---

## 6. 测试清单

| 测试类型 | 用例数 |
|----------|--------|
| 功能测试 | 65 |
| 安全测试 | 8 |
| 兼容性测试 | 6 |
| 性能测试 | 4 |
| 接口测试 | 覆盖 72 个接口 |
| **合计** | **93 个用例** |

详见 [测试用例文档](./TEST_CASES.md)。

---

## 7. 验收标准

### 7.1 功能验收
- [ ] 所有 72 个接口可正常调用
- [ ] 所有 23 个页面可正常访问
- [ ] 核心业务流程跑通：注册→登录→记账→统计→导出
- [ ] 共享账本流程跑通：邀请→接受→协同记账
- [ ] 预算预警流程跑通：创建预算→记账→触发预警→查看
- [ ] APP 升级流程跑通：发布新版本→App 检测到→下载安装

### 7.2 性能验收
- [ ] 接口响应时间 P95 < 500ms
- [ ] 10 万条数据下分页查询 < 500ms
- [ ] 100 并发下无数据错乱

### 7.3 安全验收
- [ ] 接口加密生效（CryptoFilter）
- [ ] 密码 BCrypt 存储
- [ ] JWT 24h 自动过期
- [ ] 越权访问被拒绝
- [ ] CORS 白名单生效

### 7.4 兼容性验收
- [ ] Android 5.0+ 正常运行
- [ ] iOS 10.0+ 正常运行
- [ ] Chrome 90+ / Safari 13+ / Edge 80+ 正常访问
- [ ] 微信小程序正常运行

### 7.5 文档验收
- [ ] 产品需求文档完整
- [ ] 数据库设计文档完整
- [ ] 系统架构设计文档完整
- [ ] 接口文档完整
- [ ] 测试用例文档完整
- [ ] 部署运维手册完整
- [ ] 用户使用手册完整

---

## 8. 部署信息

### 8.1 生产环境信息（示例）

| 项目 | 内容 |
|------|------|
| 域名 | https://your-domain.com |
| 后端端口 | 8070 |
| 数据库 | MySQL 8.0 |
| 反向代理 | Nginx |
| 部署方式 | Docker / systemd |

### 8.2 测试账号

| 角色 | 用户名 | 用途 |
|------|--------|------|
| 管理员 | admin | 后台管理 |
| 测试用户 | testuser | 功能测试 |
| 测试用户2 | testuser2 | 共享账本测试 |

> 密码请联系管理员获取。

---

## 9. 后续维护建议

### 9.1 短期优化
- [ ] OSS 配置 secret_key 加密存储
- [ ] 添加 Spring Boot Actuator 监控
- [ ] 添加接口限流
- [ ] 完善日志脱敏

### 9.2 中期扩展
- [ ] 交易记录分表（数据量增长后）
- [ ] 支持更多小程序平台（百度/支付宝）
- [ ] 添加数据可视化大屏
- [ ] 支持多语言

### 9.3 长期规划
- [ ] 微服务化拆分
- [ ] 支持 AI 智能分析
- [ ] 开放 API 平台
- [ ] 多租户支持

---

## 10. 交付确认

| 项目 | 内容 |
|------|------|
| 交付方签字 | - |
| 交付日期 | 2026-07-01 |
| 接收方签字 | - |
| 接收日期 | - |
| 验收意见 | - |

---

## 11. 相关文档

- [产品需求文档](./PRD_PRODUCT_REQUIREMENTS.md)
- [数据库设计](./DATABASE_DESIGN.md)
- [系统架构设计](./SYSTEM_ARCHITECTURE.md)
- [接口文档](./API_SPECIFICATION.md)
- [测试用例](./TEST_CASES.md)
- [部署运维手册](./DEPLOYMENT_GUIDE.md)
- [用户使用手册](./USER_MANUAL.md)
