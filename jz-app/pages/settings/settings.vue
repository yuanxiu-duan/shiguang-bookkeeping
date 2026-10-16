<template>
  <view class="settings-page">
    <view class="section">
      <view class="section-title">个人信息</view>
      <view class="card">
        <view class="card-item readonly">
          <text class="item-label">登录账号</text>
          <view class="item-right">
            <text class="item-value readonly-value">{{ userInfo?.username || '未设置' }}</text>
          </view>
        </view>
        <view class="card-item" @click="showNicknamePopup = true">
          <text class="item-label">昵称</text>
          <view class="item-right">
            <text class="item-value">{{ userInfo?.nickname || '未设置' }}</text>
            <text class="arrow">›</text>
          </view>
        </view>
        <view class="card-item" @click="showEmailPopup = true">
          <text class="item-label">邮箱</text>
          <view class="item-right">
            <text class="item-value">{{ userInfo?.email || '未设置' }}</text>
            <text class="arrow">›</text>
          </view>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-title">安全设置</view>
      <view class="card">
        <view class="card-item" @click="showPasswordPopup = true">
          <text class="item-label">修改密码</text>
          <view class="item-right">
            <text class="arrow">›</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 修改昵称弹窗 -->
    <view class="picker-mask" v-if="showNicknamePopup" @click="showNicknamePopup = false">
      <view class="picker-content" @click.stop>
        <view class="picker-header">
          <text class="picker-title">修改昵称</text>
          <text class="picker-close" @click="showNicknamePopup = false">✕</text>
        </view>
        <view class="picker-body">
          <input
            v-model="nicknameForm.nickname"
            class="popup-input"
            placeholder="请输入新昵称"
            maxlength="50"
            :focus="showNicknamePopup"
          />
        </view>
        <view class="picker-footer">
          <button class="btn-cancel" @click="showNicknamePopup = false">取消</button>
          <button class="btn-confirm" :disabled="nicknameLoading" :loading="nicknameLoading" @click="handleUpdateNickname">确认</button>
        </view>
      </view>
    </view>

    <!-- 修改邮箱弹窗 -->
    <view class="picker-mask" v-if="showEmailPopup" @click="showEmailPopup = false">
      <view class="picker-content" @click.stop>
        <view class="picker-header">
          <text class="picker-title">修改邮箱</text>
          <text class="picker-close" @click="showEmailPopup = false">✕</text>
        </view>
        <view class="picker-body" v-if="emailStep === 0">
          <!-- Step 1: 验证旧邮箱 -->
          <view class="form-item">
            <text class="form-label">当前邮箱</text>
            <input
              class="popup-input"
              :value="userInfo?.email || '未绑定'"
              disabled
              style="color:#909399"
            />
          </view>
          <view class="form-item">
            <text class="form-label">身份验证码</text>
            <view class="code-row">
              <input
                v-model="emailForm.oldEmailCode"
                class="popup-input code-input"
                placeholder="请输入当前邮箱验证码"
                maxlength="6"
              />
              <button
                class="btn-code"
                :disabled="oldCodeSending || oldCodeCountdown > 0"
                @click="handleSendOldCode"
              >
                {{ oldCodeCountdown > 0 ? oldCodeCountdown + '秒' : '发送验证码' }}
              </button>
            </view>
          </view>
          <view class="picker-footer" style="padding: 20rpx 0 0;">
            <button class="btn-confirm" @click="emailStep = 1" :disabled="!emailForm.oldEmailCode">下一步</button>
          </view>
        </view>
        <view class="picker-body" v-if="emailStep === 1">
          <!-- Step 2: 设置新邮箱 -->
          <view class="form-item">
            <text class="form-label">新邮箱</text>
            <input
              v-model="emailForm.newEmail"
              class="popup-input"
              placeholder="请输入新邮箱"
              maxlength="100"
            />
          </view>
          <view class="form-item">
            <text class="form-label">新邮箱验证码</text>
            <view class="code-row">
              <input
                v-model="emailForm.newEmailCode"
                class="popup-input code-input"
                placeholder="请输入新邮箱验证码"
                maxlength="6"
              />
              <button
                class="btn-code"
                :disabled="newCodeSending || newCodeCountdown > 0"
                @click="handleSendNewCode"
              >
                {{ newCodeCountdown > 0 ? newCodeCountdown + '秒' : '获取验证码' }}
              </button>
            </view>
          </view>
          <view class="picker-footer" style="padding: 20rpx 0 0;">
            <button class="btn-cancel" style="flex:1" @click="emailStep = 0">上一步</button>
            <button class="btn-confirm" style="flex:1" :disabled="emailLoading" :loading="emailLoading" @click="handleUpdateEmail">确认</button>
          </view>
        </view>
      </view>
    </view>

    <!-- 修改密码弹窗 -->
    <view class="picker-mask" v-if="showPasswordPopup" @click="showPasswordPopup = false">
      <view class="picker-content" @click.stop>
        <view class="picker-header">
          <text class="picker-title">修改密码</text>
          <text class="picker-close" @click="showPasswordPopup = false">✕</text>
        </view>
        <view class="picker-body">
          <view class="form-item">
            <text class="form-label">新密码</text>
            <input
              v-model="passwordForm.newPassword"
              class="popup-input"
              type="password"
              placeholder="请输入新密码"
              maxlength="100"
              password
            />
          </view>
          <view class="form-item">
            <text class="form-label">确认密码</text>
            <input
              v-model="passwordForm.confirmPassword"
              class="popup-input"
              type="password"
              placeholder="请再次输入新密码"
              maxlength="100"
              password
            />
          </view>
        </view>
        <view class="picker-footer">
          <button class="btn-cancel" @click="showPasswordPopup = false">取消</button>
          <button class="btn-confirm" :disabled="passwordLoading" :loading="passwordLoading" @click="handleUpdatePassword">确认</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { authApi } from '../../api/index.js'
