/**
 * RSA + AES 混合加解密工具（Web 端）
 *
 * 传输协议同 jz-app/utils/crypto.js
 */

import CryptoJS from 'crypto-js'
import JSEncrypt from 'jsencrypt'

const PUBLIC_KEY_CACHE = '_rsa_public_key'

let publicKey = null

/**
 * 获取 RSA 公钥
 */
export async function getPublicKey() {
  if (publicKey) return publicKey

  const cached = localStorage.getItem(PUBLIC_KEY_CACHE)
  if (cached) {
    publicKey = cached
    return publicKey
  }

  return await fetchPublicKey()
}

async function fetchPublicKey() {
  const baseURL = import.meta.env.VITE_API_BASE_URL
  const res = await fetch(baseURL + '/auth/public-key')
  const result = await res.json()

  if (result.code === 200) {
    publicKey = result.data
    localStorage.setItem(PUBLIC_KEY_CACHE, publicKey)
    return publicKey
  }

  throw new Error('获取RSA公钥失败')
}

/**
 * 清除公钥缓存
 */
export function clearPublicKey() {
  publicKey = null
  localStorage.removeItem(PUBLIC_KEY_CACHE)
}

function generateAesKeyAndIv() {
  const key = CryptoJS.lib.WordArray.random(32)
  const iv = CryptoJS.lib.WordArray.random(16)
  return {
    key: CryptoJS.enc.Base64.stringify(key),
    iv: CryptoJS.enc.Base64.stringify(iv)
  }
}

function rsaEncrypt(data) {
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(publicKey)
  const encrypted = encrypt.encrypt(data)
  if (!encrypted) {
    throw new Error('RSA加密失败，公钥可能已过期')
  }
  return encrypted
}

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
 */
export function encryptRequest(data, method) {
  const { key, iv } = generateAesKeyAndIv()
  const keyInfo = key + '.' + iv
  const encKey = rsaEncrypt(keyInfo)

  if (method === 'GET') {
    return {
      headers: { 'X-Enc-Key': encKey },
      data: data,
      _aesKey: key,
      _aesIv: iv
    }
  }

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
 */
export function decryptResponse(encData, aesKey, aesIv) {
  const decrypted = aesDecrypt(encData, aesKey, aesIv)
  return JSON.parse(decrypted)
}
