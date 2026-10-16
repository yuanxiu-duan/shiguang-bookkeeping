import request from '@/utils/request'

function getAccountBookId() {
  const val = localStorage.getItem('currentAccountBookId')
  return val ? JSON.parse(val) : null
}

export function getTags(keyword) {
  const params = keyword ? { keyword } : {}
  params.accountBookId = getAccountBookId()
  return request.get('/tags', { params })
}

export function createTag(data) {
  data.accountBookId = data.accountBookId || getAccountBookId()
  return request({
    url: '/tags',
    method: 'post',
    data
  })
}

export function updateTag(id, data) {
  return request({
    url: '/tags/' + id,
    method: 'put',
    data
  })
}

export function deleteTag(id) {
  return request({
    url: '/tags/' + id,
    method: 'delete'
  })
}
