<template>
  <view class="profile-page">
    <view class="user-card">
      <view class="user-card-header">
        <view class="avatar-section">
          <view class="avatar" @click="handleAvatarClick">
            <image v-if="userInfo?.avatar" class="avatar-img" :src="userInfo.avatar" mode="aspectFill"></image>
            <text v-else class="avatar-text">{{ avatarText }}</text>
            <view class="avatar-edit-badge">
              <text class="avatar-edit-icon">✎</text>
            </view>
          </view>
          <view class="user-info">
            <text class="username">{{ displayName }}</text>
            <text class="user-email">{{ userInfo?.email || '未设置邮箱' }}</text>
          </view>
        </view>
        <view class="alert-badge" v-if="alertCount > 0" @click="goToAlerts">
          <text class="alert-icon">🔔</text>
          <text class="alert-text">{{ alertCount }}条预警</text>
        </view>
      </view>
    </view>
    
    <view class="menu-section">
      <view class="menu-list">
        <view class="menu-item" @click="goToSettings">
          <view class="menu-left">
            <text class="menu-icon">⚙️</text>
            <text class="menu-title">账号设置</text>
          </view>
          <view class="menu-right">
            <text class="arrow">›</text>
          </view>
        </view>

        <view class="menu-item" @click="goToBudget">
          <view class="menu-left">
            <text class="menu-icon">💰</text>
            <text class="menu-title">预算管理</text>
          </view>
          <view class="menu-right">
            <text class="arrow">›</text>
          </view>
        </view>

        <view class="menu-item" @click="goToAlerts">
          <view class="menu-left">
            <text class="menu-icon">🔔</text>
            <text class="menu-title">预算预警</text>
          </view>
          <view class="menu-right">
            <text v-if="alertCount > 0" class="alert-dot"></text>
            <text class="arrow">›</text>
          </view>
        </view>

        <view class="menu-item" @click="goToAccountBooks">
          <view class="menu-left">
            <text class="menu-icon">📒</text>
            <text class="menu-title">账本管理</text>
          </view>
          <view class="menu-right">
            <text class="arrow">›</text>
          </view>
        </view>

        <view class="menu-item" @click="handleSync">
          <view class="menu-left">
            <text class="menu-icon">🔄</text>
            <text class="menu-title">数据同步</text>
          </view>
          <view class="menu-right">
            <text v-if="pendingCount > 0" class="pending-badge">{{ pendingCount }}条待同步</text>
            <text v-else class="sync-status">已同步</text>
            <text class="arrow">›</text>
          </view>
        </view>
        
        <!-- #ifdef H5 -->
        <view class="menu-item" @click="handleImport">
          <view class="menu-left">
            <text class="menu-icon">📥</text>
            <text class="menu-title">导入数据</text>
          </view>
          <view class="menu-right">
            <text class="arrow">›</text>
          </view>
        </view>
        <!-- #endif -->
        
        <view class="menu-item" @click="handleExport">
          <view class="menu-left">
            <text class="menu-icon">📤</text>
            <text class="menu-title">导出数据</text>
          </view>
          <view class="menu-right">
            <text class="arrow">›</text>
          </view>
        </view>
        
