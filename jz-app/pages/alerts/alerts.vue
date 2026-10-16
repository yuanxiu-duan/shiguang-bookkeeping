<template>
  <view class="alerts-page">
    <view class="custom-nav">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
      <view class="nav-bar">
        <text class="nav-title">预算预警</text>
        <view class="nav-right">
          <text class="nav-switch" @click="goToAccountBooks">{{ currentBookName }} ›</text>
          <text class="nav-clear-btn" v-if="unreadCount > 0" @click="handleMarkAllRead">全部已读</text>
        </view>
      </view>
    </view>
    
    <view v-if="loading" class="loading-state">
      <text>加载中...</text>
    </view>
    
    <view v-else-if="alerts.length === 0" class="empty-state">
      <text class="empty-icon">🔔</text>
      <text class="empty-text">暂无预警信息</text>
    </view>
    
    <view v-else class="alerts-list">
      <view 
        v-for="alert in alerts" 
        :key="alert.id" 
        class="alert-item"
        :class="{ 'is-read': alert.isRead }"
        @click="handleAlertClick(alert)"
      >
        <view class="alert-left">
          <text class="alert-category-icon">{{ alert.category?.icon || '📌' }}</text>
        </view>
        <view class="alert-content">
          <view class="alert-header">
            <text class="alert-category">{{ alert.category?.name || '未分类' }}</text>
            <text class="alert-percentage">{{ alert.percentage }}%</text>
          </view>
          <view class="alert-desc">
            预算 ¥{{ alert.budgetLimit }}，已使用 ¥{{ alert.usedAmount }}
          </view>
          <view class="alert-over" v-if="alert.overAmount && alert.overAmount > 0">
            <text class="over-label">超支: </text>
            <text class="over-amount">¥{{ alert.overAmount }}</text>
          </view>
        </view>
        <view class="alert-right">
          <text class="alert-date">{{ formatDate(alert.alertDate) }}</text>
          <view class="alert-status" v-if="!alert.isRead">
            <text class="unread-dot"></text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { budgetApi, accountBookApi } from '../../api/index.js'
import { storage } from '../../utils/storage.js'
import dayjs from '../../utils/dayjs.js'

export default {
  data() {
    return {
      statusBarHeight: 0,
      loading: true,
      alerts: [],
      unreadCount: 0,
      currentBookName: '默认账本'
    }
  },
  
  onShow() {
    this.getStatusBarHeight()
    this.checkAuth()
    this.loadCurrentBookName()
    this.loadData()
  },
  
  methods: {
    getStatusBarHeight() {
      const sysInfo = uni.getSystemInfoSync()
      this.statusBarHeight = sysInfo.statusBarHeight || 0
    },

    checkAuth() {
      const token = storage.get('token')
      if (!token) {
        uni.reLaunch({ url: '/pages/login/login' })
      }
    },

    goToAccountBooks() {
      uni.navigateTo({ url: '/pages/accountBooks/accountBooks' })
    },

    async loadCurrentBookName() {
      const bookId = storage.get('currentAccountBookId')
      if (bookId) {
        try {
          const books = await accountBookApi.getList()
          const current = books.find(b => b.id === bookId)
          if (current) {
            this.currentBookName = current.alias || current.name
          }
        } catch (e) {}
      } else {
        this.currentBookName = '默认账本'
      }
    },
    
    formatDate(date) {
      if (!date) return ''
      return dayjs(date).format('MM-DD')
    },
    
    async loadData() {
      this.loading = true
      try {
        const [alertsData, countData] = await Promise.all([
          budgetApi.getAlerts(true),
          budgetApi.getAlertCount()
        ])
        this.alerts = alertsData || []
        this.unreadCount = countData?.unreadCount || 0
      } catch (e) {
        console.error('加载预警失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    
    async handleAlertClick(alert) {
      if (!alert.isRead) {
        try {
          await budgetApi.markAlertRead(alert.id)
          alert.isRead = true
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        } catch (e) {
          console.error('标记已读失败', e)
        }
      }
      uni.switchTab({ url: '/pages/budget/budget' })
    },
    
    async handleMarkAllRead() {
      try {
        await budgetApi.markAllAlertsRead()
        this.alerts.forEach(a => a.isRead = true)
        this.unreadCount = 0
        uni.showToast({ title: '已全部标记为已读', icon: 'success' })
      } catch (e) {
        console.error('标记全部已读失败', e)
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    }
  }
}
</script>

<style>
.alerts-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.custom-nav {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.status-bar {
  width: 100%;
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 28rpx;
}

.nav-title {
  font-size: 34rpx;
  font-weight: bold;
  color: #fff;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.nav-switch {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.2);
  padding: 8rpx 24rpx;
  border-radius: 24rpx;
}

.nav-clear-btn {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.15);
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #909399;
}

.alerts-list {
  padding: 20rpx;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.alert-item.is-read {
  opacity: 0.7;
}

.alert-left {
  margin-right: 20rpx;
}

.alert-category-icon {
  font-size: 40rpx;
}

.alert-content {
  flex: 1;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}

.alert-category {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}

.alert-percentage {
  font-size: 28rpx;
  font-weight: bold;
  color: #f56c6c;
}

.alert-desc {
  font-size: 24rpx;
  color: #606266;
}

.alert-over {
  display: flex;
  align-items: center;
  margin-top: 8rpx;
}

.over-label {
  font-size: 24rpx;
  color: #909399;
}

.over-amount {
  font-size: 26rpx;
  color: #f56c6c;
  font-weight: bold;
}

.alert-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-left: 20rpx;
}

.alert-date {
  font-size: 22rpx;
  color: #c0c4cc;
  margin-bottom: 8rpx;
}

.alert-status {
  display: flex;
  align-items: center;
}

.unread-dot {
  width: 16rpx;
  height: 16rpx;
  background: #f56c6c;
  border-radius: 50%;
}
</style>
