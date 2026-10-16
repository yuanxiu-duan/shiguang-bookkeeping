import request from '@/utils/request'

function getAccountBookId() {
  const val = localStorage.getItem('currentAccountBookId')
  return val ? JSON.parse(val) : null
}

export function getMonthlyStatistics(month) {
  const params = month ? { month } : {}
  params.accountBookId = getAccountBookId()
  return request.get('/statistics/monthly', { params })
}

export function getYearlyStatistics(year) {
  const params = year ? { year } : {}
  params.accountBookId = getAccountBookId()
  return request.get('/statistics/yearly', { params })
}

export function getCurveData(mode, params = {}) {
  params.accountBookId = getAccountBookId()
  params.mode = mode
  return request.get('/statistics/curve', { params })
}

export function getMonthlyTrend(months = 12) {
  return request.get('/statistics/trend', { params: { months, accountBookId: getAccountBookId() } })
}

export function getCategoryTrend(categoryId, months = 6) {
  return request.get(`/statistics/category-trend/${categoryId}`, { params: { months, accountBookId: getAccountBookId() } })
}
