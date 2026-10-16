<template>
  <div class="accounting">
    <el-row :gutter="20">
      <el-col :span="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <span class="card-title">添加记录<span v-if="currentAccountBookName" class="book-name-tag">{{ currentAccountBookName }}</span></span>
              <div class="action-buttons">
                <el-button type="primary" link size="small" @click="downloadTemplate">
                  <el-icon><Download /></el-icon>下载模板
                </el-button>
                <el-upload
                  :show-file-list="false"
                  :auto-upload="false"
                  :on-change="handleFileChange"
                  accept=".xlsx,.xls"
                >
                  <el-button type="success" link size="small" :loading="importing">
                    <el-icon><Upload /></el-icon>导入
                  </el-button>
                </el-upload>
                <el-button type="info" link size="small" @click="handleExport">
                  <el-icon><Document /></el-icon>导出
                </el-button>
              </div>
            </div>
          </template>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
            <el-form-item label="类型">
              <el-radio-group v-model="form.type" @change="handleTypeChange">
                <el-radio-button value="EXPENSE">支出</el-radio-button>
                <el-radio-button value="INCOME">收入</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="金额" prop="amount">
              <el-input-number
                v-model="form.amount"
                :min="0"
                :precision="2"
                :step="10"
                controls-position="right"
                style="width: 100%"
                placeholder="请输入金额"
              />
            </el-form-item>
            <el-form-item label="分类" prop="categoryId">
              <el-select 
                v-model="form.categoryId" 
                placeholder="请选择分类" 
                style="width: 100%"
              >
                <el-option
                  v-for="cat in currentCategories"
                  :key="cat.id"
                  :label="`${cat.icon || '📌'} ${cat.name}`"
                  :value="cat.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="标签">
              <el-select
                v-model="form.tagIds"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="选择或创建标签"
                style="width: 100%"
                :filter-method="handleTagFilter"
                @change="handleTagChange"
              >
                <el-option
                  v-for="tag in tagOptions"
                  :key="tag.id"
                  :label="tag.name"
                  :value="tag.id"
                >
                  <span style="display: flex; align-items: center; gap: 8px;">
                    <span 
                      class="tag-color-dot"
                      :style="{ backgroundColor: tag.color }"
                    ></span>
                    <span>{{ tag.name }}</span>
                  </span>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="日期" prop="transactionDate">
              <el-date-picker
                v-model="form.transactionDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="账户">
              <el-select
                v-model="form.account"
                placeholder="请选择账户（可选）"
                clearable
                style="width: 100%"
              >
                <el-option label="微信钱包" value="微信钱包" />
                <el-option label="支付宝" value="支付宝" />
                <el-option label="银行卡" value="银行卡" />
                <el-option label="现金" value="现金" />
              </el-select>
            </el-form-item>
            <el-form-item label="备注">
              <div style="display: flex; gap: 8px; width: 100%">
                <el-input
                  v-model="form.note"
                  type="textarea"
                  :rows="2"
                  placeholder="可选，支持智能分类推荐"
                  style="flex: 1"
                />
                <el-tooltip content="智能分类" placement="top">
                  <el-button 
                    type="primary" 
                    :icon="MagicStick" 
                    @click="handleClassifySuggest"
                    :disabled="!form.note || classifying"
                    :loading="classifying"
                  />
                </el-tooltip>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" style="width: 100%" @click="handleSubmit">
                保存
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span class="card-title">最近7天记录</span>
              <el-select 
                v-model="filterTagId" 
                placeholder="按标签筛选" 
                clearable
                size="small"
                style="width: 150px;"
                @change="loadTransactions"
              >
                <el-option label="全部" :value="null" />
                <el-option
                  v-for="tag in tagOptions"
                  :key="tag.id"
                  :label="tag.name"
                  :value="tag.id"
                />
              </el-select>
            </div>
          </template>
          <div v-loading="loading" class="transactions-list">
            <el-empty v-if="groupedTransactions.length === 0" description="暂无记录" />
            <div v-for="group in groupedTransactions" :key="group.date" class="date-group">
              <div class="date-header">
                <span class="date">{{ group.date }}</span>
                <span class="date-summary">
                  支出: ¥{{ group.expense }} / 收入: ¥{{ group.income }}
                </span>
              </div>
              <el-table :data="group.transactions" size="small" stripe>
                <el-table-column width="50" align="center">
                  <template #default="{ row }">
                    <span class="category-icon">{{ row.category?.icon || '📌' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="category.name" label="分类" width="100" />
                <el-table-column prop="note" label="备注" show-overflow-tooltip>
                  <template #default="{ row }">
                    <div>
                      <div>{{ row.note || '-' }}</div>
                      <div v-if="row.tags && row.tags.length > 0" class="transaction-tags">
                        <el-tag 
                          v-for="tag in row.tags" 
                          :key="tag.id"
                          size="small"
                          :style="{ backgroundColor: tag.color + '20', borderColor: tag.color, color: tag.color }"
                        >{{ tag.name }}</el-tag>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="金额" width="100" align="right">
                  <template #default="{ row }">
                    <span :class="row.type === 'EXPENSE' ? 'expense' : 'income'">
                      {{ row.type === 'EXPENSE' ? '-' : '+' }}¥{{ row.amount }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column width="100" align="center">
                  <template #default="{ row }">
                    <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-dialog v-model="suggestionVisible" title="智能分类推荐" width="400px">
      <div v-if="suggestion && suggestion.matched" class="suggestion-content">
        <div class="suggestion-icon">✨</div>
        <div class="suggestion-text">
          检测到关键词 "<span class="highlight">{{ suggestion.matchedKeyword }}</span>"，
          建议分类为：
        </div>
        <div class="suggestion-category">
          <span class="category-badge" :class="suggestion.categoryType === 'INCOME' ? 'income' : 'expense'">
            {{ suggestion.categoryName }}
          </span>
        </div>
      </div>
      <div v-else class="suggestion-content">
        <div class="suggestion-icon">🤔</div>
        <div class="suggestion-text" style="color: #909399;">
          未匹配到关键词，建议手动选择分类
        </div>
      </div>
      <template #footer>
        <el-button @click="suggestionVisible = false">取消</el-button>
        <el-button v-if="suggestion && suggestion.matched" type="primary" @click="applySuggestion">应用</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="importResultVisible" title="导入结果" width="500px">
      <el-result
        :icon="importResult.failCount > 0 ? 'warning' : 'success'"
        :title="importResult.failCount > 0 ? '部分导入成功' : '导入成功'"
        :sub-title="`成功导入 ${importResult.successCount} 条，失败 ${importResult.failCount} 条`"
      >
        <template #extra>
          <div v-if="importResult.errors && importResult.errors.length > 0" class="import-errors">
            <div class="errors-title">错误详情：</div>
            <div class="errors-list">
              <div v-for="(error, index) in importResult.errors.slice(0, 10)" :key="index" class="error-item">
                {{ error }}
              </div>
              <div v-if="importResult.errors.length > 10" class="error-more">
                还有 {{ importResult.errors.length - 10 }} 条错误...
              </div>
            </div>
          </div>
        </template>
      </el-result>
      <template #footer>
        <el-button type="primary" @click="importResultVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getCategories } from '@/api/category'
import { 
  createTransaction, 
  getRecentTransactions, 
  deleteTransaction,
  importTransactions,
  exportTransactions,
  downloadTemplate,
  classifySuggest
} from '@/api/transaction'
import { getTags, createTag } from '@/api/tag'
import { useUserStore } from '@/stores/userStore'
import { getAccountBooks } from '@/api/accountBook'

const userStore = useUserStore()
const accountBooks = ref([])

const currentAccountBookName = computed(() => {
  if (!userStore.currentAccountBookId) return ''
  const book = accountBooks.value.find(b => b.id === userStore.currentAccountBookId)
  return book ? book.name : ''
})

const formRef = ref(null)
const submitting = ref(false)
const loading = ref(false)
const importing = ref(false)
const classifying = ref(false)
const importResultVisible = ref(false)
const suggestionVisible = ref(false)
const suggestion = ref(null)
const filterTagId = ref(null)
const tagOptions = ref([])
const importResult = ref({
  successCount: 0,
  failCount: 0,
  errors: []
})

const categories = ref({ EXPENSE: [], INCOME: [] })
const transactions = ref([])

const form = reactive({
  type: 'EXPENSE',
  amount: null,
  categoryId: null,
  transactionDate: dayjs().format('YYYY-MM-DD'),
  account: '',
  note: '',
  tagIds: []
})

const rules = {
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  transactionDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

const currentCategories = computed(() => categories.value[form.type] || [])

const groupedTransactions = computed(() => {
  const groups = {}
  transactions.value.forEach(t => {
    const date = t.transactionDate
    if (!groups[date]) {
      groups[date] = { date, expense: 0, income: 0, transactions: [] }
    }
    if (t.type === 'EXPENSE') {
      groups[date].expense = Number(groups[date].expense) + Number(t.amount)
    } else {
      groups[date].income = Number(groups[date].income) + Number(t.amount)
    }
    groups[date].transactions.push(t)
  })
  return Object.values(groups).sort((a, b) => b.date.localeCompare(a.date))
})

async function loadCategories() {
  const data = await getCategories()
  categories.value = data
}

async function loadTags() {
  const data = await getTags()
  allTagOptions.value = data
  tagOptions.value = [...data]
}

async function loadTransactions() {
  loading.value = true
  try {
    if (filterTagId.value) {
      const { getTransactions } = await import('@/api/transaction')
      const data = await getTransactions({ tagId: filterTagId.value, size: 100 })
      transactions.value = data.content || data
    } else {
      const data = await getRecentTransactions(7)
      transactions.value = data
    }
  } finally {
    loading.value = false
  }
}

function handleTypeChange() {
  form.categoryId = null
}

const allTagOptions = ref([])

function handleTagFilter(val) {
  if (val && val.trim()) {
    const filtered = allTagOptions.value.filter(tag => 
      tag.name.toLowerCase().includes(val.toLowerCase())
    )
    tagOptions.value = filtered
  } else {
    tagOptions.value = [...allTagOptions.value]
  }
}

async function handleTagChange(newTagIds) {
  if (!newTagIds || newTagIds.length === 0) return
  
  const lastTagId = newTagIds[newTagIds.length - 1]
  if (typeof lastTagId === 'string' && !allTagOptions.value.some(t => String(t.id) === lastTagId)) {
    try {
      const newTag = await createTag({ name: lastTagId })
      allTagOptions.value.push(newTag)
      tagOptions.value = [...allTagOptions.value]
      form.tagIds = newTagIds.map(id => 
        String(id) === String(lastTagId) ? newTag.id : id
      )
    } catch (e) {
      form.tagIds = newTagIds.filter(id => id !== lastTagId)
      ElMessage.error('创建标签失败')
    }
  }
}

async function handleClassifySuggest() {
  if (!form.note) {
    ElMessage.warning('请先输入备注')
    return
  }
  
  classifying.value = true
  try {
    const result = await classifySuggest(form.note)
    suggestion.value = result
    suggestionVisible.value = true
  } catch (e) {
    console.error('分类推荐失败', e)
    suggestion.value = { matched: false }
    suggestionVisible.value = true
  } finally {
    classifying.value = false
  }
}

function applySuggestion() {
  if (suggestion.value && suggestion.value.matched && suggestion.value.categoryId !== null && suggestion.value.categoryId !== undefined) {
    form.categoryId = Number(suggestion.value.categoryId)
    if (suggestion.value.categoryType) {
      form.type = suggestion.value.categoryType
    }
    suggestionVisible.value = false
    ElMessage.success('已应用推荐分类')
  } else {
    ElMessage.info('未找到匹配的分类建议')
    suggestionVisible.value = false
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  
  const submitData = {
    ...form,
    categoryId: form.categoryId ? Number(form.categoryId) : null,
    tagIds: form.tagIds.map(id => Number(id)).filter(id => !isNaN(id))
  }
  
  submitting.value = true
  try {
    await createTransaction(submitData)
    ElMessage.success('保存成功')
    resetForm()
    await loadTransactions()
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  form.amount = null
  form.categoryId = null
  form.account = ''
  form.note = ''
  form.transactionDate = dayjs().format('YYYY-MM-DD')
  form.tagIds = []
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除这条记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTransaction(row.id)
    ElMessage.success('删除成功')
    loadTransactions()
  } catch {
  }
}

function handleBeforeUpload(file) {
  const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
                  file.type === 'application/vnd.ms-excel'
  if (!isExcel) {
    ElMessage.error('只能上传 Excel 文件！')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('上传文件大小不能超过 10MB！')
    return false
  }
  return true
}

async function handleFileChange(file) {
  // el-upload on-change 在 auto-upload=false 时仅触发一次 ready；
  // 此处再加一层防重入锁，绝对避免重复调用后端导入接口
  if (!file.raw || file.status !== 'ready') return
  if (importing.value) return

  importing.value = true
  try {
    const result = await importTransactions(file.raw)
    importResult.value = result
    importResultVisible.value = true

    if (result.successCount > 0) {
      loadTransactions()
    }
  } catch (e) {
    console.error('导入失败', e)
  } finally {
    importing.value = false
  }
}

function handleExport() {
  exportTransactions()
}

onMounted(async () => {
  try {
    accountBooks.value = await getAccountBooks()
  } catch (e) {
    console.error('加载账本列表失败', e)
  }
  loadCategories()
  loadTags()
  loadTransactions()
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

.book-name-tag {
  font-size: 12px;
  font-weight: normal;
  color: #409eff;
  margin-left: 8px;
  padding: 2px 8px;
  background: #ecf5ff;
  border-radius: 4px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.category-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.tag-color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
}

.transactions-list {
  max-height: 600px;
  overflow-y: auto;
}

.date-group {
  margin-bottom: 20px;
}

.date-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 10px;
}

.date {
  font-weight: bold;
  color: #303133;
}

.date-summary {
  font-size: 12px;
  color: #909399;
}

.category-icon {
  font-size: 20px;
}

.transaction-tags {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.expense {
  color: #f56c6c;
  font-weight: bold;
}

.income {
  color: #67c23a;
  font-weight: bold;
}

.suggestion-content {
  text-align: center;
  padding: 20px 0;
}

.suggestion-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.suggestion-text {
  font-size: 14px;
  color: #606266;
  margin-bottom: 16px;
}

.suggestion-category {
  margin-top: 16px;
}

.highlight {
  color: #409eff;
  font-weight: bold;
}

.category-badge {
  display: inline-block;
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 16px;
  font-weight: bold;
  color: #fff;
}

.category-badge.expense {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.category-badge.income {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.import-errors {
  text-align: left;
}

.errors-title {
  font-weight: bold;
  color: #f56c6c;
  margin-bottom: 8px;
}

.errors-list {
  max-height: 200px;
  overflow-y: auto;
}

.error-item {
  font-size: 13px;
  color: #606266;
  padding: 4px 0;
  border-bottom: 1px solid #f5f7fa;
}

.error-more {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
