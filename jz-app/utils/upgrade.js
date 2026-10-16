import { appUpdateApi } from '../api/index.js'

/**
 * APP 升级管理模块
 *
 * 设计原则：
 *  - 所有升级配置均来源于 Admin 发布的 AppRelease（isPublished=true）
 *  - 仅在 APP-PLUS（手机端打包）环境下生效，H5/小程序自动跳过
 *  - 支持静默检查（启动时）和手动检查（用户点击）
 *  - 支持强制更新（不可取消）和可选更新
 *  - "忽略此版本"：用户可忽略非强制更新，本地记住，下次启动不再弹窗
 *  - Android：下载APK → 调用系统安装
 *  - iOS：跳转下载链接（Safari/App Store）
 */

// 获取当前APP基础信息
// 版本号唯一来源：manifest.json → 构建时注入为 plus.runtime.version/versionCode
// 打包APP后 manifest.json 文件不存在，但 plus.runtime 已内置其值，无需额外读取
function getAppInfo() {
  // #ifdef APP-PLUS
  try {
    const sysInfo = uni.getSystemInfoSync()
    return {
      platform: sysInfo.platform === 'ios' ? 'ios' : 'android',
      // plus.runtime.version/versionCode 在打包时从 manifest.json 注入，保证单一数据源
      versionName: plus.runtime.version || '0.0.0',
      versionCode: parseInt(String(plus.runtime.versionCode || '100'), 10)
    }
  } catch (e) {
    console.error('获取APP信息失败:', e)
    return { platform: 'android', versionName: '0.0.0', versionCode: 100 }
  }
  // #endif
  // #ifndef APP-PLUS
  return { platform: 'unknown', versionName: '0.0.0', versionCode: 0 }
  // #endif
}

// 跳过的版本号，存储在本地
const SKIPPED_VERSION_KEY = '_upgrade_skipped_version'

function getSkippedVersion() {
  try {
    const val = uni.getStorageSync(SKIPPED_VERSION_KEY)
    return val ? parseInt(val, 10) : 0
  } catch (e) {
    return 0
  }
}

function setSkippedVersion(versionCode) {
  try {
    uni.setStorageSync(SKIPPED_VERSION_KEY, String(versionCode))
  } catch (e) {
    console.error('存储跳过版本失败:', e)
  }
}

