# 迭代记录

## v2.0 用户设置与找回密码 (2026-06-12)
- User 新增 `nickname` 字段；前端显示优先 nickname || username
- 新增: `/api/auth/nickname`(PUT), `/api/auth/email`(PUT), `/api/auth/password`(PUT), `/api/auth/reset-password`(POST)
- 新增验证码类型: `UPDATE_EMAIL`, `RESET_PASSWORD`
- 新增页面: [settings.vue](jz-app/pages/settings/settings.vue), [forgotPassword.vue](jz-app/pages/forgotPassword/forgotPassword.vue)

## v3.0 Admin管理与APP发布 (2026-06-12)
- User 新增 `role` 字段 (USER/ADMIN)；启动时自动初始化admin账号 (配置在yml)
- 新增实体: `OssConfig`(OSS存储配置), `AppRelease`(APP发布记录)
- 新增OSS存储服务: `S3StorageService` (AWS S3 SDK, 兼容阿里云/腾讯云/七牛/MinIO)
- OSS配置存储在数据库，通过Vue管理页面动态选择提供商和配置
- 新增Admin API (需ADMIN角色):
  - `/api/admin/oss-configs` CRUD + 激活/测试连接
  - `/api/admin/app-releases` CRUD + 发布/取消发布 + 文件上传
- 新增公开API: `/api/admin/app-releases/check-update` (APP更新检查，无需认证)
- 新增页面: [AdminManage.vue](jz-web/src/views/AdminManage.vue) (OSS配置+APP发布管理)
- JwtAuthenticationFilter 支持动态角色 (ROLE_ADMIN/ROLE_USER)
- SecurityConfig: `/api/admin/**` 需ADMIN角色, check-update公开访问

## v3.1 APP升级功能 (2026-06-15)
- uni-app 新增 APP 升级检测与安装模块: [upgrade.js](jz-app/utils/upgrade.js)
  - 启动时自动静默检查更新（App.vue onLaunch/onShow，延迟3秒）
  - "我的"页面新增"检查更新"菜单入口（仅 APP-PLUS 显示）
  - 支持强制更新（不可取消）和可选更新
  - Android: 下载APK → 进度显示 → 调用系统安装器
  - iOS: 跳转下载链接至浏览器
  - 所有升级版本信息均来源于 Admin 发布的 `AppRelease`（isPublished=true）
- 新增 API: `appUpdateApi.checkUpdate(platform, versionCode, versionName)` 调用公开端点 `/api/admin/app-releases/check-update`（使用 `request()` 统一封装，`silent: true`）
- profile.vue 关于弹窗显示动态版本号 + 平台信息
- 版本比较逻辑修复: `AppReleaseService.checkUpdate` 改为优先比较 versionName 语义版本号，versionCode 作为辅助（防止 Admin 填写的 versionCode 与 APP manifest.json 不一致导致误判）
- AdminManage.vue 版本编号输入增加提示："版本编号必须大于APP当前版本编号(N)，否则APP检测不到更新"

## v3.2 账单流水异步导出 (2026-06-22)
- 新增 API: `POST /api/transactions/export-tasks` 提交账单流水导出任务，`GET /api/transactions/export-tasks` 查询最近任务
- 服务端定时串行处理 `transaction_export_tasks`，生成 Excel 到服务器目录，文件较大时压缩为 zip，并作为邮件附件发送到用户绑定邮箱
- uni-app 导出页改为提交导出任务，不再在手机端直接下载/openDocument

