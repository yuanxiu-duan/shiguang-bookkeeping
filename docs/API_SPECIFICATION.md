# 拾光账本 接口文档（API Specification）

| 项目 | 内容 |
|------|------|
| 文档版本 | V1.1 |
| 编写日期 | 2026-07-01 |
| Base URL | `http://localhost:8070` |
| 接口前缀 | `/api` |

---

## 0. 通用约定

### 0.1 请求/响应规范
- **Content-Type**：`application/json;charset=UTF-8`（除文件上传/下载外）
- **字符集**：UTF-8
- **认证方式**：HTTP Header `Authorization: Bearer <token>`
- **Token 有效期**：24 小时

### 0.2 统一响应结构

**成功响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": { ... }
}
```

**错误响应**：
```json
{
  "code": 400,
  "msg": "错误描述",
  "data": null
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误/业务异常 |
| 401 | 未登录或登录已过期 |
| 403 | 无权限 |
| 500 | 服务器内部错误 |

### 0.3 接口加密说明

除以下接口外，所有 JSON 接口默认启用 CryptoFilter 加解密：

| 排除路径 | 原因 |
|----------|------|
| `GET /api/auth/public-key` | 公钥接口 |
| `GET /api/transactions/template` | 文件下载 |
| `GET /api/transactions/export` | 文件下载 |
| `POST /api/transactions/import` | 文件上传 |
| `POST /api/admin/app-releases/upload-file` | 文件上传 |
| `GET /api/admin/app-releases/check-update` | 公开检查更新 |

**加密请求格式**：
```json
{
  "encKey": "<RSA 加密的 AES 密钥，Base64>",
  "encData": "<AES 加密的请求体，Base64>"
}
```

**加密响应格式**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "encData": "<AES 加密的响应体，Base64>"
  }
}
```

### 0.4 公共查询参数

| 参数 | 类型 | 说明 |
|------|------|------|
| accountBookId | Long | 当前账本 ID（前端自动传入） |
| page | int | 页码（默认 0） |
| size | int | 每页条数（默认 20） |

---

## 1. 认证模块（AuthController）

### 1.1 用户注册
- **接口**：`POST /api/auth/register`
- **认证**：否
- **描述**：注册成功后自动创建默认账本与默认分类，返回登录信息

**请求体**：
```json
{
  "username": "zhangsan",
  "password": "Abc123456",
  "email": "zhangsan@example.com",
  "emailCode": "123456"
}
```

**响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOi...",
    "user": {
      "id": 1,
      "username": "zhangsan",
      "nickname": null,
      "email": "zhangsan@example.com",
      "createdAt": "2026-06-25 10:00:00"
    }
  }
}
```

**错误**：
| code | msg |
|------|-----|
| 400 | 用户名已存在 |
| 400 | 邮箱已注册 |
| 400 | 验证码错误或已过期 |

---

### 1.2 用户登录
- **接口**：`POST /api/auth/login`
- **认证**：否

**密码登录**：
```json
{
  "username": "zhangsan",
  "password": "Abc123456",
  "loginType": "PASSWORD"
}
```

**验证码登录**：
```json
{
  "email": "zhangsan@example.com",
  "emailCode": "123456",
  "loginType": "CODE"
}
```

**响应**：同注册响应结构

---

### 1.3 发送邮箱验证码
- **接口**：`POST /api/auth/send-code`
- **认证**：否

**请求体**：
```json
{
  "email": "zhangsan@example.com",
  "type": "REGISTER"
}
```

| type 值 | 说明 |
|---------|------|
| REGISTER | 注册 |
| LOGIN | 登录 |
| UPDATE_EMAIL | 修改邮箱 |
| RESET_PASSWORD | 找回密码 |

**响应**：
```json
{
  "code": 200,
  "msg": "验证码已发送，请注意查收",
  "data": null
}
```

**业务规则**：
- 验证码 5 分钟有效
- 同邮箱同类型 60 秒内不可重复发送

---

