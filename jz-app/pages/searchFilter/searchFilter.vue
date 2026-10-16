<template>
  <view class="filter-page">
    <view class="custom-nav">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
      <view class="nav-bar">
        <view class="nav-back" @click="goBack">
          <text>‹</text>
        </view>
        <text class="nav-title">筛选流水</text>
        <view class="nav-reset" @click="resetFilter">
          <text>重置</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="filter-scroll">
      <!-- 时间范围 -->
      <view class="section card">
        <view class="section-title">时间范围</view>
        <view class="range-row">
          <view class="range-field" @click="openDatePicker">
            <text class="range-label">开始日期</text>
            <text :class="['range-value', form.startDate ? '' : 'placeholder']">{{ form.startDate || '请选择' }}</text>
          </view>
          <text class="range-sep">至</text>
          <view class="range-field" @click="openDatePicker">
            <text class="range-label">结束日期</text>
            <text :class="['range-value', form.endDate ? '' : 'placeholder']">{{ form.endDate || '请选择' }}</text>
          </view>
        </view>
        <view class="quick-range">
          <view
            v-for="q in quickRanges"
            :key="q.label"
            class="quick-chip"
            :class="{ active: quickActive === q.label }"
            @click="applyQuickRange(q)"
          >
            <text>{{ q.label }}</text>
          </view>
        </view>
      </view>

      <!-- 金额范围 -->
      <view class="section card">
        <view class="section-title">金额范围</view>
        <view class="amount-range">
          <input
            v-model="form.minAmount"
            type="digit"
            class="amount-input"
            placeholder="最小金额"
            @input="onAmountInput('minAmount', $event)"
          />
          <text class="amount-sep">—</text>
          <input
            v-model="form.maxAmount"
            type="digit"
            class="amount-input"
            placeholder="最大金额"
            @input="onAmountInput('maxAmount', $event)"
          />
        </view>
      </view>

      <!-- 账目类型 -->
      <view class="section card">
        <view class="section-title">账目类型</view>
        <view class="type-tabs">
          <view
            class="type-tab"
            :class="{ active: form.type === 'EXPENSE' }"
            @click="selectType('EXPENSE')"
          >
            支出
          </view>
          <view
            class="type-tab"
            :class="{ active: form.type === 'INCOME' }"
            @click="selectType('INCOME')"
          >
            收入
          </view>
          <view
            class="type-tab"
            :class="{ active: form.type === '' }"
            @click="selectType('')"
          >
            不限
          </view>
        </view>

        <!-- 分类明细：选择类型后加载 -->
        <view v-if="form.type && categories.length" class="sub-categories">
          <view class="sub-title">分类明细（可多选）</view>
          <view class="cat-chip-list">
            <view
              v-for="cat in categories"
              :key="cat.id"
              class="cat-chip"
              :class="{ active: form.categoryIds.includes(cat.id) }"
              @click="toggleCategory(cat.id)"
            >
              <text class="chip-text">{{ cat.icon || '📌' }} {{ cat.name }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 资金账号 -->
      <view class="section card">
        <view class="section-title">资金账号</view>
        <view class="account-list">
          <view
            class="account-chip"
            :class="{ active: form.account === '' }"
            @click="selectAccount('')"
          >
            <text class="chip-text">不限</text>
          </view>
          <view
            v-for="opt in accountOptions"
            :key="opt.name"
            class="account-chip"
            :class="{ active: form.account === opt.name }"
            @click="selectAccount(opt.name)"
          >
            <text class="chip-text">{{ opt.icon }} {{ opt.name }}</text>
          </view>
        </view>
      </view>

      <view class="bottom-space"></view>
    </scroll-view>

    <!-- 底部按钮 -->
    <view class="footer-bar">
      <button class="btn-reset" @click="resetFilter">重置条件</button>
      <button class="btn-confirm" @click="onConfirm">查看筛选结果</button>
    </view>

    <!-- 日期范围选择弹窗 -->
    <view v-if="showDatePicker" class="picker-mask" @click="showDatePicker = false">
      <view class="picker-content date-picker-content" @click.stop>
        <view class="picker-header">
          <text class="picker-title">选择时间范围</text>
          <text class="picker-clear" @click="clearDateRange">清除</text>
        </view>
        <view class="cal-nav">
          <view class="cal-arrow" @click="changeMonth(-1)">‹</view>
          <text class="cal-title">{{ calYear }}年{{ calMonth }}月</text>
          <view class="cal-arrow" @click="changeMonth(1)">›</view>
        </view>
        <view class="cal-weekdays">
          <text v-for="w in weekDays" :key="w" class="cal-weekday">{{ w }}</text>
        </view>
        <view class="cal-grid">
          <view
            v-for="(cell, idx) in calCells"
            :key="idx"
            class="cal-cell"
            :class="cellClass(cell)"
            @click="cell && pickDay(cell)"
          >
            <text v-if="cell">{{ cell }}</text>
          </view>
        </view>
        <view class="cal-tip">
          <text>先选开始日期，再选结束日期</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { categoryApi } from '../../api/index.js';

export default {
  data() {
    const today = new Date()
    return {
      statusBarHeight: 0,
      form: {
        startDate: '',
        endDate: '',
        minAmount: '',
        maxAmount: '',
        type: '',
        categoryIds: [],
        account: ''
      },
      quickActive: '',
      categories: [],
      categoryCache: { EXPENSE: [], INCOME: [] },
      accountOptions: [
        { name: '微信钱包', icon: '💬' },
        { name: '支付宝', icon: '🔵' },
        { name: '银行卡', icon: '💳' },
        { name: '现金', icon: '💰' }
      ],
      weekDays: ['日', '一', '二', '三', '四', '五', '六'],
      quickRanges: [
        { label: '本月', months: 0 },
        { label: '上月', months: 1 },
        { label: '近3月', months: 3 },
        { label: '近半年', months: 6 },
        { label: '今年', months: 12 },
        { label: '全部', months: 9999 }
      ],
      showDatePicker: false,
      calYear: today.getFullYear(),
      calMonth: today.getMonth() + 1,
      // 弹窗内临时选择范围
      rangeStart: '',
      rangeEnd: ''
    }
  },

  computed: {
    calCells() {
      const firstWeekday = new Date(this.calYear, this.calMonth - 1, 1).getDay()
      const days = new Date(this.calYear, this.calMonth, 0).getDate()
      const cells = []
      for (let i = 0; i < firstWeekday; i++) cells.push(null)
      for (let d = 1; d <= days; d++) cells.push(d)
      return cells
    }
  },

  onLoad() {
    const sysInfo = uni.getSystemInfoSync()
    this.statusBarHeight = sysInfo.statusBarHeight || 0
    this.preloadCategories()
  },

  methods: {
    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) })
    },

    async preloadCategories() {
      try {
        const [expense, income] = await Promise.all([
          categoryApi.getCategoriesByType('EXPENSE'),
          categoryApi.getCategoriesByType('INCOME')
        ])
        this.categoryCache.EXPENSE = Array.isArray(expense) ? expense : []
        this.categoryCache.INCOME = Array.isArray(income) ? income : []
        if (this.form.type) {
          this.categories = this.categoryCache[this.form.type] || []
        }
      } catch (e) {
        this.categoryCache = { EXPENSE: [], INCOME: [] }
      }
    },

    resetFilter() {
      this.form = {
        startDate: '',
        endDate: '',
        minAmount: '',
        maxAmount: '',
        type: '',
        categoryIds: [],
        account: ''
      }
      this.categories = []
      this.quickActive = ''
    },

    onAmountInput(field, e) {
      let v = (e.detail && e.detail.value) || ''
      v = v.replace(/[^\d.]/g, '')
      const parts = v.split('.')
      if (parts.length > 2) v = parts[0] + '.' + parts.slice(1).join('')
      if (parts[1] && parts[1].length > 2) v = parts[0] + '.' + parts[1].slice(0, 2)
      this.form[field] = v
    },

    applyQuickRange(q) {
      this.quickActive = q.label
      const now = new Date()
      const end = this.formatDate(now)
      let start
      if (q.months === 0) {
        // 本月
        start = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
      } else if (q.months === 9999) {
        // 全部：不设范围
        this.form.startDate = ''
        this.form.endDate = ''
        this.quickActive = q.label
        return
      } else {
        const d = new Date(now.getFullYear(), now.getMonth() - (q.months - 1), 1)
        start = this.formatDate(d)
      }
      this.form.startDate = start
      this.form.endDate = end
    },

    formatDate(d) {
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    },

    selectType(type) {
      if (this.form.type === type) return
      this.form.type = type
      this.form.categoryIds = []
      this.categories = type ? (this.categoryCache[type] || []) : []
    },

    toggleCategory(id) {
      const idx = this.form.categoryIds.indexOf(id)
      if (idx > -1) this.form.categoryIds.splice(idx, 1)
      else this.form.categoryIds.push(id)
    },

    selectAccount(name) {
      this.form.account = (this.form.account === name) ? '' : name
    },

    // ===== 日期范围弹窗 =====
    openDatePicker() {
      const base = this.form.startDate || this.form.endDate || this.formatDate(new Date())
      const parts = base.split('-')
      this.calYear = parseInt(parts[0], 10)
      this.calMonth = parseInt(parts[1], 10)
      this.rangeStart = this.form.startDate
      this.rangeEnd = this.form.endDate
      this.showDatePicker = true
    },

    changeMonth(delta) {
      let m = this.calMonth + delta
      let y = this.calYear
      if (m < 1) { m = 12; y-- }
      else if (m > 12) { m = 1; y++ }
      this.calMonth = m
      this.calYear = y
    },

    cellClass(cell) {
      if (!cell) return 'empty'
      const ds = `${this.calYear}-${String(this.calMonth).padStart(2, '0')}-${String(cell).padStart(2, '0')}`
      const cls = []
      if (ds === this.rangeStart) cls.push('start')
      if (ds === this.rangeEnd) cls.push('end')
      if (this.rangeStart && this.rangeEnd && ds > this.rangeStart && ds < this.rangeEnd) cls.push('in-range')
      return cls
    },

    pickDay(cell) {
      const ds = `${this.calYear}-${String(this.calMonth).padStart(2, '0')}-${String(cell).padStart(2, '0')}`
      if (!this.rangeStart || (this.rangeStart && this.rangeEnd)) {
        // 重新开始选择
        this.rangeStart = ds
        this.rangeEnd = ''
      } else {
        // 已选开始，选结束
        if (ds < this.rangeStart) {
          this.rangeEnd = this.rangeStart
          this.rangeStart = ds
        } else {
          this.rangeEnd = ds
        }
      }
      this.form.startDate = this.rangeStart
      this.form.endDate = this.rangeEnd
      this.quickActive = ''
      // 选完结束日期后自动关闭
      if (this.rangeStart && this.rangeEnd) {
        setTimeout(() => { this.showDatePicker = false }, 150)
      }
    },

    clearDateRange() {
      this.rangeStart = ''
      this.rangeEnd = ''
      this.form.startDate = ''
      this.form.endDate = ''
      this.quickActive = ''
    },

    onConfirm() {
      if (this.form.minAmount !== '' && this.form.maxAmount !== '' &&
          parseFloat(this.form.minAmount) > parseFloat(this.form.maxAmount)) {
        uni.showToast({ title: '最小金额不能大于最大金额', icon: 'none' })
        return
      }
      const payload = {
        startDate: this.form.startDate,
        endDate: this.form.endDate,
        minAmount: this.form.minAmount,
        maxAmount: this.form.maxAmount,
        type: this.form.type,
        categoryIds: this.form.categoryIds,
        account: this.form.account
      }
      const params = encodeURIComponent(JSON.stringify(payload))
      uni.navigateTo({
        url: '/pages/transactionList/transactionList?filters=' + params
      })
    }
  }
}
</script>

