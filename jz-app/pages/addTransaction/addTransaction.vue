<template>
  <view class="add-transaction-page">
    <view class="custom-nav">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
      <view class="nav-bar">
        <text class="nav-title">{{ isEditMode ? '编辑流水' : '记一笔' }}</text>
        <view class="nav-close" @click="goBack">
          <text>✕</text>
        </view>
      </view>
    </view>

    <view class="form-section card">
      <view class="type-tabs">
        <view 
          class="type-tab" 
          :class="{ active: form.type === 'EXPENSE' }"
          @click="switchType('EXPENSE')"
        >
          支出
        </view>
        <view 
          class="type-tab" 
          :class="{ active: form.type === 'INCOME' }"
          @click="switchType('INCOME')"
        >
          收入
        </view>
      </view>
      
      <view class="amount-input">
        <text class="currency">¥</text>
        <input 
          v-model="form.amount" 
          type="digit" 
          class="amount" 
          placeholder="0.00"
          :focus="amountFocus"
          @input="onAmountInput"
        />
      </view>
      
      <view class="section-title">请选择分类</view>
      <scroll-view scroll-x class="category-scroll" :show-scrollbar="false" enable-flex>
        <view class="category-chip-list">
          <view
            v-for="cat in currentCategoryList"
            :key="cat.id"
            class="category-chip"
            :class="{ active: form.categoryId === cat.id }"
            @click="selectCategory(cat)"
          >
            <text class="chip-text">{{ cat.icon || '📌' }} {{ cat.name }}</text>
          </view>
          <view class="category-chip chip-add" @click="showAddCategory = true">
            <text class="chip-text">+ 新建</text>
          </view>
        </view>
      </scroll-view>
      
      <view class="section-title">请选择标签</view>
      <scroll-view scroll-x class="category-scroll" :show-scrollbar="false" enable-flex>
        <view class="category-chip-list">
          <view
            v-for="tag in tags"
            :key="tag.id"
            class="category-chip"
            :class="{ active: form.tagIds.includes(tag.id) }"
            @click="toggleTag(tag.id)"
          >
            <text class="chip-text">{{ tag.name }}</text>
          </view>
          <view class="category-chip chip-add" @click="showAddTag = true">
            <text class="chip-text">+ 新建</text>
          </view>
        </view>
      </scroll-view>
      
      <view class="section-title">日期</view>
      <view class="date-picker" @click="openDatePicker">
        <text>📅 {{ form.transactionDate }}</text>
      </view>

      <view class="section-title">账户</view>
      <scroll-view scroll-x class="category-scroll" :show-scrollbar="false" enable-flex>
        <view class="category-chip-list">
          <view
            v-for="opt in accountOptions"
            :key="opt.name"
            class="category-chip"
            :class="{ active: account === opt.name }"
            @click="selectAccount(opt)"
          >
            <text class="chip-text">{{ opt.icon }} {{ opt.name }}</text>
          </view>
          <view v-if="account" class="category-chip chip-add" @click="clearAccount">
            <text class="chip-text">✕ 清空</text>
          </view>
        </view>
      </scroll-view>

      <view class="section-title">备注</view>
      <view class="note-row">
        <input 
          v-model="form.note" 
          class="note-input" 
          placeholder="可选，支持智能分类推荐"
          maxlength="50"
        />
        <view 
          class="suggest-btn" 
          :class="{ disabled: !form.note }"
          @click="handleSuggest"
        >
          <text>🔮</text>
        </view>
      </view>
      
      <button 
        class="btn-save" 
        :disabled="!canSave"
        @click="handleSave"
      >
        {{ isEditMode ? '保存修改' : '保存' }}
      </button>
    </view>

    <view v-if="showAddTag" class="picker-mask" @click="showAddTag = false">
      <view class="picker-content" @click.stop>
        <view class="picker-header">
          <text class="picker-title">添加标签</text>
        </view>
        <view class="add-tag-form">
          <input 
            v-model="newTagName" 
            class="tag-name-input" 
            placeholder="输入标签名称"
            maxlength="20"
          />
        </view>
        <view class="picker-footer">
          <button class="btn-cancel" @click="showAddTag = false">取消</button>
          <button class="btn-confirm" @click="handleAddTag">确认</button>
        </view>
      </view>
    </view>
    
    <view v-if="showSuggestModal" class="picker-mask" @click="showSuggestModal = false">
      <view class="picker-content" @click.stop>
        <view class="picker-header">
          <text class="picker-title">智能推荐</text>
        </view>
        <view class="suggest-content">
          <view class="suggest-note">
            <text class="suggest-label">您的备注：</text>
            <text class="suggest-text">{{ form.note }}</text>
          </view>
          <view v-if="suggestResult && suggestResult.matchedKeyword" class="suggest-match">
            <text class="suggest-label">检测到关键词：</text>
            <text class="suggest-keyword">{{ suggestResult.matchedKeyword }}</text>
          </view>
          <view v-if="suggestResult && suggestResult.matched && suggestResult.category" class="suggest-category">
            <text class="suggest-label">推荐分类：</text>
            <view class="suggest-category-badge">
              <text>{{ (suggestResult.category && suggestResult.category.icon) || '📌' }} {{ (suggestResult.category && suggestResult.category.name) || '' }}</text>
            </view>
          </view>
          <view v-else class="suggest-no-match">
            <text>未匹配到关键词，建议手动选择分类</text>
          </view>
        </view>
        <view class="picker-footer">
          <button class="btn-cancel" @click="showSuggestModal = false">取消</button>
          <button v-if="suggestResult && suggestResult.matched && suggestResult.category" class="btn-confirm" @click="applySuggestion">应用</button>
        </view>
      </view>
    </view>

    <view v-if="showAddCategory" class="picker-mask" @click="showAddCategory = false">
      <view class="picker-content" @click.stop>
        <view class="picker-header">
          <text class="picker-title">新建{{ form.type === 'EXPENSE' ? '支出' : '收入' }}分类</text>
        </view>
        <view class="add-tag-form">
          <view class="form-item-inline">
            <text class="inline-label">图标</text>
            <input
              v-model="newCategory.icon"
              class="inline-input icon-input"
              placeholder="emoji"
              maxlength="4"
            />
          </view>
          <view class="form-item-inline">
            <text class="inline-label">名称</text>
            <input
              v-model="newCategory.name"
              class="inline-input"
              placeholder="分类名称"
              maxlength="20"
            />
          </view>
        </view>
        <view class="picker-footer">
          <button class="btn-cancel" @click="showAddCategory = false">取消</button>
          <button class="btn-confirm" @click="handleAddCategory">确认</button>
        </view>
      </view>
    </view>

    <view v-if="showDatePicker" class="picker-mask" @click="showDatePicker = false">
      <view class="picker-content date-picker-content" @click.stop>
        <view class="picker-header">
          <text class="picker-title">选择日期</text>
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
            :class="{ empty: !cell, active: cell && isSelectedDay(cell) }"
            @click="cell && pickDay(cell)"
          >
            <text v-if="cell">{{ cell }}</text>
          </view>
        </view>
      </view>
    </view>

  </view>
