<template>
  <view class="avi-root">
    <!-- ====== 触发区域（支持自定义） ====== -->
    <slot name="trigger">
      <view class="avi-trigger" @click="open">
        <view class="avi-trigger-left">
          <text class="avi-trigger-icon">ℹ️</text>
          <text class="avi-trigger-label">关于</text>
        </view>
        <view class="avi-trigger-right">
          <text class="avi-trigger-hint">v{{ versionName }}</text>
          <text class="avi-trigger-arrow">›</text>
        </view>
      </view>
    </slot>

    <!-- ====== 版本信息弹窗 ====== -->
    <view class="avi-mask" v-if="visible" @click="close">
      <view class="avi-dialog" @click.stop>
        <!-- 头部 -->
        <view class="avi-header">
          <view class="avi-app-icon">
            <text class="avi-app-icon-text">📒</text>
          </view>
          <text class="avi-app-name">{{ appName }}</text>
          <text class="avi-app-desc">{{ appDesc }}</text>
        </view>

        <!-- 版本信息区 -->
        <view class="avi-version">
          <view class="avi-version-row">
            <text class="avi-version-label">当前版本</text>
            <text class="avi-version-value">{{ versionName }}</text>
          </view>
          <view class="avi-version-row">
            <text class="avi-version-label">Build</text>
            <text class="avi-version-value">{{ versionCode }}</text>
          </view>
          <!-- #ifdef APP-PLUS -->
          <view class="avi-version-row">
            <text class="avi-version-label">平台</text>
            <text class="avi-version-value">{{ platformText }}</text>
          </view>
          <!-- #endif -->
          <!-- #ifdef H5 -->
          <view class="avi-version-row">
            <text class="avi-version-label">环境</text>
            <text class="avi-version-value">Web / H5</text>
          </view>
          <!-- #endif -->
          <!-- #ifdef MP-WEIXIN -->
          <view class="avi-version-row">
            <text class="avi-version-label">环境</text>
            <text class="avi-version-value">微信小程序</text>
          </view>
          <!-- #endif -->
        </view>

        <!-- 功能特性 -->
        <view class="avi-features">
          <text class="avi-features-title">功能特性</text>
          <view class="avi-feature-list">
            <text class="avi-feature-item">📝 离线记账，随时随地记录收支</text>
            <text class="avi-feature-item">💰 预算管理与超支预警</text>
            <text class="avi-feature-item">☁️ 多端数据同步</text>
            <text class="avi-feature-item">📊 统计报表与趋势分析</text>
            <text class="avi-feature-item">📥 Excel 导入导出</text>
          </view>
        </view>

        <!-- 操作按钮 -->
        <view class="avi-actions">
          <!-- #ifdef APP-PLUS -->
          <button
            class="avi-btn avi-btn-check"
            :loading="checking"
            :disabled="checking"
            @click="handleCheckUpdate"
          >
            {{ checking ? '检查中...' : '检查更新' }}
          </button>
          <!-- #endif -->
          <button class="avi-btn avi-btn-close" @click="close">关闭</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import upgradeManager from '../utils/upgrade.js'

/**
 * AppVersionInfo - 版本信息弹窗组件
 *
 * 用法:
 *   <AppVersionInfo>
 *     <template #trigger>自定义触发按钮</template>
 *   </AppVersionInfo>
 *
 *   // 或直接使用默认触发样式:
 *   <AppVersionInfo />
 *
 * 维护指南:
 *   - appName / appDesc / features → 本组件直接修改
 *   - versionName / versionCode → upgradeManager 自动从 manifest.json 读取
 *   - 更新检查 → 调用 upgradeManager.checkAndPrompt()
 */

export default {
  name: 'AppVersionInfo',

  props: {
    /** 应用名称 */
    appName: {
      type: String,
      default: '拾光账本'
    },
    /** 应用描述 */
    appDesc: {
      type: String,
      default: '个人财务记账与预算预警系统'
    }
  },

  data() {
    return {
      visible: false,
      checking: false,
      _versionInfo: null
    }
  },

  computed: {
    versionName() {
      return this._versionInfo ? this._versionInfo.versionName : '1.0.0'
    },
    versionCode() {
      return this._versionInfo ? this._versionInfo.versionCode : 0
    },
    platformText() {
      if (!this._versionInfo) return ''
      if (this._versionInfo.platform === 'ios') return 'iOS'
      if (this._versionInfo.platform === 'android') return 'Android'
      return this._versionInfo.platform
    }
  },

  methods: {
    open() {
      this._versionInfo = upgradeManager.getVersionInfo()
      this.visible = true
    },
    close() {
      this.visible = false
      this.checking = false
    },
    async handleCheckUpdate() {
      this.checking = true
      // 静默检查，结果由 upgradeManager 内部的弹窗展示
      await upgradeManager.checkAndPrompt({ silent: false })
      this.checking = false
    }
  }
}
</script>

<style>
/* ====== 触发区域 ====== */
.avi-trigger {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 0;
}

.avi-trigger-left {
  display: flex;
  align-items: center;
}

.avi-trigger-icon {
  font-size: 36rpx;
  margin-right: 20rpx;
}

.avi-trigger-label {
  font-size: 30rpx;
  color: #303133;
}

.avi-trigger-right {
  display: flex;
  align-items: center;
}

.avi-trigger-hint {
  font-size: 26rpx;
  color: #909399;
  margin-right: 12rpx;
}

.avi-trigger-arrow {
  font-size: 32rpx;
  color: #c0c4cc;
}

/* ====== 弹窗遮罩 ====== */
.avi-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60rpx;
}

/* ====== 弹窗主体 ====== */
.avi-dialog {
  width: 100%;
  max-width: 560rpx;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  padding: 48rpx 40rpx 40rpx;
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.15);
}

/* ====== 头部 ====== */
.avi-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.avi-app-icon {
  width: 100rpx;
  height: 100rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
}

.avi-app-icon-text {
  font-size: 48rpx;
}

.avi-app-name {
  font-size: 36rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8rpx;
}

.avi-app-desc {
  font-size: 24rpx;
  color: #909399;
}

/* ====== 版本信息 ====== */
.avi-version {
  padding: 28rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.avi-version-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10rpx 0;
}

.avi-version-label {
  font-size: 28rpx;
  color: #909399;
}

.avi-version-value {
  font-size: 28rpx;
  color: #303133;
  font-weight: 500;
}

/* ====== 功能特性 ====== */
.avi-features {
  padding: 28rpx 0 8rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.avi-features-title {
  font-size: 26rpx;
  color: #909399;
  margin-bottom: 16rpx;
  display: block;
}

.avi-feature-list {
  display: flex;
  flex-direction: column;
}

.avi-feature-item {
  font-size: 26rpx;
  color: #606266;
  line-height: 1.8;
}

/* ====== 操作按钮 ====== */
.avi-actions {
  display: flex;
  gap: 20rpx;
  padding-top: 32rpx;
}

.avi-btn {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 38rpx;
  font-size: 28rpx;
  text-align: center;
  border: none;
  padding: 0;
}

.avi-btn::after {
  border: none;
}

.avi-btn-check {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.avi-btn-close {
  background: #f5f5f5;
  color: #606266;
}
</style>