<!--        <view class="menu-item" @click="downloadTemplate">
          <view class="menu-left">
            <text class="menu-icon">📋</text>
            <text class="menu-title">下载导入模板</text>
          </view>
          <view class="menu-right">
            <text class="arrow">›</text>
          </view>
        </view> -->
        
        <view class="menu-item" @click="checkNetwork">
          <view class="menu-left">
            <text class="menu-icon">📶</text>
            <text class="menu-title">网络状态</text>
          </view>
          <view class="menu-right">
            <text :class="['network-status', isOnline ? 'online' : 'offline']">
              {{ isOnline ? '已连接' : '离线' }}
            </text>
            <text class="arrow">›</text>
          </view>
        </view>
        
      <!-- #ifdef APP-PLUS -->
        <view class="menu-item" @click="handleCheckUpdate">
          <view class="menu-left">
            <text class="menu-icon">🔄</text>
            <text class="menu-title">检查更新</text>
          </view>
          <view class="menu-right">
            <text class="menu-hint">v{{ versionName }}</text>
            <text class="arrow">›</text>
          </view>
        </view>
        <!-- #endif -->
        
        <AppVersionInfo>
          <template #trigger>
            <view class="menu-item">
              <view class="menu-left">
                <text class="menu-icon">ℹ️</text>
                <text class="menu-title">关于</text>
              </view>
              <view class="menu-right">
                <text class="menu-hint">v{{ versionName }}</text>
                <text class="arrow">›</text>
              </view>
            </view>
          </template>
        </AppVersionInfo>
      </view>
    </view>
    
    <view class="logout-section">
      <button class="logout-btn" @click="handleLogout">
        退出登录
      </button>
    </view>
  </view>
</template>

<script>
import {
  isOnline,
  clearToken
} from '../../utils/request.js'
import {
  transactionApi,
  budgetApi,
  authApi,
  getTemplateUrl
} from '../../api/index.js'
import { storage } from '../../utils/storage.js'
import { offlineSync } from '../../utils/offlineSync.js'
import upgradeManager from '../../utils/upgrade.js'
import AppVersionInfo from '../../components/AppVersionInfo.vue'

