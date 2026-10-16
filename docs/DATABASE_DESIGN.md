# 拾光账本 数据库设计文档

| 项目 | 内容 |
|------|------|
| 数据库 | MySQL 8.0+ |
| 字符集 | utf8mb4 / utf8mb4_unicode_ci |
| 存储引擎 | InnoDB |
| ORM | Spring Data JPA (Hibernate) |
| 版本管理 | Flyway |
| 文档版本 | V1.1 |
| 编写日期 | 2026-07-01 |

---

## 1. 数据库总览

### 1.1 数据库列表
- **moneykeeper**：业务主库，存储用户、账本、交易、预算、预警、配置等全部业务数据

### 1.2 表清单

| 序号 | 表名 | 中文名 | 记录数级别 | 说明 |
|------|------|--------|------------|------|
| 1 | users | 用户表 | 千级 | 系统用户信息 |
| 2 | verification_codes | 验证码表 | 万级 | 邮箱验证码（5 分钟过期） |
| 3 | account_books | 账本表 | 千级 | 用户账本 |
| 4 | account_book_members | 账本成员表 | 千级 | 共享账本成员关系 |
| 5 | categories | 分类表 | 百级 | 收支分类 |
| 6 | transactions | 交易记录表 | 百万级 | 收支流水 |
| 7 | transaction_tags | 交易标签关联表 | 百万级 | 多对多关系 |
| 8 | tags | 标签表 | 百级 | 用户自定义标签 |
| 9 | budgets | 预算表 | 千级 | 月度分类预算 |
| 10 | budget_alerts | 预算预警表 | 万级 | 预算超阈值告警 |
| 11 | oss_config | OSS 配置表 | 十级 | 对象存储配置 |
| 12 | app_release | APP 发布记录表 | 十级 | APP 版本发布 |
| 13 | transaction_export_tasks | 交易导出任务表 | 千级 | 异步导出任务 |

---

## 2. ER 关系图（文字描述）

```
users (1) ──── (N) transactions
users (1) ──── (N) account_books
users (1) ──── (N) account_book_members
users (1) ──── (N) categories
users (1) ──── (N) tags
users (1) ──── (N) budgets
users (1) ──── (N) budget_alerts
users (1) ──── (N) verification_codes
users (1) ──── (N) transaction_export_tasks

account_books (1) ──── (N) account_book_members
account_books (1) ──── (N) transactions
account_books (1) ──── (N) categories
account_books (1) ──── (N) tags
account_books (1) ──── (N) budgets
account_books (1) ──── (N) budget_alerts

categories (1) ──── (N) transactions
categories (1) ──── (N) budgets
categories (1) ──── (N) budget_alerts

transactions (N) ──── (N) tags   [通过 transaction_tags]
```

---

## 3. 表结构详细设计

### 3.1 users（用户表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 否 | AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) | 否 | - | 用户名（唯一） |
| password | VARCHAR(255) | 否 | - | 密码（BCrypt 哈希） |
| email | VARCHAR(100) | 是 | NULL | 邮箱 |
| nickname | VARCHAR(50) | 是 | NULL | 昵称（显示优先） |
| role | VARCHAR(20) | 否 | 'USER' | 角色：USER/ADMIN |
| created_at | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY (username)

---

### 3.2 verification_codes（验证码表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 否 | AUTO_INCREMENT | 主键 |
| email | VARCHAR(100) | 否 | - | 邮箱 |
| code | VARCHAR(6) | 否 | - | 6 位数字验证码 |
| type | VARCHAR(50) | 否 | - | 类型：REGISTER/LOGIN/UPDATE_EMAIL/UPDATE_EMAIL_OLD_CONFIRM/RESET_PASSWORD |
| expired_at | DATETIME | 否 | - | 过期时间（创建后 5 分钟） |
| used | BOOLEAN | 否 | FALSE | 是否已使用 |
| created_at | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- PRIMARY KEY (id)
- 建议索引：idx_email_type_created (email, type, created_at)

---

### 3.3 account_books（账本表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 否 | AUTO_INCREMENT | 主键 |
| user_id | BIGINT | 否 | - | 创建者 ID |
| name | VARCHAR(50) | 否 | - | 账本名称 |
| description | VARCHAR(255) | 是 | NULL | 描述 |
| is_default | BOOLEAN | 否 | FALSE | 是否默认账本 |
| created_at | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_user_name (user_id, name)
- INDEX idx_user_id (user_id)

