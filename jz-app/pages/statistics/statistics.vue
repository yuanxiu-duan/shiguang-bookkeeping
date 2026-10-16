<template>
  <view class="statistics-page">
    <view class="custom-nav">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
      <view class="nav-bar">
        <text class="nav-title">统计</text>
        <view class="nav-right" @click="goToAccountBooks">
          <text class="nav-switch">{{ currentBookName }} ›</text>
        </view>
      </view>
    </view>

    <view class="mode-selector">
      <view 
        class="mode-tab" 
        :class="{ active: timeMode === 'month' }"
        @click="switchMode('month')"
      >
        按月
      </view>
      <view 
        class="mode-tab" 
        :class="{ active: timeMode === 'year' }"
        @click="switchMode('year')"
      >
        按年
      </view>
      <view
        class="mode-tab"
        :class="{ active: timeMode === 'all' }"
        @click="switchMode('all')"
      >
        全部
      </view>
    </view>

    <view v-if="timeMode === 'month'" class="month-selector">
      <view class="month-btn" @click="changeMonth(-1)">
        <text>◀</text>
      </view>
      <text class="current-period">{{ currentYear }}年{{ currentMonthNum }}月</text>
      <view class="month-btn" @click="changeMonth(1)">
        <text>▶</text>
      </view>
    </view>

    <view v-else-if="timeMode === 'year'" class="year-selector">
      <view class="month-btn" @click="changeYear(-1)">
        <text>◀</text>
      </view>
      <picker mode="selector" :range="yearOptions" :value="yearOptionIndex" @change="onYearPickerChange">
        <view class="current-period">
          <text>{{ currentYear }}年</text>
          <text class="picker-arrow">▼</text>
        </view>
      </picker>
      <view class="month-btn" @click="changeYear(1)">
        <text>▶</text>
      </view>
    </view>

    <view class="summary-cards">
      <view class="summary-card income">
        <text class="label">{{ timeMode === 'month' ? '本月收入' : timeMode === 'year' ? '本年收入' : '全部收入' }}</text>
        <text v-if="isLoading" class="value skeleton-text">---</text>
        <text v-else class="value">¥{{ stats.totalIncome || 0 }}</text>
      </view>
      <view class="summary-card expense">
        <text class="label">{{ timeMode === 'month' ? '本月支出' : timeMode === 'year' ? '本年支出' : '全部支出' }}</text>
        <text v-if="isLoading" class="value skeleton-text">---</text>
        <text v-else class="value">¥{{ stats.totalExpense || 0 }}</text>
      </view>
      <view class="summary-card balance" :class="{ negative: (stats.balance || 0) < 0 }">
        <text class="label">结余</text>
        <text v-if="isLoading" class="value skeleton-text">---</text>
        <text v-else class="value">¥{{ stats.balance || 0 }}</text>
      </view>
    </view>

    <view v-if="timeMode === 'month'" class="calendar-section card">
      <view class="section-header">
        <text>月度日历</text>
        <view class="legend-inline">
          <view class="legend-dot expense"></view>
          <text class="legend-text">支出</text>
          <view class="legend-dot income"></view>
          <text class="legend-text">收入</text>
        </view>
      </view>

      <view v-if="isLoading" class="calendar-skeleton">
        <view class="sk-cal-row" v-for="i in 6" :key="'skcr-'+i">
          <view class="sk-cal-cell" v-for="j in 7" :key="'skcc-'+i+'-'+j"></view>
        </view>
      </view>
      <view v-else class="calendar-grid">
        <view class="calendar-weekday" v-for="(w, i) in weekdays" :key="i">
          <text>{{ w }}</text>
        </view>
        <view
          v-for="(cell, i) in monthCalendarCells"
          :key="i"
          class="calendar-cell"
          :class="{ empty: !cell.day, today: cell.isToday, 'has-data': cell.hasData }"
          @click="onCalendarDayClick(cell)"
        >
          <text v-if="cell.day" class="cell-day">{{ cell.day }}</text>
          <text v-if="cell.day && cell.expense > 0" class="cell-amount cell-expense">-{{ formatFull(cell.expense) }}</text>
          <text v-if="cell.day && cell.income > 0" class="cell-amount cell-income">+{{ formatFull(cell.income) }}</text>
        </view>
      </view>
    </view>

    <view v-else-if="timeMode === 'year'" class="calendar-section card">
      <view class="section-header">
        <text>年度月历</text>
        <view class="legend-inline">
          <view class="legend-dot expense"></view>
          <text class="legend-text">支出</text>
          <view class="legend-dot income"></view>
          <text class="legend-text">收入</text>
        </view>
      </view>

      <view v-if="isLoading" class="calendar-skeleton">
        <view class="sk-cal-row" v-for="i in 3" :key="'skcr-'+i">
          <view class="sk-cal-cell sk-cal-cell-lg" v-for="j in 3" :key="'skcc-'+i+'-'+j"></view>
        </view>
      </view>
      <view v-else class="year-grid">
        <view
          v-for="(m, i) in yearCalendarCells"
          :key="i"
          class="year-cell"
          @click="onCalendarMonthClick(m)"
        >
          <view class="year-cell-inner" :class="{ 'has-data': m.hasData }">
            <text class="cell-month">{{ m.label }}</text>
            <text v-if="m.income > 0" class="cell-amount cell-income">收入 ¥{{ formatFull(m.income) }}</text>
            <text v-if="m.expense > 0" class="cell-amount cell-expense">支出 ¥{{ formatFull(m.expense) }}</text>
            <text v-if="!m.hasData" class="cell-amount cell-empty">无数据</text>
          </view>
        </view>
      </view>
    </view>

    <view v-else class="calendar-section card">
      <view class="section-header">
        <text>年度汇总</text>
        <view class="legend-inline">
          <view class="legend-dot expense"></view>
          <text class="legend-text">支出</text>
          <view class="legend-dot income"></view>
          <text class="legend-text">收入</text>
        </view>
      </view>

      <view v-if="isLoading" class="calendar-skeleton">
        <view class="sk-year-row" v-for="i in 3" :key="'skyr-'+i"></view>
      </view>
      <view v-else-if="allYearCells.length === 0" class="empty-chart">
        <text>暂无数据</text>
      </view>
      <view v-else class="year-list">
        <view
          v-for="(y, i) in allYearCells"
          :key="i"
          class="year-row"
          @click="onCalendarYearClick(y)"
        >
          <view class="year-row-label">
            <text class="year-label">{{ y.label }}年</text>
            <text class="year-balance" :class="{ negative: y.balance < 0 }">结余 ¥{{ formatFull(y.balance) }}</text>
          </view>
          <view class="year-row-amounts">
            <text class="cell-income">收入 ¥{{ formatFull(y.income) }}</text>
            <text class="cell-expense">支出 ¥{{ formatFull(y.expense) }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="chart-section card">
      <view class="section-header">
        <text>收支曲线</text>
        <view class="legend-inline">
          <view class="legend-dot expense"></view>
          <text class="legend-text">支出</text>
          <view class="legend-dot income"></view>
          <text class="legend-text">收入</text>
        </view>
      </view>
      
      <view v-if="isLoading" class="chart-skeleton">
        <view class="sk-chart-bar" v-for="i in 4" :key="'skbar-'+i" :style="{ width: (60 + i*15) + '%' }"></view>
      </view>
      <view v-else-if="curveData.length === 0" class="empty-chart">
        <text>暂无数据</text>
      </view>
      
      <view v-if="!isLoading && curveData.length > 0" class="chart-container">
        <canvas 
          canvas-id="curveChart" 
          id="curveChart"
          class="chart-canvas"
          :style="{ height: curveChartHeight + 'px' }"
        ></canvas>
      </view>
    </view>

    <view class="chart-section card">
      <view class="section-title">
        <text>{{ chartType === 'expense' ? '支出分类' : '收入分类' }}</text>
        <view class="type-switch">
          <text 
            :class="{ active: chartType === 'expense' }"
            @click="chartType = 'expense'"
          >支出</text>
          <text 
            :class="{ active: chartType === 'income' }"
            @click="chartType = 'income'"
          >收入</text>
        </view>
      </view>
      
      <view v-if="isLoading" class="pie-skeleton">
        <view class="sk-pie-circle"></view>
        <view class="sk-pie-legend">
          <view class="sk-pie-line" v-for="i in 4" :key="'skpie-'+i"></view>
        </view>
      </view>
      <view v-else-if="chartData.length === 0" class="empty-chart">
        <text>暂无数据</text>
      </view>
      
      <view v-if="!isLoading && chartData.length > 0" class="pie-chart">
        <view class="pie-visual" :style="{ background: pieGradient }">
          <view class="pie-center">
            <text class="pie-total">¥{{ totalAmount.toFixed(2) }}</text>
            <text class="pie-label">总计</text>
          </view>
        </view>
        <view class="pie-legend">
          <view
            v-for="(item, index) in chartData"
            :key="item.categoryId"
            class="legend-item"
            @click="onCategoryClick(item)"
          >
            <view class="legend-dot" :style="{ backgroundColor: colors[index % colors.length] }"></view>
            <text class="legend-name">{{ item.categoryIcon || '📌' }} {{ item.categoryName }}</text>
            <text class="legend-value">¥{{ item.amount }}</text>
            <text class="legend-percent">{{ item.percentage }}%</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { accountBookApi, statisticsApi } from '../../api/index.js';
import { sumAmounts } from '../../utils/money.js';
import { storage } from '../../utils/storage.js';

export default {
  data() {
    const now = new Date()
    
    return {
      statusBarHeight: 0,
      timeMode: 'month',
      currentYear: now.getFullYear(),
      currentMonthNum: now.getMonth() + 1,
      yearOptions: [],
      yearOptionIndex: 0,
      chartType: 'expense',
      stats: {},
      curveData: [],
      currentBookName: '默认账本',
      colors: ['#667eea', '#f56c6c', '#67c23a', '#e6a23c', '#909399', '#409eff', '#f06292', '#00c853'],
      curveChartHeight: 240,
      isLoading: false
    }
  },
  
  computed: {
    monthParam() {
      const month = String(this.currentMonthNum).padStart(2, '0')
      return `${this.currentYear}-${month}`
    },

    weekdays() {
      return ['日', '一', '二', '三', '四', '五', '六']
    },

    monthCalendarCells() {
      const year = this.currentYear
      const month = this.currentMonthNum
      const firstWeekday = new Date(year, month - 1, 1).getDay()
      const daysInMonth = new Date(year, month, 0).getDate()

      const dailyMap = {}
      const dailyTrend = (this.stats && this.stats.dailyTrend) || []
      dailyTrend.forEach(d => {
        dailyMap[d.date] = {
          expense: Number(d.expense || 0),
          income: Number(d.income || 0)
        }
      })

      const now = new Date()
      const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`

      const cells = []
      for (let i = 0; i < firstWeekday; i++) {
        cells.push({ day: null })
      }
      for (let d = 1; d <= daysInMonth; d++) {
        const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
        const data = dailyMap[dateStr] || { expense: 0, income: 0 }
        cells.push({
          day: d,
          date: dateStr,
          expense: data.expense,
          income: data.income,
          isToday: dateStr === todayStr,
          hasData: data.expense > 0 || data.income > 0
        })
      }
      while (cells.length % 7 !== 0) {
        cells.push({ day: null })
      }
      return cells
    },

    yearCalendarCells() {
      const monthlyTrend = (this.stats && this.stats.monthlyTrend) || []
      const monthMap = {}
      monthlyTrend.forEach(m => {
        monthMap[m.month] = {
          expense: Number(m.expense || 0),
          income: Number(m.income || 0)
        }
      })

      const cells = []
      for (let m = 1; m <= 12; m++) {
        const monthStr = `${this.currentYear}-${String(m).padStart(2, '0')}`
        const data = monthMap[monthStr] || { expense: 0, income: 0 }
        cells.push({
          month: m,
          label: m + '月',
          year: this.currentYear,
          expense: data.expense,
          income: data.income,
          hasData: data.expense > 0 || data.income > 0
        })
      }
      return cells
    },

    allYearCells() {
      if (this.timeMode !== 'all') return []
      return (this.curveData || []).map(d => ({
        label: d.label,
        year: parseInt(d.label),
        expense: Number(d.expense || 0),
        income: Number(d.income || 0),
        balance: Number(d.income || 0) - Number(d.expense || 0)
      }))
    },
    
    chartData() {
      if (this.chartType === 'expense') {
        return this.stats.expenseByCategory || []
      }
      return this.stats.incomeByCategory || []
    },
    
    totalAmount() {
      return sumAmounts(this.chartData, item => item.amount)
    },
    
    pieGradient() {
      if (this.chartData.length === 0) return 'conic-gradient(#e0e0e0 0deg 360deg)'
      const total = this.totalAmount
      if (total === 0) return 'conic-gradient(#e0e0e0 0deg 360deg)'
      let currentDeg = 0
      const stops = this.chartData.map((item, index) => {
        const percent = Number(item.amount) / total
        const startDeg = currentDeg
        currentDeg += percent * 360
        const color = this.colors[index % this.colors.length]
        return `${color} ${startDeg}deg ${currentDeg}deg`
      })
      return `conic-gradient(${stops.join(', ')})`
    }
  },
  
  onShow() {
    this.getStatusBarHeight()
    this.checkAuth()
    this.initYearOptions()
    this.loadData()
    this.loadCurrentBookName()
  },

  onReady() {
    this.$nextTick(() => {
      this.drawCurveChart()
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

    initYearOptions() {
      const now = new Date()
      const options = []
      for (let i = 0; i < 10; i++) {
        options.push(now.getFullYear() - i + '年')
      }
      this.yearOptions = options
      this.yearOptionIndex = 0
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

    switchMode(mode) {
      if (this.timeMode === mode) return
      this.timeMode = mode
      this.loadData()
    },
    
    changeMonth(delta) {
      let newMonth = this.currentMonthNum + delta
      let newYear = this.currentYear
      
      if (newMonth > 12) {
        newMonth = 1
        newYear++
      } else if (newMonth < 1) {
        newMonth = 12
        newYear--
      }
      
      this.currentMonthNum = newMonth
      this.currentYear = newYear
      this.loadData()
    },

    changeYear(delta) {
      this.currentYear += delta
      this.updateYearOptionIndex()
      this.loadData()
    },

    onYearPickerChange(e) {
      const index = e.detail.value
      const yearStr = this.yearOptions[index]
      this.currentYear = parseInt(yearStr.replace('年', ''))
      this.yearOptionIndex = index
      this.loadData()
    },

    updateYearOptionIndex() {
      const idx = this.yearOptions.findIndex(y => y === this.currentYear + '年')
      if (idx >= 0) {
        this.yearOptionIndex = idx
      }
    },
    
    async loadData() {
      try {
        this.isLoading = true

        if (this.timeMode === 'month') {
          this.stats = await statisticsApi.getMonthly(this.monthParam)
        } else if (this.timeMode === 'year') {
          this.stats = await statisticsApi.getYearly(this.currentYear)
        } else {
          this.stats = await statisticsApi.getAllTime()
        }

        const params = { mode: this.timeMode }
        if (this.timeMode === 'month') {
          params.month = this.monthParam
        } else if (this.timeMode === 'year') {
          params.year = this.currentYear
        }

        this.curveData = await statisticsApi.getCurveData(this.timeMode, params.month, params.year)
      } catch (e) {
        console.error(e)
      } finally {
        this.isLoading = false
        this.$nextTick(() => {
          this.drawCurveChart()
        })
      }
    },

    drawCurveChart() {
      if (!this.curveData || this.curveData.length === 0) return
      
      const ctx = uni.createCanvasContext('curveChart', this)
      const data = this.curveData
      const padding = { top: 20, right: 15, bottom: 30, left: 45 }
      const canvasWidth = 340
      const canvasHeight = this.curveChartHeight
      
      const width = canvasWidth - padding.left - padding.right
      const height = canvasHeight - padding.top - padding.bottom
      
      const allValues = data.flatMap(d => [Number(d.income || 0), Number(d.expense || 0)])
      const maxValue = Math.max(...allValues, 100)
      const yMax = Math.ceil(maxValue * 1.15 / 10) * 10
      const yStep = yMax / 4
      
      ctx.clearRect(0, 0, canvasWidth, canvasHeight)
      
      ctx.setStrokeStyle('#e8e8e8')
      ctx.setLineWidth(0.5)
      for (let i = 0; i <= 4; i++) {
        const y = padding.top + (height / 4) * i
        ctx.moveTo(padding.left, y)
        ctx.lineTo(padding.left + width, y)
        ctx.stroke()
        
        ctx.setFillStyle('#999')
        ctx.setFontSize(10)
        ctx.setTextAlign('right')
        const value = yMax - yStep * i
        ctx.fillText(value >= 10000 ? (value / 10000).toFixed(1) + 'w' : value >= 1000 ? (value / 1000).toFixed(1) + 'k' : value.toFixed(0), padding.left - 5, y + 3)
      }
      
      ctx.setFillStyle('#999')
      ctx.setFontSize(9)
      ctx.setTextAlign('center')
      const stepX = width / Math.max(data.length - 1, 1)
      data.forEach((item, i) => {
        const x = padding.left + stepX * i
        
        if (data.length <= 7) {
          ctx.fillText(item.label, x, canvasHeight - 8)
        } else if (i % Math.ceil(data.length / 6) === 0) {
          ctx.fillText(item.label, x, canvasHeight - 8)
        }
      })

      const getX = (i) => padding.left + stepX * i
      const getY = (value) => padding.top + height - (value / yMax) * height
      
      const expensePath = data.map((item, i) => ({ x: getX(i), y: getY(Number(item.expense || 0)) }))
      const incomePath = data.map((item, i) => ({ x: getX(i), y: getY(Number(item.income || 0)) }))
      
      ctx.setLineWidth(2)
      
      ctx.beginPath()
      ctx.setStrokeStyle('#f56c6c')
      ctx.moveTo(expensePath[0].x, expensePath[0].y)
      for (let i = 1; i < expensePath.length; i++) {
        ctx.lineTo(expensePath[i].x, expensePath[i].y)
      }
      ctx.stroke()
      
      ctx.beginPath()
      ctx.setStrokeStyle('#409eff')
      ctx.moveTo(incomePath[0].x, incomePath[0].y)
      for (let i = 1; i < incomePath.length; i++) {
        ctx.lineTo(incomePath[i].x, incomePath[i].y)
      }
      ctx.stroke()

      ctx.setLineWidth(3)
      expensePath.forEach(p => {
        ctx.beginPath()
        ctx.setStrokeStyle('#f56c6c')
        ctx.arc(p.x, p.y, 2.5, 0, 2 * Math.PI)
        ctx.stroke()
        ctx.setFillStyle('#fff')
        ctx.fill()
      })

      incomePath.forEach(p => {
        ctx.beginPath()
        ctx.setStrokeStyle('#409eff')
        ctx.arc(p.x, p.y, 2.5, 0, 2 * Math.PI)
        ctx.stroke()
        ctx.setFillStyle('#fff')
        ctx.fill()
      })
      
      ctx.draw()
    },

    formatFull(value) {
      const v = Number(value || 0)
      if (v === 0) return '0'
      const sign = v < 0 ? '-' : ''
      let str = Math.abs(v).toFixed(2)
      if (str.indexOf('.') >= 0) {
        str = str.replace(/0+$/, '').replace(/\.$/, '')
      }
      const parts = str.split('.')
      parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
      return sign + parts.join('.')
    },

    onCalendarDayClick(cell) {
      if (!cell.day) return
      const filters = { startDate: cell.date, endDate: cell.date }
      const params = encodeURIComponent(JSON.stringify(filters))
      uni.navigateTo({
        url: '/pages/transactionList/transactionList?filters=' + params
      })
    },

    onCalendarMonthClick(m) {
      const y = m.year
      const monthStr = String(m.month).padStart(2, '0')
      const startDate = `${y}-${monthStr}-01`
      const lastDay = new Date(y, m.month, 0).getDate()
      const endDate = `${y}-${monthStr}-${String(lastDay).padStart(2, '0')}`
      const filters = { startDate, endDate }
      const params = encodeURIComponent(JSON.stringify(filters))
      uni.navigateTo({
        url: '/pages/transactionList/transactionList?filters=' + params
      })
    },

    onCalendarYearClick(y) {
      const filters = {
        startDate: `${y.year}-01-01`,
        endDate: `${y.year}-12-31`
      }
      const params = encodeURIComponent(JSON.stringify(filters))
      uni.navigateTo({
        url: '/pages/transactionList/transactionList?filters=' + params
      })
    },

    onCategoryClick(item) {
      let startDate, endDate
      if (this.timeMode === 'month') {
        const y = this.currentYear
        const m = this.currentMonthNum
        startDate = `${y}-${String(m).padStart(2, '0')}-01`
        const lastDay = new Date(y, m, 0).getDate()
        endDate = `${y}-${String(m).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
      } else if (this.timeMode === 'year') {
        startDate = `${this.currentYear}-01-01`
        endDate = `${this.currentYear}-12-31`
      } else {
        startDate = '1970-01-01'
        const now = new Date()
        endDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
      }
      const filters = {
        startDate,
        endDate,
        type: this.chartType === 'expense' ? 'EXPENSE' : 'INCOME',
        categoryIds: [item.categoryId]
      }
      const params = encodeURIComponent(JSON.stringify(filters))
      uni.navigateTo({
        url: '/pages/transactionList/transactionList?filters=' + params
      })
    }
  }
}
</script>

<style>
.statistics-page {
  padding: 20rpx;
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

.mode-selector {
  display: flex;
  background: #fff;
  border-radius: 44rpx;
  padding: 6rpx;
  margin-bottom: 16rpx;
}

.mode-tab {
  flex: 1;
  text-align: center;
  padding: 14rpx 0;
  border-radius: 44rpx;
  font-size: 26rpx;
  color: #606266;
  transition: all 0.3s;
}

.mode-tab.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-weight: bold;
}

.month-selector,
.year-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18rpx 0;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
}

