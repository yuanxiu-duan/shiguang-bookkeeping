# API 接口参考手册

> 供 AI 快速查找接口定义，避免每次扫描全项目 Controller。
> 所有接口前缀 `/api`，请求/响应格式见 `Result<T>` 包装类。
>
> 加密约定：新增 JSON API 默认必须经过 `CryptoFilter` 加解密，前端必须通过统一 `request()` 封装发送。只有文件下载/上传、模板下载、APP更新检查、三方回调等确需明文或流式处理的接口可加入排除列表；排除时必须精确匹配完整路径，禁止前缀匹配导致相似接口被误排除。

---

## AuthController (`/api/auth`)

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录（密码/验证码两种模式） | 否 |
| POST | `/api/auth/register` | 注册（自动创建默认账本和分类） | 否 |
| POST | `/api/auth/send-code` | 发送邮箱验证码 | 否 |
| GET | `/api/auth/me` | 获取当前用户信息 | 是 |
| PUT | `/api/auth/nickname` | 修改昵称 | 是 |
| PUT | `/api/auth/email` | 修改邮箱（需新邮箱验证码确认） | 是 |
| PUT | `/api/auth/password` | 修改密码（需两次一致） | 是 |
| POST | `/api/auth/reset-password` | 找回密码（无需登录） | 否 |
| GET | `/api/auth/public-key` | 获取 RSA 公钥 | 否 |

### DTO

**RegisterRequest**: username, password, email, emailCode
**LoginRequest**: username, password, email, emailCode, loginType(PASSWORD/CODE)
**SendCodeRequest**: email, type(REGISTER/LOGIN/UPDATE_EMAIL/RESET_PASSWORD)
**UserResponse**: id, username, nickname, email, createdAt
**LoginResponse**: token, user(UserResponse)
**UpdateNicknameRequest**: nickname
**UpdateEmailRequest**: newEmail, code
**UpdatePasswordRequest**: newPassword, confirmPassword
**ResetPasswordRequest**: email, code, newPassword, confirmPassword

---

## AccountBookController (`/api/account-books`)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/account-books` | 获取用户所有账本 |
| POST | `/api/account-books` | 创建账本 |
| PUT | `/api/account-books/{id}` | 更新账本 |
| DELETE | `/api/account-books/{id}` | 删除账本 |
| GET | `/api/account-books/{id}/members` | 查看成员列表 |
| POST | `/api/account-books/{id}/members/invite` | 邀请成员 |
| PUT | `/api/account-books/{id}/members/{memberId}/accept` | 接受邀请 |
| PUT | `/api/account-books/{id}/members/{memberId}/reject` | 拒绝邀请 |
| DELETE | `/api/account-books/{id}/members/{memberId}` | 移除成员 |
| POST | `/api/account-books/{id}/leave` | 退出账本 |
| PUT | `/api/account-books/{id}/alias` | 设置当前用户在该账本的别名 |
| GET | `/api/account-books/invitations` | 获取待处理的邀请列表 |
| GET | `/api/account-books/{id}/role` | 获取当前用户在账本中的角色 |

### DTO

**InviteRequest**: username (被邀请人)
**MemberResponse**: id, userId, username, role, status, alias, invitedBy

---

## TransactionController (`/api/transactions`)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/transactions` | 条件/分页查询（支持 startDate/endDate/month/year/type/tagId） |
| GET | `/api/transactions/all` | 获取所有记录（分页，按 type 筛选） |
| GET | `/api/transactions/recent` | 近期记录（默认近 7 天，?days= 调整） |
| GET | `/api/transactions/monthly-summary` | 月度收支汇总 |
| POST | `/api/transactions` | 创建记录 |
| PUT | `/api/transactions/{id}` | 更新记录（仅创建者或账本 OWNER） |
| DELETE | `/api/transactions/{id}` | 删除记录（仅创建者或账本 OWNER） |
| POST | `/api/transactions/classify-suggest` | AI 智能分类（传 note 返回推荐 categoryId） |
| POST | `/api/transactions/import` | Excel 导入（MultipartFile） |
| GET | `/api/transactions/export` | Excel 导出（可选时间范围） |
| GET | `/api/transactions/template` | 下载导入模板（无需认证） |
| POST | `/api/transactions/export-tasks` | 提交异步导出任务（邮件发送） |
| GET | `/api/transactions/export-tasks` | 查询最近的导出任务 |

### DTO