### 1.4 获取当前用户信息
- **接口**：`GET /api/auth/me`
- **认证**：是

**响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "email": "zhangsan@example.com",
    "createdAt": "2026-06-25 10:00:00"
  }
}
```

---

### 1.5 修改昵称
- **接口**：`PUT /api/auth/nickname`
- **认证**：是

**请求体**：
```json
{
  "nickname": "张三"
}
```

---

### 1.6 修改邮箱
- **接口**：`PUT /api/auth/email`
- **认证**：是

**请求体**：
```json
{
  "oldEmailCode": "123456",
  "newEmail": "new@example.com",
  "newEmailCode": "654321"
}
```

---

### 1.7 修改密码
- **接口**：`PUT /api/auth/password`
- **认证**：是

**请求体**：
```json
{
  "oldPassword": "Abc123456",
  "newPassword": "Xyz654321",
  "confirmPassword": "Xyz654321"
}
```

---

### 1.8 找回密码
- **接口**：`POST /api/auth/reset-password`
- **认证**：否

**请求体**：
```json
{
  "email": "zhangsan@example.com",
  "code": "123456",
  "newPassword": "Xyz654321",
  "confirmPassword": "Xyz654321"
}
```

---

### 1.9 获取 RSA 公钥
- **接口**：`GET /api/auth/public-key`
- **认证**：否
- **加密**：排除（明文返回）

**响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9...\n-----END PUBLIC KEY-----"
}
```

---

## 2. 账本管理（AccountBookController）

### 2.1 获取用户所有账本
- **接口**：`GET /api/account-books`
- **认证**：是

**响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "name": "默认账本",
      "description": null,
      "isDefault": true,
      "createdAt": "2026-06-25 10:00:00",
      "updatedAt": "2026-06-25 10:00:00",
      "alias": "我的别名"
    }
  ]
}
```

---

### 2.2 创建账本
- **接口**：`POST /api/account-books`
- **认证**：是

**请求体**：
```json
{
  "name": "家庭账本",
  "description": "家庭日常开支"
}
```

---

### 2.3 更新账本
- **接口**：`PUT /api/account-books/{id}`
- **认证**：是

**请求体**：同创建账本

---

### 2.4 删除账本
- **接口**：`DELETE /api/account-books/{id}`
- **认证**：是

---

### 2.5 账本成员管理

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查看成员列表 | GET | `/api/account-books/{id}/members` | 仅 OWNER |
| 邀请成员 | POST | `/api/account-books/{id}/members/invite` | 仅 OWNER |
| 接受邀请 | PUT | `/api/account-books/{id}/members/{memberId}/accept` | 被邀请人 |
| 拒绝邀请 | PUT | `/api/account-books/{id}/members/{memberId}/reject` | 被邀请人 |
| 移除成员 | DELETE | `/api/account-books/{id}/members/{memberId}` | 仅 OWNER |
| 退出账本 | POST | `/api/account-books/{id}/leave` | MEMBER |
| 设置别名 | PUT | `/api/account-books/{id}/alias` | 成员本人 |
| 待处理邀请 | GET | `/api/account-books/invitations` | 当前用户 |
| 获取角色 | GET | `/api/account-books/{id}/role` | 当前用户 |

**邀请成员请求体**：
```json
{
  "username": "lisi"
}
```

**成员响应**：
```json
{
  "id": 1,
  "accountBookId": 1,
  "userId": 2,
  "username": "lisi",
  "role": "MEMBER",
  "status": "PENDING",
  "invitedBy": 1,
  "inviterUsername": "zhangsan",
  "alias": null,
  "createdAt": "2026-06-25 10:00:00"
}
```

---

## 3. 交易记录（TransactionController）

### 3.1 条件查询交易
- **接口**：`GET /api/transactions`
- **认证**：是

**查询参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| startDate | LocalDate | 起始日期 yyyy-MM-dd |
| endDate | LocalDate | 结束日期 yyyy-MM-dd |
| month | String | 月份 yyyy-MM（与 startDate/endDate 互斥） |
| year | Integer | 年份 |
| type | String | EXPENSE/INCOME/ALL |
| tagId | Long | 标签 ID |
| accountBookId | Long | 账本 ID |
| page | int | 页码（默认 0） |
| size | int | 每页条数（默认 20） |

**响应**（分页）：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "userId": 1,
        "accountBookId": 1,
        "amount": 50.00,
        "type": "EXPENSE",
        "categoryId": 1,
        "note": "午餐",
        "transactionDate": "2026-06-25",
        "createdAt": "2026-06-25 12:00:00",
        "updatedAt": "2026-06-25 12:00:00",
        "category": {
          "id": 1,
          "name": "餐饮",
          "type": "EXPENSE",
          "icon": "🍽️"
        },
        "tags": [
          { "id": 1, "name": "工作日", "color": "#409EFF" }
        ],
        "createdBy": "zhangsan"
      }
    ],
    "pageable": { ... },
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

### 3.2 创建交易
- **接口**：`POST /api/transactions`
- **认证**：是

**请求体**：
```json
{
  "amount": 50.00,
  "type": "EXPENSE",
  "categoryId": 1,
  "note": "午餐",
  "transactionDate": "2026-06-25",
  "accountBookId": 1
}
```

---

### 3.3 更新交易
- **接口**：`PUT /api/transactions/{id}?accountBookId={bookId}`
- **认证**：是
- **权限**：仅创建者或账本 OWNER

---

### 3.4 删除交易
- **接口**：`DELETE /api/transactions/{id}?accountBookId={bookId}`
- **认证**：是
- **权限**：仅创建者或账本 OWNER

---

### 3.5 月度汇总
- **接口**：`GET /api/transactions/monthly-summary`
- **认证**：是

**查询参数**：month(yyyy-MM) 或 year，accountBookId

**响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "income": 10000.00,
    "expense": 3500.00,
    "balance": 6500.00
  }
}
```

