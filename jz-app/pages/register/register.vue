<template>
  <view class="register-page">
    <view class="form-card">
      <view class="form-header">
        <text class="title">用户注册</text>
      </view>
      
      <view class="form-item">
        <text class="label">用户名</text>
        <input 
          v-model="form.username" 
          class="input" 
          placeholder="3-50个字符"
          maxlength="50"
        />
      </view>
      
      <view class="form-item">
        <text class="label">密码</text>
        <input 
          v-model="form.password" 
          class="input" 
          type="password" 
          placeholder="至少6个字符"
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
          placeholder="再次输入密码"
          maxlength="100"
          password
        />
      </view>
      
      <view class="form-item">
        <text class="label">邮箱</text>
        <view class="code-row">
          <input 
            v-model="form.email" 
            class="input code-input" 
            placeholder="请输入邮箱"
            maxlength="100"
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
      
      <view class="form-item">
        <text class="label">邮箱验证码</text>
        <input 
          v-model="form.emailCode" 
          class="input" 
          placeholder="请输入邮箱验证码"
          maxlength="6"
        />
      </view>
      
      <button 
        class="btn-primary btn-register" 
        :disabled="loading"
        :loading="loading"
        @click="handleRegister"
      >
        注册
      </button>
      
      <view class="login-link">
        <text @click="goLogin">已有账号？去登录</text>
      </view>
    </view>
  </view>
</template>

<script>
import { authApi } from '../../api/index.js'

export default {
  data() {
    return {
      loading: false,
      codeSending: false,
      codeCountdown: 0,
      codeTimer: null,
      form: {
        username: '',
        password: '',
        confirmPassword: '',
        email: '',
        emailCode: ''
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
        uni.showToast({ title: '请先输入邮箱', icon: 'none' })
        return
      }
      
      // 简单邮箱格式校验
      const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailReg.test(this.form.email)) {
        uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
        return
      }
      
      this.codeSending = true
      try {
        await authApi.sendCode({
          email: this.form.email,
          type: 'REGISTER'
        })
        uni.showToast({ title: '验证码已发送', icon: 'success' })
        // 开始60秒倒计时
        this.codeCountdown = 60
        this.codeTimer = setInterval(() => {
          this.codeCountdown--
          if (this.codeCountdown <= 0) {
            clearInterval(this.codeTimer)
            this.codeTimer = null
          }
        }, 1000)
      } catch (e) {
        console.error('[sendCode] failed:', e)
        const msg = (e && e.message) ? e.message : '发送失败，请稍后重试'
        uni.showToast({ title: msg, icon: 'none' })
      } finally {
        this.codeSending = false
      }
    },
    
    async handleRegister() {
      if (!this.form.username || this.form.username.length < 3) {
        uni.showToast({ title: '用户名至少3个字符', icon: 'none' })
        return
      }
      
      if (!this.form.password || this.form.password.length < 6) {
        uni.showToast({ title: '密码至少6个字符', icon: 'none' })
        return
      }
      
      if (this.form.password !== this.form.confirmPassword) {
        uni.showToast({ title: '两次密码不一致', icon: 'none' })
        return
      }
      
      if (!this.form.email) {
        uni.showToast({ title: '请填写邮箱', icon: 'none' })
        return
      }
      
      if (!this.form.emailCode) {
        uni.showToast({ title: '请填写邮箱验证码', icon: 'none' })
        return
      }
      
      this.loading = true
      try {
        await authApi.register({
          username: this.form.username,
          password: this.form.password,
          email: this.form.email,
          emailCode: this.form.emailCode
        })
        uni.showToast({ title: '注册成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1000)
      } catch (e) {
        console.error('[register] failed:', e)
        const msg = (e && e.message) ? e.message : '注册失败，请稍后重试'
        uni.showModal({
          title: '注册失败',
          content: msg,
          showCancel: false,
          confirmText: '我知道了'
        })
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
.register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40rpx;
  display: flex;
  align-items: center;
}

.form-card {
  width: 100%;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
}

.form-header {
  text-align: center;
  margin-bottom: 40rpx;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
  color: #303133;
}

.form-item {
  margin-bottom: 30rpx;
}

.label {
  font-size: 26rpx;
  color: #606266;
  margin-bottom: 12rpx;
  display: block;
}

.input {
  width: 100%;
  height: 80rpx;
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
  height: 80rpx;
  line-height: 80rpx;
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 44rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
}

.btn-register {
  width: 100%;
  margin-top: 20rpx;
}

.login-link {
  text-align: center;
  margin-top: 30rpx;
}

.login-link text {
  font-size: 26rpx;
  color: #667eea;
}
</style>