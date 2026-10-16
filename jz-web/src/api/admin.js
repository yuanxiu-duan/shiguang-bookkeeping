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
