import router from '@/router'
import { useUserStore } from '@/stores/userStore'
import { clearPublicKey, decryptResponse, encryptRequest, getPublicKey } from '@/utils/crypto'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

// 请求重试标记（防止死循环）
let isRetrying = false
// 401 重定向标记（防止并发 401 重复跳转）
let isRedirecting = false

// 请求拦截器：加密请求体
request.interceptors.request.use(
  async (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 清理空参数
    if (config.params) {
      Object.keys(config.params).forEach(key => {
        if (config.params[key] === null || config.params[key] === undefined) {
          delete config.params[key]
        }
      })
    }

    // 加密请求（multipart/form-data跳过加密）
    const isMultipart = config.data instanceof FormData

    if (isMultipart) {
      // FormData请求不加密，删除 Content-Type 让浏览器自动设置 boundary
      delete config.headers['Content-Type']
      return config
    }

    try {
      await getPublicKey()
      const method = (config.method || 'GET').toUpperCase()
      const originalData = method === 'GET' ? config.params : config.data

      // 保存原始数据，供重试时恢复
      config._originalData = originalData

      const encrypted = encryptRequest(originalData, method)

      if (encrypted.headers) {
        Object.assign(config.headers, encrypted.headers)
      }

      if (method === 'GET') {
        config.params = encrypted.data
      } else {
        config.data = encrypted.data
      }

      config._aesKey = encrypted._aesKey
      config._aesIv = encrypted._aesIv
    } catch (e) {
      clearPublicKey()
      return Promise.reject(e)
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：解密 + token 续期 + 公钥过期重试
request.interceptors.response.use(
  (response) => {
    // 从响应头中提取刷新后的 token 并更新本地存储（token 续期）
    const newToken = response.headers['x-auth-token'] || response.headers['X-Auth-Token']
    if (newToken) {
      localStorage.setItem('token', newToken)
      try {
        const userStore = useUserStore()
        userStore.token = newToken
      } catch (e) { /* store 可能还未初始化 */ }
    }

    let result = response.data

    if (result && result.encData && response.config._aesKey) {
      try {
        result = decryptResponse(result.encData, response.config._aesKey, response.config._aesIv)
      } catch (e) {
        clearPublicKey()
        return Promise.reject(new Error('响应解密失败'))
      }
    }

    if (result.code === 200) {
      return result.data
    } else {
      return Promise.reject(new Error(result.msg || '请求失败'))
    }
  },
  async (error) => {
    let displayMsg = '服务器开小差啦，请稍后重试'

    if (error.response) {
      let body = error.response.data
      if (body && body.encData && error.response.config._aesKey) {
        try {
          body = decryptResponse(body.encData, error.response.config._aesKey, error.response.config._aesIv)
        } catch (e) { /* 忽略 */ }
      }

      const bizCode = body?.code
      const bizMsg = body?.msg || ''
      displayMsg = bizMsg || displayMsg

      // 公钥失效：清缓存后恢复原始数据重试一次
      if (
        bizCode === 4001 ||
        bizMsg.includes('公钥已过期') ||
        bizMsg.includes('公钥可能已过期') ||
        bizMsg.includes('Request decryption failed')
      ) {
        clearPublicKey()
        if (!isRetrying) {
          isRetrying = true
          try {
            const retryConfig = { ...error.response.config }
            // 恢复原始数据（防止双重加密）
            if (retryConfig._originalData) {
              const method = (retryConfig.method || 'GET').toUpperCase()
              if (method === 'GET') {
                retryConfig.params = retryConfig._originalData
              } else {
                retryConfig.data = retryConfig._originalData
              }
            }
            delete retryConfig._aesKey
            delete retryConfig._aesIv
            return await request(retryConfig)
          } finally {
            isRetrying = false
          }
        }
        // 已重试过还失败，静默处理
        return Promise.reject(new Error(bizMsg || '公钥已过期，请刷新页面'))
      }

      if (error.response.status === 401) {
        const userStore = useUserStore()
        userStore.logout()
        if (!isRedirecting) {
          isRedirecting = true
          router.push('/login').finally(() => { isRedirecting = false })
        }
        // 避免并发 401 时多次弹出提示
        if (!ElMessage._401Shown) {
          ElMessage._401Shown = true
          ElMessage.error('登录已过期，请重新登录')
          setTimeout(() => { ElMessage._401Shown = false }, 2000)
        }
      } else {
        // 业务错误交给页面 catch 统一提示，避免重复弹窗
      }
    } else {
      // 网络层错误：超时 / 断网 / CORS 等，统一提示
      ElMessage.error(displayMsg)
    }
    return Promise.reject(new Error(displayMsg))
  }
)

export default request