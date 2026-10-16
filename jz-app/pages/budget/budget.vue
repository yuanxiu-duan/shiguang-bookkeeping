<template>
  <view class="budget-page">
    <view class="custom-nav">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
      <view class="nav-bar">
        <text class="nav-title">预算管理</text>
        <view class="nav-right" @click="goToAccountBooks">
          <text class="nav-switch">{{ currentBookName }} ›</text>
        </view>
      </view>
    </view>
    <view class="page-header">
      <text class="page-title">本月预算</text>
      <view class="add-btn" @click="showAddDialog = true">
        <text class="add-icon">+</text>
        <text class="add-text">添加</text>
      </view>
    </view>
    
    <view v-if="budgets.length === 0" class="empty-state">
      <text class="empty-icon">💰</text>
      <text class="empty-text">还没有设置预算</text>
      <text class="empty-hint">点击上方按钮创建第一个预算</text>
    </view>
    
    <view v-for="budget in budgets" :key="budget.id" class="budget-card">
      <view class="budget-header">
        <view class="category-info">
          <text class="category-icon">{{ budget.category?.icon || '📌' }}</text>
          <text class="category-name">{{ budget.category?.name || '未分类' }}</text>
        </view>
        <view class="budget-actions" @click.stop="handleLongPress(budget)">
          <text class="action-icon">⋮</text>
        </view>
      </view>
      
      <view class="budget-amounts">
        <view class="amount-item">
          <text class="amount-label">已用</text>
          <text class="amount-value used">¥{{ budget.usedAmount }}</text>
        </view>
        <view class="amount-item">
          <text class="amount-label">预算</text>
          <text class="amount-value">¥{{ budget.amountLimit }}</text>
        </view>
        <view class="amount-item">
          <text class="amount-label">剩余</text>
          <text class="amount-value remaining" :class="{ negative: getRemaining(budget) < 0 }">
            ¥{{ getRemaining(budget).toFixed(2) }}
          </text>
        </view>
      </view>
      
      <view class="progress-section">
        <view class="progress-bar">
          <view 
            class="progress-fill" 
            :class="getProgressClass(budget)"
            :style="{ width: getProgressPercent(budget) + '%' }"
          ></view>
        </view>
        <view class="progress-info">
          <text class="progress-percent">{{ getProgressPercent(budget) }}%</text>
          <text v-if="getProgressPercent(budget) >= budget.alertThreshold" class="alert-text">
            ⚠️ 已达{{ budget.alertThreshold }}%预警线
          </text>
          <text v-if="getProgressPercent(budget) >= 100" class="over-text">
            🚨 超预算！
          </text>
        </view>
      </view>
    </view>
    
    <view v-if="budgets.length > 0" class="budget-summary">
      <view class="summary-item">
        <text class="summary-icon">💵</text>
        <text class="summary-label">总预算</text>
        <text class="summary-value">¥{{ totalBudget }}</text>
      </view>
      <view class="summary-item">
        <text class="summary-icon">💰</text>
        <text class="summary-label">总支出</text>
        <text class="summary-value expense">¥{{ totalSpent }}</text>
      </view>
      <view class="summary-item">
        <text class="summary-icon">💎</text>
        <text class="summary-label">总剩余</text>
        <text class="summary-value" :class="{ negative: totalRemaining < 0 }">
          ¥{{ totalRemaining }}
        </text>
      </view>
    </view>
    
    <view v-if="showAddDialog" class="picker-mask" @click="showAddDialog = false">
      <view class="picker-content" @click.stop>
        <view class="picker-header">
          <text class="picker-title">添加预算</text>
        </view>
        <view class="add-tag-form">
          <view class="form-item">
            <text class="form-label">选择分类</text>
            <picker 
              :range="expenseCategories" 
              range-key="name"
              @change="onCategoryChange"
            >
              <view class="picker-display">
                <text v-if="addForm.categoryId">{{ selectedCategory?.icon || '📌' }} {{ selectedCategory?.name }}</text>
                <text v-else class="placeholder">请选择支出分类</text>
              </view>
            </picker>
          </view>
          <view class="form-item">
            <text class="form-label">预算额度</text>
            <input 
              v-model="addForm.amountLimit" 
              type="digit" 
              class="tag-name-input" 
              placeholder="请输入预算金额"
            />
          </view>
          <view class="form-item">
            <text class="form-label">预警阈值: {{ addForm.alertThreshold }}%</text>
            <slider 
              :value="addForm.alertThreshold" 
              :min="50" 
              :max="100" 
              :step="5"
              activeColor="#667eea"
              backgroundColor="#e4e7ed"
              @change="onAlertThresholdChange"
            />
          </view>
        </view>
        <view class="picker-footer">
          <button class="btn-cancel" @click="showAddDialog = false">取消</button>
          <button class="btn-confirm" @click="handleAddBudget">确定</button>
        </view>
      </view>
    </view>
    
    <view v-if="showEditDialog" class="picker-mask" @click="showEditDialog = false">
      <view class="picker-content" @click.stop>
        <view class="picker-header">
          <text class="picker-title">编辑预算额度</text>
        </view>
        <view class="add-tag-form">
          <view class="form-item">
            <text class="form-label">预算额度</text>
            <input 
              v-model="editBudget.amountLimit" 
              type="digit" 
              class="tag-name-input" 
              placeholder="请输入新的预算额度"
            />
          </view>
        </view>
        <view class="picker-footer">
          <button class="btn-cancel" @click="showEditDialog = false">取消</button>
          <button class="btn-confirm" @click="handleEditConfirm">确定</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { budgetApi, categoryApi, accountBookApi } from '../../api/index.js'
