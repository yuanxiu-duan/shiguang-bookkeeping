<template>
  <div class="bill-flow">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">账单流水</span>
          <div class="header-actions">
            <el-radio-group v-model="timeMode" size="small" @change="handleTimeModeChange">
              <el-radio-button value="month">按月</el-radio-button>
              <el-radio-button value="year">按年</el-radio-button>
            </el-radio-group>
            
            <el-date-picker
              v-if="timeMode === 'month'"
              v-model="currentMonth"
              type="month"
              placeholder="选择月份"
              value-format="YYYY-MM"
              size="small"
              style="width: 140px; margin-left: 12px;"
              @change="loadData"
            />
            
            <el-select
              v-else
              v-model="currentYear"
              placeholder="选择年份"
              clearable
              size="small"
              style="width: 120px; margin-left: 12px;"
              @change="loadData"
            >
              <el-option
                v-for="year in yearOptions"
                :key="year"
                :label="`${year}年`"
                :value="year"
              />
            </el-select>
            
            <el-radio-group v-model="filterType" size="small" style="margin-left: 12px;" @change="handleFilterChange">
              <el-radio-button value="EXPENSE">支出</el-radio-button>
              <el-radio-button value="INCOME">收入</el-radio-button>
              <el-radio-button value="ALL">全部</el-radio-button>
            </el-radio-group>

            <el-select
              v-model="filterAccount"
              placeholder="账户"
              clearable
              size="small"
              style="width: 130px; margin-left: 12px;"
              @change="handleFilterChange"
            >
              <el-option label="微信钱包" value="微信钱包" />
              <el-option label="支付宝" value="支付宝" />
              <el-option label="银行卡" value="银行卡" />
              <el-option label="现金" value="现金" />
            </el-select>

            <el-button type="primary" size="small" style="margin-left: 12px;" @click="handleExport">
              <el-icon><Download /></el-icon>导出
            </el-button>
          </div>
        </div>
      </template>

      <div class="summary-bar" v-loading="summaryLoading">
        <div class="summary-item income">
          <div class="summary-label">{{ timeModeLabel }}总收入</div>
          <div class="summary-value">¥{{ monthlySummary.income.toFixed(2) }}</div>
        </div>
        <div class="summary-item expense">
          <div class="summary-label">{{ timeModeLabel }}总支出</div>
          <div class="summary-value">¥{{ monthlySummary.expense.toFixed(2) }}</div>
        </div>
        <div class="summary-item balance" :class="{ negative: monthlySummary.balance < 0 }">
          <div class="summary-label">{{ timeModeLabel }}结余</div>
          <div class="summary-value">¥{{ monthlySummary.balance.toFixed(2) }}</div>
        </div>
      </div>

      <div v-loading="loading" class="transactions-list">
        <el-empty v-if="filteredTransactions.length === 0 && !loading" description="暂无账单记录" />
        
        <div v-for="group in groupedTransactions" :key="group.date" class="date-group">
          <div class="date-header">
            <span class="date">{{ formatDate(group.date) }}</span>
            <span class="date-summary">
              收: ¥{{ group.income.toFixed(2) }}, 支: ¥{{ group.expense.toFixed(2) }}, 余: ¥{{ (group.income - group.expense).toFixed(2) }}
            </span>
          </div>
          <el-table :data="group.transactions" size="small" stripe>
            <el-table-column width="50" align="center">
              <template #default="{ row }">
                <span class="category-icon">{{ row.category?.icon || '📌' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="category.name" label="分类" width="180" />
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
            <el-table-column label="账户" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.account" type="info" effect="plain" size="small">{{ row.account }}</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="添加人" width="140" align="center">
              <template #default="{ row }">
                <div class="created-by-cell">
                  <el-avatar :size="24" :src="row.createdByAvatar" icon="UserFilled" />
                  <span class="created-by-name">{{ row.createdBy || '未知' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="120" align="right">
              <template #default="{ row }">
                <span :class="row.type === 'EXPENSE' ? 'expense' : 'income'">
                  {{ row.type === 'EXPENSE' ? '-' : '+' }}¥{{ row.amount }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleEdit(row)">修改</el-button>
                <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <div class="pagination-container" v-if="totalPages > 1">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadData"
            @current-change="loadData"
          />
        </div>
      </div>
    </el-card>

    <!-- 编辑流水弹窗 -->
    <el-dialog v-model="editDialogVisible" title="修改流水" width="500px" :close-on-click-modal="false">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="80px">
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="editForm.type" @change="handleEditTypeChange">
            <el-radio-button value="EXPENSE">支出</el-radio-button>
            <el-radio-button value="INCOME">收入</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="editForm.amount" :min="0.01" :precision="2" :step="10" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="editForm.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="cat in editCategoryOptions"
              :key="cat.id"
              :label="`${cat.icon || '📌'} ${cat.name}`"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="editForm.tagIds" multiple filterable placeholder="选择标签（可选）" style="width: 100%">
            <el-option v-for="tag in editTagOptions" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="transactionDate">
          <el-date-picker v-model="editForm.transactionDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.note" maxlength="200" show-word-limit placeholder="可选" />
        </el-form-item>
        <el-form-item label="账户">
          <el-select v-model="editForm.account" placeholder="请选择账户（可选）" clearable style="width: 100%">
            <el-option label="微信钱包" value="微信钱包" />
            <el-option label="支付宝" value="支付宝" />
            <el-option label="银行卡" value="银行卡" />
            <el-option label="现金" value="现金" />
          </el-select>
        </el-form-item>
        <el-form-item label="添加人">
          <el-tag type="info">{{ editForm.createdBy || '未知' }}</el-tag>
          <span style="color: #909399; font-size: 12px; margin-left: 8px;">添加人不可修改</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getAccountBooks } from '@/api/accountBook'
import { getCategoriesByType } from '@/api/category'
import { getTags } from '@/api/tag'
import { deleteTransaction, exportTransactions, getMonthlySummary, getTransactions, updateTransaction } from '@/api/transaction'
import { useUserStore } from '@/stores/userStore'
import { Download } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

const userStore = useUserStore()

const loading = ref(false)
const summaryLoading = ref(false)
const timeMode = ref('month')
const filterType = ref('EXPENSE')
const filterAccount = ref('')
const currentMonth = ref(dayjs().format('YYYY-MM'))
const currentYear = ref(dayjs().year())
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const transactions = ref([])

const yearOptions = computed(() => {
  const current = dayjs().year()
  const years = []
  for (let i = 0; i < 10; i++) {
    years.push(current - i)
  }
  return years
})

const timeModeLabel = computed(() => {
  if (timeMode.value === 'month') {
    return currentMonth.value ? currentMonth.value + '月' : '当前月'
  } else {
    return currentYear.value ? currentYear.value + '年' : '当前年'
  }
})

const monthlySummary = ref({
  income: 0,
  expense: 0,
  balance: 0
})

const filteredTransactions = computed(() => {
  return transactions.value
})

const totalPages = computed(() => {
  return Math.ceil(total.value / pageSize.value)
})

const groupedTransactions = computed(() => {
  const groups = {}
  filteredTransactions.value.forEach(t => {
    const date = t.transactionDate
    if (!groups[date]) {
      groups[date] = { date, expense: 0, income: 0, transactions: [] }
    }
    if (t.type === 'EXPENSE') {
      groups[date].expense += Number(t.amount)
    } else {
      groups[date].income += Number(t.amount)
    }
    groups[date].transactions.push(t)
  })
  return Object.values(groups).sort((a, b) => b.date.localeCompare(a.date))
})

function formatDate(dateStr) {
  const date = dayjs(dateStr)
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${date.format('YYYY-MM-DD')} ${weekdays[date.day()]}`
}

function handleTimeModeChange() {
  currentPage.value = 1
  loadData()
  loadSummary()
}

function handleFilterChange() {
  currentPage.value = 1
  loadData()
}

async function loadSummary() {
  summaryLoading.value = true
  try {
    const params = {}
    if (timeMode.value === 'month') {
      params.month = currentMonth.value
    } else {
      params.year = currentYear.value
    }
    const res = await getMonthlySummary(params)
    monthlySummary.value = res || { income: 0, expense: 0, balance: 0 }
  } catch (e) {
    console.error('加载统计数据失败', e)
  } finally {
    summaryLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      type: filterType.value,
      page: currentPage.value - 1,
      size: pageSize.value
    }

    if (filterAccount.value) {
      params.account = filterAccount.value
    }

    if (timeMode.value === 'month') {
      params.month = currentMonth.value
    } else if (timeMode.value === 'year') {
      params.year = currentYear.value
    }

    const res = await getTransactions(params)
    if (res) {
      transactions.value = res.content || []
      total.value = res.totalElements || 0
    } else {
      transactions.value = []
      total.value = 0
    }
  } catch (e) {
    console.error('加载账单失败', e)
    ElMessage.error('加载账单失败')
  } finally {
    loading.value = false
  }
}

async function handleDelete(row) {
  // 权限校验：仅创建者或账本所有者可删除
  const allowed = await checkEditPermission(row)
  if (!allowed) {
    ElMessage.warning('只有记录创建者或账本所有者可以删除此记录')
    return
  }
  try {
    await ElMessageBox.confirm('确定要删除这条记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTransaction(row.id)
    ElMessage.success('删除成功')
    loadData()
    loadSummary()
  } catch {
    
  }
}

// ============ 编辑流水 ============
const editDialogVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref(null)
const editCategoryOptions = ref([])
const editTagOptions = ref([])
const editForm = reactive({
  id: null,
  type: 'EXPENSE',
  amount: 0,
  categoryId: null,
  tagIds: [],
  transactionDate: '',
  account: '',
  note: '',
  userId: null,
  createdBy: ''
})

const editRules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  transactionDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

async function checkEditPermission(row) {
  const me = userStore.userInfo
  if (!me) return false
  if (row.userId === me.id) return true
  // 检查是否为账本所有者
  try {
    const books = await getAccountBooks()
    const currentBookId = userStore.currentAccountBookId
    const book = (books || []).find(b => b.id === currentBookId)
    if (book && book.userId === me.id) return true
  } catch (e) {
    console.error('账本权限校验失败', e)
  }
  return false
}

async function loadEditCategories(type) {
  try {
    const cats = await getCategoriesByType(type)
    editCategoryOptions.value = cats || []
  } catch (e) {
    editCategoryOptions.value = []
  }
}

async function loadEditTags() {
  try {
    const tags = await getTags()
    editTagOptions.value = tags || []
  } catch (e) {
    editTagOptions.value = []
  }
}

async function handleEdit(row) {
  const allowed = await checkEditPermission(row)
  if (!allowed) {
    ElMessage.warning('只有记录创建者或账本所有者可以修改此记录')
    return
  }
  editForm.id = row.id
  editForm.type = row.type
  editForm.amount = Number(row.amount)
  editForm.categoryId = row.categoryId
  editForm.tagIds = (row.tags || []).map(t => t.id)
  editForm.transactionDate = row.transactionDate
  editForm.account = row.account || ''
  editForm.note = row.note || ''
  editForm.userId = row.userId
  editForm.createdBy = row.createdBy || ''

  await Promise.all([loadEditCategories(row.type), loadEditTags()])
  editDialogVisible.value = true
}

async function handleEditTypeChange(newType) {
  editForm.categoryId = null
  await loadEditCategories(newType)
}

async function handleEditSubmit() {
  if (!editFormRef.value) return
  try {
    await editFormRef.value.validate()
  } catch {
    return
  }
  editSubmitting.value = true
  try {
    await updateTransaction(editForm.id, {
      type: editForm.type,
      amount: editForm.amount,
      categoryId: editForm.categoryId,
      tagIds: editForm.tagIds,
      transactionDate: editForm.transactionDate,
      account: editForm.account,
      note: editForm.note
    })
    ElMessage.success('修改成功')
    editDialogVisible.value = false
    loadData()
    loadSummary()
  } catch (e) {
    ElMessage.error(e?.message || '修改失败')
  } finally {
    editSubmitting.value = false
  }
}

function formatDateShort(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function handleExport() {
  const params = {}

  if (timeMode.value === 'month' && currentMonth.value) {
    params.startDate = `${currentMonth.value}-01`
    const [year, month] = currentMonth.value.split('-').map(Number)
    params.endDate = formatDateShort(new Date(year, month, 0))
  } else if (timeMode.value === 'year' && currentYear.value) {
    params.startDate = `${currentYear.value}-01-01`
    params.endDate = `${currentYear.value}-12-31`
  }

  try {
    await exportTransactions(params)
    ElMessage.success('导出任务已提交，完成后将发送到您的绑定邮箱')
  } catch (e) {
    ElMessage.error(e?.message || '提交导出任务失败')
  }
}

onMounted(() => {
  loadData()
  loadSummary()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.card-title {
  font-weight: bold;
  font-size: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.summary-bar {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-radius: 8px;
}

.summary-item {
  text-align: center;
  padding: 12px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.summary-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.summary-item.income .summary-value {
  color: #67c23a;
}

.summary-item.expense .summary-value {
  color: #f56c6c;
}

.summary-item.balance .summary-value {
  color: #409eff;
}

.summary-item.balance.negative .summary-value {
  color: #f56c6c;
}

.transactions-list {
  max-height: 650px;
  overflow-y: auto;
}

.date-group {
  margin-bottom: 20px;
}

.date-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #fafafa;
  border-radius: 4px;
  margin-bottom: 10px;
  border-bottom: 2px solid #ebeef5;
}

.date {
  font-weight: bold;
  color: #303133;
  font-size: 14px;
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

.created-by-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.created-by-name {
  font-size: 13px;
  color: #606266;
}

.expense {
  color: #f56c6c;
  font-weight: bold;
}

.income {
  color: #67c23a;
  font-weight: bold;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 16px 0;
}
</style>
