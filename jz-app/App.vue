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

.text-expense {
  color: #f56c6c;
}

.text-income {
  color: #67c23a;
}
</style>
