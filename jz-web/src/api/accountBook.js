import request from '@/utils/request'

export function getAccountBooks() {
  return request.get('/account-books')
}

export function createAccountBook(data) {
  return request.post('/account-books', data)
}

export function updateAccountBook(id, data) {
  return request.put(`/account-books/${id}`, data)
}

export function deleteAccountBook(id) {
  return request.delete(`/account-books/${id}`)
}
