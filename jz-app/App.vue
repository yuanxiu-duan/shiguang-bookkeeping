<script>
import { storage, TOKEN_KEY } from './utils/storage.js'
import upgradeManager from './utils/upgrade.js'

export default {
  data() {
    return {
      _autoChecked: false // 每次启动周期只静默检查一次
    }
  },
  onLaunch: function() {
    console.log('App Launch')
    this._scheduleUpdateCheck()
  },
  onShow: function() {
    console.log('App Show')
    // 从后台切回时也尝试检查（但同一个启动周期内只执行一次）
    this._scheduleUpdateCheck()
  },
  onHide: function() {
    console.log('App Hide')
  },
  methods: {
    _scheduleUpdateCheck() {
      // 仅在APP-PLUS环境执行
      // #ifdef APP-PLUS
      if (this._autoChecked) return
      this._autoChecked = true
      // 延迟3秒，避免影响启动速度
      setTimeout(() => {
        upgradeManager.checkAndPrompt({ silent: true })
      }, 3000)
      // #endif
    }
  }
}
</script>

<style>
page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  /* 去掉 App/WebView 的点击高亮块，避免手指按下时闪一块灰蓝 */
  -webkit-tap-highlight-color: transparent;
}

input, textarea, .form-input, .note-input, .tag-name-input, .amount, .input {
  box-sizing: border-box;
}

.container {
  padding: 20rpx;
}

.card {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
}

/* ========== 按钮基础样式重置 ==========
 * uni-app 的 button 默认带 ::after 伪元素边框（浅灰细线），
 * 在弹窗里叠加页面自定义边框后会显得"边框不清晰/发虚"。
 * 统一去掉伪元素边框，并给出清晰的按下反馈。
 */
button {
  position: relative;
  box-sizing: border-box;
  border: none;
  overflow: hidden;
  transition: opacity 0.15s ease, transform 0.15s ease;
}

button::after {
  border: none;
}

button:active:not([disabled]) {
  opacity: 0.86;
  transform: scale(0.985);
}

button[disabled] {
  opacity: 0.55;
}

/* ========== 弹窗统一风格 ==========
 * 目标：所有弹窗长得一样、按钮一眼能分清主次
 *   取消 = 浅灰实底 + 灰字（弱）
 *   确认 = 紫色渐变 + 白字（强）
 *   删除 = 红色渐变 + 白字（危险）
 */
.picker-mask,
.popup-mask,
.confirm-mask {
  background: rgba(17, 24, 39, 0.55);
  backdrop-filter: blur(2px);
}

.picker-content,
.popup-content,
.confirm-card {
  border-radius: 32rpx;
  box-shadow: 0 32rpx 80rpx rgba(17, 24, 39, 0.24);
  animation: dialog-pop 0.2s ease-out;
}

.picker-header {
  padding: 40rpx 32rpx 16rpx;
  text-align: center;
  border-bottom: none;
}

.picker-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #1f2937;
}

.picker-footer,
.confirm-footer {
  display: flex;
  gap: 20rpx;
  padding: 12rpx 32rpx 36rpx;
  border-top: none;
  background: transparent;
}

.picker-footer button,
.popup-footer button,
.confirm-footer button {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  padding: 0;
  margin: 0;
  font-size: 30rpx;
  font-weight: 500;
  border-radius: 44rpx;
  text-align: center;
}

/* 取消：浅灰实底 + 灰字，视觉上"弱化" */
.btn-cancel {
  background: #f2f4f7;
  color: #5b6472;
  border: none;
}

.btn-cancel:active {
  background: #e7ebf1;
}

/* 确认：主色渐变 + 白字 + 投影，视觉上"强调" */
.btn-confirm {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  border: none;
  box-shadow: 0 10rpx 26rpx rgba(102, 126, 234, 0.34);
}

.btn-confirm:active {
  box-shadow: 0 6rpx 14rpx rgba(102, 126, 234, 0.28);
}

/* 危险操作（删除）：红色渐变 + 白字，和上面两个都不一样 */
.btn-danger {
  background: linear-gradient(135deg, #ff7a6b 0%, #f5453e 100%);
  color: #ffffff;
  border: none;
  box-shadow: 0 10rpx 26rpx rgba(245, 69, 62, 0.32);
}

.btn-danger:active {
  box-shadow: 0 6rpx 14rpx rgba(245, 69, 62, 0.26);
}

@keyframes dialog-pop {
  from {
    opacity: 0;
    transform: translateY(16rpx) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.text-expense {
  color: #f56c6c;
}

.text-income {
  color: #67c23a;
}
</style>
