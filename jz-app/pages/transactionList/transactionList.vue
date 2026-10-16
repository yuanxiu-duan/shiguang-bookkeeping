<template>
  <view class="bill-page">
    <view class="custom-nav">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
      <view class="nav-bar">
        <view class="nav-back" @click="goBack">
          <text>‹</text>
        </view>
        <text class="nav-title">筛选结果</text>
        <text class="nav-count">{{ totalElements }}笔</text>
      </view>
    </view>

    <view class="summary-header">
      <view class="summary-item income">
        <text class="summary-label">总收入</text>
        <text class="summary-value">{{ totals.income.toFixed(2) }}</text>
      </view>
      <view class="summary-item balance">
        <text class="summary-label">总结余</text>
        <text class="summary-value">{{ totals.balance.toFixed(2) }}</text>
      </view>
      <view class="summary-item expense">
        <text class="summary-label">总支出</text>
        <text class="summary-value">{{ totals.expense.toFixed(2) }}</text>
      </view>
    </view>

    <scroll-view scroll-y class="transactions-scroll" @scrolltolower="loadMore">
      <view v-if="firstLoad && allTransactions.length === 0" class="loading-skeleton">
        <view v-for="(item, idx) in 5" :key="'sk-' + idx" class="skeleton-item">
          <view class="sk-left">
            <view class="sk-icon"></view>
            <view class="sk-info">
              <view class="sk-line sk-line-short"></view>
              <view class="sk-line sk-line-long"></view>
            </view>
          </view>
          <view class="sk-amount sk-line-long"></view>
        </view>
      </view>

      <view v-if="!firstLoad && allTransactions.length === 0 && !loading" class="empty-state">
        <text class="empty-icon">🔍</text>
        <text class="empty-text">没有符合条件的流水</text>
      </view>

      <view v-for="group in groupedTransactions" :key="group.date" class="date-group">
        <view class="date-header">
          <text class="date">{{ formatDate(group.date) }}</text>
          <text class="date-summary">
            收{{ group.income.toFixed(2) }}, 支{{ group.expense.toFixed(2) }}, 余{{ (group.income - group.expense).toFixed(2) }}
          </text>
        </view>

        <view
          v-for="item in group.transactions"
          :key="item.id || item._localId"
          class="transaction-item"
          @click="viewDetail(item)"
          @longpress="onItemLongPress(item)"
        >
          <view class="item-left">
            <view class="item-avatar">
              <image v-if="item.createdByAvatar" class="avatar-img" :src="item.createdByAvatar" mode="aspectFill"></image>
              <text v-else class="avatar-text">{{ (item.createdBy || '未').charAt(0) }}</text>
            </view>
            <view class="item-info">
              <view class="item-row-main">
                <text class="item-category">{{ item.category?.name || '未分类' }}</text>
                <text :class="['item-amount', item.type === 'EXPENSE' ? 'expense' : 'income']">
                  {{ item.type === 'EXPENSE' ? '-' : '+' }}{{ item.amount }}
                </text>
              </view>
              <view class="item-row-sub">
                <view v-if="item.account" class="item-account">
                  <text class="account-icon">{{ accountIcon(item.account) }}</text>
                  <text class="account-name">{{ item.account }}</text>
                </view>
                <text v-if="item.note || item.category?.parentName" class="item-note">{{ item.note || item.category?.parentName }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view v-if="loading" class="loading-state">
        <text>加载中...</text>
      </view>

      <view v-if="noMoreData && allTransactions.length > 0" class="no-more-state">
        <text>— 没有更多了 —</text>
      </view>

      <view v-if="loadError" class="error-state" @click="retryLoad">
        <text>加载失败，点击重试</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { accountBookApi, transactionApi } from '../../api/index.js'
import { storage } from '../../utils/storage.js'
import { toCents } from '../../utils/money.js'

export default {
  data() {
    return {
      statusBarHeight: 0,
      loading: false,
      firstLoad: true,
      loadError: false,
      noMoreData: false,
      filters: {},
      transactions: [],
      allTransactions: [],
      currentPage: 0,
      pageSize: 20,
      totalElements: 0,
      currentUser: null,
      totals: { income: 0, expense: 0, balance: 0 }
    }
  },

  computed: {
    groupedTransactions() {
      const raw = {}
      this.allTransactions.forEach(t => {
        const date = t.transactionDate
        if (!raw[date]) {
          raw[date] = { date, expenseCents: 0, incomeCents: 0, transactions: [] }
        }
        const cents = toCents(t.amount)
        if (t.type === 'EXPENSE') {
          raw[date].expenseCents += cents
        } else {
          raw[date].incomeCents += cents
        }
        raw[date].transactions.push(t)
      })
      return Object.values(raw)
        .map(g => ({
          date: g.date,
          expense: g.expenseCents / 100,
          income: g.incomeCents / 100,
          transactions: g.transactions
        }))
        .sort((a, b) => b.date.localeCompare(a.date))
    }
  },

  onLoad(options) {
    const sysInfo = uni.getSystemInfoSync()
    this.statusBarHeight = sysInfo.statusBarHeight || 0
    this.currentUser = storage.get('user_info')

    if (options.filters) {
      try {
        this.filters = JSON.parse(decodeURIComponent(options.filters)) || {}
      } catch (e) {
        this.filters = {}
      }
    }

    this.loadData()
  },

  methods: {
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) })
    },

    async loadData() {
      if (this.loading) return
      this.loading = true
      this.loadError = false
      try {
        const res = await transactionApi.search({
          ...this.filters,
          page: this.currentPage,
          size: this.pageSize
        })

        if (res && res.content) {
          if (this.currentPage === 0) {
            this.allTransactions = res.content
          } else {
            this.allTransactions = [...this.allTransactions, ...res.content]
          }
          this.totalElements = res.totalElements || res.content.length
          if (this.allTransactions.length >= this.totalElements) {
            this.noMoreData = true
          }
          this.currentPage++
          this.recomputeTotals()
        } else if (Array.isArray(res)) {
          if (this.currentPage === 0) {
            this.allTransactions = res
          } else {
            this.allTransactions = [...this.allTransactions, ...res]
          }
          if (res.length < this.pageSize) {
            this.noMoreData = true
          }
          this.currentPage++
          this.recomputeTotals()
        }
      } catch (e) {
        console.error('加载筛选结果失败', e)
        this.loadError = true
      } finally {
        this.loading = false
        this.firstLoad = false
      }
    },

    recomputeTotals() {
      let incomeCents = 0
      let expenseCents = 0
      this.allTransactions.forEach(t => {
        if (t.type === 'EXPENSE') expenseCents += toCents(t.amount)
        else incomeCents += toCents(t.amount)
      })
      this.totals = {
        income: incomeCents / 100,
        expense: expenseCents / 100,
        balance: (incomeCents - expenseCents) / 100
      }
    },

    loadMore() {
      if (this.loading || this.noMoreData) return
      this.loadData()
    },

    retryLoad() {
      this.loadData()
    },

    viewDetail(item) {
      this.onItemLongPress(item)
    },

    async onItemLongPress(item) {
      if (!item.id) return

      const currentUserId = this.currentUser?.id
      const isOwn = currentUserId != null && String(item.userId) === String(currentUserId)
      // 检查是否是账本所有者
      let isBookOwner = false
      if (!isOwn) {
        try {
          const books = await accountBookApi.getList()
          const bookId = storage.get('currentAccountBookId')
          const currentBook = books.find(b => String(b.id) === String(bookId))
          if (currentBook && currentUserId != null && String(currentBook.userId) === String(currentUserId)) {
            isBookOwner = true
          }
        } catch (e) {}
      }

      if (!isOwn && !isBookOwner) return

      const itemList = ['编辑']
      if (isOwn || isBookOwner) {
        itemList.push('删除')
      }

      uni.showActionSheet({
        itemList,
        success: (res) => {
          if (res.tapIndex === 0) {
            this.openEditDialog(item)
          } else if (res.tapIndex === 1) {
            this.handleDelete(item)
          }
        }
      })
    },

    openEditDialog(item) {
      const params = encodeURIComponent(JSON.stringify({
        id: item.id,
        amount: String(item.amount),
        transactionDate: item.transactionDate,
        note: item.note || '',
        type: item.type,
        categoryId: item.categoryId,
        tagIds: (item.tags || []).map(t => t.id),
        account: item.account || ''
      }))
      uni.navigateTo({
        url: '/pages/addTransaction/addTransaction?mode=edit&data=' + params,
        events: {
          transactionAdded: () => {
            this.refreshData()
          }
        }
      })
    },

    handleDelete(item) {
      uni.showModal({
        title: '提示',
        content: '确定要删除这条流水记录吗？',
        success: async (modalRes) => {
          if (modalRes.confirm) {
            try {
              await transactionApi.delete(item.id)
              uni.showToast({ title: '删除成功', icon: 'success' })
              this.refreshData()
            } catch (e) {
              uni.showToast({ title: e.message || '删除失败', icon: 'none' })
            }
          }
        }
      })
    },

    refreshData() {
      this.currentPage = 0
      this.allTransactions = []
      this.noMoreData = false
      return this.loadData()
    },

    formatDate(dateStr) {
      const date = new Date(dateStr)
      const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      const month = date.getMonth() + 1
      const day = date.getDate()
      const weekday = weekdays[date.getDay()]
      return `${date.getFullYear()}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${weekday}`
    },

    accountIcon(name) {
      const map = {
        '微信钱包': '💬',
        '支付宝': '🔵',
        '银行卡': '💳',
        '现金': '💰'
      }
      return map[name] || '💳'
    }
  }
}
</script>

