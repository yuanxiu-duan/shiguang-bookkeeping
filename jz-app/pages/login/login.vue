<template>
  <view class="login-page">
    <view class="login-header">
      <text class="logo">💰</text>
      <text class="title">拾光账本</text>
      <text class="subtitle">轻松管理您的财务</text>
    </view>
    
    <view class="login-form">
      <!-- 登录方式切换 -->
      <view class="tab-row">
        <view 
          class="tab-item" 
          :class="{ active: loginMode === 'password' }"
          @click="loginMode = 'password'"
        >密码登录</view>
        <view 
          class="tab-item" 
          :class="{ active: loginMode === 'code' }"
          @click="loginMode = 'code'"
        >验证码登录</view>
      </view>
      
      <!-- 密码登录 -->
      <template v-if="loginMode === 'password'">
        <view class="form-item">
          <text class="label">账号 / 邮箱</text>
          <input 
            v-model="form.username" 
            class="input" 
            placeholder="请输入用户名或邮箱"
            maxlength="50"
          />
        </view>
        
        <view class="form-item">
          <text class="label">密码</text>
          <input 
            v-model="form.password" 
            class="input" 
            type="password" 
            placeholder="请输入密码"
            maxlength="100"
            password
          />
        </view>
        
        <button 
          class="btn-primary btn-login" 
          :disabled="loading"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </button>
        
        <view class="remember-row" @click="remember = !remember">
          <view class="checkbox-wrap">
            <view class="checkbox" :class="{ checked: remember }">{{ remember ? '✓' : '' }}</view>
            <text class="checkbox-label">记住账号密码</text>
          </view>
        </view>
      </template>
      
      <!-- 验证码登录 -->
      <template v-if="loginMode === 'code'">
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
          <text class="label">验证码</text>
          <input 
            v-model="form.emailCode" 
            class="input" 
            placeholder="请输入验证码"
            maxlength="6"
          />
        </view>
        
        <button 
          class="btn-primary btn-login" 
          :disabled="loading"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </button>
      </template>
      
      <view class="register-link">
        <text @click="goRegister">还没有账号？去注册</text>
      </view>
      <view class="forgot-link">
        <text @click="goForgotPassword">忘记密码？</text>
      </view>
    </view>
  </view>
</template>

<script>
import { accountBookApi, authApi } from '../../api/index.js';
import { setToken } from '../../utils/request.js';
import { storage } from '../../utils/storage.js';

export default {
  data() {
    return {
      loading: false,
      codeSending: false,
      codeCountdown: 0,
      codeTimer: null,
      remember: false,
      loginMode: 'password',
      form: {
        username: '',
        password: '',
        email: '',
        emailCode: ''
      }
    }
  },
  
  onLoad() {
    const token = storage.get('token')
    if (token) {
      uni.switchTab({ url: '/pages/index/index' })
      return
    }
    // 读取记住的账号密码，只填充不自动登录
    this.remember = storage.get('remember_login', false)
    if (this.remember) {
      this.form.username = storage.get('saved_username', '')
      this.form.password = storage.get('saved_password', '')
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
      
      const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailReg.test(this.form.email)) {
        uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
        return
      }
      
      this.codeSending = true
      try {
        await authApi.sendCode({
          email: this.form.email,
          type: 'LOGIN'
        })
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
        console.error('[sendCode] failed:', e)
        const msg = (e && e.message) ? e.message : '发送失败，请稍后重试'
        uni.showToast({ title: msg, icon: 'none' })
      } finally {
        this.codeSending = false
      }
    },
    
    async handleLogin() {
      if (this.loginMode === 'password') {
        if (!this.form.username || !this.form.password) {
          uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
          return
        }
      } else {
        if (!this.form.email) {
          uni.showToast({ title: '请输入邮箱', icon: 'none' })
          return
        }
        if (!this.form.emailCode) {
          uni.showToast({ title: '请输入验证码', icon: 'none' })
          return
        }
      }
      
      this.loading = true
      try {
        let loginData = {}
        if (this.loginMode === 'password') {
          const account = this.form.username
          if (account.includes('@')) {
            loginData = { email: account, password: this.form.password, loginType: 'PASSWORD' }
          } else {
            loginData = { username: account, password: this.form.password, loginType: 'PASSWORD' }
          }
        } else {
          loginData = {
            email: this.form.email,
            emailCode: this.form.emailCode,
            loginType: 'CODE'
          }
        }
        
        const result = await authApi.login(loginData)
        setToken(result.token)
        storage.set('user_info', result.user)
        
        try {
          const books = await accountBookApi.getList()
          if (books && books.length > 0) {
            const savedBookId = storage.get(`currentAccountBookId_${result.user.id}`)
            const savedBook = savedBookId ? books.find(b => String(b.id) === String(savedBookId)) : null
            const currentBook = savedBook || books.find(b => b.isDefault) || books[0]
            storage.set('currentAccountBookId', currentBook.id)
            storage.set(`currentAccountBookId_${result.user.id}`, currentBook.id)
          }
        } catch (e) {
          console.error('获取账本列表失败:', e)
        }
        
        uni.showToast({ title: '登录成功', icon: 'success' })
        
        // 记住账号密码（仅密码登录模式）
        if (this.loginMode === 'password' && this.remember) {
          storage.set('remember_login', true)
          storage.set('saved_username', this.form.username)
          storage.set('saved_password', this.form.password)
        } else {
          storage.remove('remember_login')
          storage.remove('saved_username')
          storage.remove('saved_password')
        }
        
        setTimeout(() => {
          uni.switchTab({ url: '/pages/index/index' })
        }, 1000)
      } catch (e) {
        console.error('[login] failed:', e)
        const msg = (e && e.message) ? e.message : '登录失败，请检查账号密码或网络'
        uni.showModal({
          title: '登录失败',
          content: msg,
          showCancel: false,
          confirmText: '我知道了'
        })
      } finally {
        this.loading = false
      }
    },
    
    goRegister() {
      uni.navigateTo({ url: '/pages/register/register' })
    },

    goForgotPassword() {
      uni.navigateTo({ url: '/pages/forgotPassword/forgotPassword' })
    }
  }
}
</script>

<style>
page {
  overflow: hidden;
  height: 100%;
}

.login-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 80rpx 40rpx;
}

.login-header {
  text-align: center;
  margin-bottom: 80rpx;
}

.logo {
  font-size: 120rpx;
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

.login-form {
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
}

.tab-row {
  display: flex;
  margin-bottom: 30rpx;
  border-bottom: 2rpx solid #eee;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  font-size: 28rpx;
  color: #909399;
  position: relative;
}

.tab-item.active {
  color: #667eea;
  font-weight: bold;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -2rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 4rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2rpx;
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 44rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
}

.btn-login {
  width: 100%;
  margin-top: 20rpx;
}

.remember-row {
  margin-top: 20rpx;
}

.checkbox-wrap {
  display: flex;
  align-items: center;
}

.checkbox {
  width: 36rpx;
  height: 36rpx;
  border: 2rpx solid #dcdfe6;
  border-radius: 6rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: #fff;
  margin-right: 12rpx;
  flex-shrink: 0;
}

.checkbox.checked {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
}

.checkbox-label {
  font-size: 26rpx;
  color: #909399;
}

.register-link {
  text-align: center;
  margin-top: 30rpx;
}

.register-link text {
  font-size: 26rpx;
  color: #667eea;
}

.forgot-link {
  text-align: center;
  margin-top: 16rpx;
}

.forgot-link text {
  font-size: 26rpx;
  color: #909399;
}
</style>