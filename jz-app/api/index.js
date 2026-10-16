import { getAbsoluteBaseUrl } from '../utils/config.js'
import { isOnline, request } from '../utils/request.js'
import { getPendingTransactions, PENDING_KEY, storage } from '../utils/storage.js'

export { isOnline }

function getCurrentAccountBookId() {
  return storage.get('currentAccountBookId') || null
}

export const accountBookApi = {
  getList() {
    return request({
      url: '/account-books',
      method: 'GET'
    })
  },

  create(data) {
    return request({
      url: '/account-books',
      method: 'POST',
      data
    })
  },

  update(id, data) {
    return request({
      url: '/account-books/' + id,
      method: 'PUT',
      data
    })
  },

  delete(id) {
    return request({
      url: '/account-books/' + id,
      method: 'DELETE'
    })
  },

  setAlias(id, alias) {
    return request({
      url: '/account-books/' + id + '/alias',
      method: 'PUT',
      data: { alias }
    })
  }
}

export const authApi = {
  login(data) {
    return request({
      url: '/auth/login',
      method: 'POST',
      data
    })
  },

  register(data) {
    return request({
      url: '/auth/register',
      method: 'POST',
      data
    })
  },

  sendCode(data) {
    return request({
      url: '/auth/send-code',
      method: 'POST',
      data
    })
  },

  getCurrentUser() {
    return request({
      url: '/auth/me'
    })
  },

  updateNickname(nickname) {
    return request({
      url: '/auth/nickname',
      method: 'PUT',
      data: { nickname }
    })
  },

  updateEmail(data) {
    return request({
      url: '/auth/email',
      method: 'PUT',
      data
    })
  },

  updatePassword(data) {
    return request({
      url: '/auth/password',
      method: 'PUT',
      data
    })
  },

  resetPassword(data) {
    return request({
      url: '/auth/reset-password',
      method: 'POST',
      data
    })
  },

  /**
   * 上传头像（multipart，CryptoFilter 已排除 /api/auth/avatar）
   * @param {string} filePath - uni.chooseImage 返回的临时文件路径
   */
  uploadAvatar(filePath) {
    return new Promise((resolve, reject) => {
      const token = storage.get('token')
      const baseUrl = getAbsoluteBaseUrl()
      uni.uploadFile({
        url: baseUrl + '/auth/avatar',
        filePath: filePath,
        name: 'file',
        header: {
          'Authorization': token ? `Bearer ${token}` : ''
        },
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            if (data.code === 200 || data.code === 0) {
              resolve(data.data)
            } else {
              reject(new Error(data.msg || '头像上传失败'))
            }
          } catch (e) {
            reject(new Error('解析响应失败'))
          }
        },
        fail: (err) => {
          reject(err)
        }
      })
    })
  }
}

export const categoryApi = {
  async getCategories() {
    return request({
      url: '/categories',
      method: 'GET',
      data: { accountBookId: getCurrentAccountBookId() }
    })
  },

  async getCategoriesByType(type) {
    return request({
      url: '/categories/type/' + type,
      method: 'GET',
      data: { accountBookId: getCurrentAccountBookId() }
    })
  },

  async createCategory(data) {
    return request({
      url: '/categories',
      method: 'POST',
      data: { ...data, accountBookId: getCurrentAccountBookId() }
    })
  },

  async updateCategory(id, data) {
    return request({
      url: '/categories/' + id,
      method: 'PUT',
      data
    })
  },

  async deleteCategory(id) {
    return request({
      url: '/categories/' + id,
      method: 'DELETE',
      data: { accountBookId: getCurrentAccountBookId() }
    })
  }
}