</template>

<script>
import { categoryApi, tagApi, transactionApi } from '../../api/index.js';
import { offlineSync } from '../../utils/offlineSync.js';
import { isOnline } from '../../utils/request.js';

export default {
  data() {
    const today = new Date()
    const dateStr = today.toISOString().split('T')[0]
    
    return {
      statusBarHeight: 0,
      isEditMode: false,
      editId: null,
      loading: false,
      categories: { EXPENSE: [], INCOME: [] },
      tags: [],
      showAddTag: false,
      showSuggestModal: false,
      showAddCategory: false,
      newTagName: '',
      newCategory: { name: '', icon: '' },
      suggestResult: {},
      account: '',
      accountOptions: [
        { name: '微信钱包', icon: '💬' },
        { name: '支付宝', icon: '🔵' },
        { name: '银行卡', icon: '💳' },
        { name: '现金', icon: '💰' }
      ],
      amountFocus: false,
      showDatePicker: false,
      calYear: 0,
      calMonth: 0,
      weekDays: ['日', '一', '二', '三', '四', '五', '六'],
      form: {
        amount: '',
        type: 'EXPENSE',
        categoryId: null,
        transactionDate: dateStr,
        note: '',
        tagIds: []
      }
    }
  },
  
  computed: {
    currentCategoryList() {
      return this.categories[this.form.type] || []
    },
    
    selectedCategory() {
      if (!this.form.categoryId) return null
      const allCategories = [...(this.categories.EXPENSE || []), ...(this.categories.INCOME || [])]
      return allCategories.find(c => c.id === this.form.categoryId)
    },

    canSave() {
      return this.form.amount && parseFloat(this.form.amount) > 0 && this.form.categoryId
    },

    calCells() {
      const firstWeekday = new Date(this.calYear, this.calMonth - 1, 1).getDay()
      const days = new Date(this.calYear, this.calMonth, 0).getDate()
      const cells = []
      for (let i = 0; i < firstWeekday; i++) cells.push(null)
      for (let d = 1; d <= days; d++) cells.push(d)
      return cells
    }
  },

  
  async onLoad(options) {
    this.eventChannel = options.eventChannel
    // 获取状态栏高度
    const sysInfo = uni.getSystemInfoSync()
    this.statusBarHeight = sysInfo.statusBarHeight || 0

    // 编辑模式：解析传入的流水数据
    if (options.mode === 'edit' && options.data) {
      try {
        const editData = JSON.parse(decodeURIComponent(options.data))
        this.isEditMode = true
        this.editId = editData.id
        this.form.amount = editData.amount || ''
        this.form.type = editData.type || 'EXPENSE'
        this.form.categoryId = editData.categoryId || null
        this.form.transactionDate = editData.transactionDate || this.form.transactionDate
        this.form.note = editData.note || ''
        this.form.tagIds = editData.tagIds || []
        this.account = editData.account || ''
      } catch (e) {
        console.error('解析编辑数据失败:', e)
      }
    }
    
    await this.loadData()
  },

  onReady() {
    // 进入页面默认聚焦金额输入框并调起数字键盘
    this.amountFocus = true
  },
  
  methods: {
    goBack() {
      if (this.eventChannel && this.eventChannel.emit) {
        this.eventChannel.emit('transactionAdded')
      }
      uni.navigateBack({
        fail: () => {
          uni.switchTab({ url: '/pages/index/index' })
        }
      })
    },
    
    async loadData() {
      try {
        const [catData, tagData] = await Promise.all([
          categoryApi.getCategories().catch(e => { console.error('getCategories error:', e); return { EXPENSE: [], INCOME: [] } }),
          tagApi.getTags().catch(e => { console.error('getTags error:', e); return [] })
        ])
        this.categories = catData && catData.EXPENSE ? catData : { EXPENSE: [], INCOME: [] }
        this.tags = tagData || []
      } catch (e) {
        console.error(e)
      }
    },
    
    toggleTag(tagId) {
      const index = this.form.tagIds.indexOf(tagId)
      if (index > -1) {
        this.form.tagIds.splice(index, 1)
      } else {
        this.form.tagIds.push(tagId)
      }
    },

    async handleAddTag() {
      if (!this.newTagName.trim()) {
        uni.showToast({ title: '请输入标签名称', icon: 'none' })
        return
      }
      try {
        const newTag = await tagApi.createTag({ name: this.newTagName.trim() })
        this.tags.push(newTag)
        this.form.tagIds.push(newTag.id)
        this.showAddTag = false
        this.newTagName = ''
      } catch (e) {
        uni.showToast({ title: '添加失败', icon: 'none' })
      }
    },
    
    async handleSuggest() {
      if (!this.form.note) {
        uni.showToast({ title: '请先输入备注', icon: 'none' })
        return
      }
      try {
        const result = await transactionApi.classifySuggest(this.form.note)
        this.suggestResult = result || { matched: false, category: null }
        this.showSuggestModal = true
      } catch (e) {
        console.error('推荐失败:', e)
        this.suggestResult = { matched: false, category: null }
        this.showSuggestModal = true
      }
    },
    
    applySuggestion() {
      if (this.suggestResult && this.suggestResult.categoryId) {
        this.form.categoryId = this.suggestResult.categoryId
        if (this.suggestResult.categoryType) {
          this.form.type = this.suggestResult.categoryType
        } else if (this.suggestResult.category && this.suggestResult.category.type) {
          this.form.type = this.suggestResult.category.type
        }
      }
      this.showSuggestModal = false
    },
    
    switchType(type) {
      this.form.type = type
      this.form.categoryId = null
    },
    
    selectCategory(cat) {
      this.form.categoryId = cat.id
    },

    async handleAddCategory() {
      if (!this.newCategory.name.trim()) {
        uni.showToast({ title: '请输入分类名称', icon: 'none' })
        return
      }
      try {
        const created = await categoryApi.createCategory({
          name: this.newCategory.name.trim(),
          type: this.form.type,
          icon: this.newCategory.icon || '📌'
        })
        if (!this.categories[this.form.type]) {
          this.categories[this.form.type] = []
        }
        this.categories[this.form.type].push(created)
        this.form.categoryId = created.id
        this.showAddCategory = false
        this.newCategory = { name: '', icon: '' }
        uni.showToast({ title: '分类创建成功', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: '创建失败', icon: 'none' })
      }
    },
    
    onAmountInput(e) {
      let v = (e.detail && e.detail.value) || ''
      // 只允许数字和小数点，且最多一个小数点、两位小数
      v = v.replace(/[^\d.]/g, '')
      const parts = v.split('.')
      if (parts.length > 2) {
        v = parts[0] + '.' + parts.slice(1).join('')
      }
      if (parts[1] && parts[1].length > 2) {
        v = parts[0] + '.' + parts[1].slice(0, 2)
      }
      this.form.amount = v
    },

    openDatePicker() {
      const parts = (this.form.transactionDate || '').split('-')
      this.calYear = parseInt(parts[0], 10) || new Date().getFullYear()
      this.calMonth = parseInt(parts[1], 10) || new Date().getMonth() + 1
      this.showDatePicker = true
    },

    changeMonth(delta) {
      let m = this.calMonth + delta
      let y = this.calYear
      if (m < 1) {
        m = 12
        y--
      } else if (m > 12) {
        m = 1
        y++
      }
      this.calMonth = m
      this.calYear = y
    },

    isSelectedDay(day) {
      const parts = (this.form.transactionDate || '').split('-')
      const sy = parseInt(parts[0], 10)
      const sm = parseInt(parts[1], 10)
      const sd = parseInt(parts[2], 10)
      return this.calYear === sy && this.calMonth === sm && day === sd
    },

    pickDay(day) {
      const mm = String(this.calMonth).padStart(2, '0')
      const dd = String(day).padStart(2, '0')
      this.form.transactionDate = `${this.calYear}-${mm}-${dd}`
      this.showDatePicker = false
    },

    selectAccount(opt) {
      this.account = opt.name
    },

    clearAccount() {
      this.account = ''
    },

    async handleSave() {
      if (!this.canSave) return
      
      const data = {
        amount: parseFloat(this.form.amount),
        type: this.form.type,
        categoryId: this.form.categoryId,
        transactionDate: this.form.transactionDate,
        note: this.form.note || null,
        tagIds: this.form.tagIds.length > 0 ? this.form.tagIds : null,
        account: this.account || null
      }
      
      // 编辑模式：调用 update
      if (this.isEditMode && this.editId) {
        try {
          await transactionApi.update(this.editId, data)
          uni.showToast({ title: '修改成功', icon: 'success' })
          setTimeout(() => {
            this.goBack()
          }, 1500)
        } catch (e) {
          uni.showToast({ title: e.message || '修改失败', icon: 'none' })
        }
        return
      }
      
      // 新增模式
      const online = await isOnline()
      
      if (online) {
        try {
          await transactionApi.create(data)
          uni.showToast({ title: '保存成功', icon: 'success' })
          setTimeout(() => {
            this.goBack()
          }, 1500)
        } catch (e) {
          offlineSync.addTransaction(data)
          uni.showToast({ title: '保存到本地，待同步', icon: 'none' })
          setTimeout(() => {
            this.goBack()
          }, 1500)
        }
      } else {
        offlineSync.addTransaction(data)
        uni.showToast({ title: '离线保存，联网后同步', icon: 'none' })
        setTimeout(() => {
          this.goBack()
        }, 1500)
      }
    }
  }
}
</script>

