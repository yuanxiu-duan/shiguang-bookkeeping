/**
 * 金额计算工具
 *
 * JS 中 0.1 + 0.2 !== 0.3，浮点数直接累加在金额统计场景会产生精度丢失。
 * 这里统一以「分」为整数单位进行累加，再换算回「元」，彻底规避误差。
 */

/**
 * 将金额（元，数字或字符串）转换为整数「分」，四舍五入到 2 位小数。
 * 非法的输入统一按 0 处理。
 */
export function toCents(value) {
  const n = Number(value)
  if (!isFinite(n)) return 0
  return Math.round(n * 100)
}

/**
 * 对一组金额求和，返回「元」（保留两位小数，但内部用整数分累加避免精度丢失）。
 * @param {Array} list 数据列表
 * @param {Function} [getVal] 取金额的函数 (item) => 金额(元)。不传则把元素本身当作金额。
 */
export function sumAmounts(list, getVal) {
  let cents = 0
  if (list && list.length) {
    for (let i = 0; i < list.length; i++) {
      const v = getVal ? getVal(list[i]) : list[i]
      cents += toCents(v)
    }
  }
  return cents / 100
}

/**
 * 两个金额相减，返回「元」（整数分运算，避免 0.x - 0.y 的浮点误差）。
 */
export function subtractAmount(a, b) {
  return (toCents(a) - toCents(b)) / 100
}
