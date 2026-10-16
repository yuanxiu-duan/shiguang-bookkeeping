<template>
  <view class="account-books-page">
    <view class="page-header">
      <text class="page-title">账本管理</text>
      <view class="header-right">
        <view class="current-book-display">
          <text class="current-book-name">{{ currentBookName }}</text>
        </view>
        <view class="add-btn" @click="showAddDialog = true">
          <text class="add-icon">+</text>
          <text class="add-text">新建</text>
        </view>
      </view>
    </view>

    <view v-for="book in accountBooks" :key="book.id" class="book-card" :class="{ active: currentBookId === book.id }" @click="switchBook(book)">
      <view class="book-info">
        <view class="book-header">
          <text class="book-name">{{ book.alias || book.name }}</text>
          <text v-if="book.alias" class="alias-hint">({{ book.name }})</text>
          <text v-if="book.isDefault" class="default-badge">默认</text>
          <text :class="isOwnBook(book) ? 'owner-badge' : 'shared-badge'">
            {{ isOwnBook(book) ? '个人' : '共享' }}
          </text>
        </view>
        <text class="book-desc">{{ book.description || '暂无描述' }}</text>
      </view>
      <view class="book-actions" @click.stop="handleLongPress(book)">
        <text class="action-icon">⋮</text>
      </view>
    </view>

    <view v-if="showAddDialog" class="picker-mask" @click="showAddDialog = false">
      <view class="picker-content" @click.stop>
        <view class="picker-header">
          <text class="picker-title">新建账本</text>
        </view>
        <view class="add-tag-form">
          <view class="form-item">
            <text class="form-label">账本名称</text>
            <input v-model="addForm.name" class="tag-name-input" placeholder="请输入账本名称" maxlength="20" />
          </view>
          <view class="form-item">
            <text class="form-label">描述</text>
            <input v-model="addForm.description" class="tag-name-input" placeholder="可选描述" maxlength="100" />
          </view>
        </view>
        <view class="picker-footer">
          <button class="btn-cancel" @click="showAddDialog = false">取消</button>
          <button class="btn-confirm" @click="handleAdd">确认</button>
        </view>
      </view>
    </view>

    <view v-if="showEditDialog" class="picker-mask" @click="showEditDialog = false">
      <view class="picker-content" @click.stop>
        <view class="picker-header">
          <text class="picker-title">编辑账本</text>
        </view>
        <view class="add-tag-form">
          <view class="form-item">
            <text class="form-label">账本名称</text>
            <input v-if="!editForm.isShared" v-model="editForm.name" class="tag-name-input" placeholder="请输入账本名称" maxlength="20" />
            <text v-else class="form-input-readonly-text">{{ editForm.name }}</text>
          </view>
          <view class="form-item">
            <text class="form-label">描述</text>
            <input v-if="!editForm.isShared" v-model="editForm.description" class="tag-name-input" placeholder="可选描述" maxlength="100" />
            <text v-else class="form-input-readonly-text">{{ editForm.description || '暂无描述' }}</text>
          </view>
          <view class="form-item">
            <text class="form-label">别名</text>
            <input v-model="editForm.alias" class="tag-name-input" placeholder="请输入别名" maxlength="20" />
          </view>
        </view>
        <view class="picker-footer">
          <button class="btn-cancel" @click="showEditDialog = false">取消</button>
          <button class="btn-confirm" @click="handleEdit">确认</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { accountBookApi } from '../../api/index.js'
import { storage } from '../../utils/storage.js'

