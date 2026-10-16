<template>
  <el-container class="dashboard">
    <el-aside :width="collapsed ? '64px' : '200px'" class="sidebar">
      <div class="logo">
        <span v-if="!collapsed">💰 记账系统</span>
        <span v-else>💰</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        :collapse="collapsed"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <template v-if="userStore.userInfo?.role !== 'ADMIN'">
          <el-menu-item index="/bills">
            <el-icon><List /></el-icon>
            <template #title>账单流水</template>
          </el-menu-item>
          <el-menu-item index="/accounting">
            <el-icon><Edit /></el-icon>
            <template #title>记账</template>
          </el-menu-item>
          <el-menu-item index="/reports">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>报表</template>
          </el-menu-item>
          <el-menu-item index="/budget">
            <el-icon><Wallet /></el-icon>
            <template #title>预算管理</template>
          </el-menu-item>
          <el-menu-item index="/categories">
            <el-icon><Folder /></el-icon>
            <template #title>分类管理</template>
          </el-menu-item>
          <el-menu-item index="/account-books">
            <el-icon><Notebook /></el-icon>
            <template #title>账本管理</template>
          </el-menu-item>
        </template>
        <el-menu-item v-if="userStore.userInfo?.role === 'ADMIN'" index="/admin">
          <el-icon><Setting /></el-icon>
          <template #title>系统管理</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleSidebar">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
        </div>
        <div class="header-right">
          <template v-if="!isAdmin">
            <el-select
              v-model="currentAccountBookId"
              placeholder="选择账本"
              size="small"
              style="width: 200px; margin-right: 12px;"
              @change="handleAccountBookChange"
            >
              <el-option
                v-for="book in accountBooks"
                :key="book.id"
                :label="getDisplayName(book) + (isOwner(book) ? '' : ' (共享)')"
                :value="book.id"
              >
                <span>{{ getDisplayName(book) }}</span>
                <el-tag v-if="!isOwner(book)" type="success" size="small" style="margin-left: 8px;">共享</el-tag>
                <span v-if="book.alias && book.alias !== book.name" style="font-size: 12px; color: #909399; margin-left: 4px;">（原名：{{ book.name }}）</span>
              </el-option>
            </el-select>
            <el-button type="primary" link size="small" @click="router.push('/account-books')" style="margin-right: 12px;">
              管理账本
            </el-button>
            <el-popover
              placement="bottom"
              :width="360"
              trigger="click"
              v-model:visible="alertPopoverVisible"
            >
              <template #reference>
                <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notify-badge">
                  <el-icon class="notify-icon" @click="loadAlerts"><Bell /></el-icon>
                </el-badge>
              </template>
              <div class="alert-popover">
                <div class="alert-header">
                  <span class="alert-title">预算预警</span>
                  <el-button link type="primary" size="small" @click="markAllRead" v-if="unreadCount > 0">
                    全部已读
                  </el-button>
                </div>
                <div class="alert-list" v-if="alerts.length > 0">
                  <div 
                    v-for="alert in alerts" 
                    :key="alert.id" 
                    class="alert-item"
                    :class="{ 'is-read': alert.isRead }"
                    @click="handleAlertClick(alert)"
                  >
                    <div class="alert-left">
                      <span class="alert-category-icon">{{ alert.category?.icon || '📌' }}</span>
                    </div>
                    <div class="alert-content">
                      <div class="alert-category">{{ alert.category?.name || '未分类' }}</div>
                      <div class="alert-desc">
                        预算 ¥{{ alert.budgetLimit }}，已使用 ¥{{ alert.usedAmount }} ({{ alert.percentage }}%)
                      </div>
                      <div class="alert-over" v-if="alert.overAmount && alert.overAmount > 0">
                        <span class="over-amount">超支 ¥{{ alert.overAmount }}</span>
                      </div>
                    </div>
                    <div class="alert-date">{{ formatDate(alert.alertDate) }}</div>
                  </div>
                </div>
                <el-empty v-else description="暂无预警" />
              </div>
            </el-popover>

            <el-badge :value="inviteCount" :hidden="inviteCount === 0" class="notify-badge">
              <el-icon class="notify-icon" @click="router.push('/account-books')"><ChatDotRound /></el-icon>
            </el-badge>
          </template>

          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar" icon="UserFilled" />
              <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="settings">账号设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Notebook, List, Setting } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/userStore'