import { storage } from '../../utils/storage.js'

export default {
  data() {
    return {
      userInfo: null,
      showNicknamePopup: false,
      showEmailPopup: false,
      showPasswordPopup: false,
      nicknameLoading: false,
      emailLoading: false,
      passwordLoading: false,
      emailStep: 0,
      oldCodeSending: false,
      oldCodeCountdown: 0,
      oldCodeTimer: null,
      newCodeSending: false,
      newCodeCountdown: 0,
      newCodeTimer: null,
      nicknameForm: { nickname: '' },
      emailForm: { oldEmailCode: '', newEmail: '', newEmailCode: '' },
      passwordForm: { newPassword: '', confirmPassword: '' }
    }
  },

  onShow() {
    this.loadUserInfo()
  },

  beforeDestroy() {
    if (this.oldCodeTimer) clearInterval(this.oldCodeTimer)
    if (this.newCodeTimer) clearInterval(this.newCodeTimer)
  },

  methods: {
    loadUserInfo() {
      this.userInfo = storage.get('user_info')
    },

    async handleUpdateNickname() {
      if (!this.nicknameForm.nickname.trim()) {
        uni.showToast({ title: '请输入昵称', icon: 'none' })
        return
      }
      this.nicknameLoading = true
      try {
        const result = await authApi.updateNickname(this.nicknameForm.nickname.trim())
        storage.set('user_info', result)
        this.userInfo = result
        uni.showToast({ title: '昵称修改成功', icon: 'success' })
        this.showNicknamePopup = false
      } catch (e) {
        uni.showToast({ title: e.message || '修改失败', icon: 'none' })
      } finally {
        this.nicknameLoading = false
      }
    },

    async handleSendOldCode() {
      const email = this.userInfo?.email
      if (!email) {
        uni.showToast({ title: '当前账号未绑定邮箱', icon: 'none' })
        return
      }
      this.oldCodeSending = true
      try {
        await authApi.sendCode({ email, type: 'UPDATE_EMAIL_OLD_CONFIRM' })
        uni.showToast({ title: '身份验证码已发送', icon: 'success' })
        this.oldCodeCountdown = 60
        this.oldCodeTimer = setInterval(() => {
          this.oldCodeCountdown--
          if (this.oldCodeCountdown <= 0) {
            clearInterval(this.oldCodeTimer)
            this.oldCodeTimer = null
          }
        }, 1000)
      } catch (e) {
        uni.showToast({ title: e.message || '发送失败', icon: 'none' })
      } finally {
        this.oldCodeSending = false
      }
    },

    async handleSendNewCode() {
      if (!this.emailForm.newEmail) {
        uni.showToast({ title: '请先输入新邮箱', icon: 'none' })
        return
      }
      const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailReg.test(this.emailForm.newEmail)) {
        uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
        return
      }
      this.newCodeSending = true
      try {
        await authApi.sendCode({ email: this.emailForm.newEmail, type: 'UPDATE_EMAIL' })
        uni.showToast({ title: '验证码已发送', icon: 'success' })
        this.newCodeCountdown = 60
        this.newCodeTimer = setInterval(() => {
          this.newCodeCountdown--
          if (this.newCodeCountdown <= 0) {
            clearInterval(this.newCodeTimer)
            this.newCodeTimer = null
          }
        }, 1000)
      } catch (e) {
        uni.showToast({ title: e.message || '发送失败', icon: 'none' })
      } finally {
        this.newCodeSending = false
      }
    },

    async handleUpdateEmail() {
      if (!this.emailForm.oldEmailCode || !this.emailForm.newEmail || !this.emailForm.newEmailCode) {
        uni.showToast({ title: '请完整填写信息', icon: 'none' })
        return
      }
      this.emailLoading = true
      try {
        const result = await authApi.updateEmail({
          oldEmailCode: this.emailForm.oldEmailCode,
          newEmail: this.emailForm.newEmail,
          newEmailCode: this.emailForm.newEmailCode
        })
        storage.set('user_info', result)
        this.userInfo = result
        uni.showToast({ title: '邮箱修改成功', icon: 'success' })
        this.showEmailPopup = false
        this.emailForm = { oldEmailCode: '', newEmail: '', newEmailCode: '' }
        this.emailStep = 0
      } catch (e) {
        uni.showToast({ title: e.message || '修改失败', icon: 'none' })
      } finally {
        this.emailLoading = false
      }
    },

    async handleUpdatePassword() {
      if (!this.passwordForm.newPassword) {
        uni.showToast({ title: '请输入新密码', icon: 'none' })
        return
      }
      if (this.passwordForm.newPassword.length < 6) {
        uni.showToast({ title: '密码至少6位', icon: 'none' })
        return
      }
      if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
        return
      }
      this.passwordLoading = true
      try {
        await authApi.updatePassword({
          newPassword: this.passwordForm.newPassword,
          confirmPassword: this.passwordForm.confirmPassword
        })
        uni.showToast({ title: '密码修改成功', icon: 'success' })
        this.showPasswordPopup = false
        this.passwordForm = { newPassword: '', confirmPassword: '' }
      } catch (e) {
        uni.showToast({ title: e.message || '修改失败', icon: 'none' })
      } finally {
        this.passwordLoading = false
      }
    }
  }
}
</script>