---

### 3.6 近期交易
- **接口**：`GET /api/transactions/recent?days=7&accountBookId={id}`
- **认证**：是

---

### 3.7 智能分类推荐
- **接口**：`POST /api/transactions/classify-suggest?accountBookId={id}`
- **认证**：是

**请求体**：
```json
{
  "note": "星巴克咖啡"
}
```

**响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "matched": true,
    "matchedKeyword": "星巴克",
    "matchedKeywords": ["星巴克"],
    "categoryId": 1,
    "categoryName": "餐饮",
    "categoryType": "EXPENSE",
    "category": {
      "id": 1,
      "name": "餐饮",
      "type": "EXPENSE"
    }
  }
}
```

---

### 3.8 Excel 导入
- **接口**：`POST /api/transactions/import?accountBookId={id}`
- **认证**：是
- **加密**：排除（文件上传）
- **Content-Type**：`multipart/form-data`

**参数**：file (MultipartFile)

**响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "totalRows": 100,
    "successCount": 95,
    "failedCount": 5,
    "errors": ["第 3 行：金额格式错误"]
  }
}
```

---

### 3.9 Excel 同步导出
- **接口**：`GET /api/transactions/export?startDate=&endDate=&accountBookId=`
- **认证**：是
- **加密**：排除（文件下载）
- **响应**：直接返回 Excel 文件流（Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet）

---

### 3.10 下载导入模板
- **接口**：`GET /api/transactions/template`
- **认证**：否
- **加密**：排除（文件下载）

---

### 3.11 提交异步导出任务
- **接口**：`POST /api/transactions/export-tasks`
- **认证**：是

**请求体**：
```json
{
  "startDate": "2026-01-01",
  "endDate": "2026-06-30",
  "email": "zhangsan@example.com",
  "accountBookId": 1
}
```

**响应**：
```json
{
  "code": 200,
  "msg": "导出任务已提交，完成后将发送到您的邮箱",
  "data": {
    "id": 1,
    "userId": 1,
    "email": "zhangsan@example.com",
    "startDate": "2026-01-01",
    "endDate": "2026-06-30",
    "status": "PENDING",
    "createdAt": "2026-06-25 10:00:00"
  }
}
```