export default {
  data() {
    return {
      accountBooks: [],
      currentBookId: null,
      currentUser: null,
      showAddDialog: false,
      showEditDialog: false,
      addForm: {
        name: '',
        description: ''
      },
      editForm: {
        id: null,
        name: '',
        description: '',
        alias: '',
        isShared: false
      }
    }
  },

  onShow() {
    this.currentBookId = storage.get('currentAccountBookId')
    this.currentUser = storage.get('user_info')
    this.loadData()
  },

  computed: {
    currentBookName() {
      const currentBook = this.accountBooks.find(b => b.id === this.currentBookId)
      if (!currentBook) return ''
      return currentBook.alias || currentBook.name
    }
  },

  methods: {
    isOwnBook(book) {
      const currentUserId = this.currentUser?.id
      return currentUserId != null && String(book.userId) === String(currentUserId)
    },
    async loadData() {
      try {
        const data = await accountBookApi.getList()
        this.accountBooks = data
      } catch (e) {
        console.error(e)
      }
    },

    switchBook(book) {
      this.currentBookId = book.id
      storage.set('currentAccountBookId', book.id)
      if (this.currentUser?.id) {
        storage.set(`currentAccountBookId_${this.currentUser.id}`, book.id)
      }
      uni.showToast({ title: '已切换到: ' + book.name, icon: 'none' })
    },

    async handleAdd() {
      if (!this.addForm.name.trim()) {
        uni.showToast({ title: '请输入账本名称', icon: 'none' })
        return
      }
      try {
        await accountBookApi.create({
          name: this.addForm.name.trim(),
          description: this.addForm.description.trim()
        })
        uni.showToast({ title: '创建成功', icon: 'success' })
        this.showAddDialog = false
        this.addForm = { name: '', description: '' }
        this.loadData()
      } catch (e) {
        uni.showToast({ title: '创建失败', icon: 'none' })
      }
    },

    handleLongPress(book) {
      const isOwn = this.isOwnBook(book)
      const itemList = isOwn ? ['编辑账本', '删除账本'] : ['编辑账本']
      
      uni.showActionSheet({
        itemList,
        success: async (res) => {
          if (res.tapIndex === 0) {
            this.editForm = {
              id: book.id,
              name: book.name,
              description: book.description || '',
              alias: book.alias || '',
              isShared: !isOwn
            }
            this.showEditDialog = true
          } else if (res.tapIndex === 1 && isOwn) {
            uni.showModal({
              title: '提示',
              content: '确定要删除这个账本吗？删除后数据无法恢复。',
              success: async (modalRes) => {
                if (modalRes.confirm) {
                  try {
                    await accountBookApi.delete(book.id)
                    if (this.currentBookId === book.id) {
                      this.currentBookId = null
                      storage.set('currentAccountBookId', null)
                      if (this.currentUser?.id) {
                        storage.set(`currentAccountBookId_${this.currentUser.id}`, null)
                      }
                    }
                    uni.showToast({ title: '删除成功', icon: 'success' })
                    this.loadData()
                  } catch (e) {
                    uni.showToast({ title: '删除失败', icon: 'none' })
                  }
                }
              }
            })
          }
        }
      })
    },

    async handleEdit() {
      try {
        if (!this.editForm.isShared) {
          if (!this.editForm.name.trim()) {
            uni.showToast({ title: '请输入账本名称', icon: 'none' })
            return
          }
          await accountBookApi.update(this.editForm.id, {
            name: this.editForm.name.trim(),
            description: this.editForm.description.trim()
          })
        }
        await accountBookApi.setAlias(this.editForm.id, this.editForm.alias.trim())
        uni.showToast({ title: '更新成功', icon: 'success' })
        this.showEditDialog = false
        this.loadData()
      } catch (e) {
        uni.showToast({ title: '更新失败', icon: 'none' })
      }
    }
  }
}
</script>

<style>
.account-books-page {
  padding: 30rpx 20rpx 120rpx;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
}

.page-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.current-book-display {
  display: flex;
  align-items: center;
}

.current-book-name {
  font-size: 28rpx;
  color: #303133;
  max-width: 200rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.book-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 2rpx solid transparent;
}

.book-card.active {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.05);
}

.book-info {
  flex: 1;
}

.book-header {
  display: flex;
  align-items: center;
  margin-bottom: 10rpx;
}

.book-name {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
  margin-right: 16rpx;
}

.alias-hint {
  font-size: 22rpx;
  color: #909399;
  margin-right: 16rpx;
}

.default-badge {
  font-size: 22rpx;
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 4rpx 16rpx;
  border-radius: 16rpx;
  margin-right: 8rpx;
}

.owner-badge {
  font-size: 22rpx;
  color: #67c23a;
  background: rgba(67, 194, 58, 0.1);
  padding: 4rpx 16rpx;
  border-radius: 16rpx;
}

.shared-badge {
  font-size: 22rpx;
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.1);
  padding: 4rpx 16rpx;
  border-radius: 16rpx;
}

.book-desc {
  font-size: 26rpx;
  color: #909399;
}

.book-actions {
  padding: 10rpx;
}

.action-icon {
  font-size: 36rpx;
  color: #909399;
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

.tag-name-input {
  width: 100%;
  height: 80rpx;
  padding: 0 20rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.form-input-readonly {
  background: #f0f0f0;
  color: #909399;
}

.form-input-readonly-text {
  display: block;
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  padding: 0 20rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #909399;
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
