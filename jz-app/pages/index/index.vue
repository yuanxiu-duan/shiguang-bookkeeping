<template>
  <view class="bill-page">
    <view class="custom-nav">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
      <view class="nav-bar">
        <text class="nav-title">记账</text>
        <view class="nav-right" @click="goToAccountBooks">
          <text class="nav-switch">{{ currentBookName }} ›</text>
        </view>
      </view>
    </view>

    <view class="search-bar" @click="goToSearch">
      <text class="search-icon">🔍</text>
      <text class="search-placeholder">搜索账单</text>
      <text class="search-arrow">›</text>
    </view>

    <view class="summary-header">
      <view class="summary-item income">
        <text class="summary-label">{{ currentYearMonth }}总收入</text>
        <text v-if="summaryLoading" class="summary-value skeleton-text">---</text>
        <text v-else class="summary-value">{{ monthlySummary.income.toFixed(2) }}</text>
      </view>
      <view class="summary-item balance">
        <text class="summary-label">{{ currentYearMonth }}总结余</text>
        <text v-if="summaryLoading" class="summary-value skeleton-text">---</text>
        <text v-else class="summary-value">{{ monthlySummary.balance.toFixed(2) }}</text>
      </view>
      <view class="summary-item expense">
        <text class="summary-label">{{ currentYearMonth }}总支出</text>
        <text v-if="summaryLoading" class="summary-value skeleton-text">---</text>
        <text v-else class="summary-value">{{ monthlySummary.expense.toFixed(2) }}</text>
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
        <text class="empty-icon">📝</text>
        <text class="empty-text">暂无账单记录</text>
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
          hover-class="transaction-item-hover"
          :hover-stay-time="80"
          @click="viewDetail(item)"
          @touchstart="onItemTouchStart(item, $event)"
          @touchmove="onItemTouchMove"
          @touchend="onItemTouchEnd"
          @touchcancel="onItemTouchEnd"
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

    <view class="fab-button" @click="goToAddTransaction">
      <text class="fab-icon">+</text>
    </view>

    <!-- 统一确认弹窗（替代系统 uni.showModal） -->
    <confirm-dialog ref="confirmDialog" />
  </view>
</template>

<script>
import { accountBookApi, getMonthlySummary, transactionApi } from '../../api/index.js';
import { storage } from '../../utils/storage.js';
import { toCents } from '../../utils/money.js';
import { withGuard } from '../../utils/guard.js';
import ConfirmDialog from '../../components/ConfirmDialog.vue';