---

### 3.12 查询导出任务列表
- **接口**：`GET /api/transactions/export-tasks`
- **认证**：是

**响应**：返回当前用户最近的导出任务列表

| status | 说明 |
|--------|------|
| PENDING | 待处理 |
| PROCESSING | 处理中 |
| SUCCESS | 成功 |
| FAILED | 失败 |

---

## 4. 分类管理（CategoryController）

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 获取全部分类 | GET | `/api/categories` | 按 INCOME/EXPENSE 分组 |
| 按类型获取 | GET | `/api/categories/type/{type}` | type=EXPENSE/INCOME |
| 创建分类 | POST | `/api/categories` | |
| 更新分类 | PUT | `/api/categories/{id}` | |
| 删除分类 | DELETE | `/api/categories/{id}` | |

**创建分类请求体**：
```json
{
  "name": "宠物",
  "type": "EXPENSE",
  "icon": "🐾"
}
```

---

## 5. 预算管理（BudgetController）

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 当月预算 | GET | `/api/budgets/current` | |
| 指定月预算 | GET | `/api/budgets/month/{month}` | month=yyyy-MM |
| 创建预算 | POST | `/api/budgets` | |
| 更新预算 | PUT | `/api/budgets/{id}` | |
| 删除预算 | DELETE | `/api/budgets/{id}` | |

**创建预算请求体**：
```json
{
  "categoryId": 1,
  "month": "2026-06",
  "amountLimit": 2000.00,
  "alertThreshold": 80,
  "accountBookId": 1
}
```

---

## 6. 预算预警（BudgetAlertController）

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 告警列表 | GET | `/api/budgets/alerts?all=false` | 默认未读 |
| 未读数 | GET | `/api/budgets/alerts/count` | 返回 { unreadCount } |
| 标记已读 | PUT | `/api/budgets/alerts/{id}/read` | |
| 全部已读 | PUT | `/api/budgets/alerts/read-all` | |