export const transactionApi = {
  async getRecent(days = 7) {
    return request({
      url: '/transactions/recent',
      method: 'GET',
      data: { days, accountBookId: getCurrentAccountBookId() }
    })
  },
  
  async getAllPaged(page = 0, size = 20, type = null) {
    const params = { page, size, accountBookId: getCurrentAccountBookId() }
    if (type) params.type = type
    return request({
      url: '/transactions/all',
      method: 'GET',
      data: params
    })
  },

  /**
   * 复合筛选流水
   * @param {object} params
   *  - page, size
   *  - startDate, endDate (yyyy-MM-dd)
   *  - type ('EXPENSE'|'INCOME'，不传表示不限)
   *  - categoryIds (number[])
   *  - account (资金账号名)
   *  - minAmount, maxAmount (number)
   *  - keyword (备注 / 分类名称模糊匹配)
   */
  async search(params = {}) {
    const data = {
      page: params.page || 0,
      size: params.size || 20,
      accountBookId: getCurrentAccountBookId()
    }
    if (params.startDate) data.startDate = params.startDate
    if (params.endDate) data.endDate = params.endDate
    if (params.type) data.type = params.type
    if (params.categoryIds && params.categoryIds.length) data.categoryIds = params.categoryIds.join(',')
    if (params.account) data.account = params.account
    if (params.minAmount != null && params.minAmount !== '') data.minAmount = params.minAmount
    if (params.maxAmount != null && params.maxAmount !== '') data.maxAmount = params.maxAmount
    if (params.keyword) data.keyword = params.keyword
    return request({
      url: '/transactions/search',
      method: 'GET',
      data
    })
  },

  async create(data) {
    return request({
      url: '/transactions',
      method: 'POST',
      data: { ...data, accountBookId: getCurrentAccountBookId() }
    })
  },

  async update(id, data) {
    const bookId = getCurrentAccountBookId()
    return request({
      url: '/transactions/' + id + (bookId ? '?accountBookId=' + encodeURIComponent(bookId) : ''),
      method: 'PUT',
      data
    })
  },

  async delete(id) {
    const bookId = getCurrentAccountBookId()
    return request({
      url: '/transactions/' + id + (bookId ? '?accountBookId=' + encodeURIComponent(bookId) : ''),
      method: 'DELETE'
    })
  },

  async classifySuggest(note) {
    return request({
      url: '/transactions/classify-suggest',
      method: 'POST',
      data: { note, accountBookId: getCurrentAccountBookId() }
    })
  },

  async import(filePath) {
    return new Promise((resolve, reject) => {
      const token = storage.get('token')
      const bookId = getCurrentAccountBookId()
      const baseUrl = getAbsoluteBaseUrl()

      uni.uploadFile({
        url: baseUrl + '/transactions/import' + (bookId ? '?accountBookId=' + bookId : ''),
        filePath: filePath,
        name: 'file',
        header: {
          'Authorization': token ? `Bearer ${token}` : ''
        },
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            if (data.code === 200 || data.code === 0) {
              resolve(data.data)
            } else {
              reject(new Error(data.msg || '导入失败'))
            }
          } catch (e) {
            reject(new Error('解析响应失败'))
          }
        },
        fail: (err) => {
          reject(err)
        }
      })
    })
  }
}

export const budgetApi = {
  async getCurrent() {
    return request({
      url: '/budgets/current',
      method: 'GET',
      data: { accountBookId: getCurrentAccountBookId() }
    })
  },

  async createBudget(data) {
    return request({
      url: '/budgets',
      method: 'POST',
      data: { ...data, accountBookId: getCurrentAccountBookId() }
    })
  },

  async updateBudget(id, data) {
    return request({
      url: '/budgets/' + id,
      method: 'PUT',
      data: { ...data, accountBookId: getCurrentAccountBookId() }
    })
  },

  async deleteBudget(id) {
    return request({
      url: '/budgets/' + id,
      method: 'DELETE',
      data: { accountBookId: getCurrentAccountBookId() }
    })
  },

  async getAlerts(all = false) {
    return request({
      url: '/budgets/alerts',
      method: 'GET',
      data: { all, accountBookId: getCurrentAccountBookId() }
    })
  },

  async getAlertCount() {
    return request({
      url: '/budgets/alerts/count',
      method: 'GET',
      data: { accountBookId: getCurrentAccountBookId() }
    })
  },

  async markAlertRead(id) {
    return request({
      url: '/budgets/alerts/' + id + '/read',
      method: 'PUT',
      data: { accountBookId: getCurrentAccountBookId() }
    })
  },

  async markAllAlertsRead() {
    return request({
      url: '/budgets/alerts/read-all',
      method: 'PUT',
      data: { accountBookId: getCurrentAccountBookId() }
    })
  }
}

export const statisticsApi = {
  getMonthly(month) {
    return request({
      url: '/statistics/monthly',
      data: month ? { month, accountBookId: getCurrentAccountBookId() } : { accountBookId: getCurrentAccountBookId() }
    })
  },

  getYearly(year) {
    return request({
      url: '/statistics/yearly',
      data: year ? { year, accountBookId: getCurrentAccountBookId() } : { accountBookId: getCurrentAccountBookId() }
    })
  },

  getAllTime() {
    return request({
      url: '/statistics/all',
      data: { accountBookId: getCurrentAccountBookId() }
    })
  },

  getCurveData(mode, month, year) {
    const params = { mode, accountBookId: getCurrentAccountBookId() }
    if (mode === 'year' && year) params.year = year
    if (mode === 'month' && month) params.month = month
    return request({
      url: '/statistics/curve',
      data: params
    })
  },

  getMonthlyTrend(months = 12) {
    return request({
      url: '/statistics/trend',
      data: { months, accountBookId: getCurrentAccountBookId() }
    })
  },

  getCategoryTrend(categoryId, months = 6) {
    return request({
      url: '/statistics/category-trend/' + categoryId,
      data: { months, accountBookId: getCurrentAccountBookId() }
    })
  }
}

