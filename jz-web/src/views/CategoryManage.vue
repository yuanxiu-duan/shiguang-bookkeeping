<template>
  <div class="category-manage">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span class="card-title">支出分类</span>
              <el-button type="primary" size="small" @click="openDialog('EXPENSE')">
                <el-icon><Plus /></el-icon>
                添加
              </el-button>
            </div>
          </template>
          <el-table :data="categories.EXPENSE" v-loading="loading" stripe>
            <el-table-column width="60" align="center">
              <template #default="{ row }">
                <span class="category-icon">{{ row.icon || '📌' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
            <el-table-column label="操作" width="150" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" :disabled="row.isDefault" @click="editCategory(row)">
                  编辑
                </el-button>
                <el-button link type="danger" size="small" :disabled="row.isDefault" @click="handleDelete(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span class="card-title">收入分类</span>
              <el-button type="primary" size="small" @click="openDialog('INCOME')">
                <el-icon><Plus /></el-icon>
                添加
              </el-button>
            </div>
          </template>
          <el-table :data="categories.INCOME" v-loading="loading" stripe>
            <el-table-column width="60" align="center">
              <template #default="{ row }">
                <span class="category-icon">{{ row.icon || '📌' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
            <el-table-column label="操作" width="150" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" :disabled="row.isDefault" @click="editCategory(row)">
                  编辑
                </el-button>
                <el-button link type="danger" size="small" :disabled="row.isDefault" @click="handleDelete(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
    
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '添加分类'" width="400px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="60px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="输入emoji，如：🍽️" maxlength="10" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
    
    <el-alert 
      title="提示：带锁图标或操作按钮禁用的是系统默认分类，不可编辑或删除" 
      type="info" 
      show-icon 
      style="margin-top: 20px" 
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategories, createCategory, updateCategory, deleteCategory as deleteCategoryApi } from '@/api/category'

const loading = ref(false)
const categories = ref({ EXPENSE: [], INCOME: [] })
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const editingId = ref(null)

const form = reactive({
  name: '',
  type: 'EXPENSE',
  icon: '',
  sortOrder: 100
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

async function loadCategories() {
  loading.value = true
  try {
    const data = await getCategories()
    categories.value = data
  } finally {
    loading.value = false
  }
}

function openDialog(type) {
  isEdit.value = false
  editingId.value = null
  form.name = ''
  form.type = type
  form.icon = ''
  form.sortOrder = 100
  dialogVisible.value = true
}

function editCategory(row) {
  isEdit.value = true
  editingId.value = row.id
  form.name = row.name
  form.type = row.type
  form.icon = row.icon || ''
  form.sortOrder = row.sortOrder
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCategory(editingId.value, {
        name: form.name,
        icon: form.icon,
        sortOrder: form.sortOrder
      })
      ElMessage.success('更新成功')
    } else {
      await createCategory({
        name: form.name,
        type: form.type,
        icon: form.icon,
        sortOrder: form.sortOrder
      })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadCategories()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除分类"${row.name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCategoryApi(row.id)
    ElMessage.success('删除成功')
    loadCategories()
  } catch {
  }
}

onMounted(() => {
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

.category-icon {
  font-size: 24px;
}
</style>
