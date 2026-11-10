import request from '@/utils/request'

// ========== OSS配置 ==========

export function getOssConfigs() {
  return request.get('/admin/oss-configs')
}

export function getOssConfig(id) {
  return request.get(`/admin/oss-configs/${id}`)
}

export function createOssConfig(data) {
  return request.post('/admin/oss-configs', data)
}

export function updateOssConfig(id, data) {
  return request.put(`/admin/oss-configs/${id}`, data)
}

export function deleteOssConfig(id) {
  return request.delete(`/admin/oss-configs/${id}`)
}

export function activateOssConfig(id) {
  return request.put(`/admin/oss-configs/${id}/activate`)
}

export function testOssConnection(id) {
  return request.post(`/admin/oss-configs/${id}/test`)
}

// ========== APP发布 ==========

export function getAppReleases(platform) {
  return request.get('/admin/app-releases', { params: { platform } })
}

export function getAppRelease(id) {
  return request.get(`/admin/app-releases/${id}`)
}

/**
 * 第一步：上传APP文件到OSS，返回 {downloadUrl, fileKey, fileSize, fileName}
 */
export function uploadAppFile(file, platform, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('platform', platform || 'android')

  // 用 XHR 直接发送，支持进度回调，绕开 axios 拦截器
  const baseURL = import.meta.env.VITE_API_BASE_URL || ''
  const token = localStorage.getItem('token')

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', baseURL + '/admin/app-releases/upload-file')

    if (token) {
      xhr.setRequestHeader('Authorization', 'Bearer ' + token)
    }

    if (onProgress) {
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
          onProgress(e)
        }
      }
    }

    xhr.onload = function () {
      try {
        const result = JSON.parse(xhr.responseText)
        if (result.code === 200) {
          resolve(result.data)
        } else {
          reject(new Error(result.msg || '上传失败'))
        }
      } catch (e) {
        reject(new Error('响应解析失败'))
      }
    }

    xhr.onerror = function () {
      reject(new Error('网络连接失败'))
    }

    xhr.send(formData)
  })
}

/**
 * 直传（推荐）：先向服务端要一个一次性预签名地址，再由浏览器直接 PUT 到对象存储。
 * 文件不经过服务器，绕开服务器上行带宽瓶颈（服务器到 R2 实测约 620KB/s，浏览器直传快 8 倍）。
 * 预签名地址短时有效（30 分钟）、只能写这一个 key，浏览器即使泄露也拿不到长期密钥。
 */
export async function uploadAppFileDirect(file, platform, onProgress) {
  const info = await request.post(
    '/admin/app-releases/presign',
    { fileName: file.name, fileSize: file.size },
    { params: { platform: platform || 'android' } }
  )

  await new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('PUT', info.uploadUrl)
    // Content-Type 已参与签名，必须与服务端返回的完全一致
    xhr.setRequestHeader('Content-Type', info.contentType)

    if (onProgress) {
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
          onProgress(e)
        }
      }
    }

    xhr.onload = function () {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve()
      } else {
        reject(new Error(`上传到对象存储失败（HTTP ${xhr.status}）`))
      }
    }
    xhr.onerror = function () {
      reject(new Error('直传对象存储网络错误，请检查存储桶 CORS 配置'))
    }
    xhr.ontimeout = function () {
      reject(new Error('上传超时，请重试'))
    }

    xhr.send(file)
  })

  return {
    downloadUrl: info.downloadUrl,
    fileKey: info.fileKey,
    fileSize: info.fileSize,
    fileName: info.fileName
  }
}

/**
 * 第二步：提交版本发布信息（JSON body）
 */
export function createAppRelease(data) {
  return request.post('/admin/app-releases', data)
}

export function publishAppRelease(id) {
  return request.put(`/admin/app-releases/${id}/publish`)
}

export function unpublishAppRelease(id) {
  return request.put(`/admin/app-releases/${id}/unpublish`)
}

export function deleteAppRelease(id) {
  return request.delete(`/admin/app-releases/${id}`)
}

// ========== APP更新检查（公开接口） ==========

export function checkAppUpdate(platform, versionCode) {
  return request.get('/admin/app-releases/check-update', {
    params: { platform, versionCode }
  })
}
