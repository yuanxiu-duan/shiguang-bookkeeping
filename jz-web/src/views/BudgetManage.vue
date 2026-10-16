<template>
  <div class="budget-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">本月预算</span>
          <el-button type="primary" @click="openDialog">
            <el-icon><Plus /></el-icon>
            添加预算
          </el-button>
        </div>
      </template>
      
      <div v-loading="loading">
        <el-empty v-if="budgets.length === 0" description="暂无预算设置" />
        <el-row :gutter="20">
          <el-col :span="8" v-for="budget in budgets" :key="budget.id">
            <el-card class="budget-card" shadow="hover">
              <div class="budget-header">
                <span class="category">
                  {{ budget.category?.icon || '📌' }} {{ budget.category?.name }}
                </span>
                <el-dropdown @command="(cmd) => handleCommand(cmd, budget)">
                  <el-icon class="action-icon"><MoreFilled /></el-icon>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="edit">编辑</el-dropdown-item>
                      <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <div class="budget-info">
                <span class="used">已用: ¥{{ budget.usedAmount }}</span>
                <span class="limit">预算: ¥{{ budget.amountLimit }}</span>
              </div>
              <div class="custom-progress">
                <div 
                  class="custom-progress-bar"
                  :class="{
                    'progress-normal': !isOverBudget(budget) && progressPercent(budget) < budget.alertThreshold,
                    'progress-warning': !isOverBudget(budget) && progressPercent(budget) >= budget.alertThreshold,
                    'progress-danger': isOverBudget(budget)
                  }"
                  :style="{ width: Math.min(progressPercent(budget), 100) + '%' }"
                ></div>
              </div>
              <div class="progress-info">
                <span 
                  :class="{
                    'warning': !isOverBudget(budget) && progressPercent(budget) >= budget.alertThreshold,
                    'over-percent': isOverBudget(budget)
                  }"
                >
                  {{ displayProgressPercent(budget) }}%
                  <span v-if="isOverBudget(budget)" class="over-indicator">
                    (超出 {{ displayProgressPercent(budget) - 100 }}%)
                  </span>
                </span>
                <span v-if="!isOverBudget(budget) && progressPercent(budget) >= budget.alertThreshold" class="alert">
                  ⚠️ 已达预警线
                </span>
                <span v-if="isOverBudget(budget)" class="over-budget">
                  🚨 超预算 {{ (Number(budget.usedAmount) - Number(budget.amountLimit)).toFixed(2) }} 元
                </span>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>
    
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑预算' : '添加预算'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="月份" prop="month">
          <el-date-picker
            v-model="form.month"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择支出分类" style="width: 100%" :disabled="isEdit">
            <el-option
              v-for="cat in expenseCategories"
              :key="cat.id"
              :label="`${cat.icon || '📌'} ${cat.name}`"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预算额度" prop="amountLimit">
          <el-input-number
            v-model="form.amountLimit"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入预算额度"
          />
        </el-form-item>
        <el-form-item label="预警阈值" prop="alertThreshold">
          <el-slider
            v-model="form.alertThreshold"
            :min="50"
            :max="100"
            :step="5"
            show-input
          />
          <span style="font-size: 12px; color: #909399;">当使用比例达到 {{ form.alertThreshold }}% 时预警</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { getCurrentBudgets, createBudget, updateBudget, deleteBudget } from '@/api/budget'
import { getCategories } from '@/api/category'

const loading = ref(false)
const budgets = ref([])
const expenseCategories = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  month: dayjs().format('YYYY-MM'),
  categoryId: null,
  amountLimit: null,
  alertThreshold: 80
})

const rules = {
  month: [{ required: true, message: '请选择月份', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  amountLimit: [{ required: true, message: '请输入预算额度', trigger: 'blur' }]
}

function progressPercent(budget) {
  if (!budget.amountLimit || Number(budget.amountLimit) === 0) return 0
  return Math.round((Number(budget.usedAmount) / Number(budget.amountLimit)) * 100)
}

function displayProgressPercent(budget) {
  return progressPercent(budget)
}

function progressStatus(budget) {
  const percent = progressPercent(budget)
  if (percent >= 100) return 'exception'
  if (percent >= budget.alertThreshold) return 'warning'
  return ''
}

function isOverBudget(budget) {
  return Number(budget.usedAmount) > Number(budget.amountLimit)
}

async function loadBudgets() {
  loading.value = true
  try {
    budgets.value = await getCurrentBudgets()
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  const data = await getCategories()
  expenseCategories.value = data.EXPENSE || []
}

function openDialog(budget = null) {
  isEdit.value = !!budget
  if (budget) {
    form.month = budget.month
    form.categoryId = budget.categoryId
    form.amountLimit = Number(budget.amountLimit)
    form.alertThreshold = budget.alertThreshold
  } else {
    form.month = dayjs().format('YYYY-MM')
    form.categoryId = null
    form.amountLimit = null
    form.alertThreshold = 80
  }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      const editingBudget = budgets.value.find(b => b.categoryId === form.categoryId && b.month === form.month)
      if (editingBudget) {
        await updateBudget(editingBudget.id, form)
        ElMessage.success('更新成功')
      }
    } else {
      await createBudget(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadBudgets()
  } finally {
    submitting.value = false
  }
}

async function handleCommand(command, budget) {
  if (command === 'edit') {
    openDialog(budget)
  } else if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确定要删除这个预算吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await deleteBudget(budget.id)
      ElMessage.success('删除成功')
      loadBudgets()
    } catch {
    }
  }
}

onMounted(() => {
  loadBudgets()
  loadCategories()
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

.budget-card {
  margin-bottom: 20px;
}

.budget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.category {
  font-size: 16px;
  font-weight: bold;
}

.action-icon {
  cursor: pointer;
  font-size: 18px;
  color: #909399;
}

.budget-info {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.used {
  color: #f56c6c;
}

.limit {
  color: #909399;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  margin-top: 8px;
}

.warning {
  color: #e6a23c;
  font-weight: bold;
}

.alert {
  color: #e6a23c;
}

.over-budget {
  color: #f56c6c;
  font-weight: bold;
}

.custom-progress {
  height: 12px;
  background: #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
  margin: 8px 0;
}

.custom-progress-bar {
  height: 100%;
  border-radius: 6px;
  transition: width 0.3s ease;
}

.progress-normal {
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
}

.progress-warning {
  background: linear-gradient(90deg, #e6a23c 0%, #f3d19e 100%);
}

.progress-danger {
  background: linear-gradient(90deg, #f56c6c 0%, #f78989 100%);
}

.over-percent {
  color: #f56c6c;
  font-weight: bold;
}

.over-indicator {
  font-size: 11px;
  margin-left: 4px;
}
</style>
