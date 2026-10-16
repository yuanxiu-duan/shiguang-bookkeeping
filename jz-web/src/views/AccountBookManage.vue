<template>
  <div class="account-book-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">账本管理</span>
          <div class="header-actions">
            <el-badge :value="pendingCount" :hidden="pendingCount === 0" class="invite-badge">
              <el-button type="warning" plain size="small" @click="showInvitations">
                邀请消息
              </el-button>
            </el-badge>
            <el-button type="primary" @click="openDialog">
              <el-icon><Plus /></el-icon>
              添加账本
            </el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <el-empty v-if="accountBooks.length === 0" description="暂无账本" />
        <el-row :gutter="20">
          <el-col :span="8" v-for="book in accountBooks" :key="book.id">
            <el-card class="book-card" shadow="hover">
              <div class="book-header">
                <span class="book-name">
                  {{ book.alias || book.name }}
                  <el-tag v-if="book.isDefault" type="danger" size="small" style="margin-left: 8px;">默认</el-tag>
                  <el-tag v-if="!isOwner(book)" type="success" size="small" style="margin-left: 8px;">共享</el-tag>
                </span>
                <el-dropdown @command="(cmd) => handleCommand(cmd, book)">
                  <el-icon class="action-icon"><MoreFilled /></el-icon>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-if="isOwner(book)" command="edit">编辑</el-dropdown-item>
                      <el-dropdown-item command="setAlias">设置别名</el-dropdown-item>
                      <el-dropdown-item v-if="isOwner(book)" command="members">成员管理</el-dropdown-item>
                      <el-dropdown-item v-if="isOwner(book)" command="invite">邀请成员</el-dropdown-item>
                      <el-dropdown-item v-if="!isOwner(book)" command="leave" divided>退出账本</el-dropdown-item>
                      <el-dropdown-item v-if="isOwner(book)" command="delete" divided :disabled="book.isDefault">删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <div class="book-alias-hint" v-if="book.alias && book.alias !== book.name">
                原名：{{ book.name }}
              </div>
              <div class="book-desc" v-if="book.description">{{ book.description }}</div>
              <div class="book-desc" v-else style="color: #c0c4cc;">暂无描述</div>
              <div class="book-footer">
                <span class="book-time">{{ formatDate(book.createdAt) }}</span>
                <span class="book-owner" v-if="!isOwner(book)">
                  <el-icon><User /></el-icon>
                  共享账本
                </span>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑账本' : '添加账本'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入账本名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入账本描述（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aliasDialogVisible" title="设置别名" width="500px">
      <el-form label-width="80px">
        <el-form-item label="账本原名">
          <span>{{ aliasBook?.name }}</span>
        </el-form-item>
        <el-form-item label="别名">
          <el-input
            v-model="aliasValue"
            placeholder="请输入别名（仅自己可见）"
            clearable
          />
        </el-form-item>
        <div class="alias-hint">别名仅自己可见，其他成员不会看到您设置的别名</div>
      </el-form>
      <template #footer>
        <el-button @click="aliasDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="aliasSaving" @click="handleSaveAlias">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="inviteDialogVisible" title="邀请成员" width="500px">
      <el-form @submit.prevent="handleInvite">
        <el-form-item label="用户名">
          <el-input
            v-model="inviteUsername"
            placeholder="请输入要邀请的用户名"
            :loading="inviting"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inviteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="inviting" @click="handleInvite">发送邀请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="memberDialogVisible" title="成员管理" width="650px">
      <div v-loading="membersLoading">
        <el-table :data="members" style="width: 100%">
          <el-table-column prop="username" label="用户" />
          <el-table-column prop="role" label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 'OWNER' ? 'warning' : 'info'" size="small">
                {{ row.role === 'OWNER' ? '所有者' : '成员' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACCEPTED' ? 'success' : 'warning'" size="small">
                {{ row.status === 'ACCEPTED' ? '已加入' : '待加入' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="时间" width="170" />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button
                v-if="row.role !== 'OWNER'"
                type="danger"
                link
                size="small"
                @click="handleRemoveMember(row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="memberDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="openInviteFromMemberDialog">邀请成员</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="invitationDialogVisible" title="待处理邀请" width="500px">
      <div v-loading="invitationsLoading">
        <el-empty v-if="pendingInvitations.length === 0" description="暂无待处理邀请" />
        <div v-else class="invitation-list">
          <div v-for="inv in pendingInvitations" :key="inv.id" class="invitation-item">
            <div class="invitation-info">
              <span class="invitation-text">
                <strong>{{ inv.inviterUsername }}</strong> 邀请您加入账本
              </span>
              <span class="invitation-time">{{ inv.createdAt }}</span>
            </div>
            <div class="invitation-actions">
              <el-button type="success" size="small" @click="handleAcceptInvitation(inv)">接受</el-button>
              <el-button type="danger" size="small" @click="handleRejectInvitation(inv)">拒绝</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getAccountBooks, createAccountBook, updateAccountBook, deleteAccountBook } from '@/api/accountBook'
import {
  getMembers,
  inviteMember,
  acceptInvitation,
  rejectInvitation,
  removeMember,
  leaveAccountBook,
  getPendingInvitations,
  setAlias
} from '@/api/accountBookMember'
import { useUserStore } from '@/stores/userStore'

const userStore = useUserStore()
const loading = ref(false)
const accountBooks = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const editingId = ref(null)

const aliasDialogVisible = ref(false)
const aliasBook = ref(null)
const aliasValue = ref('')
const aliasSaving = ref(false)

const inviteDialogVisible = ref(false)
const inviteUsername = ref('')
const inviting = ref(false)
const invitingBookId = ref(null)

const memberDialogVisible = ref(false)
const members = ref([])
const membersLoading = ref(false)
const currentBook = ref(null)

const invitationDialogVisible = ref(false)
const pendingInvitations = ref([])
const invitationsLoading = ref(false)
const pendingCount = ref(0)

const form = reactive({
  name: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入账本名称', trigger: 'blur' }]
}

function formatDate(date) {
  if (!date) return ''
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

function isOwner(book) {
  return book.userId === userStore.userInfo?.id
}

async function loadAccountBooks() {
  loading.value = true
  try {
    accountBooks.value = await getAccountBooks()
  } finally {
    loading.value = false
  }
}

async function loadPendingCount() {
  try {
    const list = await getPendingInvitations()
    pendingCount.value = list.length
  } catch {
    pendingCount.value = 0
  }
}

function openDialog(book = null) {
  isEdit.value = !!book
  if (book) {
    editingId.value = book.id
    form.name = book.name
    form.description = book.description || ''
  } else {
    editingId.value = null
    form.name = ''
    form.description = ''
  }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateAccountBook(editingId.value, {
        name: form.name,
        description: form.description
      })
      ElMessage.success('更新成功')
    } else {
      await createAccountBook({
        name: form.name,
        description: form.description
      })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadAccountBooks()
  } finally {
    submitting.value = false
  }
}

async function handleCommand(command, book) {
  if (command === 'edit') {
    openDialog(book)
  } else if (command === 'setAlias') {
    aliasBook.value = book
    aliasValue.value = book.alias || ''
    aliasDialogVisible.value = true
  } else if (command === 'delete') {
    if (book.isDefault) {
      ElMessage.warning('默认账本不可删除')
      return
    }
    try {
      await ElMessageBox.confirm(`确定要删除账本"${book.name}"吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await deleteAccountBook(book.id)
      ElMessage.success('删除成功')
      loadAccountBooks()
    } catch {
    }
  } else if (command === 'members') {
    openMemberDialog(book)
  } else if (command === 'invite') {
    openInviteDialog(book)
  } else if (command === 'leave') {
    try {
      await ElMessageBox.confirm(`确定要退出账本"${book.alias || book.name}"吗？退出后将无法访问该账本数据。`, '提示', {
        confirmButtonText: '确定退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await leaveAccountBook(book.id)
      ElMessage.success('已退出账本')
      loadAccountBooks()
    } catch {
    }
  }
}

async function handleSaveAlias() {
  aliasSaving.value = true
  try {
    await setAlias(aliasBook.value.id, aliasValue.value)
    ElMessage.success('别名已保存')
    aliasDialogVisible.value = false
    loadAccountBooks()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || e.message || '保存失败')
  } finally {
    aliasSaving.value = false
  }
}

function openInviteDialog(book) {
  invitingBookId.value = book.id
  inviteUsername.value = ''
  inviteDialogVisible.value = true
}

function openInviteFromMemberDialog() {
  invitingBookId.value = currentBook.value.id
  inviteDialogVisible.value = false
  setTimeout(() => {
    inviteUsername.value = ''
    inviteDialogVisible.value = true
  }, 200)
}

async function handleInvite() {
  if (!inviteUsername.value.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  inviting.value = true
  try {
    await inviteMember(invitingBookId.value, inviteUsername.value.trim())
    ElMessage.success('邀请已发送')
    inviteDialogVisible.value = false
    if (memberDialogVisible.value) {
      loadMembers(invitingBookId.value)
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || e.message || '邀请失败')
  } finally {
    inviting.value = false
  }
}

function openMemberDialog(book) {
  currentBook.value = book
  memberDialogVisible.value = true
  loadMembers(book.id)
}

async function loadMembers(accountBookId) {
  membersLoading.value = true
  try {
    members.value = await getMembers(accountBookId)
  } finally {
    membersLoading.value = false
  }
}

async function handleRemoveMember(member) {
  const statusText = member.status === 'PENDING' ? '待加入' : ''
  try {
    await ElMessageBox.confirm(
      `确定要移除${statusText}成员"${member.username}"吗？${member.status === 'PENDING' ? '移除后邀请将同步撤回。' : ''}`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await removeMember(currentBook.value.id, member.id)
    ElMessage.success('已移除成员')
    loadMembers(currentBook.value.id)
    loadPendingCount()
  } catch {
  }
}

async function showInvitations() {
  invitationDialogVisible.value = true
  invitationsLoading.value = true
  try {
    pendingInvitations.value = await getPendingInvitations()
  } finally {
    invitationsLoading.value = false
  }
}

async function handleAcceptInvitation(inv) {
  try {
    await acceptInvitation(inv.accountBookId, inv.id)
    ElMessage.success('已接受邀请')
    loadPendingCount()
    loadAccountBooks()
    showInvitations()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || e.message || '操作失败')
  }
}

async function handleRejectInvitation(inv) {
  try {
    await rejectInvitation(inv.accountBookId, inv.id)
    ElMessage.success('已拒绝邀请')
    loadPendingCount()
    showInvitations()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || e.message || '操作失败')
  }
}

onMounted(() => {
  loadAccountBooks()
  loadPendingCount()
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.invite-badge {
  line-height: 1;
}

.book-card {
  margin-bottom: 20px;
}

.book-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.book-name {
  font-size: 16px;
  font-weight: bold;
  display: flex;
  align-items: center;
}

.book-alias-hint {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.action-icon {
  cursor: pointer;
  font-size: 18px;
  color: #909399;
}

.book-desc {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
  line-height: 1.5;
}

.book-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.book-time {
  font-size: 12px;
  color: #c0c4cc;
}

.book-owner {
  font-size: 12px;
  color: #67c23a;
  display: flex;
  align-items: center;
  gap: 4px;
}

.alias-hint {
  font-size: 12px;
  color: #909399;
  margin-top: -8px;
  padding-left: 80px;
}

.invitation-list {
  max-height: 400px;
  overflow-y: auto;
}

.invitation-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f2f5;
}

.invitation-item:last-child {
  border-bottom: none;
}

.invitation-info {
  display: flex;
  flex-direction: column;
}

.invitation-text {
  font-size: 14px;
}

.invitation-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}

.invitation-actions {
  display: flex;
  gap: 8px;
}
</style>