export default {
  components: { AppVersionInfo },
  data() {
    return {
      userInfo: null,
      pendingCount: 0,
      alertCount: 0,
      isOnline: false,
      avatarUploading: false
    }
  },
  
  computed: {
    displayName() {
      if (!this.userInfo) return '未登录'
      return this.userInfo.nickname || this.userInfo.username
    },
    avatarText() {
      const name = this.displayName
      if (!name || name === '未登录') return 'U'
      return name.charAt(0).toUpperCase()
    },
    versionName() {
      const info = upgradeManager.getVersionInfo()
      return info.versionName || '1.0.0'
    }
  },
  
  onShow() {
    this.checkAuth()
    this.loadUserInfo()
    this.updateStatus()
    this.loadAlertCount()
  },
  
  methods: {
    checkAuth() {
      const token = storage.get('token')
      if (!token) {
        uni.reLaunch({ url: '/pages/login/login' })
      }
    },

    loadUserInfo() {
      // 先用本地缓存快速渲染（避免每次进入页面闪烁）
      this.userInfo = storage.get('user_info')
      // 再异步从服务器拉最新数据（同步 web 端的头像/昵称等改动）
      this.refreshUserInfoFromServer()
    },

    async refreshUserInfoFromServer() {
      try {
        const result = await authApi.getCurrentUser()
        if (result) {
          storage.set('user_info', result)
          this.userInfo = result
        }
      } catch (e) {
        // 静默失败，保持本地缓存
      }
    },

    handleAvatarClick() {
      if (this.avatarUploading) return
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const filePath = res.tempFilePaths[0]
          this.uploadAvatar(filePath)
        }
      })
    },

    async uploadAvatar(filePath) {
      this.avatarUploading = true
      uni.showLoading({ title: '上传中...' })
      try {
        const result = await authApi.uploadAvatar(filePath)
        storage.set('user_info', result)
        this.userInfo = result
        uni.showToast({ title: '头像修改成功', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e.message || '头像上传失败', icon: 'none' })
      } finally {
        this.avatarUploading = false
        uni.hideLoading()
      }
    },
    
    async updateStatus() {
      this.pendingCount = offlineSync.getPendingCount()
      const online = await isOnline()
      this.isOnline = online
    },
    
    async loadAlertCount() {
      try {
        const data = await budgetApi.getAlertCount()
        this.alertCount = data.unreadCount || 0
      } catch (e) {
        console.error('加载预警数量失败', e)
      }
    },
    
    async checkNetwork() {
      const online = await isOnline()
      this.isOnline = online
      uni.showToast({ 
        title: online ? '网络已连接' : '当前处于离线状态', 
        icon: online ? 'success' : 'none' 
      })
    },
    
    async handleSync() {
      const online = await isOnline()
      
      if (!online) {
        uni.showToast({ title: '当前无网络，请稍后重试', icon: 'none' })
        return
      }
      
      if (this.pendingCount === 0) {
        uni.showToast({ title: '没有待同步的数据', icon: 'none' })
        return
      }
      
      uni.showLoading({ title: '同步中...' })
      try {
        const result = await offlineSync.syncAll()
        uni.hideLoading()
        
        if (result.fail === 0) {
          uni.showToast({ 
            title: result.message || `同步成功: ${result.success}条`, 
            icon: 'success' 
          })
        } else {
          uni.showToast({ 
            title: result.message || `成功${result.success}条，失败${result.fail}条`, 
            icon: 'none' 
          })
        }
        
        this.updateStatus()
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: '同步失败', icon: 'none' })
        console.error(e)
      }
    },
    
    handleImport() {
      const bookId = storage.get('currentAccountBookId')
      if (!bookId) {
        uni.showModal({
          title: '提示',
          content: '请先选择要导入数据的账本',
          confirmText: '去选择',
          success: (res) => {
            if (res.confirm) {
              uni.navigateTo({ url: '/pages/accountBooks/accountBooks' })
            }
          }
        })
        return
      }
      uni.chooseFile({
        count: 1,
        extension: ['.xlsx', '.xls'],
        success: async (res) => {
          const filePath = res.tempFilePaths[0]
          
          uni.showLoading({ title: '导入中...' })
          try {
            const result = await transactionApi.import(filePath)
            uni.hideLoading()
            
            let message = `成功导入 ${result.successCount} 条`
            if (result.failCount > 0) {
              message += `，失败 ${result.failCount} 条`
            }
            
            uni.showModal({
              title: '导入完成',
              content: message,
              showCancel: false,
              success: () => {
                if (result.errors && result.errors.length > 0) {
                  let errorMsg = result.errors.slice(0, 5).join('\n')
                  if (result.errors.length > 5) {
                    errorMsg += `\n...还有 ${result.errors.length - 5} 条错误`
                  }
                  uni.showModal({
                    title: '错误详情',
                    content: errorMsg,
                    showCancel: false
                  })
                }
              }
            })
            
          } catch (e) {
            uni.hideLoading()
            uni.showToast({ title: '导入失败: ' + (e.message || '未知错误'), icon: 'none' })
            console.error(e)
          }
        },
        fail: (err) => {
          if (err.errMsg !== 'chooseFile:fail cancel') {
            uni.showToast({ title: '选择文件失败', icon: 'none' })
          }
        }
      })
    },
    
    handleExport() {
      uni.navigateTo({ url: '/pages/exportData/exportData' })
    },
    
    downloadTemplate() {
      uni.showLoading({ title: '准备下载...' })
      
      const url = getTemplateUrl()
      uni.hideLoading()
      
      // #ifdef H5
      window.open(url, '_blank')
      uni.showToast({ title: '开始下载模板', icon: 'success' })
      // #endif
      
      // #ifndef H5
      uni.downloadFile({
        url: url,
        success: (res) => {
          uni.openDocument({
            filePath: res.tempFilePath,
            showMenu: true,
            success: () => {
              uni.showToast({ title: '打开模板成功', icon: 'success' })
            },
            fail: () => {
              uni.showToast({ title: '打开文件失败，请从文件管理器查看', icon: 'none' })
            }
          })
        },
        fail: (err) => {
          uni.showToast({ title: '下载失败', icon: 'none' })
          console.error(err)
        }
      })
      // #endif
    },
    
    goToSettings() {
      uni.navigateTo({ url: '/pages/settings/settings' })
    },

    goToBudget() {
      uni.navigateTo({ url: '/pages/budget/budget' })
    },

    goToAlerts() {
      uni.navigateTo({ url: '/pages/alerts/alerts' })
    },

    goToAccountBooks() {
      uni.navigateTo({ url: '/pages/accountBooks/accountBooks' })
    },
    
    handleCheckUpdate() {
      upgradeManager.checkAndPrompt({ silent: false })
    },

    showAbout() {
      const versionInfo = upgradeManager.getVersionInfo()
      // #ifdef APP-PLUS
      const platformText = versionInfo.platform === 'ios' ? 'iOS' : 'Android'
      const content = '拾光账本 - 个人财务记账与预算预警系统\n'
        + '版本: ' + versionInfo.versionName + ' (Build ' + versionInfo.versionCode + ')\n'
        + '平台: ' + platformText + '\n\n'
        + '支持离线记账、预算预警、多端同步、Excel导入导出'
      // #endif
      // #ifndef APP-PLUS
      const content = '拾光账本 - 个人财务记账与预算预警系统\n'
        + '版本: ' + versionInfo.versionName + '\n\n'
        + '支持离线记账、预算预警、多端同步、Excel导入导出'
      // #endif

      uni.showModal({
        title: '关于拾光账本',
        content: content,
        showCancel: false
      })
    },
    
    async handleLogout() {
      const res = await uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        confirmText: '退出',
        confirmColor: '#f56c6c'
      })
      
      if (res.confirm) {
        clearToken()
        storage.remove('user_info')
        uni.reLaunch({ url: '/pages/login/login' })
      }
    }
  }
}
</script>

