<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="title">拾光账本</h2>
      
      <!-- 登录方式切换 -->
      <el-tabs v-model="loginMode" class="login-tabs">
        <el-tab-pane label="密码登录" name="password" />
        <el-tab-pane label="验证码登录" name="code" />
      </el-tabs>
      
      <!-- 密码登录 -->
      <el-form v-if="loginMode === 'password'" :model="form" :rules="passwordRules" ref="formRef" label-position="top">
        <el-form-item label="账号 / 邮箱" prop="account">
          <el-input v-model="form.account" placeholder="请输入用户名或邮箱" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="submit-btn" @click="handleLogin">
            登录
          </el-button>
          <div class="login-links">
            <el-button text @click="goRegister">还没有账号？去注册</el-button>
            <el-button text @click="goForgot">忘记密码？</el-button>
          </div>
        </el-form-item>
      </el-form>
      
      <!-- 验证码登录 -->
      <el-form v-if="loginMode === 'code'" :model="form" :rules="codeRules" ref="codeFormRef" label-position="top">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" prefix-icon="Message">
            <template #append>
              <el-button 
                :disabled="codeSending || codeCountdown > 0"
                @click="handleSendCode"
              >
                {{ codeCountdown > 0 ? codeCountdown + '秒' : '获取验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="验证码" prop="emailCode">
          <el-input v-model="form.emailCode" placeholder="请输入验证码" maxlength="6" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="submit-btn" @click="handleLogin">
            登录
          </el-button>
          <div class="login-links">
            <el-button text @click="goRegister">还没有账号？去注册</el-button>
            <el-button text @click="goForgot">忘记密码？</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/userStore'
import { sendCode } from '@/api/login'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const codeFormRef = ref(null)
const loading = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
const loginMode = ref('password')
let codeTimer = null

const form = reactive({
  account: '',
  password: '',
  email: '',
  emailCode: ''
})

const passwordRules = {
  account: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const codeRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  emailCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function handleSendCode() {
  await codeFormRef.value.validateField('email')
  
  codeSending.value = true
  try {
    await sendCode({ email: form.email, type: 'LOGIN' })
    ElMessage.success('验证码已发送')
    codeCountdown.value = 60
    codeTimer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) {
        clearInterval(codeTimer)
        codeTimer = null
      }
    }, 1000)
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  } finally {
    codeSending.value = false
  }
}

async function handleLogin() {
  if (loginMode.value === 'password') {
    await formRef.value.validate()
    loading.value = true
    try {
      const account = form.account
      // 判断是邮箱还是用户名
      if (account.includes('@')) {
        await userStore.doLoginByEmail(account, form.password)
      } else {
        await userStore.doLogin(account, form.password)
      }
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || (userStore.userInfo?.role === 'ADMIN' ? '/admin' : '/')
      router.push(redirect)
    } catch (e) {
      ElMessage.error(e.message || '登录失败')
    } finally {
      loading.value = false
    }
  } else {
    await codeFormRef.value.validate()
    loading.value = true
    try {
      await userStore.doLoginByCode(form.email, form.emailCode)
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || (userStore.userInfo?.role === 'ADMIN' ? '/admin' : '/')
      router.push(redirect)
    } catch (e) {
      ElMessage.error(e.message || '登录失败')
    } finally {
      loading.value = false
    }
  }
}

function goRegister() {
  router.push('/register')
}

function goForgot() {
  router.push('/forgot-password')
}

onBeforeUnmount(() => {
  if (codeTimer) {
    clearInterval(codeTimer)
    codeTimer = null
  }
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  padding: 20px;
}

.title {
  text-align: center;
  margin-bottom: 10px;
  color: #303133;
}

.login-tabs {
  margin-bottom: 20px;
}

.submit-btn {
  width: 100%;
}

.login-links {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  width: 100%;
}
</style>