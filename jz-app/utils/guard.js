/**
 * 提交防重工具
 *
 * 移动端存在误触、连点、网络慢时重复点击等情况，会导致同一条数据被提交多次。
 * withGuard 在 key 维度做互斥锁：执行中的任务不会再次进入；
 * 任务结束后再保留一小段冷却时间（cooldown），防止"请求刚返回就被二次点击"。
 *
 * 用法：
 *   withGuard('addTransaction:save', async () => { ... }, 1000)
 */
const locks = Object.create(null)

export function isLocked(key) {
  return !!locks[key]
}

export function withGuard(key, fn, cooldown = 1000) {
  if (locks[key]) {
    return Promise.resolve(null)
  }
  locks[key] = true

  const release = () => {
    // 冷却一段时间再释放，避免请求快速返回后用户连点造成重复提交
    if (cooldown > 0) {
      setTimeout(() => { delete locks[key] }, cooldown)
    } else {
      delete locks[key]
    }
  }

  let result
  try {
    result = fn()
  } catch (e) {
    release()
    throw e
  }

  if (result && typeof result.then === 'function') {
    return result.then(
      (value) => { release(); return value },
      (err) => { release(); throw err }
    )
  }

  release()
  return Promise.resolve(result)
}
