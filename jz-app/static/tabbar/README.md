# TabBar 图标

本目录用于存放 UniApp 底部导航栏图标文件。

## 图标要求

- 格式：PNG
- 尺寸：建议 81x81px（适配各端）
- 背景：透明

## 需要的图标文件

| 文件名 | 说明 | 颜色建议 |
|--------|------|----------|
| `account.png` | 记账（未选中） | `#999999` 灰色 |
| `account-active.png` | 记账（选中） | `#409EFF` 蓝色 |
| `statistics.png` | 统计（未选中） | `#999999` 灰色 |
| `statistics-active.png` | 统计（选中） | `#409EFF` 蓝色 |
| `budget.png` | 预算（未选中） | `#999999` 灰色 |
| `budget-active.png` | 预算（选中） | `#409EFF` 蓝色 |
| `profile.png` | 我的（未选中） | `#999999` 灰色 |
| `profile-active.png` | 我的（选中） | `#409EFF` 蓝色 |

## 获取图标

可以从以下渠道获取免费图标：

1. **iconfont.cn** - 阿里巴巴矢量图标库
2. **flaticon.com** - 免费矢量图标
3. **iconfinder.com** - 图标搜索
4. **iconpark.bytedance.com** - 字节跳动图标库

## 图标推荐

- 记账：账本、铅笔、加号等图标
- 统计：图表、折线图、饼图等图标
- 预算：钱包、金钱、进度条等图标
- 我的：用户、头像、个人中心等图标

## 注意事项

1. 确保文件名与 `pages.json` 中的配置一致
2. 图标放置在 `uniapp/static/tabbar/` 目录下
3. 建议使用统一风格的图标
4. 添加图标后需要重新编译项目