---

### 3.4 account_book_members（账本成员表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 否 | AUTO_INCREMENT | 主键 |
| account_book_id | BIGINT | 否 | - | 账本 ID |
| user_id | BIGINT | 否 | - | 成员用户 ID |
| role | VARCHAR(20) | 否 | 'MEMBER' | 角色：OWNER/MEMBER |
| status | VARCHAR(20) | 否 | 'PENDING' | 状态：PENDING/ACCEPTED/REJECTED |
| invited_by | BIGINT | 是 | NULL | 邀请人 ID |
| alias | VARCHAR(50) | 是 | NULL | 在该账本的别名 |
| created_at | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_book_user (account_book_id, user_id)
- INDEX idx_user_status (user_id, status)
- INDEX idx_book_status (account_book_id, status)

**外键**：
- fk_member_book → account_books(id) ON DELETE CASCADE
- fk_member_user → users(id) ON DELETE CASCADE
- fk_member_inviter → users(id) ON DELETE SET NULL

---

### 3.5 categories（分类表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 否 | AUTO_INCREMENT | 主键 |
| user_id | BIGINT | 是 | NULL | 用户 ID（NULL 表示系统默认分类） |
| account_book_id | BIGINT | 是 | NULL | 账本 ID（NULL 表示用户级分类） |
| name | VARCHAR(50) | 否 | - | 分类名称 |
| type | ENUM('EXPENSE','INCOME') | 否 | - | 类型 |
| icon | VARCHAR(20) | 是 | NULL | 图标（emoji） |
| parent_id | BIGINT | 是 | NULL | 父分类 ID（预留层级） |
| sort_order | INT | 否 | 0 | 排序 |
| is_default | BOOLEAN | 否 | FALSE | 是否系统默认 |
| created_at | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- INDEX idx_user_type (user_id, type)
- INDEX idx_parent (parent_id)
- INDEX idx_account_book_id (account_book_id)

**初始数据**：13 条系统默认分类（user_id=NULL），见 init.sql

---

### 3.6 transactions（交易记录表）★ 核心

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 否 | AUTO_INCREMENT | 主键 |
| user_id | BIGINT | 否 | - | 创建者 ID |
| account_book_id | BIGINT | 是 | NULL | 账本 ID |
| amount | DECIMAL(10,2) | 否 | - | 金额 |
| type | ENUM('EXPENSE','INCOME') | 否 | - | 类型 |
| category_id | BIGINT | 否 | - | 分类 ID |
| note | VARCHAR(255) | 是 | NULL | 备注 |
| transaction_date | DATE | 否 | - | 交易日期 |
| created_at | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**（7 个）：
- PRIMARY KEY (id)
- INDEX idx_user_date (user_id, transaction_date)
- INDEX idx_user_category (user_id, category_id)
- INDEX idx_user_type (user_id, type)
- INDEX idx_account_book_id (account_book_id)
- INDEX idx_user_account_book_date (user_id, account_book_id, transaction_date DESC)
- INDEX idx_user_account_book_type (user_id, account_book_id, type)
- INDEX idx_user_account_book_created (user_id, account_book_id, created_at DESC)

**性能说明**：针对高频查询场景"用户在某账本内按时间倒序查交易"做了复合索引优化。

---

### 3.7 transaction_tags（交易-标签关联表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 否 | AUTO_INCREMENT | 主键 |
| transaction_id | BIGINT | 否 | - | 交易 ID |
| tag_id | BIGINT | 否 | - | 标签 ID |

**索引**：
- PRIMARY KEY (id)
- INDEX idx_transaction_id (transaction_id)
- INDEX idx_tag_id (tag_id)
- INDEX idx_transaction_tag_transaction_id (transaction_id)
- INDEX idx_transaction_tag_tag_id (tag_id)

**外键**：
- fk_tt_transaction → transactions(id) ON DELETE CASCADE
- fk_tt_tag → tags(id) ON DELETE CASCADE

---