<style>
.filter-page {
  min-height: 100vh;
  background: #f5f6fa;
  display: flex;
  flex-direction: column;
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

.nav-reset {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.9);
  padding: 8rpx 16rpx;
}

.filter-scroll {
  flex: 1;
  height: calc(100vh - 88rpx - 120rpx);
}

.section {
  margin: 20rpx;
  padding: 24rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 20rpx;
}

.range-row {
  display: flex;
  align-items: center;
}

.range-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16rpx 20rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
}

.range-label {
  font-size: 22rpx;
  color: #909399;
  margin-bottom: 8rpx;
}

.range-value {
  font-size: 26rpx;
  color: #303133;
}

.range-value.placeholder {
  color: #c0c4cc;
}

.range-sep {
  padding: 0 20rpx;
  color: #909399;
  font-size: 26rpx;
}

.quick-range {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 20rpx;
}

.quick-chip {
  padding: 10rpx 28rpx;
  background: #f5f5f5;
  border-radius: 28rpx;
  font-size: 24rpx;
  color: #606266;
}

.quick-chip.active {
  background: linear-gradient(135deg, #4a90ff 0%, #3b7ce8 100%);
  color: #fff;
}

.amount-range {
  display: flex;
  align-items: center;
}

.amount-input {
  flex: 1;
  height: 80rpx;
  padding: 0 24rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.amount-sep {
  padding: 0 20rpx;
  color: #909399;
  font-size: 28rpx;
}

.type-tabs {
  display: flex;
  background: #f5f5f5;
  border-radius: 44rpx;
  padding: 8rpx;
}

.type-tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: 44rpx;
  font-size: 28rpx;
  color: #606266;
}

.type-tab.active {
  background: #fff;
  color: #409EFF;
  font-weight: bold;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
}

.sub-categories {
  margin-top: 24rpx;
  border-top: 1rpx solid #f0f0f0;
  padding-top: 20rpx;
}

.sub-title {
  font-size: 26rpx;
  color: #909399;
  margin-bottom: 16rpx;
}

.cat-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.cat-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 64rpx;
  padding: 0 28rpx;
  border-radius: 32rpx;
  background: #fff;
  border: 1rpx solid #d6e3f3;
}

