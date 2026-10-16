# MoneyKeeper - 拾光账本

个人与家庭轻量级多端记账应用，支持 **iOS / Android / H5 / 微信小程序 / Web 管理端** 五端覆盖。

## 技术栈

### 后端 (jz-server)
- Spring Boot 3.2 + JDK 21
- Spring Security + JWT 无状态认证
- Spring Data JPA (Hibernate) + Flyway
- MySQL 8.0
- AWS S3 SDK 2.25（多 OSS 提供商兼容）
- EasyExcel 3.3.3（导入导出）
- spring-boot-starter-mail（邮件发送）

### Web 管理端 (jz-web)
- Vue 3 + `<script setup>` 组合式 API
- Element Plus UI 组件库
- Vite 5 构建工具
- Pinia 状态管理 + Vue Router 4
- Axios + ECharts + dayjs

### UniApp 移动端 (jz-app)
- uni-app (Vue 2 Options API)
- 支持 H5 / 微信小程序 / Android / iOS
- 离线记账同步
- uCharts 统计图表

## 项目结构

```
shiguang-bookkeeping/
├── jz-server/          # Spring Boot 3.2 后端 (端口 8070)
│   ├── controller/     # 9 个控制器
│   ├── service/        # 15 个业务服务
│   ├── repository/     # 13 个 JPA Repository
│   ├── entity/         # 13 个实体类
│   ├── dto/            # 数据传输对象
│   ├── config/         # 安全/加密/异常处理配置
│   ├── filter/         # CryptoFilter 加解密过滤器
│   └── storage/        # S3 兼容对象存储服务
├── jz-web/             # Vue 3 + Element Plus 管理端 (11 个路由页面)
├── jz-app/             # UniApp 跨端 (12 个页面，支持 H5/小程序/App)
├── docs/               # 项目文档
├── docker-compose.yml  # Docker 编排 (MySQL + 后端)
└── start-server.bat    # Windows 后端启动脚本
```

## 环境要求

- JDK 21+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+
- HBuilderX（可选，用于 UniApp 打包）

## 快速启动

### 1. 数据库准备

```sql
CREATE DATABASE IF NOT EXISTS moneykeeper
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 2. 后端启动

```bash
cd jz-server
mvn spring-boot:run -Dspring.profiles.active=local
```

或双击 `start-server.bat`。

后端启动后访问 `http://localhost:8070/api/auth/public-key` 验证。

### 3. Web 端启动

```bash
cd jz-web
npm install
npm run dev
```

访问 `http://localhost:6173`

### 4. UniApp 启动

```bash
cd jz-app
npm install
npm run dev:h5
```

## 核心功能

| 模块 | 功能 |
|------|------|
| 用户认证 | 注册 / 密码登录 / 验证码登录 / 找回密码 |
| 账本管理 | 创建 / 编辑 / 删除 / 默认账本 |
| 共享账本 | 邀请 / 接受 / 拒绝 / 移除 / 退出 / 别名 |
| 交易记录 | 记一笔 / 编辑 / 删除 / 筛选 / 分页 |
| 智能分类 | 关键词匹配自动推荐分类 |
| 分类管理 | 默认分类 / 自定义分类 CRUD |
| 标签管理 | 创建 / 编辑 / 删除 / 多标签 |
| 预算管理 | 月度分类预算 / 阈值告警 |
| 预算预警 | 告警生成 / 已读标记 / 未读数 |
| 统计报表 | 月统计 / 年统计 / 曲线 / 趋势 |
| 数据导入导出 | Excel 导入 / 同步导出 / 异步导出 / 邮件发送 |
| APP 升级 | 版本检测 / 强制更新 / 下载安装 |
| 后台管理 | OSS 配置 / APP 发布管理 |
| 接口加密 | RSA + AES 混合加密 (CryptoFilter) |
| 离线记账 | App 端离线记账 + 自动同步 |

## API 概览

所有接口前缀 `/api`，需认证接口使用 `Authorization: Bearer <token>`。

| 模块 | 路径 | 接口数 |
|------|------|--------|
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

完整接口文档见 [API.md](API.md)。

## 响应格式

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 / 业务异常 |
| 401 | 未登录或 Token 过期 |
| 403 | 无权限 |
| 500 | 服务器内部错误 |

## 相关文档

- [API 接口参考](API.md)
- [项目结构参考](STRUCTURE.md)
- [AI 行为指令](AGENTS.md)
- 详细文档见 [docs/](./docs/) 目录