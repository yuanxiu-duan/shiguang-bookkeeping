<template>
  <view v-if="visible" class="confirm-mask" :style="zIndexStyle" @click="onMaskClick">
    <view class="confirm-card" @click.stop>
      <view v-if="danger" class="confirm-icon">!</view>

      <view class="confirm-head">
        <text class="confirm-title">{{ title }}</text>
      </view>

      <view class="confirm-body">
        <text class="confirm-text">{{ content }}</text>
      </view>

      <view class="confirm-footer">
        <button v-if="showCancel" class="btn-cancel" :disabled="loading" @click="onCancel">{{ cancelText }}</button>
        <button
          :class="danger ? 'btn-danger' : 'btn-confirm'"
          :disabled="loading"
          @click="onConfirm"
        >{{ confirmText }}</button>
      </view>
    </view>
  </view>
</template>

<script>
/**
 * 统一确认弹窗（替换系统 uni.showModal —— 系统弹窗样式改不了，看起来很 low）
 *
 * 用法：
 *   <confirm-dialog ref="confirmDialog" />
 *
 *   const ok = await this.$refs.confirmDialog.open({
 *     title: '删除流水',
 *     content: '删除后无法恢复，确定删除这条记录吗？',
 *     danger: true,
 *     confirmText: '删除'
 *   })
 *   if (!ok) return
 */
export default {
  name: 'ConfirmDialog',
  props: {
    // 遮罩是否可点击关闭
    maskClosable: {
      type: Boolean,
      default: true
    },
    zIndex: {
      type: [Number, String],
      default: 1200
    }
  },
  data() {
    return {
      visible: false,
      loading: false,
      title: '提示',
      content: '',
      danger: false,
      confirmText: '确定',
      cancelText: '取消',
      // 纯提示类弹窗（如"导入完成"）可以只要一个按钮
      showCancel: true,
      _resolve: null
    }
  },
  computed: {
    zIndexStyle() {
      return 'z-index: ' + this.zIndex + ';'
    }
  },
  methods: {
    open(options = {}) {
      this.title = options.title || '提示'
      this.content = options.content || ''
      this.danger = !!options.danger
      this.confirmText = options.confirmText || '确定'
      this.cancelText = options.cancelText || '取消'
      this.showCancel = options.showCancel !== false
      this.loading = false
      this.visible = true
      // 已经开着就先关掉上一个，避免 Promise 悬挂
      this._settle(false)
      return new Promise((resolve) => {
        this._resolve = resolve
      })
    },

    close() {
      this.visible = false
      this.loading = false
    },

    _settle(value) {
      const resolve = this._resolve
      this._resolve = null
      if (resolve) resolve(value)
    },

    onMaskClick() {
      if (!this.maskClosable || this.loading) return
      this.close()
      this._settle(false)
    },

    onCancel() {
      if (this.loading) return
      this.close()
      this._settle(false)
    },

    onConfirm() {
      if (this.loading) return
      this.close()
      this._settle(true)
    }
  }
}
</script>

<style>
.confirm-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.confirm-card {
  position: relative;
  width: 620rpx;
  background: #ffffff;
  padding-top: 40rpx;
}

/* 危险操作顶部的小红标，让删除弹窗不那么"素" */
.confirm-icon {
  width: 96rpx;
  height: 96rpx;
  margin: 0 auto 20rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffe1dd 0%, #ffc9c2 100%);
  color: #f5453e;
  font-size: 52rpx;
  font-weight: 700;
  line-height: 96rpx;
  text-align: center;
}

.confirm-head {
  padding: 0 40rpx;
  text-align: center;
}

.confirm-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #1f2937;
}

.confirm-body {
  padding: 20rpx 48rpx 8rpx;
  text-align: center;
}

.confirm-text {
  font-size: 28rpx;
  line-height: 44rpx;
  color: #6b7280;
  /* 支持 \n 换行（"关于"弹窗那种多行文案） */
  white-space: pre-wrap;
}
</style>
