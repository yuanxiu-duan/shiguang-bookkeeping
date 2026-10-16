import request from '@/utils/request'

function getAccountBookId() {
  const val = localStorage.getItem('currentAccountBookId')
  return val ? JSON.parse(val) : null
}

export function getTransactions(params) {
  params = params || {}
  params.accountBookId = params.accountBookId || getAccountBookId()
  return request({
    url: '/transactions',
    method: 'get',
    params
  })
}

export function getRecentTransactions(days) {
  const params = { days, accountBookId: getAccountBookId() }
  return request({
    url: '/transactions/recent',
    method: 'get',
    params
  })
}

export function createTransaction(data) {
  data.accountBookId = data.accountBookId || getAccountBookId()
  return request({
    url: '/transactions',
    method: 'post',
    data
  })
}

export function updateTransaction(id, data) {
  return request({
    url: '/transactions/' + id,
    method: 'put',
    params: { accountBookId: getAccountBookId() },
    data
  })
}

export function deleteTransaction(id) {
  return request({
    url: '/transactions/' + id,
    method: 'delete',
    params: { accountBookId: getAccountBookId() }
  })
}

export function classifySuggest(note) {
  return request({
    url: '/transactions/classify-suggest',
    method: 'post',
    data: { note, accountBookId: getAccountBookId() }
  })
}

export function importTransactions(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  const accountBookId = getAccountBookId()
  if (accountBookId) {
    formData.append('accountBookId', accountBookId)
  }
  
  return request({
    url: '/transactions/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: onProgress
  })
}

export function exportTransactions(params) {
  params = params || {}
  params.accountBookId = params.accountBookId || getAccountBookId()
  return request({
    url: '/transactions/export-tasks',
    method: 'post',
    data: params
  })
}

export function downloadTemplate() {
  const token = localStorage.getItem('token')
  const accountBookId = getAccountBookId()
  const param = accountBookId ? `&accountBookId=${encodeURIComponent(accountBookId)}` : ''
  window.open(`/api/transactions/template?token=${encodeURIComponent(token)}${param}`, '_blank')
}

export function getMonthlySummary(params) {
  params = params || {}
  params.accountBookId = params.accountBookId || getAccountBookId()
  return request({
    url: '/transactions/monthly-summary',
    method: 'get',
    params
  })
}
