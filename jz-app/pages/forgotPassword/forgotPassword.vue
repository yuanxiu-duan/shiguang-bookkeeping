<template>
  <view class="forgot-page">
    <view class="forgot-header">
      <text class="logo">🔑</text>
      <text class="title">找回密码</text>
      <text class="subtitle">{{ step === 1 ? '请输入注册邮箱获取验证码' : '请设置新密码' }}</text>
    </view>

    <view class="forgot-form">
      <!-- 步骤1: 输入邮箱和验证码 -->
      <template v-if="step === 1">
        <view class="form-item">
          <text class="label">注册邮箱</text>
          <input
            v-model="form.email"
            class="input"
            placeholder="请输入注册时使用的邮箱"
            maxlength="100"
          />
        </view>

        <view class="form-item">
          <text class="label">验证码</text>
          <view class="code-row">
            <input
              v-model="form.code"
              class="input code-input"
              placeholder="请输入验证码"
              maxlength="6"
            />
            <button
              class="btn-code"
              :disabled="codeSending || codeCountdown > 0"
              @click="handleSendCode"
            >
              {{ codeCountdown > 0 ? codeCountdown + '秒' : '获取验证码' }}
            </button>
          </view>
        </view>

        <button
          class="btn-primary"
          :disabled="loading"
          :loading="loading"
          @click="handleVerifyCode"
        >
          下一步
        </button>
      </template>

      <!-- 步骤2: 设置新密码 -->
      <template v-if="step === 2">
        <view class="form-item">
          <text class="label">新密码</text>
          <input
            v-model="form.newPassword"
            class="input"
            type="password"
            placeholder="请输入新密码（至少6位）"
            maxlength="100"
            password
          />
        </view>

        <view class="form-item">
          <text class="label">确认密码</text>
          <input
            v-model="form.confirmPassword"
            class="input"
            type="password"
            placeholder="请再次输入新密码"
            maxlength="100"
            password
          />
        </view>

        <button
          class="btn-primary"
          :disabled="loading"
          :loading="loading"
          @click="handleResetPassword"
        >
          重置密码
        </button>
      </template>

      <view class="back-link">
        <text @click="goLogin">返回登录</text>
      </view>
    </view>
  </view>
</template>

<script>
import { authApi } from '../../api/index.js'

export default {
  data() {
    return {
      step: 1,
      loading: false,
      codeSending: false,
      codeCountdown: 0,
      codeTimer: null,
      form: {
        email: '',
        code: '',
        newPassword: '',
        confirmPassword: ''
      }
    }
  },

  beforeDestroy() {
    if (this.codeTimer) {
      clearInterval(this.codeTimer)
    }
  },

  methods: {
    async handleSendCode() {
      if (!this.form.email) {
        uni.showToast({ title: '请输入邮箱', icon: 'none' })
        return
      }
      const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailReg.test(this.form.email)) {
        uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
        return
      }

      this.codeSending = true
      try {
        await authApi.sendCode({ email: this.form.email, type: 'RESET_PASSWORD' })
        uni.showToast({ title: '验证码已发送', icon: 'success' })
        this.codeCountdown = 60
        this.codeTimer = setInterval(() => {
          this.codeCountdown--
          if (this.codeCountdown <= 0) {
            clearInterval(this.codeTimer)
            this.codeTimer = null
          }
        }, 1000)
      } catch (e) {
        uni.showToast({ title: e.message || '发送失败', icon: 'none' })
      } finally {
        this.codeSending = false
      }
    },

    handleVerifyCode() {
      if (!this.form.email) {
        uni.showToast({ title: '请输入邮箱', icon: 'none' })
        return
      }
      if (!this.form.code) {
        uni.showToast({ title: '请输入验证码', icon: 'none' })
        return
      }
      this.step = 2
    },

    async handleResetPassword() {
      if (!this.form.newPassword) {
        uni.showToast({ title: '请输入新密码', icon: 'none' })
        return
      }
      if (this.form.newPassword.length < 6) {
        uni.showToast({ title: '密码至少6位', icon: 'none' })
        return
      }
      if (this.form.newPassword !== this.form.confirmPassword) {
        uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
        return
      }

      this.loading = true
      try {
        await authApi.resetPassword({
          email: this.form.email,
          code: this.form.code,
          newPassword: this.form.newPassword,
          confirmPassword: this.form.confirmPassword
        })
        uni.showToast({ title: '密码重置成功，请登录', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (e) {
        uni.showToast({ title: e.message || '重置失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },

    goLogin() {
      uni.navigateBack()
    }
  }
}
</script>

<style>
page {
  overflow: hidden;
  height: 100%;
}

.forgot-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 80rpx 40rpx;
}

.forgot-header {
  text-align: center;
  margin-bottom: 80rpx;
}

.logo {
  font-size: 100rpx;
  display: block;
  margin-bottom: 20rpx;
}

.title {
  font-size: 48rpx;
  font-weight: bold;
  color: #fff;
  display: block;
  margin-bottom: 10rpx;
}

.subtitle {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
}

.forgot-form {
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
}

.form-item {
  margin-bottom: 30rpx;
}

.label {
  font-size: 28rpx;
  color: #303133;
  margin-bottom: 12rpx;
  display: block;
}

.input {
  width: 100%;
  height: 88rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.code-row {
  display: flex;
  gap: 16rpx;
}

.code-input {
  flex: 1;
}

.btn-code {
  flex-shrink: 0;
  height: 88rpx;
  line-height: 88rpx;
  padding: 0 24rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 12rpx;
  font-size: 24rpx;
  white-space: nowrap;
}

.btn-code[disabled] {
  opacity: 0.6;
}

.btn-primary {
  width: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 44rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  margin-top: 20rpx;
}

.back-link {
  text-align: center;
  margin-top: 30rpx;
}

.back-link text {
  font-size: 26rpx;
  color: #667eea;
}
</style>
