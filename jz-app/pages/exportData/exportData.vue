<template>
  <view class="export-page">
    <view class="custom-nav">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
      <view class="nav-bar">
        <text class="nav-title">导出数据</text>
        <view class="nav-right" @click="goToAccountBooks">
          <text class="nav-switch">{{ currentBookName }} ›</text>
        </view>
      </view>
    </view>

    <view class="section-title">时间范围</view>
    <view class="range-options">
      <view class="range-item" :class="{ active: dateRange === '1m' }" @click="dateRange = '1m'">近1个月</view>
      <view class="range-item" :class="{ active: dateRange === '3m' }" @click="dateRange = '3m'">近3个月</view>
      <view class="range-item" :class="{ active: dateRange === '6m' }" @click="dateRange = '6m'">近6个月</view>
      <view class="range-item" :class="{ active: dateRange === '1y' }" @click="dateRange = '1y'">近1年</view>
    </view>

    <button class="export-btn" @click="handleExport">导出 Excel</button>
  </view>
</template>

<script>
import { accountBookApi, submitExportTask } from '../../api/index.js'
import { storage } from '../../utils/storage.js'

export default {
  data() {
    return {
      statusBarHeight: 0,
      currentBookName: '默认账本',
      dateRange: '3m'
    }
  },

  computed: {
    dateParams() {
      const now = new Date()
      let startDate = new Date()
      switch (this.dateRange) {
        case '1m': startDate.setMonth(now.getMonth() - 1); break
        case '3m': startDate.setMonth(now.getMonth() - 3); break
        case '6m': startDate.setMonth(now.getMonth() - 6); break
        case '1y': startDate.setFullYear(now.getFullYear() - 1); break
      }
      const fmt = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      return { startDate: fmt(startDate), endDate: fmt(now) }
    }
  },

  onShow() {
    this.getStatusBarHeight()
    this.loadCurrentBookName()
  },

  methods: {
    getStatusBarHeight() {
      const sysInfo = uni.getSystemInfoSync()
      this.statusBarHeight = sysInfo.statusBarHeight || 0
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

    goToAccountBooks() {
      uni.navigateTo({ url: '/pages/accountBooks/accountBooks' })
    },

    async handleExport() {
      const bookId = storage.get('currentAccountBookId')
      if (!bookId) {
        uni.showModal({
          title: '提示',
          content: '请先选择要导出数据的账本',
          confirmText: '去选择',
          success: (res) => {
            if (res.confirm) {
              uni.navigateTo({ url: '/pages/accountBooks/accountBooks' })
            }
          }
        })
        return
      }

      const { startDate, endDate } = this.dateParams
      uni.showLoading({ title: '提交中...' })
      try {
        await submitExportTask(startDate, endDate)
        uni.hideLoading()
        uni.showModal({
          title: '提交成功',
          content: '账单流水导出任务已提交，服务端生成完成后会发送到您的绑定邮箱。',
          showCancel: false
        })
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '提交失败', icon: 'none' })
      }
    }
  }
}
</script>

<style>
.export-page {
  padding: 20rpx;
  padding-bottom: 120rpx;
  background: #f5f6fa;
  min-height: 100vh;
}

.custom-nav {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: -20rpx -20rpx 0;
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
}

.nav-switch {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.2);
  padding: 8rpx 24rpx;
  border-radius: 24rpx;
}

.current-book {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-top: 20rpx;
  margin-bottom: 30rpx;
}

.book-label {
  font-size: 26rpx;
  color: #909399;
}

.book-name {
  font-size: 28rpx;
  font-weight: bold;
  color: #303133;
}

.section-title {
  font-size: 28rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 16rpx;
}

.range-options {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.range-item {
  background: #fff;
  border-radius: 32rpx;
  padding: 14rpx 28rpx;
  font-size: 26rpx;
  color: #606266;
  border: 2rpx solid #e4e7ed;
}

.range-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-color: transparent;
}

.export-btn {
  margin-top: 60rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 44rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  font-weight: bold;
}
</style>