<style>
.profile-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}

.user-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40rpx;
  margin: 0;
}

.user-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.avatar-section {
  display: flex;
  align-items: center;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
  position: relative;
  overflow: hidden;
}

.avatar-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
}

.avatar-text {
  font-size: 48rpx;
  font-weight: bold;
  color: #fff;
}

.avatar-edit-badge {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 40rpx;
  height: 40rpx;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid #fff;
}

.avatar-edit-icon {
  font-size: 22rpx;
  color: #fff;
}

.user-info {
  flex: 1;
}

.username {
  font-size: 36rpx;
  font-weight: bold;
  color: #fff;
  display: block;
  margin-bottom: 8rpx;
}

.user-email {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}

.alert-badge {
  display: flex;
  align-items: center;
  background: rgba(255, 193, 7, 0.9);
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
}

.alert-icon {
  font-size: 28rpx;
  margin-right: 8rpx;
}

.alert-text {
  font-size: 22rpx;
  color: #fff;
  font-weight: bold;
}

.menu-section {
  padding: 20rpx;
}

.menu-list {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-left {
  display: flex;
  align-items: center;
}

.menu-icon {
  font-size: 36rpx;
  margin-right: 20rpx;
}

.menu-title {
  font-size: 30rpx;
  color: #303133;
}

.menu-right {
  display: flex;
  align-items: center;
}

.pending-badge {
  font-size: 24rpx;
  color: #fff;
  background: #f56c6c;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  margin-right: 12rpx;
}

.sync-status {
  font-size: 24rpx;
  color: #67c23a;
  margin-right: 12rpx;
}

.network-status {
  font-size: 24rpx;
  margin-right: 12rpx;
}

.network-status.online {
  color: #67c23a;
}

.network-status.offline {
  color: #f56c6c;
}

.menu-hint {
  font-size: 26rpx;
  color: #909399;
  margin-right: 12rpx;
}

.arrow {
  font-size: 32rpx;
  color: #c0c4cc;
}

.alert-dot {
  width: 16rpx;
  height: 16rpx;
  background: #f56c6c;
  border-radius: 50%;
  margin-right: 12rpx;
}

.logout-section {
  padding: 40rpx 20rpx;
}

.logout-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #fff;
  color: #f56c6c;
  border: none;
  border-radius: 44rpx;
  font-size: 30rpx;
}
</style>