<style>
.settings-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}

.section {
  padding: 20rpx;
}

.section-title {
  font-size: 26rpx;
  color: #909399;
  padding: 10rpx 0;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.card-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.card-item:last-child {
  border-bottom: none;
}

.item-label {
  font-size: 30rpx;
  color: #303133;
}

.item-right {
  display: flex;
  align-items: center;
}

.item-value {
  font-size: 28rpx;
  color: #909399;
  margin-right: 8rpx;
}

.readonly-value {
  color: #606266;
  margin-right: 0;
}

.arrow {
  font-size: 32rpx;
  color: #c0c4cc;
}

/* 弹窗样式 */
.picker-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.picker-content {
  width: 100%;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
}

.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 30rpx 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.picker-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
}

.picker-close {
  font-size: 36rpx;
  color: #909399;
  padding: 10rpx;
}

.picker-body {
  padding: 30rpx;
}

.form-item {
  margin-bottom: 24rpx;
}

.form-label {
  font-size: 28rpx;
  color: #303133;
  margin-bottom: 12rpx;
  display: block;
}

.popup-input {
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

.picker-footer {
  display: flex;
  gap: 20rpx;
  padding: 20rpx 30rpx 30rpx;
}

.btn-cancel {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  background: #f5f5f5;
  color: #606266;
  border: none;
  border-radius: 44rpx;
  font-size: 30rpx;
}

.btn-confirm {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 44rpx;
  font-size: 30rpx;
}

.btn-confirm[disabled] {
  opacity: 0.6;
}
</style>
