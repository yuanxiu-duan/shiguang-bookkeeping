# uni-app + Vue3 项目最佳实践（社区规范摘录）

> 来源：DCloud 官方文档、CSDN/掘金 2025-2026 年企业级实战文章、awesome-cursorrules 社区模板。
> 用途：作为 `uniapp-structure-protection` skill 的扩展知识，在做重大重构 / 新建 uni-app 项目时参考。

## 一、目录命名"黄金总则"

1. **导出 Vue 组件（UI 实体）**：一律用大驼峰 `PascalCase`（如 `GoodsCard.vue`）。
2. **纯 JS/TS 逻辑文件**（工具、接口、状态、混入）：一律用小驼峰 `camelCase`（如 `formatTime.js`、`useUser.js`）。
3. **文件夹、URL 路由、静态资源、分包名**：一律用短横线 `kebab-case`（如 `user-profile/`）。
4. **Vue3 Composable / Hook 文件**：必须以 `use` 开头（`usePagination.js`）。

## 二、两种官方支持的项目布局

### A. 根目录布局（HBuilderX 兼容 / uni-app CLI 旧默认）
```
project-root/
├── App.vue
├── main.js
├── manifest.json
├── pages.json
├── uni.scss
├── index.html
├── pages/
│   ├── index/index.vue
│   └── login/login.vue
├── static/
├── components/
├── api/
├── utils/
└── package.json
```
- 启动：在 `package.json` 中显式设置 `UNI_INPUT_DIR=./`（建议用 `cross-env`）；
- 优点：兼容 HBuilderX 直接打开；旧仓库迁移成本低。

### B. src 子目录布局（vite-plugin-uni 新默认，推荐新项目）
```
project-root/
├── package.json
├── vite.config.ts
├── tsconfig.json
└── src/
    ├── App.vue
    ├── main.ts
    ├── manifest.json
    ├── pages.json
    ├── uni.scss
    ├── pages/
    ├── static/
    ├── components/
    ├── api/
    ├── store/
    ├── hooks/
    └── utils/
```
- 启动：默认 `npm run dev:h5` 即可，CLI 会自动读取 `src/`；
- 优点：与现代 Vite 生态对齐，更易接 TS、ESLint、单测。

> **铁律**：两种布局必须二选一，**不能混用**。CLI 找不到 `manifest.json` 就立刻报 ENOENT。

## 三、企业级目录扩展（按业务域）

```
src/
├── api/                  # 接口请求（按模块拆分）
│   ├── request.ts        # axios/uni.request 封装（拦截器/统一错误）
│   ├── auth.ts
│   └── system/
├── components/
│   ├── common/           # 通用 UI（BaseButton/BaseModal）
│   └── layout/           # 布局组件
├── hooks/                # Composition API（use 前缀）
├── pages/                # 主包页面（kebab-case 目录 + index.vue）
├── subpackages/          # 分包（小程序主包 2MB 限制）
│   ├── order-center/
│   └── user-center/
├── store/                # Pinia
├── types/                # 全局 TS 类型
├── utils/                # 纯工具函数
├── App.vue
├── main.ts
├── manifest.json
├── pages.json
└── uni.scss
```

## 四、小程序分包硬性约束
- 微信小程序：主包 ≤ 2MB，单分包 ≤ 2MB，总和 ≤ 20MB；
- 主包**只放**：Tabbar 页、首页、登录页、全局组件；
- 其他业务**必须**分包；
- 分包路径必须在 `pages.json` 的 `subPackages` 中声明。

## 五、跨端条件编译（uni-app 核心能力）
```js
// #ifdef H5
const baseUrl = '/api'
// #endif
// #ifdef MP-WEIXIN
const baseUrl = 'https://prod.example.com/api'
// #endif
// #ifdef APP-PLUS
const baseUrl = 'https://prod.example.com/api'
// #endif
```
适配规则：
- **路径分隔符**：Windows 下 Vite 别名要用 `path.resolve(__dirname, 'src')`，避免硬编码 `/`；
- **网络请求**：H5 走 vite proxy（`/api`），其它端走完整域名；
- **存储**：统一封装 `storage.js`，不要直接调 `uni.setStorageSync`。

## 六、Vite 配置要点（uni-app 项目）
```js
import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import path from 'path'

export default defineConfig({
  plugins: [uni()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')   // 或 './' 视布局而定
    }
  },
  server: {
    port: 5174,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8070',    // 严禁硬编码 LAN IP
        changeOrigin: true
      }
    }
  }
})
```

## 七、Windows 环境常见坑
| 坑 | 解决 |
|----|------|
| `KEY=VAL cmd` 不生效 | 用 `cross-env` 或 `$env:KEY='VAL'; cmd` |
| `Move-Item` 嵌套目录 | 先 `Remove-Item` 目标，或用 `Get-ChildItem | Move-Item` 平移 |
| `Remove-Item -Recurse -Force` 失败 | 改用 `[System.IO.Directory]::Delete($path, $true)` |
| dev server 占用文件无法删除 | 先 `Stop-Process` 或工具的 `StopCommand` |
| Vite proxy 不通 | 别用 LAN IP，用 `localhost`；模拟器走 dev server 输出的 Network 地址 |

## 八、参考资料
1. DCloud uni-app 官方文档：https://uniapp.dcloud.net.cn/
2. uni_modules 包管理规范：https://uniapp.dcloud.net.cn/plugin/uni_modules.html
3. 企业级目录规范实战（CSDN）：搜索"uni-app Vue3 TypeScript 项目结构最佳实践"
4. awesome-cursorrules：https://github.com/PatrickJS/awesome-cursorrules
5. 按业务域划分的 Vue3 目录规范（掘金）：搜索"Vue3 项目目录结构规范 按业务域划分"
