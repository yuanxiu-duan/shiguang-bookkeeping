function padZero(num) {
  return num < 10 ? '0' + num : '' + num
}

function parseDate(date) {
  if (!date) return null
  
  if (date instanceof Date) {
    return date
  }
  
  if (typeof date === 'string') {
    if (date.includes('T')) {
      date = date.replace('T', ' ').replace(/\..+$/, '')
    }
    return new Date(date.replace(/-/g, '/'))
  }
  
  return new Date(date)
}

function format(date, format = 'YYYY-MM-DD') {
  const d = parseDate(date)
  if (!d || isNaN(d.getTime())) return ''
  
  const year = d.getFullYear()
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hours = d.getHours()
  const minutes = d.getMinutes()
  const seconds = d.getSeconds()
  
  return format
    .replace(/YYYY/g, year)
    .replace(/MM/g, padZero(month))
    .replace(/DD/g, padZero(day))
    .replace(/HH/g, padZero(hours))
    .replace(/mm/g, padZero(minutes))
    .replace(/ss/g, padZero(seconds))
}

export default {
  format
}