const upgradeManager = {
  _downloading: false,
  _downloadTask: null,

  /**
   * 检查并提示更新
   * @param {Object} options
   * @param {boolean} options.silent - true=静默模式(启动时自动检查)；false=手动检查(用户主动点击)
   */
  async checkAndPrompt({ silent = false } = {}) {
    // 仅在APP环境执行
    // #ifndef APP-PLUS
    if (!silent) {
      uni.showModal({
        title: '提示',
        content: '当前为开发/浏览器环境，APP升级功能仅在手机端可用',
        showCancel: false
      })
    }
    return
    // #endif

    // #ifdef APP-PLUS
    if (this._downloading) return

    const appInfo = getAppInfo()
    console.log('[Upgrade] APP信息: platform=' + appInfo.platform + ', versionCode=' + appInfo.versionCode + ', versionName=' + appInfo.versionName)
    if (!silent) {
      uni.showLoading({ title: '检查更新中...', mask: false })
    }

    try {
      const result = await appUpdateApi.checkUpdate(appInfo.platform, appInfo.versionCode, appInfo.versionName)
      console.log('[Upgrade] API返回: hasUpdate=' + result.hasUpdate + ', server_versionName=' + result.versionName + ', server_versionCode=' + result.versionCode)

      if (!silent) {
        uni.hideLoading()
      }

      if (result.hasUpdate) {
        // 静默模式下检查是否已跳过此版本
        if (silent) {
          const skipped = getSkippedVersion()
          if (skipped >= result.versionCode && !result.isForceUpdate) {
            console.log('已跳过版本 v' + result.versionName + ' (' + result.versionCode + ')，跳过弹窗')
            return
          }
        }
        this._showUpdateDialog(result, appInfo, silent)
      } else {
        if (!silent) {
          uni.showModal({
            title: '已是最新版本',
            content: '当前版本 v' + appInfo.versionName + ' 已是最新，无需更新',
            showCancel: false,
            confirmColor: '#667eea'
          })
        }
      }
    } catch (e) {
      if (!silent) {
        uni.hideLoading()
      }
      console.error('检查更新失败:', e)

      // 静默模式下不弹错误提示
      if (!silent) {
        const errMsg = e.message || ''
        uni.showModal({
          title: '检查更新失败',
          content: errMsg
            ? '无法连接到更新服务器，请检查网络后重试\n\n错误详情: ' + errMsg
            : '无法连接到更新服务器，请检查网络后重试',
          showCancel: false,
          confirmColor: '#f56c6c'
        })
      }
    }
    // #endif
  },

  /**
   * 显示更新弹窗
   */
  _showUpdateDialog(updateInfo, appInfo, silent) {
    const parts = []

    // 当前版本 vs 最新版本
    parts.push('当前版本: v' + appInfo.versionName + '  →  最新版本: v' + updateInfo.versionName)

    // 更新内容
    if (updateInfo.releaseNotes) {
      parts.push('\n更新内容:\n' + updateInfo.releaseNotes)
    }

    // 强制更新提示
    if (updateInfo.isForceUpdate) {
      parts.push('\n⚠️ 此版本为强制更新，需立即升级后才能继续使用')
    }

    const content = parts.join('\n')
    const isForce = Boolean(updateInfo.isForceUpdate)

    uni.showModal({
      title: '发现新版本',
      content: content,
      confirmText: '立即更新',
      cancelText: isForce ? '' : '忽略此版本',
      showCancel: !isForce,
      confirmColor: '#667eea',
      cancelColor: '#909399',
      success: (res) => {
        if (res.confirm) {
          this._startDownload(updateInfo)
        } else if (isForce) {
          // 强制更新下用户无法取消（showCancel=false 不会触发这里，但以防万一）
          this._showUpdateDialog(updateInfo, appInfo, silent)
        } else {
          // 用户点击"忽略此版本"：记住此版本，不再提示
          setSkippedVersion(updateInfo.versionCode)
          console.log('用户忽略版本 v' + updateInfo.versionName)
          uni.showToast({ title: '已忽略此版本', icon: 'none', duration: 1500 })
        }
      }
    })
  },

  /**
   * 开始下载APK
   */
  _startDownload(updateInfo) {
    const downloadUrl = updateInfo.downloadUrl
    if (!downloadUrl) {
      uni.showToast({ title: '下载地址无效', icon: 'none' })
      return
    }

    this._downloading = true
    uni.showLoading({ title: '正在下载更新 0%', mask: true })

    const downloadTask = uni.downloadFile({
      url: downloadUrl,
      success: (res) => {
        this._downloading = false
        uni.hideLoading()

        if (res.statusCode === 200) {
          this._installApk(res.tempFilePath, updateInfo)
        } else {
          uni.showModal({
            title: '下载失败',
            content: '服务器返回状态码: ' + res.statusCode + '，请稍后重试',
            showCancel: false,
            confirmColor: '#f56c6c'
          })
        }
      },
      fail: (err) => {
        this._downloading = false
        uni.hideLoading()

        // 用户主动取消不算错误
        if (err.errMsg && err.errMsg.includes('abort')) {
          console.log('下载已取消')
          return
        }

        uni.showModal({
          title: '下载失败',
          content: '下载更新包失败，请检查网络后重试',
          confirmText: '重试',
          cancelText: '取消',
          confirmColor: '#667eea',
          success: (modalRes) => {
            if (modalRes.confirm) {
              this._startDownload(updateInfo)
            }
          }
        })
        console.error('下载APK失败:', err)
      }
    })

    this._downloadTask = downloadTask

    // 监听下载进度
    downloadTask.onProgressUpdate((res) => {
      if (res.progress > 0) {
        uni.showLoading({ title: '正在下载更新 ' + res.progress + '%', mask: true })
      }
    })
  },

  /**
   * 安装APK
   * - Android: 调用系统安装器
   * - iOS: 打开下载链接
   */
  _installApk(filePath, updateInfo) {
    // #ifdef APP-PLUS
    const platform = uni.getSystemInfoSync().platform

    if (platform === 'android') {
      plus.runtime.install(filePath, {
        force: false
      }, () => {
        console.log('APK安装触发成功，APP即将退出')
        // 安装成功后 app 会自动退出重启
      }, (e) => {
        console.error('APK安装失败:', e)

        // 部分Android设备 install 失败，尝试用 openFile 打开
        plus.runtime.openFile(filePath, {
          pname: 'android'
        }, () => {
          console.log('使用系统程序打开APK成功')
        }, (openErr) => {
          uni.showModal({
            title: '安装失败',
            content: '无法安装更新包，请稍后重试\n错误信息: ' + (e.message || openErr.message || '未知错误'),
            showCancel: false,
            confirmColor: '#f56c6c'
          })
        })
      })
    } else {
      // iOS: 无法在APP内直接安装，打开下载链接
      plus.runtime.openURL(updateInfo.downloadUrl, () => {
        uni.showToast({ title: '即将跳转到浏览器下载', icon: 'none' })
      }, (err) => {
        uni.showToast({ title: '无法打开下载链接', icon: 'none' })
      })
    }
    // #endif
  },

  /**
   * 取消下载
   */
  cancelDownload() {
    if (this._downloadTask) {
      this._downloadTask.abort()
      this._downloading = false
      this._downloadTask = null
      uni.hideLoading()
    }
  },

  /**
   * 获取当前版本信息（供外部展示）
   */
  getVersionInfo() {
    return getAppInfo()
  }
}

export default upgradeManager
