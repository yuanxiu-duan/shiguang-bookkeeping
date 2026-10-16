/**
 * 全局 API/环境配置 - 唯一可信源
 *
 * 设计目标：免配置 / 零修改
 *   - HBuilderX 直接打包 App、小程序、H5 → 自动指向生产后端，无需手动调整本文件
 *   - 开发模式下，H5 走 vite 代理（/api）避免 CORS；小程序/App 自动用生产地址联调
 *   - 仅在"想用本机后端调试 App/小程序"这一种特殊场景下，开发者把下面 USE_LOCAL_NATIVE_DEV 改为 true
 *
 * 切换正式后端地址：只改 API_BASE_URL 一个常量。
 */

// ============ 唯一需要维护的配置 ============

/** 正式后端地址（HBuilderX 打包 App / 发布小程序 / H5 生产环境都使用此地址） */
const API_BASE_URL = 'https://your-domain.com/api'

/**
 * 是否在"开发模式下用本机后端调试 App/小程序"
 * 默认 false：开发者运行/发行 App 或小程序时直接连线上后端，免配置
 * true：App/小程序运行时连本机后端（必须同时配置 LOCAL_DEV_BASE_URL，且手机/模拟器与电脑同局域网）
 */
const USE_LOCAL_NATIVE_DEV = false

/** 仅当 USE_LOCAL_NATIVE_DEV=true 时生效；改成本机 LAN IP，例如 'http://192.168.1.10:8070/api' */
const LOCAL_DEV_BASE_URL = 'http://192.168.1.10:8070/api'

/** H5 相对路径，开发时走 vite proxy，生产时走 nginx 反代（永远不需要改） */
const H5_RELATIVE_BASE = '/api'

// ============ 自动判断当前平台/环境 ============

/** 是否处于开发模式（HBuilderX "运行" / `npm run dev:*`）；vite 编译期注入 */
const IS_DEV = process.env.NODE_ENV !== 'production'

/**
 * 业务请求统一使用的 API base URL
 *  - H5（dev & prod）：'/api'（dev 走 vite/manifest devServer 代理；prod 走 nginx 反代）
 *  - 小程序 / App：开发模式默认线上；开启 USE_LOCAL_NATIVE_DEV 后走本机
 */
export function getBaseUrl() {
  // #ifdef H5
  return H5_RELATIVE_BASE
  // #endif

  // #ifndef H5
  if (IS_DEV && USE_LOCAL_NATIVE_DEV) {
    return LOCAL_DEV_BASE_URL
  }
  return API_BASE_URL
  // #endif
}

/**
 * 需要"完整 URL"的场景：uni.uploadFile / 浏览器 window.open 下载 / 拼接 token 跳转
 *  - H5：仍返回相对路径 '/api'（uni.uploadFile 在 H5 下能正常走代理）
 *  - 小程序 / App：返回绝对地址
 */
export function getAbsoluteBaseUrl() {
  // #ifdef H5
  return H5_RELATIVE_BASE
  // #endif

  // #ifndef H5
  if (IS_DEV && USE_LOCAL_NATIVE_DEV) {
    return LOCAL_DEV_BASE_URL
  }
  return API_BASE_URL
  // #endif
}

/** 默认导出，方便 `import config from '@/utils/config.js'` */
export default {
  getBaseUrl,
  getAbsoluteBaseUrl,
  API_BASE_URL,
  H5_RELATIVE_BASE,
  IS_DEV
}
