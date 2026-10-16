---
name: "uniapp-modal-input-focus"
description: "uni-app 弹窗内 input 无法聚焦/光标的修复规范。当 uniapp 项目里自定义弹窗（picker-mask / modal / popup）中的 input/textarea 在 H5/小程序/App 上无法点击聚焦、光标不出现时调用。"
---

# uni-app 弹窗内输入框不可聚焦修复规范

## 触发场景
- 用户反馈"弹窗里的输入框不能输入"/"点击没有光标"/"键盘弹不出来"
- uniapp 项目的自定义遮罩弹窗（`picker-mask`、`modal-mask`、`popup-overlay` 等）内的 `<input>` / `<textarea>` 失去聚焦能力
- 同一个项目里**有的弹窗能输入、有的不能** → 一定是新弹窗事件绑定 / 样式与"能用的弹窗"不一致

## Root Cause（高频根因排查清单）

按"出现频率"从高到低排：

### 1️⃣ **input 没有显式 `height` + `padding` 用 rpx**（H5 端**头号根因**，绝大多数情况下就是它）
**反面案例**：
```css
.form-input {
  width: 100%;
  padding: 20rpx 24rpx;   /* ❌ 只有 padding，没 height */
  border: 1rpx solid #dcdfe6;
  border-radius: 12rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  background: #fff;
}
```
**症状**：
- input 视觉上看起来正常（有边框、有高度）
- 但点击/触摸时**完全没反应**，光标不出现，键盘不弹起
- 同页面其他位置的 input（如登录页直接放在 page 下的）能正常工作
- 同项目 `tag-name-input`（有 `height: 80rpx`）能正常工作

**原因**：
- uni-app 在 H5 端把 `<input>` 编译为 `<uni-input>` Web Component
- 内部的真实 `<input>` 默认 `height: auto`，依赖外部 `<uni-input>` 给出尺寸
- 当 `.form-input` 类只设 `padding` 不设 `height` 时，外层 `<uni-input>` 的盒模型计算在某些路径下（特别是被 flex/absolute 包裹时）会把内部 input 的可点击区域压缩为 0
- 而 budget.vue 的 `.tag-name-input` 显式设了 `height: 80rpx`，内部 input 有了明确空间，能正常聚焦

**修复（任选其一）**：
```css
/* ✅ 方案A：显式 height + padding 改 0 上下 */
.form-input {
  width: 100%;
  height: 80rpx;
  padding: 0 24rpx;
  ...
}

/* ✅ 方案B：直接用项目中已验证可工作的 input class */
<input class="tag-name-input" ... />
```

### 2️⃣ 在外层 mask 上使用了 `@touchstart.stop` / `@mousedown.stop` / `@touchmove.stop.prevent`
**反面案例**：
```vue
<view v-if="show" class="mask" @click="show=false" @touchmove.stop.prevent>
  <view class="content" @click.stop @touchstart.stop @mousedown.stop>
    <input v-model="x" />   <!-- ❌ 点不出光标 -->
  </view>
</view>
```
**原因**：
- H5/移动端浏览器**必须**让 `touchstart` / `mousedown` 完整冒泡到 input 本身，原生才能聚焦并唤起虚拟键盘
- `.stop` 修饰符阻止冒泡，input 还没收到事件就被外层吃掉
- `@touchmove.stop.prevent` 在 mask 上看似"防止滚动穿透"，实际把整个弹窗区域的触摸事件全废了

**正确写法**：**只用 `@click.stop`**，不要碰 touch/mousedown：
```vue
<view v-if="show" class="mask" @click="show=false">
  <view class="content" @click.stop>
    <input v-model="x" />   <!-- ✅ -->
  </view>
</view>
```

### 3️⃣ input 外层 view 套了 `pointer-events: none` / `user-select: none`
- 检查 input 自身 & 所有祖先 view 的 CSS
- 这些属性会把 input 变成纯展示，无法点击聚焦