<style>
.bill-page {
  min-height: 100vh;
  background: #f5f6fa;
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
  padding: 0 20rpx;
}

.nav-back {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  color: #fff;
}

.nav-title {
  font-size: 34rpx;
  font-weight: bold;
  color: #fff;
}

.nav-count {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
}

.summary-header {
  display: flex;
  background: #fff;
  margin: 20rpx 20rpx 0;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.summary-item {
  flex: 1;
  padding: 24rpx 16rpx;
  text-align: center;
  border-right: 1rpx solid #f0f0f0;
}

.summary-item:last-child {
  border-right: none;
}

.summary-item.income {
  background: linear-gradient(180deg, #f0f9eb 0%, #fff 100%);
}

.summary-item.balance {
  background: linear-gradient(180deg, #ecf5ff 0%, #fff 100%);
}

.summary-item.expense {
  background: linear-gradient(180deg, #fef0f0 0%, #fff 100%);
}

.summary-label {
  display: block;
  font-size: 22rpx;
  color: #909399;
  margin-bottom: 8rpx;
}

.summary-value {
  display: block;
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
}

.summary-item.income .summary-value {
  color: #67c23a;
}

.summary-item.balance .summary-value {
  color: #409eff;
}

.summary-item.expense .summary-value {
  color: #f56c6c;
}

.transactions-scroll {
  height: calc(100vh - 330rpx);
  margin-top: 20rpx;
}

.empty-state {
  text-align: center;
  padding: 120rpx 0;
}

.empty-icon {
  font-size: 100rpx;
  display: block;
  margin-bottom: 24rpx;
}

.empty-text {
  color: #909399;
  font-size: 28rpx;
}

.date-group {
  margin: 0 20rpx 16rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.date-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 24rpx;
  background: #fafafa;
  border-bottom: 1rpx solid #f0f0f0;
}

.date {
  font-weight: bold;
  color: #303133;
  font-size: 26rpx;
}

.date-summary {
  font-size: 22rpx;
  color: #909399;
}

.transaction-item {
  display: flex;
  align-items: center;
  padding: 24rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.transaction-item:last-child {
  border-bottom: none;
}

.item-left {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.item-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  flex-shrink: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.avatar-text {
  color: #fff;
  font-size: 28rpx;
  font-weight: 500;
  line-height: 1;
}

.item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.item-row-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.item-category {
  font-size: 28rpx;
  color: #303133;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.item-row-sub {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.item-account {
  display: flex;
  align-items: center;
  font-size: 20rpx;
  color: #67c23a;
  background: rgba(103, 194, 58, 0.1);
  padding: 2rpx 12rpx;
  border-radius: 16rpx;
  flex-shrink: 0;
}

.item-account .account-name {
  font-size: 20rpx;
  line-height: 1;
}

.item-account .account-icon {
  font-size: 20rpx;
  margin-right: 4rpx;
  line-height: 1;
}

.item-note {
  font-size: 22rpx;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
  text-align: right;
}

.item-amount {
  font-size: 32rpx;
  font-weight: bold;
  flex-shrink: 0;
}

.item-amount.expense {
  color: #f56c6c;
}

.item-amount.income {
  color: #409eff;
}

.loading-state {
  text-align: center;
  padding: 40rpx 0;
  color: #909399;
  font-size: 26rpx;
}

.no-more-state {
  text-align: center;
  padding: 30rpx 0;
  color: #c0c4cc;
  font-size: 24rpx;
}

.error-state {
  text-align: center;
  padding: 40rpx 0;
  color: #f56c6c;
  font-size: 26rpx;
}

.loading-skeleton {
  margin: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
}

.skeleton-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.skeleton-item:last-child {
  border-bottom: none;
}

.sk-left {
  display: flex;
  align-items: center;
}

.sk-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
  margin-right: 20rpx;
}

.sk-info {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.sk-line {
  height: 20rpx;
  border-radius: 8rpx;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

.sk-line-short {
  width: 120rpx;
}

.sk-line-long {
  width: 200rpx;
}

.sk-amount {
  height: 28rpx;
  border-radius: 8rpx;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
