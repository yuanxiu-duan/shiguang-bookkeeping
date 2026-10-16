<template>
  <div class="reports">
    <!-- 月度统计 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span class="card-title">月度统计</span>
              <el-date-picker
                v-model="selectedMonth"
                type="month"
                placeholder="选择月份"
                value-format="YYYY-MM"
                @change="loadMonthlyData"
              />
            </div>
          </template>
          <div v-loading="monthlyLoading">
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="stat-card income">
                <div class="stat-label">本月收入</div>
                <div class="stat-value">¥{{ monthlyStats.totalIncome || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card expense">
                <div class="stat-label">本月支出</div>
                <div class="stat-value">¥{{ monthlyStats.totalExpense || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card balance" :class="{ negative: (monthlyStats.balance || 0) < 0 }">
                <div class="stat-label">结余</div>
                <div class="stat-value">¥{{ monthlyStats.balance || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card saving">
                <div class="stat-label">储蓄率</div>
                <div class="stat-value">
                  {{ monthlySavingRate }}%
                </div>
              </div>
            </el-col>
          </el-row>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 年度统计 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span class="card-title">年度统计</span>
              <el-date-picker
                v-model="selectedYear"
                type="year"
                placeholder="选择年份"
                value-format="YYYY"
                @change="loadYearlyData"
              />
            </div>
          </template>
          <div v-loading="yearlyLoading">
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="stat-card income">
                <div class="stat-label">本年收入</div>
                <div class="stat-value">¥{{ yearlyStats.totalIncome || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card expense">
                <div class="stat-label">本年支出</div>
                <div class="stat-value">¥{{ yearlyStats.totalExpense || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card balance" :class="{ negative: (yearlyStats.balance || 0) < 0 }">
                <div class="stat-label">结余</div>
                <div class="stat-value">¥{{ yearlyStats.balance || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card saving">
                <div class="stat-label">储蓄率</div>
                <div class="stat-value">
                  {{ yearlySavingRate }}%
                </div>
              </div>
            </el-col>
          </el-row>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <el-tabs v-model="activeTab" class="reports-tabs">
              <el-tab-pane label="月度详情" name="monthly">
                <template #label>
                  <el-icon><Calendar /></el-icon>
                  <span>月度详情</span>
                </template>
              </el-tab-pane>
              <el-tab-pane label="年度详情" name="yearly">
                <template #label>
                  <el-icon><Calendar /></el-icon>
                  <span>年度详情</span>
                </template>
              </el-tab-pane>
              <el-tab-pane label="趋势分析" name="trend">
                <template #label>
                  <el-icon><TrendCharts /></el-icon>
                  <span>趋势分析</span>
                </template>
              </el-tab-pane>
              <el-tab-pane label="分类趋势" name="categoryTrend">
                <template #label>
                  <el-icon><DataAnalysis /></el-icon>
                  <span>分类趋势</span>
                </template>
              </el-tab-pane>
            </el-tabs>
          </template>
          
          <!-- 月度详情 -->
          <div v-show="activeTab === 'monthly'">
            <el-row :gutter="20">
              <el-col :span="12">
                <div class="sub-card-title">每日收支</div>
                <div ref="trendChart" style="width: 100%; height: 350px;"></div>
              </el-col>
              <el-col :span="12">
                <div class="card-header" style="margin-bottom: 10px;">
                  <div class="sub-card-title">分类统计</div>
                  <el-radio-group v-model="chartType" size="small">
                    <el-radio-button value="expense">支出</el-radio-button>
                    <el-radio-button value="income">收入</el-radio-button>
                  </el-radio-group>
                </div>
                <div ref="pieChart" style="width: 100%; height: 350px;"></div>
              </el-col>
            </el-row>
          </div>

          <!-- 年度详情 -->
          <div v-show="activeTab === 'yearly'">
            <el-row :gutter="20">
              <el-col :span="12">
                <div class="sub-card-title">每月收支</div>
                <div ref="yearlyTrendChart" style="width: 100%; height: 350px;"></div>
              </el-col>
              <el-col :span="12">
                <div class="card-header" style="margin-bottom: 10px;">
                  <div class="sub-card-title">年度分类统计</div>
                  <el-radio-group v-model="yearlyChartType" size="small">
                    <el-radio-button value="expense">支出</el-radio-button>
                    <el-radio-button value="income">收入</el-radio-button>
                  </el-radio-group>
                </div>
                <div ref="yearlyPieChart" style="width: 100%; height: 350px;"></div>
              </el-col>
            </el-row>
          </div>
          
          <div v-show="activeTab === 'trend'">
            <div class="card-header" style="margin-bottom: 10px;">
              <div class="sub-card-title">近12个月收支趋势</div>
              <el-select v-model="trendMonths" size="small" @change="loadTrendData">
                <el-option :label="6 + '个月'" :value="6" />
                <el-option :label="12 + '个月'" :value="12" />
                <el-option :label="24 + '个月'" :value="24" />
              </el-select>
            </div>
            <div ref="monthlyTrendChart" style="width: 100%; height: 400px;"></div>
          </div>
          
          <div v-show="activeTab === 'categoryTrend'">
            <div class="card-header" style="margin-bottom: 10px;">
              <div class="sub-card-title">分类趋势分析</div>
              <div style="display: flex; gap: 10px;">
                <el-select 
                  v-model="selectedCategoryId" 
                  placeholder="选择分类" 
                  size="small"
                  style="width: 200px;"
                  @change="loadCategoryTrendData"
                >
                  <el-option
                    v-for="cat in allCategories"
                    :key="cat.id"
                    :label="`${cat.icon || '📌'} ${cat.name}`"
                    :value="cat.id"
                  />
                </el-select>
                <el-select v-model="categoryTrendMonths" size="small" @change="loadCategoryTrendData">
                  <el-option :label="3 + '个月'" :value="3" />
                  <el-option :label="6 + '个月'" :value="6" />
                  <el-option :label="12 + '个月'" :value="12" />
                </el-select>
              </div>
            </div>
            <div ref="categoryTrendChart" style="width: 100%; height: 400px;"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import { Calendar, TrendCharts, DataAnalysis } from '@element-plus/icons-vue'
import { getMonthlyStatistics, getYearlyStatistics, getCurveData, getMonthlyTrend, getCategoryTrend } from '@/api/statistics'
import { getCategories } from '@/api/category'

// 月度统计
const selectedMonth = ref(dayjs().format('YYYY-MM'))
const chartType = ref('expense')
const monthlyStats = ref({})
const monthlyLoading = ref(false)

// 年度统计
const selectedYear = ref(dayjs().format('YYYY'))
const yearlyChartType = ref('expense')
const yearlyStats = ref({})
const yearlyLoading = ref(false)

// 通用
const activeTab = ref('monthly')
const trendMonths = ref(12)
const categoryTrendMonths = ref(6)
const selectedCategoryId = ref(null)
const allCategories = ref([])
const monthlyTrendData = ref([])
const categoryTrendData = ref([])

// 图表ref
const trendChart = ref(null)
const pieChart = ref(null)
const yearlyTrendChart = ref(null)
const yearlyPieChart = ref(null)
const monthlyTrendChart = ref(null)
const categoryTrendChart = ref(null)

// 图表实例
let trendChartInstance = null
let pieChartInstance = null
let yearlyTrendChartInstance = null
let yearlyPieChartInstance = null
let monthlyTrendChartInstance = null
let categoryTrendChartInstance = null

const monthlySavingRate = computed(() => {
  const income = Number(monthlyStats.value.totalIncome || 0)
  const balance = Number(monthlyStats.value.balance || 0)
  if (income <= 0) return 0
  return Math.round((balance / income) * 100)
})

const yearlySavingRate = computed(() => {
  const income = Number(yearlyStats.value.totalIncome || 0)
  const balance = Number(yearlyStats.value.balance || 0)
  if (income <= 0) return 0
  return Math.round((balance / income) * 100)
})

async function loadCategories() {
  const data = await getCategories()
  allCategories.value = [...(data.EXPENSE || []), ...(data.INCOME || [])]
}

async function loadMonthlyData() {
  monthlyLoading.value = true
  try {
    const data = await getMonthlyStatistics(selectedMonth.value)
    monthlyStats.value = data
    nextTick(() => {
      renderTrendChart()
      renderPieChart()
    })
  } catch (e) {
    console.error('加载月度统计数据失败', e)
  } finally {
    monthlyLoading.value = false
  }
}

async function loadYearlyData() {
  yearlyLoading.value = true
  try {
    const year = parseInt(selectedYear.value)
    const data = await getYearlyStatistics(year)
    yearlyStats.value = data
    nextTick(() => {
      renderYearlyTrendChart()
      renderYearlyPieChart()
    })
  } catch (e) {
    console.error('加载年度统计数据失败', e)
  } finally {
    yearlyLoading.value = false
  }
}

async function loadTrendData() {
  const data = await getMonthlyTrend(trendMonths.value)
  monthlyTrendData.value = data
  nextTick(() => {
    renderMonthlyTrendChart()
  })
}

async function loadCategoryTrendData() {
  if (!selectedCategoryId.value) return
  const data = await getCategoryTrend(selectedCategoryId.value, categoryTrendMonths.value)
  categoryTrendData.value = data
  nextTick(() => {
    renderCategoryTrendChart()
  })
}

function renderTrendChart() {
  if (!trendChart.value) return
  if (!trendChartInstance) {
    trendChartInstance = echarts.init(trendChart.value)
  }

  const daily = monthlyStats.value.dailyTrend || []
  const dates = daily.map(d => d.date.substring(5))
  const expenses = daily.map(d => Number(d.expense) || 0)
  const incomes = daily.map(d => Number(d.income) || 0)

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['支出', '收入'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value' },
    series: [
      { name: '支出', type: 'line', smooth: true, data: expenses, itemStyle: { color: '#f56c6c' }, areaStyle: { color: 'rgba(245, 108, 108, 0.1)' } },
      { name: '收入', type: 'line', smooth: true, data: incomes, itemStyle: { color: '#409eff' }, areaStyle: { color: 'rgba(64, 158, 255, 0.1)' } }
    ]
  }
  trendChartInstance.setOption(option)
  trendChartInstance.resize()
}

function renderPieChart() {
  if (!pieChart.value) return
  if (!pieChartInstance) {
    pieChartInstance = echarts.init(pieChart.value)
  }

  const data = chartType.value === 'expense'
    ? (monthlyStats.value.expenseByCategory || [])
    : (monthlyStats.value.incomeByCategory || [])

  const pieData = data.map(item => ({
    name: `${item.categoryIcon || '📌'} ${item.categoryName}`,
    value: Number(item.amount)
  }))

  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { type: 'scroll', orient: 'vertical', right: 10, top: 20, bottom: 20 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      labelLine: { show: false },
      data: pieData
    }]
  }
  pieChartInstance.setOption(option)
  pieChartInstance.resize()
}

function renderYearlyTrendChart() {
  if (!yearlyTrendChart.value) return
  if (!yearlyTrendChartInstance) {
    yearlyTrendChartInstance = echarts.init(yearlyTrendChart.value)
  }

  const monthly = yearlyStats.value.monthlyTrend || []
  const months = monthly.map(d => d.month)
  const expenses = monthly.map(d => Number(d.expense) || 0)
  const incomes = monthly.map(d => Number(d.income) || 0)

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['支出', '收入'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value' },
    series: [
      { name: '支出', type: 'bar', data: expenses, itemStyle: { color: '#f56c6c' }, barGap: '10%' },
      { name: '收入', type: 'bar', data: incomes, itemStyle: { color: '#409eff' } }
    ]
  }
  yearlyTrendChartInstance.setOption(option, true)
  yearlyTrendChartInstance.resize()
}

function renderYearlyPieChart() {
  if (!yearlyPieChart.value) return
  if (!yearlyPieChartInstance) {
    yearlyPieChartInstance = echarts.init(yearlyPieChart.value)
  }

  const data = yearlyChartType.value === 'expense'
    ? (yearlyStats.value.expenseByCategory || [])
    : (yearlyStats.value.incomeByCategory || [])

  const pieData = data.map(item => ({
    name: `${item.categoryIcon || '📌'} ${item.categoryName}`,
    value: Number(item.amount)
  }))

  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { type: 'scroll', orient: 'vertical', right: 10, top: 20, bottom: 20 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      labelLine: { show: false },
      data: pieData
    }]
  }
  yearlyPieChartInstance.setOption(option, true)
  yearlyPieChartInstance.resize()
}

function renderMonthlyTrendChart() {
  if (!monthlyTrendChart.value) return
  if (!monthlyTrendChartInstance) {
    monthlyTrendChartInstance = echarts.init(monthlyTrendChart.value)
  }

  const data = monthlyTrendData.value || []
  const months = data.map(d => d.month)
  const expenses = data.map(d => Number(d.expense) || 0)
  const incomes = data.map(d => Number(d.income) || 0)

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['支出', '收入'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: months, boundaryGap: false },
    yAxis: { type: 'value' },
    series: [
      { name: '支出', type: 'line', smooth: true, data: expenses, itemStyle: { color: '#f56c6c' }, areaStyle: { color: 'rgba(245, 108, 108, 0.1)' } },
      { name: '收入', type: 'line', smooth: true, data: incomes, itemStyle: { color: '#409eff' }, areaStyle: { color: 'rgba(64, 158, 255, 0.1)' } }
    ]
  }
  monthlyTrendChartInstance.setOption(option)
  monthlyTrendChartInstance.resize()
}

function renderCategoryTrendChart() {
  if (!categoryTrendChart.value) return
  if (!categoryTrendChartInstance) {
    categoryTrendChartInstance = echarts.init(categoryTrendChart.value)
  }
  
  const data = categoryTrendData.value || []
  const months = data.map(d => d.month)
  const amounts = data.map(d => Number(d.amount) || 0)
  
  const selectedCategory = allCategories.value.find(c => c.id === selectedCategoryId.value)
  const categoryName = selectedCategory ? selectedCategory.name : '分类'
  
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: [categoryName] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: months, boundaryGap: false },
    yAxis: { type: 'value' },
    series: [
      { 
        name: categoryName, 
        type: 'line', 
        smooth: true, 
        data: amounts, 
        itemStyle: { color: '#409eff' }, 
        areaStyle: { color: 'rgba(64, 158, 255, 0.1)' },
        markPoint: {
          data: [
            { type: 'max', name: '最大值' },
            { type: 'min', name: '最小值' }
          ]
        },
        markLine: {
          data: [
            { type: 'average', name: '平均值' }
          ]
        }
      }
    ]
  }
  categoryTrendChartInstance.setOption(option)
  categoryTrendChartInstance.resize()
}

watch(chartType, () => {
  nextTick(() => renderPieChart())
})

watch(yearlyChartType, () => {
  nextTick(() => renderYearlyPieChart())
})

watch(activeTab, (newTab) => {
  nextTick(() => {
    if (newTab === 'monthly') {
      renderTrendChart()
      renderPieChart()
    } else if (newTab === 'yearly') {
      // 切换到年度tab时，若已加载数据则直接渲染并resize；否则加载
      if (yearlyStats.value.monthlyTrend) {
        renderYearlyTrendChart()
        renderYearlyPieChart()
      } else {
        loadYearlyData()
      }
    } else if (newTab === 'trend') {
      if (monthlyTrendData.value.length) {
        renderMonthlyTrendChart()
      } else {
        loadTrendData()
      }
    } else if (newTab === 'categoryTrend') {
      if (categoryTrendData.value.length) {
        renderCategoryTrendChart()
      }
    }
  })
})

onMounted(async () => {
  await loadCategories()
  await loadMonthlyData()
  await loadYearlyData()
  window.addEventListener('resize', () => {
    trendChartInstance?.resize()
    pieChartInstance?.resize()
    yearlyTrendChartInstance?.resize()
    yearlyPieChartInstance?.resize()
    monthlyTrendChartInstance?.resize()
    categoryTrendChartInstance?.resize()
  })
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-weight: bold;
  font-size: 16px;
}

.sub-card-title {
  font-weight: bold;
  font-size: 14px;
  color: #303133;
}

.reports-tabs {
  width: 100%;
}

.reports-tabs :deep(.el-tabs__nav-wrap) {
  justify-content: flex-start;
}

.stat-card {
  padding: 20px;
  border-radius: 8px;
  color: white;
}

.stat-card.income {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}

.stat-card.expense {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.stat-card.balance {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}

.stat-card.balance.negative {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

.stat-card.saving {
  background: linear-gradient(135deg, #909399 0%, #b1b3b8 100%);
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-top: 8px;
}
</style>
