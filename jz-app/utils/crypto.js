/**
 * RSA + AES 混合加解密工具（uni-app 多端兼容版）
 *
 * 使用 node-forge 替代 jsencrypt，解决小程序/App 环境兼容问题：
 *   - jsencrypt 依赖 window/crypto/navigator，小程序/App 不支持
 *   - node-forge 是纯 JS 实现，无浏览器 API 依赖
 *
 * RSA 填充模式：PKCS#1 v1.5（与后端 RSA/ECB/PKCS1Padding 对应）
 */

import CryptoJS from 'crypto-js'
import forge from 'node-forge'
import { storage } from './storage.js'
import { getBaseUrl } from './config.js'

// 强制 node-forge 使用纯 JS 实现（避免在非浏览器环境访问 crypto.subtle）
forge.options.usePureJavaScript = true

const PUBLIC_KEY_CACHE = '_rsa_public_key'

let publicKey = null
let forgePublicKey = null  // 缓存解析后的 forge 公钥对象

/**
 * 获取 RSA 公钥（优先缓存，否则从后端拉取）
 */
export async function getPublicKey() {
  if (publicKey && forgePublicKey) return publicKey

  const cached = storage.get(PUBLIC_KEY_CACHE)
  if (cached) {
    publicKey = cached
    forgePublicKey = parsePublicKey(cached)
    return publicKey
  }

  return await fetchPublicKey()
}

async function fetchPublicKey() {
  const res = await new Promise((resolve, reject) => {
    uni.request({
      url: getBaseUrl() + '/auth/public-key',
      method: 'GET',
      success: resolve,
      fail: reject
    })
  })

  if (res.statusCode === 200 && res.data && res.data.code === 200) {
    publicKey = res.data.data
    forgePublicKey = parsePublicKey(publicKey)
    storage.set(PUBLIC_KEY_CACHE, publicKey)
    return publicKey
  }

  throw new Error('获取RSA公钥失败')
}

/**
 * 解析 PEM 格式公钥为 forge 公钥对象
 */
function parsePublicKey(pem) {
  try {
    return forge.pki.publicKeyFromPem(pem)
  } catch (e) {
    console.error('RSA公钥解析失败:', e)
    return null
  }
}

/**
 * 清除公钥缓存（服务端重启后密钥不匹配时调用）
 */
export function clearPublicKey() {
  publicKey = null
  forgePublicKey = null
  storage.remove(PUBLIC_KEY_CACHE)
}

/**
 * 生成随机 AES-256 密钥和 IV（Base64 编码）
 */
function generateAesKeyAndIv() {
  const key = CryptoJS.lib.WordArray.random(32)  // 256 bits
  const iv = CryptoJS.lib.WordArray.random(16)   // 128 bits
  return {
    key: CryptoJS.enc.Base64.stringify(key),
    iv: CryptoJS.enc.Base64.stringify(iv)
  }
}

/**
 * RSA 加密（使用 node-forge，PKCS#1 v1.5 填充）
 * 与后端 RSA/ECB/PKCS1Padding 对应
 */
function rsaEncrypt(data) {
  if (!forgePublicKey) {
    throw new Error('RSA加密失败，公钥未初始化')
  }
  try {
    const encrypted = forgePublicKey.encrypt(data, 'RSAES-PKCS1-V1_5')
    return forge.util.encode64(encrypted)
  } catch (e) {
    throw new Error('RSA加密失败，公钥可能已过期')
  }
}

/**
 * AES-CBC 加密
 */
function aesEncrypt(plainText, keyBase64, ivBase64) {
  const key = CryptoJS.enc.Base64.parse(keyBase64)
  const iv = CryptoJS.enc.Base64.parse(ivBase64)
  const encrypted = CryptoJS.AES.encrypt(plainText, key, {
    iv: iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })
  return encrypted.toString()
}

/**
 * AES-CBC 解密
 */
function aesDecrypt(cipherText, keyBase64, ivBase64) {
  const key = CryptoJS.enc.Base64.parse(keyBase64)
  const iv = CryptoJS.enc.Base64.parse(ivBase64)
  const decrypted = CryptoJS.AES.decrypt(cipherText, key, {
    iv: iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })
  return decrypted.toString(CryptoJS.enc.Utf8)
}

/**
 * 加密请求数据
 * @param {Object|undefined} data  原始请求数据
 * @param {string} method          HTTP 方法
 * @returns {{ data: Object, headers?: Object, _aesKey: string, _aesIv: string }}
 */
export function encryptRequest(data, method) {
  const { key, iv } = generateAesKeyAndIv()

  // RSA 加密 AES 密钥信息 "base64Key.base64Iv"
  const keyInfo = key + '.' + iv
  const encKey = rsaEncrypt(keyInfo)

  if (method === 'GET') {
    // GET 请求：密钥放 header，参数原样传递
    return {
      headers: { 'X-Enc-Key': encKey },
      data: data,
      _aesKey: key,
      _aesIv: iv
    }
  }

  // POST/PUT/PATCH/DELETE：加密请求体
  const plainText = JSON.stringify(data || {})
  const encData = aesEncrypt(plainText, key, iv)
  return {
    data: { encKey, encData },
    _aesKey: key,
    _aesIv: iv
  }
}

/**
 * 解密响应数据
 * @param {string} encData  加密的响应体
 * @param {string} aesKey   AES 密钥（Base64）
 * @param {string} aesIv    AES IV（Base64）
 * @returns {Object} 解密后的 JSON 对象
 */
export function decryptResponse(encData, aesKey, aesIv) {
  const decrypted = aesDecrypt(encData, aesKey, aesIv)
  return JSON.parse(decrypted)
}
