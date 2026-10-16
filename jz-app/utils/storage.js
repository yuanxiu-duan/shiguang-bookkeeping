export const storage = {
  set(key, value) {
    try {
      uni.setStorageSync(key, JSON.stringify(value))
    } catch (e) {
      console.error('Storage set error:', e)
    }
  },
  
  get(key, defaultValue = null) {
    try {
      const value = uni.getStorageSync(key)
      if (value === '' || value === null || value === undefined) {
        return defaultValue
      }
      if (typeof value === 'string') {
        try {
          return JSON.parse(value)
        } catch (e) {
          // 兼容旧版本直接写入的裸字符串（未经 JSON 序列化），返回原值避免报错刷屏
          console.warn('[storage] 读取到旧版裸字符串，已兼容返回:', key)
          return value
        }
      }
      return value
    } catch (e) {
      console.error('Storage get error [' + key + ']:', e)
      return defaultValue
    }
  },
  
  remove(key) {
    try {
      uni.removeStorageSync(key)
    } catch (e) {
      console.error('Storage remove error:', e)
    }
  },
  
  clear() {
    try {
      uni.clearStorageSync()
    } catch (e) {
      console.error('Storage clear error:', e)
    }
  }
}

export const PENDING_TRANSACTIONS_KEY = 'pending_transactions'
export const PENDING_KEY = PENDING_TRANSACTIONS_KEY
export const TOKEN_KEY = 'token'
export const USER_INFO_KEY = 'user_info'

export function getPendingTransactions() {
  return storage.get(PENDING_KEY, [])
}