export const tagApi = {
  getTags() {
    return request({
      url: '/tags',
      method: 'GET',
      data: { accountBookId: getCurrentAccountBookId() }
    })
  },

  createTag(data) {
    return request({
      url: '/tags',
      method: 'POST',
      data: { ...data, accountBookId: getCurrentAccountBookId() }
    })
  },

  updateTag(id, data) {
    return request({
      url: '/tags/' + id,
      method: 'PUT',
      data
    })
  },

  deleteTag(id) {
    return request({
      url: '/tags/' + id,
      method: 'DELETE'
    })
  }
}

export async function saveTransactionLocal(transaction) {
  const pending = getPendingTransactions()
  transaction._localId = Date.now()
  transaction._pending = true
  pending.push(transaction)
  storage.set(PENDING_KEY, pending)
}

export function getPendingCount() {
  return getPendingTransactions().length
}

export async function syncPendingTransactions() {
  const pending = getPendingTransactions()
  if (pending.length === 0) return { success: 0, fail: 0 }

  const online = await isOnline()
  if (!online) return { success: 0, fail: 0 }

  let success = 0
  let fail = 0
  const remaining = []

  for (const transaction of pending) {
    try {
      await transactionApi.create(transaction)
      success++
    } catch (e) {
      console.error('同步失败:', e)
      remaining.push(transaction)
      fail++
    }
  }

  storage.set(PENDING_KEY, remaining)
  return { success, fail, total: pending.length }
}

export function getTemplateUrl() {
  const token = storage.get('token')
  const bookId = getCurrentAccountBookId()
  let url = getAbsoluteBaseUrl() + '/transactions/template?token=' + encodeURIComponent(token || '')
  if (bookId) url += '&accountBookId=' + bookId
  return url
}

export function getExportUrl(startDate, endDate) {
  const token = storage.get('token')
  const bookId = getCurrentAccountBookId()
  let url = getAbsoluteBaseUrl() + '/transactions/export?token=' + encodeURIComponent(token || '')
  if (bookId) url += '&accountBookId=' + bookId
  if (startDate) url += '&startDate=' + encodeURIComponent(startDate)
  if (endDate) url += '&endDate=' + encodeURIComponent(endDate)
  return url
}

export function submitExportTask(startDate, endDate) {
  return request({
    url: '/transactions/export-tasks',
    method: 'POST',
    data: {
      accountBookId: getCurrentAccountBookId(),
      startDate,
      endDate
    }
  })
}

export function getExportTasks() {
  return request({
    url: '/transactions/export-tasks',
    method: 'GET'
  })
}

export function getMonthlySummary(params) {
  params = params || {}
  params.accountBookId = getCurrentAccountBookId()
  return request({
    url: '/transactions/monthly-summary',
    method: 'GET',
    data: params
  })
}

// ========== APP升级检查（公开接口，无需认证） ==========

export const appUpdateApi = {
  /**
   * 检查APP更新
   * 使用项目统一的 request() 函数，而非直接调 uni.request()
   * 原因：CryptoFilter 已排除此路径(/api/admin/app-releases/check-update)，
   *   服务端不做加密/解密 → 客户端 request() 对无 encData 的响应自动跳过解密
   *   因此 request() 完全适用于此公开接口，且保留了统一错误处理、重试等能力
   *
   * @param {string} platform - 'android' | 'ios'
   * @param {number} versionCode - 当前APP的versionCode
   * @param {string} versionName - 当前APP的versionName
   * @returns {Promise<{hasUpdate, versionName, versionCode, downloadUrl, releaseNotes, isForceUpdate}>}
   */
  checkUpdate(platform, versionCode, versionName) {
    const data = { platform, versionCode }
    if (versionName) data.versionName = versionName
    console.log('[AppUpdate] 请求检查更新: platform=' + platform + ', versionCode=' + versionCode + ', versionName=' + versionName)
    return request({
      url: '/admin/app-releases/check-update',
      method: 'GET',
      data,
      silent: true  // 静默模式，不弹toast（升级检查失败不应打扰用户）
    }).then(result => {
      console.log('[AppUpdate] 检查成功: hasUpdate=' + (result && result.hasUpdate))
      return result
    }).catch(err => {
      console.error('[AppUpdate] 检查更新失败:', err.message)
      throw err
    })
  }
}
