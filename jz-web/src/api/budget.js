import request from '@/utils/request'

function getAccountBookId() {
  const val = localStorage.getItem('currentAccountBookId')
  return val ? JSON.parse(val) : null
}

export function getCurrentBudgets() {
  return request({
    url: '/budgets/current',
    method: 'get',
    params: { accountBookId: getAccountBookId() }
  })
}

export function getBudgetsByMonth(month) {
  return request({
    url: '/budgets/month/' + month,
    method: 'get',
    params: { accountBookId: getAccountBookId() }
  })
}

export function createBudget(data) {
  data.accountBookId = data.accountBookId || getAccountBookId()
  return request({
    url: '/budgets',
    method: 'post',
    data
  })
}

export function updateBudget(id, data) {
  return request({
    url: '/budgets/' + id,
    method: 'put',
    params: { accountBookId: getAccountBookId() },
    data
  })
}

export function deleteBudget(id) {
  return request({
    url: '/budgets/' + id,
    method: 'delete',
    params: { accountBookId: getAccountBookId() }
  })
}

export function getAlerts(all = false) {
  return request({
    url: '/budgets/alerts',
    method: 'get',
    params: { all, accountBookId: getAccountBookId() }
  })
}

export function getAlertCount() {
  return request({
    url: '/budgets/alerts/count',
    method: 'get',
    params: { accountBookId: getAccountBookId() }
  })
}

export function markAlertRead(id) {
  return request({
    url: '/budgets/alerts/' + id + '/read',
    method: 'put',
    params: { accountBookId: getAccountBookId() }
  })
}

export function markAllAlertsRead() {
  return request({
    url: '/budgets/alerts/read-all',
    method: 'put',
    params: { accountBookId: getAccountBookId() }
  })
}