**TransactionRequest**: amount, type(EXPENSE/INCOME), categoryId, note, transactionDate, accountBookId
**TransactionExcelDTO**: 导入导出用的行列映射

---

## CategoryController (`/api/categories`)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/categories` | 获取全部分类（按 INCOME/EXPENSE 分组） |
| GET | `/api/categories/type/{type}` | 按类型获取分类 |
| POST | `/api/categories` | 创建自定义分类 |
| PUT | `/api/categories/{id}` | 更新分类 |
| DELETE | `/api/categories/{id}` | 删除分类 |

---

## BudgetController (`/api/budgets`)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/budgets/current` | 获取当月预算 |
| GET | `/api/budgets/month/{month}` | 获取指定月预算（yyyy-MM） |
| POST | `/api/budgets` | 创建预算 |
| PUT | `/api/budgets/{id}` | 更新预算 |
| DELETE | `/api/budgets/{id}` | 删除预算 |

### DTO

**BudgetRequest**: categoryId, month(yyyy-MM), amountLimit, alertThreshold(默认80), accountBookId

---

## BudgetAlertController (`/api/budgets/alerts`)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/budgets/alerts` | 获取告警列表（默认未读，?all=true 获取全部） |
| GET | `/api/budgets/alerts/count` | 未读告警数（返回 { unreadCount }） |
| PUT | `/api/budgets/alerts/{id}/read` | 标记单条已读 |
| PUT | `/api/budgets/alerts/read-all` | 全部标记已读 |

---

## StatisticsController (`/api/statistics`)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/statistics/monthly` | 月统计 |
| GET | `/api/statistics/yearly` | 年统计 |
| GET | `/api/statistics/curve` | 曲线数据（mode: day/week/month） |
| GET | `/api/statistics/trend` | 月度趋势（默认近12个月） |
| GET | `/api/statistics/category-trend/{categoryId}` | 分类趋势（默认近6个月） |

---

## TagController (`/api/tags`)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/tags` | 搜索/获取标签列表（可选 keyword 模糊搜索） |
| POST | `/api/tags` | 创建标签（name, color） |
| PUT | `/api/tags/{id}` | 更新标签 |
| DELETE | `/api/tags/{id}` | 删除标签 |

---

## AdminController (`/api/admin`)

> 所有接口需 `ROLE_ADMIN` 角色，除 `check-update` 外。

### OSS 配置管理

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/admin/oss-configs` | 配置列表 |
| GET | `/api/admin/oss-configs/{id}` | 配置详情 |
| POST | `/api/admin/oss-configs` | 创建配置 |
| PUT | `/api/admin/oss-configs/{id}` | 更新配置 |
| DELETE | `/api/admin/oss-configs/{id}` | 删除配置 |
| PUT | `/api/admin/oss-configs/{id}/activate` | 激活配置 |
| POST | `/api/admin/oss-configs/{id}/test` | 测试连接 |

### APP 发布管理

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/admin/app-releases` | 发布列表 |
| GET | `/api/admin/app-releases/{id}` | 发布详情 |
| POST | `/api/admin/app-releases/upload-file` | 上传 APP 文件（multipart） |
| POST | `/api/admin/app-releases` | 创建发布记录 |
| PUT | `/api/admin/app-releases/{id}/publish` | 发布 |
| PUT | `/api/admin/app-releases/{id}/unpublish` | 取消发布 |
| DELETE | `/api/admin/app-releases/{id}` | 删除发布 |
| GET | `/api/admin/app-releases/check-update` | APP 更新检查（公开，无需认证） |

### DTO

**OssConfigRequest**: provider, name, endpoint, region, accessKey, secretKey, bucketName, pathStyleAccess, publicDomain, prefix
**AppReleaseRequest**: versionName, versionCode, platform, releaseNotes, downloadUrl, fileSize, fileKey, isForceUpdate

---

## 通用响应结构

**Result<T>** (`com.moneykeeper.dto.Result`):
```json
{ "code": 200, "msg": "success", "data": ... }
```

错误时 code=400/401/500，msg 为错误描述。

---

## 认证方式

请求头: `Authorization: Bearer <token>`

Token 通过登录/注册接口获取，有效期 24 小时。

---

## 公共参数

大部分 GET 接口支持以下公共查询参数：
- `accountBookId` — 当前账本 ID（前端自动从 storage 获取传入）
- `page`, `size` — 分页参数（交易记录相关接口）