<template>
  <div class="forgot-container">
    <el-card class="forgot-card">
      <h2 class="title">找回密码</h2>

      <el-steps :active="step" finish-status="success" simple style="margin-bottom: 24px;">
        <el-step title="验证邮箱" />
        <el-step title="设置新密码" />
      </el-steps>

      <!-- Step 1: 邮箱验证 -->
      <el-form v-if="step === 0" :model="emailForm" :rules="emailRules" ref="emailFormRef" label-position="top">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="emailForm.email" placeholder="请输入注册时绑定的邮箱" prefix-icon="Message" />
        </el-form-item>
        <el-form-item label="验证码" prop="emailCode">
          <el-input v-model="emailForm.emailCode" placeholder="请输入邮箱验证码" maxlength="6">
            <template #append>
              <el-button :disabled="codeSending || countdown > 0" @click="handleSendCode">
                {{ countdown > 0 ? countdown + '秒后重试' : '获取验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" @click="handleVerifyEmail">下一步</el-button>
          <el-button text @click="$router.push('/login')">返回登录</el-button>
        </el-form-item>
      </el-form>

      <!-- Step 2: 重置密码 -->
      <el-form v-if="step === 1" :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-position="top">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="6-20 位" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="submit-btn" @click="handleReset">重置密码</el-button>
          <el-button text @click="step = 0">上一步</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendCode, resetPassword } from '@/api/login'

const router = useRouter()
const step = ref(0)
const codeSending = ref(false)
const countdown = ref(0)
const loading = ref(false)
let timer = null

const emailFormRef = ref(null)
const emailForm = reactive({ email: '', emailCode: '' })
const emailRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码 6 位', trigger: 'blur' }
  ]
}

const pwdFormRef = ref(null)
const pwdForm = reactive({ newPassword: '', confirmPassword: '' })

const validateConfirm = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleSendCode() {
  await emailFormRef.value.validateField('email')
  codeSending.value = true
  try {
    await sendCode({ email: emailForm.email, type: 'RESET_PASSWORD' })
    ElMessage.success('验证码已发送')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  } finally {
    codeSending.value = false
  }
}

async function handleVerifyEmail() {
  await emailFormRef.value.validate()
  // 不在此处真正验证，进入第二步统一提交
  step.value = 1
}

async function handleReset() {
  await pwdFormRef.value.validate()
  loading.value = true
  try {
    await resetPassword({
      email: emailForm.email,
      emailCode: emailForm.emailCode,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    })
    ElMessage.success('密码重置成功，请用新密码登录')
    setTimeout(() => router.push('/login'), 1500)
  } catch (e) {
    ElMessage.error(e.message || '重置失败')
    // 验证码错误时回到第一步
    if (e.message && e.message.includes('验证码')) {
      step.value = 0
    }
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.forgot-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.forgot-card {
  width: 460px;
  padding: 20px;
}

.title {
  text-align: center;
  margin-bottom: 20px;
  color: #303133;
}

.submit-btn {
  width: 100%;
  margin-bottom: 8px;
}
</style>