### 4️⃣ z-index 错位 / 上面有透明遮罩盖住了 input
- 浏览器开 DevTools，鼠标 hover input 位置，看 Computed 面板里"被谁拦截"
- 弹窗 mask z-index 1000，弹窗 content 应该自然在 mask 内部，无需再设 z-index

### 5️⃣ `disabled` / `readonly` 误绑定
- 检查 `<input>` 是否有上述属性，特别是从父组件透传 `v-bind="$attrs"` 的场景

## 标准排查 SOP（5 步法）

```
1. 找一个"项目里能正常输入"的弹窗（如 budget.vue / accountBooks.vue 里的添加/编辑弹窗）
2. 用 Grep 对比模板：
   - 外层 mask 和内层 content 的事件绑定差异
   - input 的属性差异（class/type/disabled/readonly/v-model）
3. 对比样式：
   - mask / content / input 三层的 CSS
   - 重点看 pointer-events / user-select / z-index / height
4. 把"坏弹窗"改成和"好弹窗"完全一致的写法
5. 删除所有自作聪明的"防御性"事件修饰符和 CSS（.stop / .prevent / user-select / z-index）
```

## 黄金弹窗模板（直接复制可用）

```vue
<template>
  <view v-if="showDialog" class="picker-mask" @click="showDialog = false">
    <view class="picker-content" @click.stop>
      <view class="picker-header">
        <text class="picker-title">{{ title }}</text>
      </view>
      <view class="add-tag-form">
        <view class="form-item">
          <text class="form-label">金额</text>
          <input
            v-model="form.amount"
            type="digit"
            class="tag-name-input"
            placeholder="请输入金额"
          />
        </view>
        <view class="form-item">
          <text class="form-label">备注</text>
          <input
            v-model="form.note"
            class="tag-name-input"
            placeholder="备注（可选）"
            maxlength="100"
            confirm-type="done"
          />
        </view>
      </view>
      <view class="picker-footer">
        <button class="btn-cancel" @click="showDialog = false">取消</button>
        <button class="btn-confirm" @click="handleSave">保存</button>
      </view>
    </view>
  </view>
</template>

<style>
.picker-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 1000;
  display: flex; align-items: center; justify-content: center;
}
.picker-content {
  width: 600rpx;
  max-height: 70vh;
  background: #fff;
  border-radius: 24rpx;
  display: flex; flex-direction: column;
  overflow: hidden;
}
.form-item { margin-bottom: 20rpx; }
.form-label {
  font-size: 26rpx; color: #606266;
  margin-bottom: 12rpx; display: block;
}
.tag-name-input {
  width: 100%;
  height: 80rpx;            /* ⚠️ 必给固定高度，避免 H5 端压缩为 0 */
  padding: 0 20rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}
</style>
```

## Anti-Pattern（绝对禁止）

❌ **`@touchstart.stop` / `@mousedown.stop`** —— 会让弹窗内所有 input 失去聚焦能力。

❌ **`@touchmove.stop.prevent` 写在 mask 上** —— 想防滚动穿透？请用 `overflow: hidden` 锁 body 或 `<page-meta>`，不要拦事件。

❌ **input 上加 `:adjust-position` / `:cursor-spacing` 等附加属性来"治"问题** —— 这些是键盘出现后调整滚动位置的，**根本不影响聚焦**。乱加属性反而是"看着像在治病"的安慰剂。

❌ **input 加 `pointer-events: auto` / `z-index: 1` / `position: relative`** —— 如果项目里"好的弹窗"没加，你加了反而引入新问题。

❌ **不对比"能用的弹窗"就动手** —— 这是耗费最多调试时间的根因。同项目里同类组件**永远先 diff**。

## 验证
1. 改完后保存，等 vite HMR 自动 reload（dev server 日志会出现 `page reload`）
2. 浏览器 / 模拟器打开弹窗
3. 点击金额 input → 光标应立即出现 → 键盘弹出
4. 点击备注 input → 同上
5. 点击弹窗外灰色遮罩 → 弹窗关闭（验证 click 关闭逻辑没被破坏）
