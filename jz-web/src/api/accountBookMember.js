import request from '@/utils/request'

export function getMembers(accountBookId) {
  return request.get(`/account-books/${accountBookId}/members`)
}

export function inviteMember(accountBookId, username) {
  return request.post(`/account-books/${accountBookId}/members/invite`, { username })
}

export function acceptInvitation(accountBookId, memberId) {
  return request.put(`/account-books/${accountBookId}/members/${memberId}/accept`)
}

export function rejectInvitation(accountBookId, memberId) {
  return request.put(`/account-books/${accountBookId}/members/${memberId}/reject`)
}

export function removeMember(accountBookId, memberId) {
  return request.delete(`/account-books/${accountBookId}/members/${memberId}`)
}

export function leaveAccountBook(accountBookId) {
  return request.post(`/account-books/${accountBookId}/leave`)
}

export function getPendingInvitations() {
  return request.get('/account-books/invitations')
}

export function getMyRole(accountBookId) {
  return request.get(`/account-books/${accountBookId}/role`)
}

export function setAlias(accountBookId, alias) {
  return request.put(`/account-books/${accountBookId}/alias`, { alias })
}
