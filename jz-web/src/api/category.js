import request from '@/utils/request'

function getAccountBookId() {
  const val = localStorage.getItem('currentAccountBookId')
  return val ? JSON.parse(val) : null
}

export function getCategories() {
  return request.get('/categories', { params: { accountBookId: getAccountBookId() } })
}

export function getCategoriesByType(type) {
  return request.get(`/categories/type/${type}`, { params: { accountBookId: getAccountBookId() } })
}

export function createCategory(data) {
  data.accountBookId = data.accountBookId || getAccountBookId()
  return request.post('/categories', data)
}

export function updateCategory(id, data) {
  return request.put(`/categories/${id}`, data)
}

export function deleteCategory(id) {
  return request.delete(`/categories/${id}`)
}