import { storage } from '../../utils/storage.js'
import { sumAmounts, toCents, subtractAmount } from '../../utils/money.js'

export default {
  data() {
    return {
      statusBarHeight: 0,
      budgets: [],
      expenseCategories: [],
      currentBookName: '默认账本',
      showAddDialog: false,
      showEditDialog: false,
      editBudget: { id: null, amountLimit: '' },
      addForm: {
        categoryId: null,
        amountLimit: '',
        alertThreshold: 80
      }
    }
  },
  
  computed: {
    totalBudget() {
      return sumAmounts(this.budgets, b => b.amountLimit).toFixed(2)
    },
    
    totalSpent() {
      return sumAmounts(this.budgets, b => b.usedAmount).toFixed(2)
    },
    
    totalRemaining() {
      return subtractAmount(this.totalBudget, this.totalSpent).toFixed(2)
    },
    
    selectedCategory() {
      return this.expenseCategories.find(c => c.id === this.addForm.categoryId)
    }
  },
  
  onShow() {
    this.getStatusBarHeight()
    this.checkAuth()
    this.loadData()
    this.loadCurrentBookName()
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
    
    goToAccountBooks() {
      uni.navigateTo({ url: '/pages/accountBooks/accountBooks' })
    },
    
    async loadData() {
      try {
        const [budgetData, categoryData] = await Promise.all([
          budgetApi.getCurrent().catch(e => { console.error(e); return [] }),
          categoryApi.getCategories().catch(e => { console.error(e); return { EXPENSE: [], INCOME: [] } })
        ])
        this.budgets = budgetData || []
        this.expenseCategories = (categoryData && categoryData.EXPENSE) ? categoryData.EXPENSE : []
      } catch (e) {
        console.error(e)
      }
    },
    
    getRemaining(budget) {
      return (toCents(budget.amountLimit) - toCents(budget.usedAmount)) / 100
    },
    
    getProgressPercent(budget) {
      if (!budget.amountLimit || Number(budget.amountLimit) === 0) return 0
      return Math.min(Math.round((Number(budget.usedAmount) / Number(budget.amountLimit)) * 100), 100)
    },
    
    getProgressClass(budget) {
      const percent = this.getProgressPercent(budget)
      if (percent >= 100) return 'danger'
      if (percent >= budget.alertThreshold) return 'warning'
      return 'normal'
    },
    
    onCategoryChange(e) {
      const index = e.detail.value
      this.addForm.categoryId = this.expenseCategories[index].id
    },
    
    onAlertThresholdChange(e) {
      this.addForm.alertThreshold = e.detail.value
    },
    
    async handleLongPress(budget) {
      uni.showActionSheet({
        itemList: ['编辑预算', '删除预算'],
        success: async (res) => {
          if (res.tapIndex === 0) {
            this.editBudget = { id: budget.id, amountLimit: String(budget.amountLimit) }
            this.showEditDialog = true
          } else if (res.tapIndex === 1) {
            uni.showModal({
              title: '提示',
              content: '确定要删除这个预算吗？',
              success: async (modalRes) => {
                if (modalRes.confirm) {
                  try {
                    await budgetApi.deleteBudget(budget.id)
                    uni.showToast({ title: '删除成功', icon: 'success' })
                    this.loadData()
                  } catch (e) {
                    console.error(e)
                  }
                }
              }
            })
          }
        }
      })
    },
    
    async handleEditConfirm() {
      if (!this.editBudget.amountLimit || parseFloat(this.editBudget.amountLimit) <= 0) {
        uni.showToast({ title: '请输入有效的预算额度', icon: 'none' })
        return
      }
      
      try {
        await budgetApi.updateBudget(this.editBudget.id, {
          amountLimit: parseFloat(this.editBudget.amountLimit)
        })
        uni.showToast({ title: '更新成功', icon: 'success' })
        this.showEditDialog = false
        this.loadData()
      } catch (e) {
        console.error(e)
      }
    },
    
    async handleAddBudget() {
      if (!this.addForm.categoryId) {
        uni.showToast({ title: '请选择分类', icon: 'none' })
        return
      }
      if (!this.addForm.amountLimit || parseFloat(this.addForm.amountLimit) <= 0) {
        uni.showToast({ title: '请输入有效的预算额度', icon: 'none' })
        return
      }
      
      const now = new Date()
      const month = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
      
      try {
        await budgetApi.createBudget({
          categoryId: this.addForm.categoryId,
          month,
          amountLimit: parseFloat(this.addForm.amountLimit),
          alertThreshold: this.addForm.alertThreshold
        })
        uni.showToast({ title: '添加成功', icon: 'success' })
        this.showAddDialog = false
        this.addForm = { categoryId: null, amountLimit: '', alertThreshold: 80 }
        this.loadData()
      } catch (e) {
        console.error(e)
      }
    }
  }
}
</script>