export default {
  components: { ConfirmDialog },
  data() {
    const now = new Date()
    return {
      statusBarHeight: 0,
      loading: false,
      firstLoad: true,
      loadError: false,
      noMoreData: false,
      deleting: false,
      currentBookName: '默认账本',
      transactions: [],
      allTransactions: [],
      currentPage: 0,
      pageSize: 20,
      totalElements: 0,
      currentYear: now.getFullYear(),
      currentMonth: now.getMonth() + 1,
      monthlySummary: {
        income: 0,
        expense: 0,
        balance: 0
      },
      summaryLoading: false,
      currentUser: null
    }
  },

  computed: {
    currentYearMonth() {
      return `${this.currentYear}年${this.currentMonth}月`
    },

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

  async onShow() {
    this.getStatusBarHeight()
    this.checkAuth()
    this.loadCurrentUser()
    await this.ensureAccountBookId()
    
    this.currentPage = 0
    this.allTransactions = []
    this.noMoreData = false
    this.firstLoad = true
    
    await Promise.all([
      this.loadData(),
      this.loadSummary()
    ])
    this.firstLoad = false
    this.loadCurrentBookName()
  },

  onPullDownRefresh() {
    Promise.all([
      this.refreshData(),
      this.loadSummary()
    ]).finally(() => {
      uni.stopPullDownRefresh()
    })
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

    async ensureAccountBookId() {
      const bookId = storage.get('currentAccountBookId')
      if (bookId) return
      try {
        const books = await accountBookApi.getList()
        if (books && books.length > 0) {
          const defaultBook = books.find(b => b.isDefault) || books[0]
          storage.set('currentAccountBookId', defaultBook.id)
        }
      } catch (e) {
        // 未登录/ token 失效导致的 401 属启动期正常现象，不刷 error 日志
        if (e && e.message && e.message.includes('未授权')) {
          console.warn('ensureAccountBookId: 未登录，等待授权后重试')
        } else {
          console.error('ensureAccountBookId error:', e)
        }
      }
    },

    goToAccountBooks() {
      uni.navigateTo({ url: '/pages/accountBooks/accountBooks' })
    },

    goToSearch() {
      uni.navigateTo({ url: '/pages/searchFilter/searchFilter' })
    },

    async loadSummary() {
      this.summaryLoading = true
      try {
        const monthStr = `${this.currentYear}-${String(this.currentMonth).padStart(2, '0')}`
        const res = await getMonthlySummary({ month: monthStr })
        
        if (res && typeof res === 'object') {
          this.monthlySummary = {
            income: Number(res.income || 0),
            expense: Number(res.expense || 0),
            balance: Number(res.balance || 0)
          }
        }
      } catch (e) {
        console.error('加载统计数据失败', e)
      } finally {
        this.summaryLoading = false
      }
    },

    async loadData() {
      if (this.loading) return
      
      this.loading = true
      this.loadError = false
      
      try {
        const res = await transactionApi.getAllPaged(this.currentPage, this.pageSize)
        
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
        }
      } catch (e) {
        console.error('加载账单失败', e)
        this.loadError = true
      } finally {
        this.loading = false
      }
    },
    
    async refreshData() {
      this.currentPage = 0
      this.noMoreData = false
      this.allTransactions = []
      return this.loadData()
    },

    loadMore() {
      if (this.loading || this.noMoreData) return
      this.loadData()
    },
    
    retryLoad() {
      this.loadData()
    },

    formatDate(dateStr) {
      const date = new Date(dateStr)
      const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      const month = date.getMonth() + 1
      const day = date.getDate()
      const weekday = weekdays[date.getDay()]
      return `${date.getFullYear()}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${weekday}`
    },

    viewDetail(item) {
      // 长按已经弹出操作菜单，抬手时系统补发的 click 直接忽略，避免菜单弹两次
      if (this._pressFired) {
        this._pressFired = false
        return
      }
      this.showItemActions(item)
    },

    loadCurrentUser() {
      this.currentUser = storage.get('user_info')
    },

    /* ---------- 长按：自己计时，不依赖 uni 的 longpress ----------
       Android WebView 里 @longpress 常被文本选择/滑动手势抢走，表现为「长按没反应」。
       这里用 touchstart/touchmove/touchend 自己判定：按住 420ms 且位移 < 12px 才算长按。 */
    onItemTouchStart(item, e) {
      this.clearItemPress()
      const t = (e.touches && e.touches[0]) || (e.changedTouches && e.changedTouches[0])
      if (!t) return
      this._pressStart = {
        x: t.clientX != null ? t.clientX : t.pageX,
        y: t.clientY != null ? t.clientY : t.pageY
      }
      this._pressFired = false
      this._pressTimer = setTimeout(() => {
        this._pressTimer = null
        this._pressStart = null
        this._pressFired = true
        // 长按生效给一下轻震动反馈，让用户确认按到了
        try { uni.vibrateShort && uni.vibrateShort({}) } catch (err) {}
        this.showItemActions(item)
      }, 420)
    },

    onItemTouchMove(e) {
      if (!this._pressStart) return
      const t = (e.touches && e.touches[0]) || (e.changedTouches && e.changedTouches[0])
      if (!t) return
      const x = t.clientX != null ? t.clientX : t.pageX
      const y = t.clientY != null ? t.clientY : t.pageY
      // 手指移动超过阈值即视为滑动列表，取消长按
      if (Math.abs(x - this._pressStart.x) > 12 || Math.abs(y - this._pressStart.y) > 12) {
        this.clearItemPress()
      }
    },

    onItemTouchEnd() {
      this.clearItemPress()
    },

    clearItemPress() {
      this._pressStart = null
      if (this._pressTimer) {
        clearTimeout(this._pressTimer)
        this._pressTimer = null
      }
    },

    async showItemActions(item) {
      if (!item || !item.id) return
      // 上一个操作菜单还没关闭时忽略新触发，避免菜单叠加/错乱
      if (this._actionSheetOpen) return

      const currentUserId = this.currentUser?.id
      // 列表没带 userId（或本地还没拿到登录用户）时不做拦截，交给后端校验
      const isOwn = item.userId == null || currentUserId == null ||
        String(item.userId) === String(currentUserId)
      // 检查是否是账本所有者
      let isBookOwner = false
      if (!isOwn) {
        try {
          const books = await accountBookApi.getList()
          const bookId = storage.get('currentAccountBookId')
          const currentBook = books.find(b => String(b.id) === String(bookId))
          isBookOwner = !!(currentBook && String(currentBook.userId) === String(currentUserId))
        } catch (e) {
          // 拉不到账本信息时不要静默拦掉（以前静默 return，长按看起来就像没反应）
          isBookOwner = true
        }
      }

      if (!isOwn && !isBookOwner) {
        uni.showToast({ title: '只能编辑自己记录的流水', icon: 'none' })
        return
      }

      this._actionSheetOpen = true
      uni.showActionSheet({
        itemList: ['编辑', '删除'],
        success: (res) => {
          if (res.tapIndex === 0) {
            this.openEditDialog(item)
          } else if (res.tapIndex === 1) {
            this.handleDelete(item)
          }
        },
        complete: () => {
          this._actionSheetOpen = false
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
            this.loadSummary()
          }
        }
      })
    },

    async handleDelete(item) {
      if (this.deleting) return
      const ok = await this.$refs.confirmDialog.open({
        title: '删除流水',
        content: '删除后无法恢复，确定删除这条记录吗？',
        danger: true,
        confirmText: '删除'
      })
      if (!ok) return
      return withGuard('index:delete:' + item.id, async () => {
        this.deleting = true
        try {
          await transactionApi.delete(item.id)
          uni.showToast({ title: '删除成功', icon: 'success' })
          this.refreshData()
          this.loadSummary()
        } catch (e) {
          uni.showToast({ title: e.message || '删除失败', icon: 'none' })
        } finally {
          this.deleting = false
        }
      }, 800)
    },

    goToAddTransaction() {
      uni.navigateTo({ 
        url: '/pages/addTransaction/addTransaction',
        events: {
          transactionAdded: () => {
            this.refreshData()
            this.loadSummary()
          }
        }
      })
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

.summary-header {
  display: flex;
  background: #fff;
  margin: 20rpx 20rpx 0;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.search-bar {
  display: flex;
  align-items: center;
  margin: 20rpx 20rpx 0;
  padding: 0 24rpx;
  height: 76rpx;
  background: #fff;
  border-radius: 38rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.search-icon {
  font-size: 28rpx;
  margin-right: 12rpx;
}

.search-placeholder {
  flex: 1;
  font-size: 26rpx;
  color: #909399;
}

.search-arrow {
  font-size: 36rpx;
  color: #c0c4cc;
  line-height: 1;
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

.skeleton-text {
  color: transparent;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
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
  height: calc(100vh - 430rpx);
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
  /* 关键：长按不允许选中文字。否则 Android/iOS WebView 的原生长按（选词/呼出菜单）
     会把手势吃掉，长按事件就不触发了 —— 这是「长按很难触发」的主因之一 */
  -webkit-user-select: none;
  user-select: none;
  -webkit-touch-callout: none;
  transition: background-color 0.15s ease;
}

/* 按下/长按过程中的视觉反馈，让用户知道按到了 */
.transaction-item-hover {
  background-color: #f2f4f7;
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

/* 用户头像：替代原分类图标 */
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

/* 第一行：分类名（左） + 金额（右） */
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

/* 第二行：账户胶囊（左） + 备注（右） */
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

.fab-button {
  position: fixed;
  right: 40rpx;
  bottom: 180rpx;
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 8rpx 24rpx rgba(102, 126, 234, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  transition: transform 0.2s;
}

.fab-button:active {
  transform: scale(0.95);
}

.fab-icon {
  font-size: 56rpx;
  color: #fff;
  font-weight: 300;
  margin-top: -6rpx;
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

/* 编辑弹窗样式 */
.picker-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.picker-content {
  width: 600rpx;
  background: #fff;
  border-radius: 32rpx;
  overflow: hidden;
}

.picker-header {
  padding: 30rpx;
  text-align: center;
  border-bottom: 1rpx solid #f0f0f0;
}

.picker-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
}

.edit-form {
  padding: 20rpx 30rpx;
}

/* 与 budget.vue 弹窗完全对齐的可工作样式 —— H5 端 input 必须显式 height，否则不可聚焦 */
.add-tag-form {
  padding: 20rpx 30rpx;
}

.tag-name-input {
  width: 100%;
  height: 80rpx;
  padding: 0 20rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}
</style>
