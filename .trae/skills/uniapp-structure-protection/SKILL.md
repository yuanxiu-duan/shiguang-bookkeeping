---
name: "uniapp-structure-protection"
description: "uni-app 项目目录结构保护规则。当涉及 uniapp/uni-app 项目（jz-app 等）的目录调整、文件移动、CLI 启动报错（ENOENT manifest.json/pages.json）时必须调用。严禁未确认布局就移动 App.vue/main.js/pages.json/manifest.json 等核心文件。"
---

# uni-app 项目结构保护规则

## 触发场景（MUST 调用）
- 用户提到 uniapp / uni-app / jz-app 项目
- 准备移动、重命名、删除 uniapp 项目下的核心文件或目录
- uniapp CLI 启动报错（`ENOENT: no such file or directory ... manifest.json` / `pages.json` 等）
- 调整 `vite.config.js`、`package.json` 中和 uni-app 相关的脚本配置
- 切换 uniapp 项目的"根目录布局"与"src 子目录布局"

## 核心原则

### 1. 严禁未经确认就移动 uniapp 项目结构
uni-app 项目有两种**合法的**项目布局，必须二选一，**禁止混用**或部分迁移：

| 布局模式 | 入口文件位置 | 触发方式 |
|---------|------------|---------|
| **根目录布局**（uni-app CLI 旧默认 / HBuilderX 兼容） | `App.vue`、`main.js`、`pages.json`、`manifest.json`、`pages/`、`static/` 都在项目根目录 | 启动时设置 `UNI_INPUT_DIR=./`，通常借助 `cross-env` |
| **src 子目录布局**（vite-plugin-uni 新默认） | 上述所有文件统一放在 `src/` 下 | 默认行为，无需额外配置 |

**禁止**：把根目录的部分文件移到 `src/`，又留一份在根目录；或部分目录迁移、部分未迁移——这会导致 CLI 找不到 `manifest.json` 或读取到错误版本，**必然报错**。

### 2. 操作前必做的 4 步检查（Root Cause Analysis）
在动 uniapp 目录之前：
1. **查现状**：`Glob` / `LS` 确认根目录与 `src/` 都有什么；
2. **看脚本**：`Read package.json` 看 `scripts.dev:h5` 是 `uni`（默认 src）还是带 `UNI_INPUT_DIR` 环境变量（根目录）；
3. **看 vite.config**：确认是否有 `root: 'src'` 之类的覆盖；
4. **再决策**：明确目标布局，**整体**迁移，不要"试一下"。

### 3. CLI 启动报错处理矩阵

| 报错信息 | 根因 | 正确修复 |
|---------|------|---------|
| `ENOENT ... src\manifest.json` | CLI 走 src 布局但文件在根目录 | **方案A**（推荐）：保留根目录布局，将 `scripts.dev:h5` 改为 `cross-env UNI_INPUT_DIR=./ uni`，先 `npm i -D cross-env`；**方案B**：整体把 `App.vue/main.js/pages.json/manifest.json/uni.scss/pages/static/utils/api` 全部 `Move-Item` 到 `src/`，**不能漏文件、不能嵌套** |
| `ENOENT ... pages.json` | 同上 | 同上 |
| Move 出现 `src/pages/pages/` 嵌套 | PowerShell `Move-Item` 在目标已存在同名目录时会嵌套 | 移动前先 `Remove-Item` 目标目录，或使用 `Get-ChildItem src\xxx | Move-Item -Destination ..` 平移内容 |

### 4. Windows / PowerShell 注意
- 删除被 dev server 占用的目录前**必须**先停 dev server（`Get-Process node | Stop-Process` 或 `StopCommand` 工具）；
- `Remove-Item -Recurse -Force` 在某些权限下会失败，可改用 `[System.IO.Directory]::Delete($path, $true)`；
- 路径分隔符统一用 `\`，含空格要用双引号；
- 严禁 `cmd /c`，统一 PowerShell；
- 设置环境变量启动 CLI 要用 `cross-env`（PowerShell 不支持 `KEY=VAL cmd` 语法）。

### 5. 安全修改流程（Reliability Strategy）
```
1. 思考：现状 → 目标布局 → 影响面（vite/package.json/dev server）
2. 备份意识：dev server 必须先停
3. 小步执行：先 LS 看一下，再做单一动作（如先改 package.json）
4. 立即验证：每步后 LS / Read 验证结果
5. 启动验证：用 RunCommand 启动 dev server，看输出有无新报错
```

### 6. proxy 与本机 IP
- `vite.config.js` 中 proxy target **不要硬编码 LAN IP**（如 `192.168.1.10`），应使用 `localhost` 或 `127.0.0.1`；
- 模拟器/手机访问 dev server 时，URL 用 dev server 启动后输出的 `Network` 地址（机器 IP 可能 DHCP 变化）；
- 给出 dev server 地址时优先 `localhost:5174`，IP 地址只在需要 LAN 访问时给出。

## Anti-Pattern（绝对禁止）

❌ **盲目移动单个文件**：`Move-Item App.vue src/` 后就以为完成了——其他 `pages.json`、`manifest.json` 等仍在根目录会报错。

❌ **重复嵌套**：执行 `Move-Item pages src\pages` 时如果 `src\pages` 已存在，会变成 `src\pages\pages\`。

❌ **直接删除根目录文件**：不确认 uniapp CLI 实际读哪里就 rm，可能把唯一可用副本删掉。

❌ **修改后不重启 dev server**：`vite.config.js` / `package.json` 的修改对热更新无效，必须重启。

❌ **PowerShell 用 Unix 语法**：`KEY=VAL uni` 在 PowerShell 不生效，必须 `cross-env` 或 `$env:KEY="VAL"; uni`。

## 参考项目结构（jz-app 当前采用：根目录布局 + cross-env）

```
jz-app/
├── App.vue
├── main.js
├── manifest.json
├── pages.json
├── uni.scss
├── index.html
├── package.json          # scripts.dev:h5 = "cross-env UNI_INPUT_DIR=./ uni"
├── vite.config.js        # proxy target = http://localhost:8070
├── api/
├── pages/                # 每个页面一个文件夹
├── static/
└── utils/
```

## 推荐的企业级 uni-app + Vue3 结构（如未来重构可参考）
详见同目录 `references/uniapp-best-practices.md`。
