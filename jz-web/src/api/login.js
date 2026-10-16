import request from '@/utils/request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function sendCode(data) {
  return request.post('/auth/send-code', data)
}

export function getCurrentUser() {
  return request.get('/auth/me')
}

// === v2.0 用户设置 / 找回密码 ===
export function updateNickname(data) {
  return request.put('/auth/nickname', data)
}

export function updateEmail(data) {
  return request.put('/auth/email', data)
}

export function updatePassword(data) {
  return request.put('/auth/password', data)
}

export function resetPassword(data) {
  return request.post('/auth/reset-password', data)
}

// === 头像上传（multipart，已排除 CryptoFilter） ===
export function updateAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/auth/avatar', formData)
}