<style>
.add-transaction-page {
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

.nav-close {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  font-size: 32rpx;
  color: #fff;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin: 20rpx;
}

.type-tabs {
  display: flex;
  background: #f5f5f5;
  border-radius: 44rpx;
  padding: 8rpx;
  margin-bottom: 30rpx;
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

.amount-input {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30rpx;
}

.currency {
  font-size: 48rpx;
  color: #303133;
  margin-right: 10rpx;
}

.amount {
  font-size: 72rpx;
  font-weight: bold;
  color: #303133;
  min-width: 200rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 16rpx;
}

.category-scroll {
  width: 100%;
  white-space: nowrap;
  margin-bottom: 20rpx;
}

.category-chip-list {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  padding: 6rpx 4rpx;
  gap: 16rpx;
}

.category-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 64rpx;
  padding: 0 28rpx;
  border-radius: 32rpx;
  background: #fff;
  border: 1rpx solid #d6e3f3;
  transition: all 0.2s;
  flex-shrink: 0;
}

.category-chip .chip-text {
  font-size: 26rpx;
  color: #3a6db0;
  line-height: 1;
  white-space: nowrap;
}

.category-chip.active {
  background: linear-gradient(135deg, #4a90ff 0%, #3b7ce8 100%);
  border-color: transparent;
  box-shadow: 0 4rpx 12rpx rgba(74, 144, 255, 0.35);
}

.category-chip.active .chip-text {
  color: #fff;
  font-weight: 600;
}

.category-chip.chip-add {
  background: #f5f8ff;
  border-style: dashed;
}

.category-chip.chip-add .chip-text {
  color: #6b8bbf;
}

.date-picker {
  padding: 16rpx 24rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  font-size: 26rpx;
  margin-bottom: 20rpx;
}

.date-picker-content {
  width: 640rpx;
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

.cal-cell.active {
  color: #fff;
}

.cal-cell.active text {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-weight: bold;
}

.btn-confirm.full {
  flex: 1;
}

.tag-select {
  display: none;
}

.note-row {
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border-radius: 12rpx;
  margin-bottom: 24rpx;
}

.note-input {
  flex: 1;
  padding: 20rpx 24rpx;
  font-size: 26rpx;
  box-sizing: border-box;
  min-width: 0;
  min-height: 80rpx;
  line-height: 1.6;
}

.suggest-btn {
  width: 80rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  padding-right: 16rpx;
}

.suggest-btn.disabled {
  opacity: 0.4;
}

.btn-save {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 44rpx;
  font-size: 30rpx;
}

.btn-save[disabled] {
  opacity: 0.6;
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

.suggest-content {
  padding: 30rpx 24rpx;
}

.suggest-note,
.suggest-match,
.suggest-category {
  margin-bottom: 24rpx;
}

.suggest-label {
  font-size: 26rpx;
  color: #909399;
  display: block;
  margin-bottom: 12rpx;
}

.suggest-text {
  font-size: 28rpx;
  color: #303133;
}

.suggest-keyword {
  font-size: 28rpx;
  color: #e6a23c;
  font-weight: bold;
}

.suggest-category-badge {
  display: inline-block;
  padding: 12rpx 24rpx;
  background: rgba(64, 158, 255, 0.1);
  border-radius: 24rpx;
}

.suggest-category-badge text {
  font-size: 28rpx;
  color: #409eff;
}

.suggest-no-match {
  text-align: center;
  padding: 40rpx 0;
}

.suggest-no-match text {
  font-size: 26rpx;
  color: #909399;
}

.popup-mask,
.popup-content,
.popup-header,
.popup-title,
.popup-close,
.popup-list,
.popup-item,
.popup-empty,
.popup-add-btn,
.popup-footer-bar {
  display: none;
}

.form-item-inline {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.inline-label {
  font-size: 28rpx;
  color: #606266;
  width: 80rpx;
  flex-shrink: 0;
}

.inline-input {
  flex: 1;
  height: 80rpx;
  padding: 0 20rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.icon-input {
  width: 120rpx;
  flex: none;
  text-align: center;
}

.popup-footer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  border-top: 2rpx solid #eee;
}

.selected-count {
  font-size: 26rpx;
  color: #909399;
}

.btn-confirm-small {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 32rpx;
  padding: 12rpx 40rpx;
  font-size: 26rpx;
  line-height: 1.4;
}
</style>
