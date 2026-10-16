<template>
  <view class="trend-page">
    <view class="tabs">
      <view 
        :class="{ active: activeTab === 'monthly' }" 
        class="tab-item"
        @click="activeTab = 'monthly'"
      >
        <text>月度趋势</text>
      </view>
      <view 
        :class="{ active: activeTab === 'category' }" 
        class="tab-item"
        @click="activeTab = 'category'"
      >
        <text>分类趋势</text>
      </view>
    </view>
    
    <view v-if="activeTab === 'monthly'" class="chart-section">
      <view class="section-header">
        <text class="section-title">收支趋势</text>
        <picker mode="selector" :range="monthsRange" @change="onMonthChange">
          <view class="picker-btn">
            <text>{{ trendMonths }}个月</text>
            <text class="arrow">▼</text>
          </view>
        </picker>
      </view>
      <view class="chart-container">
        <canvas 
          canvas-id="monthlyChart" 
          class="chart-canvas"
          :style="{ height: chartHeight + 'px' }"
        ></canvas>
        <view v-if="monthlyData.length === 0" class="empty-tip">
          <text>暂无数据</text>
        </view>
      </view>
      <view class="legend">
        <view class="legend-item">
          <view class="legend-dot expense"></view>
          <text>支出</text>
        </view>
        <view class="legend-item">
          <view class="legend-dot income"></view>
          <text>收入</text>
        </view>
      </view>
      <view v-if="monthlyData.length > 0" class="summary">
        <view class="summary-item">
          <text class="summary-label">总支出</text>
          <text class="summary-value expense">¥{{ totalExpense }}</text>
        </view>
        <view class="summary-item">
          <text class="summary-label">总收入</text>
          <text class="summary-value income">¥{{ totalIncome }}</text>
        </view>
        <view class="summary-item">
          <text class="summary-label">净收入</text>
          <text :class="['summary-value', totalBalance >= 0 ? 'income' : 'expense']">¥{{ totalBalance }}</text>
        </view>
      </view>
    </view>
    
    <view v-if="activeTab === 'category'" class="chart-section">
      <view class="section-header">
        <text class="section-title">分类趋势</text>
      </view>
      <view class="selectors">
        <view class="selector-item">
          <text class="selector-label">选择分类</text>
          <view class="picker-btn large" @click="openCategoryPopup">
            <text>{{ selectedCategoryName || '请选择分类' }}</text>
            <text class="arrow">▼</text>
          </view>
        </view>
        <view class="selector-item">
          <text class="selector-label">时间范围</text>
          <picker mode="selector" :range="categoryMonthsRange" @change="onCategoryMonthChange">
            <view class="picker-btn">
              <text>{{ categoryTrendMonths }}个月</text>
              <text class="arrow">▼</text>
            </view>
          </picker>
        </view>
      </view>
      <view class="chart-container">
        <canvas 
          canvas-id="categoryChart" 
          class="chart-canvas"
          :style="{ height: chartHeight + 'px' }"
        ></canvas>
        <view v-if="!selectedCategoryId" class="empty-tip">
          <text>请先选择分类</text>
        </view>
        <view v-else-if="categoryData.length === 0" class="empty-tip">
          <text>该分类暂无数据</text>
        </view>
      </view>
      <view v-if="categoryData.length > 0" class="summary">
        <view class="summary-item">
          <text class="summary-label">总支出</text>
          <text class="summary-value expense">¥{{ categoryTotal }}</text>
        </view>
        <view class="summary-item">
          <text class="summary-label">月均支出</text>
          <text class="summary-value">¥{{ categoryAverage }}</text>
        </view>
      </view>
    </view>

    <view v-if="showCategoryPopup" class="popup-mask" @click="showCategoryPopup = false">
      <view class="popup-content" @click.stop>
        <view class="popup-header">
          <text class="popup-title">选择分类</text>
          <text class="popup-close" @click="showCategoryPopup = false">✕</text>
        </view>
        <scroll-view scroll-y class="popup-list" :style="{height: '50vh'}">
          <view
            v-for="cat in categories"
            :key="cat.id"
            class="popup-item"
            :class="{ active: selectedCategoryId === cat.id }"
            @click="selectCategory(cat)"
          >
            <text class="item-icon">{{ cat.icon || '📌' }}</text>
            <text class="item-name">{{ cat.name }}</text>
            <text v-if="selectedCategoryId === cat.id" class="item-check">✓</text>
          </view>
          <view v-if="categories.length === 0" class="popup-empty">
            <text>暂无分类</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { statisticsApi, categoryApi } from '../../api/index.js'
