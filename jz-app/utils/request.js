import { storage, TOKEN_KEY } from './storage.js'
import { getBaseUrl } from './config.js'
import { getPublicKey, encryptRequest, decryptResponse, clearPublicKey } from './crypto.js'

let token = storage.get(TOKEN_KEY, '')

export function setToken(newToken) {
  token = newToken
  storage.set(TOKEN_KEY, newToken)
}

export function clearToken() {
  token = ''
  storage.remove(TOKEN_KEY)
}

/**
 * 处理 401 未授权
 * - 无 token：首次进入/未登录 → 静默跳转登录页，不弹 toast
 * - 有 token：token 过期 → 弹提示后跳转登录页
 */
function handle401() {
  const hadToken = !!token
  clearToken()
  storage.remove('user_info')
  if (hadToken) {
    uni.showToast({ title: '登录已过期', icon: 'none' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/login/login' })
    }, 1500)
  } else {
    // 无 token：静默跳转
    uni.reLaunch({ url: '/pages/login/login' })
  }
}

/**
 * 加密请求（含自动重试：公钥过期时清除缓存重试一次）
 */
export function request(options) {
  return _request(options).catch(err => {
    const msg = err && err.message ? err.message : ''
    const code = err && err._code
    // 公钥失效场景（服务端重启 / 缓存过期）：清缓存后重试一次
    if (
      code === 4001 ||
      msg.includes('公钥可能已过期') ||
      msg.includes('公钥已过期') ||
      msg.includes('Request decryption failed') ||
      msg.includes('响应解密失败')
    ) {
      clearPublicKey()
      if (options._retried) throw err
      options._retried = true
      return _request(options)
    }
    throw err
  })
}

async function _request(options) {
  // 确保已获取 RSA 公钥
  await getPublicKey()

  return new Promise((resolve, reject) => {
    const header = {
      'Content-Type': 'application/json'
    }

    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }

    // 使用原始数据（重试时 options.data 已被加密，从 _originalData 恢复）
    const method = (options.method || 'GET').toUpperCase()
    let data = options._originalData
    if (data === undefined) {
      data = options.data
      // 保存原始数据，供重试时恢复（防止双重加密）
      if (data && (method === 'POST' || method === 'PUT')) {
        options._originalData = data
      }
    }
    if (data && method !== 'POST' && method !== 'PUT') {
      data = Object.keys(data).reduce((acc, key) => {
        if (data[key] !== null && data[key] !== undefined) {
          acc[key] = data[key]
        }
        return acc
      }, {})
    }

    // 加密请求数据
    let encrypted
    try {
      encrypted = encryptRequest(data, method)
    } catch (e) {
      // 加密失败（公钥过期等），清除缓存后拒绝
      clearPublicKey()
      reject(e)
      return
    }

    // 合并加密头
    if (encrypted.headers) {
      Object.assign(header, encrypted.headers)
    }

    // 是否抑制自动 toast
    const silent = options.silent === true

    uni.request({
      url: getBaseUrl() + options.url,
      method: method,
      data: encrypted.data,
      header,
      timeout: 15000,
      success: (res) => {
        // 从响应头中提取刷新后的 token 并更新本地存储（token 续期）
        const authHeader = res.header || res.headers || {}
        const newToken = authHeader['x-auth-token'] || authHeader['X-Auth-Token']
        if (newToken) {
          setToken(newToken)
        }

        if (res.statusCode === 200) {
          let result = res.data

          // 解密响应数据
          if (result && result.encData) {
            try {
              result = decryptResponse(result.encData, encrypted._aesKey, encrypted._aesIv)
            } catch (e) {
              clearPublicKey()
              reject(new Error('响应解密失败'))
              return
            }
          }

          if (result.code === 200 || result.code === 0) {
            resolve(result.data)
          } else if (res.statusCode === 401 || result.code === 401) {
            handle401()
            reject(new Error(result.msg || '未授权'))
          } else {
            const msg = result.msg || '请求失败'
            if (!silent) uni.showToast({ title: msg, icon: 'none' })
            reject(new Error(msg))
          }
        } else if (res.statusCode === 401) {
          // 401 响应可能也是加密的
          let body = res.data
          if (body && body.encData) {
            try {
              body = decryptResponse(body.encData, encrypted._aesKey, encrypted._aesIv)
            } catch (e) { /* 忽略解密错误 */ }
          }
          handle401()
          reject(new Error(body?.msg || '未授权'))
        } else {
          let body = res.data
          if (body && body.encData) {
            try {
              body = decryptResponse(body.encData, encrypted._aesKey, encrypted._aesIv)
            } catch (e) { /* 忽略解密错误 */ }
          }
          const msg = body?.msg || '服务器开小差啦，请稍后重试'
          const bizCode = body?.code
          // 公钥失效：静默重试，不弹 toast
          if (bizCode !== 4001 && !silent) uni.showToast({ title: msg, icon: 'none' })
          const e = new Error(msg)
          e._code = bizCode
          reject(e)
        }
      },
      fail: (err) => {
        // 统一拦截网络层错误，不暴露 errMsg / timeout 等技术细节给用户
        const msg = '服务器开小差啦，请稍后重试'
        if (!silent) uni.showToast({ title: msg, icon: 'none' })
        reject(new Error(msg))
      }
    })
  })
}

export function isOnline() {
  return new Promise((resolve) => {
    uni.getNetworkType({
      success: (res) => {
        resolve(res.networkType !== 'none')
      },
      fail: () => {
        resolve(false)
      }
    })
  })
}