### 3.8 tags（标签表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 否 | AUTO_INCREMENT | 主键 |
| user_id | BIGINT | 否 | - | 用户 ID |
| account_book_id | BIGINT | 是 | NULL | 账本 ID |
| name | VARCHAR(50) | 否 | - | 标签名称 |
| color | VARCHAR(20) | 否 | '#409EFF' | 颜色 |
| created_at | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_user_name (user_id, name)
- INDEX idx_user_id (user_id)
- INDEX idx_account_book_id (account_book_id)

---

### 3.9 budgets（预算表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 否 | AUTO_INCREMENT | 主键 |
| user_id | BIGINT | 否 | - | 用户 ID |
| account_book_id | BIGINT | 是 | NULL | 账本 ID |
| category_id | BIGINT | 是 | NULL | 分类 ID（NULL 表示总预算） |
| month | VARCHAR(7) | 否 | - | 月份 yyyy-MM |
| amount_limit | DECIMAL(10,2) | 否 | - | 预算限额 |
| alert_threshold | INT | 否 | 80 | 告警阈值百分比 |
| created_at | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_user_month_category (user_id, month, category_id)
- INDEX idx_user_month (user_id, month)
- INDEX idx_account_book_id (account_book_id)

---

### 3.10 budget_alerts（预算预警表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 否 | AUTO_INCREMENT | 主键 |
| user_id | BIGINT | 否 | - | 用户 ID |
| account_book_id | BIGINT | 是 | NULL | 账本 ID |
| category_id | BIGINT | 否 | - | 分类 ID |
| month | VARCHAR(7) | 否 | - | 月份 yyyy-MM |
| alert_date | DATE | 否 | - | 告警日期 |
| is_notified | TINYINT(1) | 否 | 0 | 是否已通知 |
| is_read | TINYINT(1) | 否 | 0 | 是否已读 |
| budget_limit | DECIMAL(10,2) | 否 | - | 预算限额 |
| used_amount | DECIMAL(10,2) | 否 | - | 已使用金额 |
| percentage | INT | 否 | - | 使用百分比 |
| over_amount | DECIMAL(10,2) | 是 | NULL | 超出金额 |
| created_at | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY unique_user_category_month_date (user_id, category_id, month, alert_date)
- INDEX idx_user_id (user_id)
- INDEX idx_user_read (user_id, is_read)
- INDEX idx_account_book_id (account_book_id)

---

### 3.11 oss_config（OSS 配置表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 否 | AUTO_INCREMENT | 主键 |
| provider | VARCHAR(50) | 否 | - | 提供商：ALIYUN/TENCENT/QINIU/MINIO/AWS |
| name | VARCHAR(100) | 否 | - | 配置名称 |
| endpoint | VARCHAR(255) | 否 | - | 服务端点 |
| region | VARCHAR(100) | 是 | 'us-east-1' | 区域 |
| access_key | VARCHAR(255) | 否 | - | AccessKey |
| secret_key | VARCHAR(255) | 否 | - | SecretKey |
| bucket_name | VARCHAR(255) | 否 | - | 存储桶名称 |
| path_style_access | BOOLEAN | 是 | TRUE | 是否路径样式访问 |
| public_domain | VARCHAR(255) | 是 | NULL | 自定义公开访问域名 |
| prefix | VARCHAR(255) | 是 | '' | 存储路径前缀 |
| is_active | BOOLEAN | 是 | FALSE | 是否当前使用 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

---

### 3.12 app_release（APP 发布记录表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 否 | AUTO_INCREMENT | 主键 |
| version_name | VARCHAR(50) | 否 | - | 版本号 如 1.0.0 |
| version_code | INT | 否 | - | 版本编号 如 100 |
| platform | VARCHAR(20) | 否 | 'android' | 平台：android/ios |
| release_notes | TEXT | 是 | NULL | 更新说明 |
| download_url | VARCHAR(500) | 否 | - | 下载链接 |
| file_size | BIGINT | 是 | NULL | 文件大小（字节） |
| file_key | VARCHAR(500) | 是 | NULL | OSS 存储 key |
| is_force_update | BOOLEAN | 是 | FALSE | 是否强制更新 |
| is_published | BOOLEAN | 是 | FALSE | 是否已发布 |
| published_at | DATETIME | 是 | NULL | 发布时间 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

---

### 3.13 transaction_export_tasks（交易导出任务表）