import { useAppStore } from '@/stores/appStore'
import { getAlertCount, getAlerts, markAlertRead, markAllAlertsRead } from '@/api/budget'
import { getAccountBooks } from '@/api/accountBook'
import { getPendingInvitations } from '@/api/accountBookMember'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const collapsed = computed(() => appStore.sidebarCollapsed)
const activeMenu = computed(() => route.path)
const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')

const alertPopoverVisible = ref(false)
const unreadCount = ref(0)
const alerts = ref([])
const accountBooks = ref([])
const inviteCount = ref(0)
const currentAccountBookId = computed({
  get: () => userStore.currentAccountBookId,
  set: (val) => userStore.setCurrentAccountBookId(val)
})

let timer = null

function isOwner(book) {
  return book.userId === userStore.userInfo?.id
}

function getDisplayName(book) {
  return book.alias || book.name
}

async function loadAlertCount() {
  try {
    const data = await getAlertCount()
    unreadCount.value = data.unreadCount || 0
  } catch (e) {
    console.error('加载预警数量失败', e)
  }
}

async function loadAlerts() {
  try {
    alerts.value = await getAlerts(false)
  } catch (e) {
    console.error('加载预警失败', e)
  }
}

function formatDate(date) {
  if (!date) return ''
  return dayjs(date).format('MM-DD')
}

async function handleAlertClick(alert) {
  try {
    if (!alert.isRead) {
      await markAlertRead(alert.id)
      alert.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  } catch (e) {
    console.error('标记已读失败', e)
  }
  alertPopoverVisible.value = false
  router.push('/budget')
}

async function markAllRead() {
  try {
    await markAllAlertsRead()
    alerts.value.forEach(a => a.isRead = true)
    unreadCount.value = 0
  } catch (e) {
    console.error('标记全部已读失败', e)
  }
}

function toggleSidebar() {
  appStore.toggleSidebar()
}

async function handleCommand(command) {
  if (command === 'settings') {
    router.push('/settings')
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      userStore.logout()
      router.push('/login')
    } catch {
    }
  }
}

async function loadAccountBooks() {
  try {
    const data = await getAccountBooks()
    accountBooks.value = data
    if (!userStore.currentAccountBookId && data.length > 0) {
      userStore.setCurrentAccountBookId(data[0].id)
    }
  } catch (e) {
    console.error('加载账本列表失败', e)
  }
}

async function loadInviteCount() {
  try {
    const list = await getPendingInvitations()
    inviteCount.value = list.length
  } catch {
    inviteCount.value = 0
  }
}

function handleAccountBookChange() {
  router.go(0)
}

onMounted(() => {
  if (isAdmin.value) return
  loadAccountBooks()
  loadAlertCount()
  loadInviteCount()
  timer = setInterval(() => {
    loadAlertCount()
    loadInviteCount()
  }, 60000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.dashboard {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #1f2d3d;
}

.el-menu {
  border-right: none;
}

.header {
  background-color: white;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  transition: transform 0.3s;
}

.collapse-btn:hover {
  color: #409EFF;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notify-badge {
  line-height: 1;
}

.notify-icon {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;
}

.notify-icon:hover {
  color: #409EFF;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 8px;
}

.username {
  margin-left: 8px;
}

.main {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}

.alert-popover {
  max-height: 400px;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 12px;
}

.alert-title {
  font-weight: bold;
  font-size: 14px;
  color: #303133;
}

.alert-list {
  max-height: 300px;
  overflow-y: auto;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid #f5f7fa;
  cursor: pointer;
  transition: background-color 0.2s;
}

.alert-item:hover {
  background-color: #f5f7fa;
}

.alert-item.is-read {
  opacity: 0.6;
}

.alert-left {
  margin-right: 12px;
}

.alert-category-icon {
  font-size: 24px;
}

.alert-content {
  flex: 1;
}

.alert-category {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.alert-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.alert-over {
  margin-top: 4px;
}

.over-amount {
  font-size: 12px;
  color: #f56c6c;
  font-weight: bold;
}

.alert-date {
  font-size: 12px;
  color: #c0c4cc;
  margin-left: 12px;
}
</style>