.month-btn {
  padding: 10rpx 30rpx;
  font-size: 32rpx;
  color: #606266;
}

.current-period {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
  padding: 0 40rpx;
  display: flex;
  align-items: center;
}

.picker-arrow {
  font-size: 22rpx;
  margin-left: 8rpx;
  color: #909399;
}

.summary-cards {
  display: flex;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.summary-card {
  flex: 1;
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx 12rpx;
  text-align: center;
}

.summary-card .label {
  font-size: 20rpx;
  color: #909399;
  display: block;
  margin-bottom: 6rpx;
}

.summary-card .value {
  font-size: 28rpx;
  font-weight: bold;
  color: #303133;
}

.summary-card.income .value {
  color: #409eff;
}

.summary-card.expense .value {
  color: #f56c6c;
}

.summary-card.balance .value {
  color: #409eff;
}

.summary-card.balance.negative .value {
  color: #f56c6c;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.section-header text:first-child {
  font-size: 28rpx;
  font-weight: bold;
  color: #303133;
}

.legend-inline {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.legend-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
}

.legend-dot.expense {
  background: #f56c6c;
}

.legend-dot.income {
  background: #409eff;
}

.legend-text {
  font-size: 22rpx;
  color: #606266;
}

.section-title {
  font-size: 28rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 16rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.type-switch {
  display: flex;
  background: #f5f5f5;
  border-radius: 24rpx;
  padding: 4rpx;
}

.type-switch text {
  padding: 8rpx 20rpx;
  font-size: 24rpx;
  color: #606266;
  border-radius: 20rpx;
}

.type-switch text.active {
  background: #409eff;
  color: #fff;
}

.empty-chart {
  text-align: center;
  padding: 60rpx 0;
  color: #909399;
  font-size: 26rpx;
}

.chart-container {
  position: relative;
}

.chart-canvas {
  width: 100%;
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

.chart-skeleton {
  padding: 40rpx 24rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.sk-chart-bar {
  height: 120rpx;
  border-radius: 8rpx;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

.pie-skeleton {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 30rpx 24rpx;
}

.sk-pie-circle {
  width: 180rpx;
  height: 180rpx;
  border-radius: 50%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
  flex-shrink: 0;
}

.sk-pie-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.sk-pie-line {
  height: 28rpx;
  border-radius: 8rpx;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

.pie-chart {
  display: flex;
  align-items: flex-start;
  gap: 24rpx;
}

.pie-visual {
  width: 180rpx;
  height: 180rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  flex-shrink: 0;
  margin-top: 10rpx;
}

.pie-center {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.pie-total {
  font-size: 22rpx;
  font-weight: bold;
  color: #303133;
}

.pie-label {
  font-size: 18rpx;
  color: #909399;
}

.pie-legend {
  flex: 1;
}

.legend-item {
  display: flex;
  align-items: center;
  padding: 10rpx 8rpx;
  border-radius: 8rpx;
}

.legend-item:active {
  background: #f5f5f5;
}

.legend-item .legend-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  margin-right: 10rpx;
  flex-shrink: 0;
}

.legend-name {
  flex: 1;
  font-size: 24rpx;
  color: #303133;
}

.legend-value {
  font-size: 24rpx;
  color: #606266;
  margin-right: 12rpx;
}

.legend-percent {
  font-size: 20rpx;
  color: #909399;
  width: 70rpx;
  text-align: right;
}

.calendar-section {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.calendar-grid {
  display: flex;
  flex-wrap: wrap;
  border-top: 1rpx solid #f0f0f0;
  border-left: 1rpx solid #f0f0f0;
  border-radius: 8rpx;
  overflow: hidden;
}

.calendar-weekday {
  width: calc(100% / 7);
  text-align: center;
  font-size: 22rpx;
  color: #909399;
  padding: 10rpx 0;
  border-right: 1rpx solid #f0f0f0;
  border-bottom: 1rpx solid #f0f0f0;
  box-sizing: border-box;
  background: #fafafa;
}

.calendar-cell {
  width: calc(100% / 7);
  min-height: 100rpx;
  padding: 6rpx 2rpx;
  border-right: 1rpx solid #f0f0f0;
  border-bottom: 1rpx solid #f0f0f0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
}

.calendar-cell.empty {
  background: #fafafa;
}

.calendar-cell.today {
  background: #f0f7ff;
}

.calendar-cell.today .cell-day {
  color: #fff;
  background: #409eff;
  border-radius: 50%;
  width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  text-align: center;
}

.calendar-cell.has-data {
  background: #fff;
}

.calendar-cell:active.has-data {
  background: #f5f5f5;
}

.cell-day {
  font-size: 22rpx;
  color: #303133;
  margin-bottom: 2rpx;
  line-height: 1.2;
}

.cell-amount {
  font-size: 16rpx;
  line-height: 1.3;
  text-align: center;
  word-break: break-all;
}

.cell-expense {
  color: #f56c6c;
}

.cell-income {
  color: #409eff;
}

.cell-empty {
  color: #c0c4cc;
  font-size: 20rpx;
}

.cell-month {
  font-size: 26rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8rpx;
}

.year-grid {
  display: flex;
  flex-wrap: wrap;
}

.year-cell {
  width: calc(100% / 3);
  padding: 8rpx;
  box-sizing: border-box;
}

.year-cell-inner {
  background: #f9fafc;
  border: 1rpx solid #ebeef5;
  border-radius: 12rpx;
  padding: 18rpx 8rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 120rpx;
  justify-content: center;
}

.year-cell-inner.has-data {
  background: #fff;
  border-color: #d9ecff;
}

.year-cell-inner:active.has-data {
  background: #f5f5f5;
}

.year-cell-inner .cell-amount {
  font-size: 20rpx;
}

.year-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.year-row {
  background: #f9fafc;
  border: 1rpx solid #ebeef5;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.year-row:active {
  background: #f5f5f5;
}

.year-row-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.year-label {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}

.year-balance {
  font-size: 24rpx;
  color: #409eff;
  font-weight: bold;
}

.year-balance.negative {
  color: #f56c6c;
}

.year-row-amounts {
  display: flex;
  gap: 24rpx;
}

.year-row-amounts .cell-income,
.year-row-amounts .cell-expense {
  font-size: 24rpx;
}

.calendar-skeleton {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.sk-cal-row {
  display: flex;
  gap: 4rpx;
}

.sk-cal-cell {
  flex: 1;
  height: 80rpx;
  border-radius: 6rpx;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

.sk-cal-cell-lg {
  height: 120rpx;
}

.sk-year-row {
  height: 100rpx;
  border-radius: 12rpx;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

</style>