| 字段名 | 类型 | 可空 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 否 | AUTO_INCREMENT | 主键 |
| user_id | BIGINT | 否 | - | 用户 ID |
| account_book_id | BIGINT | 是 | NULL | 账本 ID |
| email | VARCHAR(100) | 否 | - | 接收邮箱 |
| start_date | DATE | 否 | - | 起始日期 |
| end_date | DATE | 否 | - | 结束日期 |
| status | VARCHAR(20) | 否 | 'PENDING' | 状态：PENDING/PROCESSING/SUCCESS/FAILED |
| file_path | VARCHAR(500) | 是 | NULL | 文件路径 |
| file_name | VARCHAR(200) | 是 | NULL | 文件名 |
| error_message | VARCHAR(1000) | 是 | NULL | 错误信息 |
| started_at | DATETIME | 是 | NULL | 开始处理时间 |
| completed_at | DATETIME | 是 | NULL | 完成时间 |
| emailed_at | DATETIME | 是 | NULL | 邮件发送时间 |
| created_at | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- INDEX idx_export_task_status_created (status, created_at)
- INDEX idx_export_task_user_created (user_id, created_at)

---

## 4. 数据库版本管理（Flyway）

| 版本 | 脚本 | 说明 |
|------|------|------|
| 基线 | init.sql | 初始化数据库与默认分类 |
| V100 | V100__extend_verification_code_type.sql | 扩展 verification_codes.type 字段长度 |
| V200 | V200__add_admin_and_oss_config.sql | 新增用户角色字段、OSS 配置表、APP 发布表 |
| V300 | V300__create_transaction_export_tasks.sql | 新增交易导出任务表 |

**配置**（application.yml）：
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: false
    locations: classpath:db/migration
```

---

## 5. 数据初始化

### 5.1 系统默认分类（13 条）
注册用户时自动初始化（user_id=NULL 的系统分类对所有用户可见）：

**支出类（8 条）**：
| 名称 | 图标 | 排序 |
|------|------|------|
| 餐饮 | 🍽️ | 1 |
| 交通 | 🚗 | 2 |
| 日用品 | 🛒 | 3 |
| 娱乐 | 🎮 | 4 |
| 居住 | 🏠 | 5 |
| 医疗 | 💊 | 6 |
| 教育 | 📚 | 7 |
| 其他支出 | 📤 | 99 |

**收入类（5 条）**：
| 名称 | 图标 | 排序 |
|------|------|------|
| 工资 | 💰 | 1 |
| 奖金 | 🎁 | 2 |
| 投资 | 📈 | 3 |
| 兼职 | 💼 | 4 |
| 其他收入 | 📥 | 99 |

### 5.2 管理员账号
启动时通过 `AdminInitializer` 自动初始化，账号配置在 `application.yml`：
```yaml
app:
  admin:
    username: admin
    password: <初始密码>
    email: admin@example.com
```

---

## 6. 数据安全与备份

### 6.1 备份策略
| 类型 | 频率 | 保留期 |
|------|------|--------|
| 全量备份 | 每日 1 次（凌晨低峰期） | 30 天 |
| 增量备份 | 每小时 1 次 | 7 天 |
| Binlog | 实时 | 7 天 |

### 6.2 敏感数据保护
| 字段 | 保护方式 |
|------|----------|
| users.password | BCrypt 加盐哈希（cost=10） |
| oss_config.secret_key | 明文存储（建议后续加密） |
| verification_codes.code | 5 分钟自动过期 |
| JWT Token | 24 小时有效期 |

### 6.3 数据归档
- `transaction_export_tasks` 完成超过 30 天的记录可归档
- `verification_codes` 已使用或过期的记录可定期清理

---

## 7. 性能优化建议

### 7.1 索引优化
- `transactions` 表已针对"用户+账本+时间/类型"建立复合索引
- 高频查询 `getRecentTransactions` 命中 `idx_user_account_book_created`

### 7.2 分页优化
- 列表查询统一使用 Spring Data JPA 分页（Pageable）
- 大数据量导出采用 EasyExcel 流式写入

### 7.3 后续扩展建议
- 交易记录表数据量增长后可按月分表
- 验证码表可设置定时清理任务
- OSS 配置 secret_key 建议加密存储