**未读数响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "unreadCount": 3
  }
}
```

---

## 7. 统计分析（StatisticsController）

### 7.1 月统计
- **接口**：`GET /api/statistics/monthly?month=2026-06&accountBookId={id}`
- **认证**：是

### 7.2 年统计
- **接口**：`GET /api/statistics/yearly?year=2026&accountBookId={id}`
- **认证**：是

### 7.3 曲线数据
- **接口**：`GET /api/statistics/curve?mode=month&month=2026-06&accountBookId={id}`
- **认证**：是
- **mode**：day/week/month

### 7.4 月度趋势
- **接口**：`GET /api/statistics/trend?months=12&accountBookId={id}`
- **认证**：是
- **默认**：近 12 个月

### 7.5 分类趋势
- **接口**：`GET /api/statistics/category-trend/{categoryId}?months=6&accountBookId={id}`
- **认证**：是
- **默认**：近 6 个月

---

## 8. 标签管理（TagController）

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 标签列表 | GET | `/api/tags?keyword=` | 可选关键词模糊搜索 |
| 创建标签 | POST | `/api/tags` | |
| 更新标签 | PUT | `/api/tags/{id}` | |
| 删除标签 | DELETE | `/api/tags/{id}` | |

**创建标签请求体**：
```json
{
  "name": "工作日",
  "color": "#409EFF"
}
```

---

## 9. 后台管理（AdminController）

> 所有 `/api/admin/**` 接口需 `ROLE_ADMIN` 角色，除 `check-update` 外

### 9.1 OSS 配置管理

| 接口 | 方法 | 路径 |
|------|------|------|
| 配置列表 | GET | `/api/admin/oss-configs` |
| 配置详情 | GET | `/api/admin/oss-configs/{id}` |
| 创建配置 | POST | `/api/admin/oss-configs` |
| 更新配置 | PUT | `/api/admin/oss-configs/{id}` |
| 删除配置 | DELETE | `/api/admin/oss-configs/{id}` |
| 激活配置 | PUT | `/api/admin/oss-configs/{id}/activate` |
| 测试连接 | POST | `/api/admin/oss-configs/{id}/test` |

**创建配置请求体**：
```json
{
  "provider": "ALIYUN",
  "name": "阿里云OSS",
  "endpoint": "oss-cn-hangzhou.aliyuncs.com",
  "region": "cn-hangzhou",
  "accessKey": "xxx",
  "secretKey": "xxx",
  "bucketName": "moneykeeper",
  "pathStyleAccess": false,
  "publicDomain": "https://cdn.example.com",
  "prefix": "app/"
}
```

| provider | 说明 |
|----------|------|
| ALIYUN | 阿里云 OSS |
| TENCENT | 腾讯云 COS |
| QINIU | 七牛云 |
| MINIO | MinIO |
| AWS | AWS S3 |

---

### 9.2 APP 发布管理

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 发布列表 | GET | `/api/admin/app-releases?platform=` | |
| 发布详情 | GET | `/api/admin/app-releases/{id}` | |
| 上传 APP 文件 | POST | `/api/admin/app-releases/upload-file` | multipart，排除加密 |
| 创建发布 | POST | `/api/admin/app-releases` | JSON body |
| 发布 | PUT | `/api/admin/app-releases/{id}/publish` | |
| 取消发布 | PUT | `/api/admin/app-releases/{id}/unpublish` | |
| 删除发布 | DELETE | `/api/admin/app-releases/{id}` | |

**上传 APP 文件**：
- Content-Type: multipart/form-data
- 参数：file (MultipartFile), platform (默认 android)
- 响应：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "fileKey": "app/moneykeeper-1.0.3.apk",
    "downloadUrl": "https://cdn.example.com/app/moneykeeper-1.0.3.apk",
    "fileSize": 15728640
  }
}
```

**创建发布请求体**：
```json
{
  "versionName": "1.0.3",
  "versionCode": 103,
  "platform": "android",
  "releaseNotes": "1. 修复已知问题\n2. 优化性能",
  "downloadUrl": "https://cdn.example.com/app/moneykeeper-1.0.3.apk",
  "fileSize": 15728640,
  "fileKey": "app/moneykeeper-1.0.3.apk",
  "isForceUpdate": false
}
```

---

### 9.3 APP 更新检查（公开）
- **接口**：`GET /api/admin/app-releases/check-update?platform=android&versionCode=103&versionName=1.0.3`
- **认证**：否
- **加密**：排除

**响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "hasUpdate": true,
    "versionName": "1.0.4",
    "versionCode": 104,
    "releaseNotes": "1. 新增功能\n2. 修复问题",
    "downloadUrl": "https://cdn.example.com/app/moneykeeper-1.0.4.apk",
    "fileSize": 15728640,
    "isForceUpdate": false
  }
}
```

---

## 10. 错误码说明

| code | HTTP 状态 | 说明 |
|------|-----------|------|
| 200 | 200 | 成功 |
| 400 | 200 | 参数错误/业务异常 |
| 401 | 401 | 未登录或登录已过期 |
| 403 | 200 | 无权限 |
| 500 | 200 | 服务器内部错误 |

---

## 11. 接口清单总览

| 模块 | 接口数 | 认证 |
|------|--------|------|
| 认证 | 9 | 部分 |
| 账本 | 12 | 是 |
| 交易 | 12 | 是 |
| 分类 | 5 | 是 |
| 预算 | 5 | 是 |
| 预算预警 | 4 | 是 |
| 统计 | 5 | 是 |
| 标签 | 4 | 是 |
| 后台管理 | 16 | ADMIN |
| **合计** | **72** | |

---

## 12. 相关文档
- [产品需求文档](./PRD_PRODUCT_REQUIREMENTS.md)
- [数据库设计](./DATABASE_DESIGN.md)
- [系统架构设计](./SYSTEM_ARCHITECTURE.md)
- [测试用例](./TEST_CASES.md)
