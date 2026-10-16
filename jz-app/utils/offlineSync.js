import { storage } from './storage.js'
import { transactionApi, tagApi } from '../api/index.js'
import { isOnline } from './request.js'

const PENDING_QUEUE_KEY = 'offline_transaction_queue'
const SYNC_LOG_KEY = 'offline_sync_log'
const MAX_RETRY = 3

const offlineSync = {
  getQueue() {
    return storage.get(PENDING_QUEUE_KEY, [])
  },

  setQueue(queue) {
    storage.set(PENDING_QUEUE_KEY, queue)
  },

  addTransaction(transaction) {
    const queue = this.getQueue()
    const item = {
      id: Date.now() + '_' + Math.random().toString(36).substr(2, 9),
      data: transaction,
      type: 'create',
      status: 'pending',
      retryCount: 0,
      createdAt: Date.now(),
      error: null
    }
    queue.push(item)
    this.setQueue(queue)
    this._addLog('新增待同步交易', item.id)
    return item
  },

  getPendingCount() {
    const queue = this.getQueue()
    return queue.filter(item => item.status === 'pending' || item.status === 'failed').length
  },

  getQueueStatus() {
    const queue = this.getQueue()
    const pending = queue.filter(item => item.status === 'pending').length
    const failed = queue.filter(item => item.status === 'failed').length
    const success = queue.filter(item => item.status === 'success').length
    return {
      total: queue.length,
      pending,
      failed,
      success,
      needsSync: pending + failed > 0
    }
  },

  async syncAll() {
    const online = await isOnline()
    if (!online) {
      return {
        success: 0,
        fail: 0,
        skipped: 0,
        message: '当前无网络，无法同步'
      }
    }

    const queue = this.getQueue()
    const pendingItems = queue.filter(
      item => item.status === 'pending' || item.status === 'failed'
    )

    if (pendingItems.length === 0) {
      return {
        success: 0,
        fail: 0,
        skipped: 0,
        message: '没有待同步的数据'
      }
    }

    this._addLog('开始同步', `共${pendingItems.length}条待同步`)

    let success = 0
    let fail = 0

    for (const item of pendingItems) {
      try {
        if (item.retryCount >= MAX_RETRY) {
          fail++
          this._updateItemStatus(item.id, 'failed', '重试次数已达上限')
          continue
        }

        const result = await this._syncSingleItem(item)
        success++
        this._updateItemStatus(item.id, 'success')
        this._addLog('同步成功', item.id)
      } catch (e) {
        fail++
        item.retryCount++
        this._updateItemStatus(item.id, 'failed', e.message || '同步失败')
        this._addLog('同步失败', `${item.id}: ${e.message}`)
      }
    }

    this._cleanupSuccessItems()

    const result = {
      success,
      fail,
      skipped: pendingItems.length - success - fail,
      total: pendingItems.length,
      message: fail === 0 ? '同步完成' : `同步完成，${fail}条失败`
    }

    this._addLog('同步结束', `成功: ${success}, 失败: ${fail}`)
    return result
  },

  async _syncSingleItem(item) {
    if (item.type === 'create') {
      const transactionData = { ...item.data }
      if (transactionData.tags && transactionData.tags.length > 0 && !transactionData.tagIds) {
        try {
          const tagIds = []
          for (const tagName of transactionData.tags) {
            const tags = await tagApi.getTags()
            let tag = tags.find(t => t.name === tagName)
            if (!tag) {
              tag = await tagApi.createTag({ name: tagName })
            }
            tagIds.push(tag.id)
          }
          transactionData.tagIds = tagIds
          delete transactionData.tags
        } catch (e) {
          console.warn('标签处理失败，跳过标签', e)
        }
      }

      return await transactionApi.create(transactionData)
    } else if (item.type === 'update') {
      return await transactionApi.update(item.data.id, item.data)
    } else if (item.type === 'delete') {
      return await transactionApi.delete(item.data.id)
    } else {
      throw new Error(`未知的操作类型: ${item.type}`)
    }
  },

  _updateItemStatus(id, status, error = null) {
    const queue = this.getQueue()
    const item = queue.find(item => item.id === id)
    if (item) {
      item.status = status
      if (error) {
        item.error = error
      }
      if (status === 'success') {
        item.syncedAt = Date.now()
      }
      this.setQueue(queue)
    }
  },

  _cleanupSuccessItems() {
    const queue = this.getQueue()
    const now = Date.now()
    const oneDay = 24 * 60 * 60 * 1000
    
    const filtered = queue.filter(item => {
      if (item.status === 'success') {
        return now - (item.syncedAt || 0) < oneDay
      }
      return true
    })
    
    if (filtered.length !== queue.length) {
      this.setQueue(filtered)
    }
  },

  clearSuccessItems() {
    const queue = this.getQueue()
    const filtered = queue.filter(item => item.status !== 'success')
    this.setQueue(filtered)
  },

  clearAll() {
    this.setQueue([])
  },

  _addLog(action, detail) {
    const logs = storage.get(SYNC_LOG_KEY, [])
    logs.unshift({
      time: new Date().toLocaleString(),
      action,
      detail
    })
    if (logs.length > 100) {
      logs.pop()
    }
    storage.set(SYNC_LOG_KEY, logs)
  },

  getLogs() {
    return storage.get(SYNC_LOG_KEY, [])
  },

  clearLogs() {
    storage.remove(SYNC_LOG_KEY)
  },

  startAutoSync(intervalMs = 60000) {
    if (this.autoSyncTimer) {
      return
    }
    
    this.autoSyncTimer = setInterval(async () => {
      const status = this.getQueueStatus()
      if (status.needsSync) {
        const online = await isOnline()
        if (online) {
          await this.syncAll()
        }
      }
    }, intervalMs)
    
    this._addLog('开启自动同步', `间隔: ${intervalMs}ms`)
  },

  stopAutoSync() {
    if (this.autoSyncTimer) {
      clearInterval(this.autoSyncTimer)
      this.autoSyncTimer = null
      this._addLog('停止自动同步')
    }
  }
}

export default offlineSync
export { offlineSync }