import { storage } from '../../utils/storage.js'
import { sumAmounts } from '../../utils/money.js'

export default {
  data() {
    return {
      activeTab: 'monthly',
      trendMonths: 12,
      categoryTrendMonths: 6,
      monthsRange: ['6个月', '12个月', '24个月'],
      categoryMonthsRange: ['3个月', '6个月', '12个月'],
      monthlyData: [],
      categoryData: [],
      categories: [],
      selectedCategoryId: null,
      showCategoryPopup: false,
      chartHeight: 280,
      isLoading: false
    }
  },
  
  computed: {
    selectedCategoryName() {
      if (!this.selectedCategoryId) return '请选择分类'
      const cat = this.categories.find(c => c.id === this.selectedCategoryId)
      return cat ? `${cat.icon || '📌'} ${cat.name}` : '请选择分类'
    },
    
    totalExpense() {
      return sumAmounts(this.monthlyData, item => item.expense || 0).toFixed(2)
    },
    
    totalIncome() {
      return sumAmounts(this.monthlyData, item => item.income || 0).toFixed(2)
    },
    
    totalBalance() {
      const incomeCents = Math.round(Number(this.totalIncome) * 100)
      const expenseCents = Math.round(Number(this.totalExpense) * 100)
      return ((incomeCents - expenseCents) / 100).toFixed(2)
    },
    
    categoryTotal() {
      return sumAmounts(this.categoryData, item => item.amount || 0).toFixed(2)
    },
    
    categoryAverage() {
      if (this.categoryData.length === 0) return '0.00'
      return (Number(this.categoryTotal) / this.categoryData.length).toFixed(2)
    }
  },
  
  onLoad() {
    this.checkAuth()
    this.init()
  },
  
  onReady() {
    this.$nextTick(() => {
      this.drawMonthlyChart()
      this.drawCategoryChart()
    })
  },
  
  methods: {
    checkAuth() {
      const token = storage.get('token')
      if (!token) {
        uni.reLaunch({ url: '/pages/login/login' })
      }
    },
    
    async init() {
      try {
        this.isLoading = true
        await Promise.all([
          this.loadMonthlyData(),
          this.loadCategories()
        ])
      } catch (e) {
        console.error(e)
      } finally {
        this.isLoading = false
      }
    },
    
    async loadMonthlyData() {
      try {
        this.monthlyData = await statisticsApi.getMonthlyTrend(this.trendMonths)
        this.$nextTick(() => {
          this.drawMonthlyChart()
        })
      } catch (e) {
        console.error('加载月度趋势失败:', e)
      }
    },
    
    async loadCategories() {
      try {
        const data = await categoryApi.getCategories()
        const expenseList = (data && data.EXPENSE) ? data.EXPENSE : []
        const incomeList = (data && data.INCOME) ? data.INCOME : []
        this.categories = [...expenseList, ...incomeList]
      } catch (e) {
        console.error('加载分类失败:', e)
      }
    },
    
    async loadCategoryData() {
      if (!this.selectedCategoryId) return
      try {
        this.categoryData = await statisticsApi.getCategoryTrend(
          this.selectedCategoryId, 
          this.categoryTrendMonths
        )
        this.$nextTick(() => {
          this.drawCategoryChart()
        })
      } catch (e) {
        console.error('加载分类趋势失败:', e)
      }
    },
    
    onMonthChange(e) {
      const index = e.detail.value
      this.trendMonths = index === 0 ? 6 : index === 1 ? 12 : 24
      this.loadMonthlyData()
    },
    
    onCategoryMonthChange(e) {
      const index = e.detail.value
      this.categoryTrendMonths = index === 0 ? 3 : index === 1 ? 6 : 12
      this.loadCategoryData()
    },
    
    openCategoryPopup() {
      if (this.categories.length === 0) {
        uni.showToast({ title: '暂无分类', icon: 'none' })
        return
      }
      this.showCategoryPopup = true
    },

    selectCategory(cat) {
      this.selectedCategoryId = cat.id
      this.showCategoryPopup = false
      this.loadCategoryData()
    },
    
    onMonthChange(e) {
      if (this.monthlyData.length === 0) return
      
      const ctx = uni.createCanvasContext('monthlyChart', this)
      const data = this.monthlyData
      const padding = { top: 30, right: 20, bottom: 40, left: 50 }
      const canvasWidth = 340
      const canvasHeight = this.chartHeight
      
      const width = canvasWidth - padding.left - padding.right
      const height = canvasHeight - padding.top - padding.bottom
      
      const allValues = data.flatMap(d => [Number(d.expense || 0), Number(d.income || 0)])
      const maxValue = Math.max(...allValues, 100)
      const yMax = Math.ceil(maxValue * 1.1)
      const yStep = yMax / 4
      
      ctx.clearRect(0, 0, canvasWidth, canvasHeight)
      
      ctx.setStrokeStyle('#e0e0e0')
      ctx.setLineWidth(0.5)
      for (let i = 0; i <= 4; i++) {
        const y = padding.top + (height / 4) * i
        ctx.moveTo(padding.left, y)
        ctx.lineTo(padding.left + width, y)
        ctx.stroke()
        
        ctx.setFillStyle('#909399')
        ctx.setFontSize(10)
        ctx.setTextAlign('right')
        const value = yMax - yStep * i
        ctx.fillText(value >= 1000 ? (value / 1000).toFixed(1) + 'k' : value.toFixed(0), padding.left - 8, y + 4)
      }
      
      ctx.setFillStyle('#909399')
      ctx.setFontSize(9)
      ctx.setTextAlign('center')
      const stepX = width / Math.max(data.length - 1, 1)
      data.forEach((item, i) => {
        const x = padding.left + stepX * i
        ctx.fillText(item.month.substring(5), x, canvasHeight - 10)
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
      ctx.setStrokeStyle('#67c23a')
      ctx.moveTo(incomePath[0].x, incomePath[0].y)
      for (let i = 1; i < incomePath.length; i++) {
        ctx.lineTo(incomePath[i].x, incomePath[i].y)
      }
      ctx.stroke()
      
      ctx.setLineWidth(4)
      expensePath.forEach(p => {
        ctx.beginPath()
        ctx.setStrokeStyle('#f56c6c')
        ctx.arc(p.x, p.y, 3, 0, 2 * Math.PI)
        ctx.stroke()
        ctx.setFillStyle('#fff')
        ctx.fill()
      })
      
      incomePath.forEach(p => {
        ctx.beginPath()
        ctx.setStrokeStyle('#67c23a')
        ctx.arc(p.x, p.y, 3, 0, 2 * Math.PI)
        ctx.stroke()
        ctx.setFillStyle('#fff')
        ctx.fill()
      })
      
      ctx.draw()
    },
    
    drawCategoryChart() {
      if (this.categoryData.length === 0) return
      
      const ctx = uni.createCanvasContext('categoryChart', this)
      const data = this.categoryData
      const padding = { top: 30, right: 20, bottom: 40, left: 50 }
      const canvasWidth = 340
      const canvasHeight = this.chartHeight
      
      const width = canvasWidth - padding.left - padding.right
      const height = canvasHeight - padding.top - padding.bottom
      
      const values = data.map(d => Number(d.amount || 0))
      const maxValue = Math.max(...values, 100)
      const yMax = Math.ceil(maxValue * 1.1)
      const yStep = yMax / 4
      
      ctx.clearRect(0, 0, canvasWidth, canvasHeight)
      
      ctx.setStrokeStyle('#e0e0e0')
      ctx.setLineWidth(0.5)
      for (let i = 0; i <= 4; i++) {
        const y = padding.top + (height / 4) * i
        ctx.moveTo(padding.left, y)
        ctx.lineTo(padding.left + width, y)
        ctx.stroke()
        
        ctx.setFillStyle('#909399')
        ctx.setFontSize(10)
        ctx.setTextAlign('right')
        const value = yMax - yStep * i
        ctx.fillText(value >= 1000 ? (value / 1000).toFixed(1) + 'k' : value.toFixed(0), padding.left - 8, y + 4)
      }
      
      ctx.setFillStyle('#909399')
      ctx.setFontSize(9)
      ctx.setTextAlign('center')
      const stepX = width / Math.max(data.length - 1, 1)
      data.forEach((item, i) => {
        const x = padding.left + stepX * i
        ctx.fillText(item.month.substring(5), x, canvasHeight - 10)
      })
      
      const getX = (i) => padding.left + stepX * i
      const getY = (value) => padding.top + height - (value / yMax) * height
      
      const path = data.map((item, i) => ({ x: getX(i), y: getY(Number(item.amount || 0)) }))
      
      ctx.setLineWidth(2)
      ctx.beginPath()
      ctx.setStrokeStyle('#409eff')
      ctx.moveTo(path[0].x, path[0].y)
      for (let i = 1; i < path.length; i++) {
        ctx.lineTo(path[i].x, path[i].y)
      }
      ctx.stroke()
      
      ctx.setLineWidth(4)
      path.forEach(p => {
        ctx.beginPath()
        ctx.setStrokeStyle('#409eff')
        ctx.arc(p.x, p.y, 3, 0, 2 * Math.PI)
        ctx.stroke()
        ctx.setFillStyle('#fff')
        ctx.fill()
      })
      
      ctx.draw()
    }
  }
}
</script>

<style>
.trend-page {
  padding: 20rpx;
  background: #f5f7fa;
  min-height: 100vh;
}

.tabs {
  display: flex;
  background: #fff;
  border-radius: 12rpx;
  padding: 8rpx;
  margin-bottom: 20rpx;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: 8rpx;
}

.tab-item text {
  font-size: 28rpx;
  color: #606266;
}

.tab-item.active {
  background: #409eff;
}

.tab-item.active text {
  color: #fff;
  font-weight: bold;
}

.chart-section {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: bold;
  color: #303133;
}

.picker-btn {
  display: flex;
  align-items: center;
  padding: 12rpx 20rpx;
  background: #f5f7fa;
  border-radius: 8rpx;
  font-size: 24rpx;
  color: #606266;
}

.picker-btn.large {
  padding: 16rpx 20rpx;
}

.picker-btn .arrow {
  margin-left: 8rpx;
  font-size: 20rpx;
  color: #909399;
}

.selectors {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.selector-item {
  display: flex;
  align-items: center;
}

.selector-label {
  width: 140rpx;
  font-size: 26rpx;
  color: #606266;
}

.chart-container {
  position: relative;
  margin-bottom: 20rpx;
}

.chart-canvas {
  width: 100%;
}

.empty-tip {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 26rpx;
  color: #909399;
}

.legend {
  display: flex;
  justify-content: center;
  gap: 40rpx;
  margin-bottom: 20rpx;
}

.legend-item {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  color: #606266;
}

.legend-dot {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  margin-right: 10rpx;
}

.legend-dot.expense {
  background: #f56c6c;
}

.legend-dot.income {
  background: #67c23a;
}

.summary {
  display: flex;
  justify-content: space-around;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.summary-label {
  font-size: 24rpx;
  color: #909399;
  margin-bottom: 8rpx;
}

.summary-value {
  font-size: 28rpx;
  font-weight: bold;
  color: #303133;
}

.summary-value.expense {
  color: #f56c6c;
}

.summary-value.income {
  color: #67c23a;
}

.popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 9999;
  display: flex;
  align-items: flex-end;
}

.popup-content {
  width: 100%;
  max-height: 70vh;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.popup-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}

.popup-close {
  font-size: 36rpx;
  color: #909399;
  padding: 10rpx;
}

.popup-list {
}

.popup-item {
  display: flex;
  align-items: center;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid #f5f5f5;
  transition: background 0.15s;
}

.popup-item:active {
  background: #f5f7fa;
}

.popup-item.active {
  background: rgba(64, 158, 255, 0.06);
}

.popup-item .item-icon {
  font-size: 36rpx;
  margin-right: 20rpx;
  width: 44rpx;
  text-align: center;
}

.popup-item .item-name {
  flex: 1;
  font-size: 30rpx;
  color: #303133;
}

.popup-item.active .item-name {
  color: #409eff;
  font-weight: bold;
}

.popup-item .item-check {
  font-size: 32rpx;
  color: #409eff;
  font-weight: bold;
}

.popup-empty {
  text-align: center;
  padding: 80rpx 0;
}

.popup-empty text {
  font-size: 28rpx;
  color: #909399;
}
</style>