## v3.3 用户头像 + 报表修复 (2026-07-06)
- User 新增 `avatar`(URL) 和 `avatarKey`(OSS fullKey) 字段；UserResponse 新增 `avatar` 字段
- 新增 API: `POST /api/auth/avatar` 上传头像（multipart，已排除 CryptoFilter），存储到 OSS `avatars/` 二级目录
- 头像上传限制：仅图片格式，最大 5MB；上传新头像时 best-effort 删除旧文件
- jz-web: Settings.vue 新增"修改头像"tab + 顶部头像展示；Dashboard.vue 顶栏头像同步显示
- uni-app: profile.vue 头像可点击上传（chooseImage + uploadFile），有头像显示图片，无头像显示首字母
- jz-web 报表修复: 年度统计柱状图/饼状图样式问题（setOption(option,true) + resize()）；收支颜色统一为 支出红色(#f56c6c) / 收入蓝色(#409eff)
- uni-app 统计页: 收支曲线收入线条颜色 绿→蓝(#409eff)，summary-card/legend-dot 同步
- uni-app 统计页: 支出分类模块取消固定高度（删除 max-height:280rpx + overflow-y:auto），右侧 legend 自适应高度
- Transaction 实体新增 `createdByAvatar` transient 字段；TransactionService.enrichWithCreatedBy 同步填充用户头像
- jz-web BillFlow.vue: 添加人列显示头像 + 用户名（el-avatar + 文本）
- uni-app index.vue: 账单列表添加人显示头像 + 用户名（小圆头像 + 胶囊样式）

## v3.4 默认分类重构 + 账户字段 (2026-07-06)
- **默认分类清单调整**（CategoryService.java）：
  - 支出（16项）：吃喝/交通/孩子/服饰鞋包/化妆护肤/日用品/发红包/人情往来/话费/娱乐/学习/医疗/水电燃气房租物业/快递/保险/其他支出
  - 收入（7项）：工资/投资/奖金/兼职/收红包/父母资助/其他（"其他收入"改名为"其他"）
  - 移除旧分类：餐饮(改名吃喝)、居住、教育、外卖、红包(改名收红包)
- **默认分类允许删除**：CategoryService.deleteCategory 移除"默认分类不能删除"限制，新增 findFallbackCategory 兜底逻辑（同账本同类型其他默认分类优先），删除当前账本默认分类不影响其他账本
- **Transaction 新增 account 字段**（String, length=30）：微信钱包/支付宝/银行卡/现金
  - TransactionRequest DTO 同步新增 account 字段
  - TransactionService createTransaction/updateTransaction 同步设置 account
  - TransactionExcelDTO 新增第7列"账户"（index=6），ExcelService 导入/导出/模板均支持
- **jz-web 前端**：
  - Accounting.vue: 添加表单新增"账户"el-select（clearable）
  - BillFlow.vue: 表格新增"账户"列(el-tag) + 筛选区新增"账户"el-select + 编辑弹窗新增账户字段
- **uni-app 前端**：
  - addTransaction.vue: 新增"账户"选择行 + picker-mask/picker-content 弹窗（4选项+清空）
  - index.vue: 账单列表新增账户胶囊标签（绿色 #67c23a，与添加人胶囊同风格）
- **数据迁移产物**（不入库，.gitignore 已排除）：
  - `工作簿1_新分类.xlsx`：基于工作簿1_原始备份转换，531行，7列（交易日期/类型/金额/分类名称/备注/创建人/账户），餐饮→吃喝，金额取绝对值
  - `reset_categories.sql`：清理4个现有账本旧默认分类 + 插入新默认分类（每账本 16支出+7收入=23分类）

## v3.5 统计页日历视图 (2026-07-20)
- uni-app 统计页 [statistics.vue](jz-app/pages/statistics/statistics.vue) 在收支曲线上方新增日历区块，三种模式：
  - **按月**：月度日历网格（周日～周六 7列），默认当月，复用顶部 ◀ ▶ 按月切换；每个日期格子展示当日支出(红)/收入(蓝)汇总，金额超 1k/1w 自动缩写，今日高亮
  - **按年**：12 月网格（4行×3列），默认当前年度，复用顶部 ◀ ▶ 按年切换；每个格子展示当月支出/收入汇总
  - **全部**：年度汇总列表，展示已有数据每年的收入/支出/结余
- 数据复用现有后端接口返回的 `dailyTrend`(月度)、`monthlyTrend`(年度)、`curveData`(全部)，无需后端改动
- 点击任意日/月/年格子跳转到流水列表页 `transactionList?filters=`，按对应日期范围筛选（startDate+endDate），与现有 `onCategoryClick` 同模式
- 新增 `formatFull(value)` 工具方法：完整展示金额（不缩写），去除小数尾部 0（120.50→120.5、5000.00→5000），整数部分加千分位分隔符（100000→100,000），支持负数（结余为负时）
- 字号按可用宽度分级：日历日格子 16rpx、年度月格子 20rpx、全部年度行 24rpx；日格子 padding 收窄、min-height 增至 100rpx 以完整展示数字
- 加载中显示骨架屏（calendar-skeleton），无数据显示"暂无数据"