.cat-chip .chip-text {
  font-size: 26rpx;
  color: #3a6db0;
  line-height: 1;
  white-space: nowrap;
}

.cat-chip.active {
  background: linear-gradient(135deg, #4a90ff 0%, #3b7ce8 100%);
  border-color: transparent;
}

.cat-chip.active .chip-text {
  color: #fff;
  font-weight: 600;
}

.sub-loading {
  margin-top: 20rpx;
  font-size: 26rpx;
  color: #909399;
}

.account-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.account-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 64rpx;
  padding: 0 28rpx;
  border-radius: 32rpx;
  background: #fff;
  border: 1rpx solid #d6e3f3;
}

.account-chip .chip-text {
  font-size: 26rpx;
  color: #3a6db0;
  line-height: 1;
  white-space: nowrap;
}

.account-chip.active {
  background: linear-gradient(135deg, #4a90ff 0%, #3b7ce8 100%);
  border-color: transparent;
}

.account-chip.active .chip-text {
  color: #fff;
  font-weight: 600;
}

.bottom-space {
  height: 40rpx;
}

.footer-bar {
  height: 120rpx;
  padding: 16rpx 20rpx;
  background: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.btn-reset {
  width: 220rpx;
  height: 88rpx;
  line-height: 88rpx;
  background: #f5f5f5;
  color: #606266;
  border: none;
  border-radius: 44rpx;
  font-size: 30rpx;
}

.btn-reset::after {
  border: none;
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

.btn-confirm::after {
  border: none;
}

/* 日期弹窗 */
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
  width: 640rpx;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
}

.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.picker-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}

.picker-clear {
  font-size: 26rpx;
  color: #f56c6c;
}

.cal-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx 24rpx 16rpx;
}

.cal-arrow {
  width: 64rpx;
  height: 64rpx;
  line-height: 64rpx;
  text-align: center;
  font-size: 40rpx;
  color: #606266;
  border-radius: 50%;
  background: #f5f5f5;
}

.cal-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}

.cal-weekdays {
  display: flex;
  padding: 0 12rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.cal-weekday {
  flex: 1;
  text-align: center;
  font-size: 24rpx;
  color: #909399;
  padding: 12rpx 0;
}

.cal-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 12rpx;
}

.cal-cell {
  width: 14.28%;
  height: 76rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  color: #303133;
  box-sizing: border-box;
}

.cal-cell.empty {
  visibility: hidden;
}

.cal-cell.start text,
.cal-cell.end text {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-weight: bold;
}

.cal-cell.in-range {
  background: rgba(102, 126, 234, 0.12);
}

.cal-tip {
  text-align: center;
  font-size: 22rpx;
  color: #c0c4cc;
  padding: 0 0 24rpx;
}
</style>