<style>
.budget-page {
  padding: 20rpx;
  padding-bottom: 120rpx;
}

.custom-nav {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: -30rpx -20rpx 0;
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

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 30rpx;
  margin-bottom: 30rpx;
}

.page-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #303133;
}

.add-btn {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 32rpx;
  padding: 10rpx 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(102, 126, 234, 0.3);
}

.add-icon {
  font-size: 28rpx;
  margin-right: 4rpx;
  line-height: 1;
}

.add-text {
  font-size: 24rpx;
  line-height: 1;
}

.empty-state {
  text-align: center;
  padding: 100rpx 0;
}

.empty-icon {
  font-size: 100rpx;
  display: block;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 32rpx;
  color: #303133;
  display: block;
  margin-bottom: 10rpx;
}

.empty-hint {
  font-size: 26rpx;
  color: #909399;
}

.budget-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.budget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.category-info {
  display: flex;
  align-items: center;
}

.category-icon {
  font-size: 40rpx;
  margin-right: 16rpx;
}

.category-name {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}

.action-icon {
  font-size: 36rpx;
  color: #909399;
  padding: 10rpx;
}

.budget-amounts {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.amount-item {
  text-align: center;
  flex: 1;
}

.amount-label {
  font-size: 22rpx;
  color: #909399;
  display: block;
  margin-bottom: 6rpx;
}

.amount-value {
  font-size: 28rpx;
  font-weight: bold;
  color: #303133;
}

.amount-value.used {
  color: #f56c6c;
}

.amount-value.remaining {
  color: #67c23a;
}

.amount-value.remaining.negative {
  color: #f56c6c;
}

.progress-section {
  margin-top: 10rpx;
}

.progress-bar {
  height: 16rpx;
  background: #f0f0f0;
  border-radius: 8rpx;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 8rpx;
  transition: width 0.3s;
}

.progress-fill.normal {
  background: linear-gradient(90deg, #667eea, #764ba2);
}

.progress-fill.warning {
  background: linear-gradient(90deg, #e6a23c, #f3d19e);
}

.progress-fill.danger {
  background: linear-gradient(90deg, #f56c6c, #f78989);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10rpx;
}

.progress-percent {
  font-size: 24rpx;
  color: #606266;
}

.alert-text {
  font-size: 22rpx;
  color: #e6a23c;
}

.over-text {
  font-size: 22rpx;
  color: #f56c6c;
  font-weight: bold;
}

.budget-summary {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 24rpx 20rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  display: flex;
  justify-content: space-around;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.1);
}

.summary-item {
  text-align: center;
  flex: 1;
}

.summary-icon {
  font-size: 36rpx;
  display: block;
  margin-bottom: 8rpx;
}

.summary-label {
  font-size: 26rpx;
  color: #909399;
  display: block;
  margin-bottom: 8rpx;
}

.summary-value {
  font-size: 36rpx;
  font-weight: bold;
  color: #303133;
}

.summary-value.expense {
  color: #f56c6c;
}

.summary-value.negative {
  color: #f56c6c;
}

.picker-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.picker-content {
  width: 600rpx;
  max-height: 70vh;
  background: #fff;
  border-radius: 24rpx;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.picker-header {
  padding: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.picker-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}

.add-tag-form {
  padding: 30rpx 24rpx;
}

.form-item {
  margin-bottom: 20rpx;
}

.form-label {
  font-size: 26rpx;
  color: #606266;
  margin-bottom: 12rpx;
  display: block;
}

.picker-display {
  padding: 20rpx 24rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.picker-display .placeholder {
  color: #909399;
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

.picker-footer {
  display: flex;
  padding: 20rpx 24rpx;
  border-top: 1rpx solid #f0f0f0;
  gap: 20rpx;
}

.btn-cancel {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  background: #f5f5f5;
  color: #606266;
  border: none;
  border-radius: 40rpx;
  font-size: 28rpx;
}

.btn-confirm {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 40rpx;
  font-size: 28rpx;
}
</style>
