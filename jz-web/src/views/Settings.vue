<template>
  <div class="settings-container">
    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <span>账号设置</span>
        </div>
      </template>

      <!-- 当前账号信息 -->
      <el-descriptions :column="1" border class="user-info-desc">
        <el-descriptions-item label="头像">
          <el-avatar :size="60" :src="userStore.userInfo?.avatar">
            <el-icon :size="28"><User /></el-icon>
          </el-avatar>
        </el-descriptions-item>
        <el-descriptions-item label="用户名">{{ userStore.userInfo?.username }}</el-descriptions-item>
        <el-descriptions-item label="当前邮箱">{{ userStore.userInfo?.email || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ userStore.userInfo?.nickname || '未设置' }}</el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="activeTab" class="settings-tabs">
        <!-- 修改头像 -->
        <el-tab-pane label="修改头像" name="avatar">
          <div class="avatar-upload-wrap">
            <el-avatar :size="120" :src="userStore.userInfo?.avatar" class="avatar-preview">
              <el-icon :size="56"><User /></el-icon>
            </el-avatar>
            <el-upload
              :show-file-list="false"
              :auto-upload="true"
              :before-upload="beforeAvatarUpload"
              :http-request="handleAvatarUpload"
              accept="image/*"
            >
              <el-button type="primary" :loading="loadingAvatar">上传新头像</el-button>
            </el-upload>
            <div class="avatar-tips">支持 JPG/PNG/GIF 等图片格式，文件不超过 5MB</div>
          </div>
        </el-tab-pane>

        <!-- 修改昵称 -->
        <el-tab-pane label="修改昵称" name="nickname">
          <el-form :model="nicknameForm" :rules="nicknameRules" ref="nicknameFormRef" label-width="100px">
            <el-form-item label="新昵称" prop="nickname">
              <el-input v-model="nicknameForm.nickname" placeholder="请输入新昵称" maxlength="20" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loadingNickname" @click="handleUpdateNickname">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 修改邮箱 -->
        <el-tab-pane label="修改邮箱" name="email">
          <template v-if="emailStep === 0">
            <!-- Step 1: 验证旧邮箱 -->
            <el-form :model="emailForm" :rules="oldEmailRules" ref="oldEmailFormRef" label-width="100px">
              <el-form-item label="当前邮箱">
                <el-input :model-value="userStore.userInfo?.email" disabled />
              </el-form-item>
              <el-form-item label="身份验证码" prop="oldEmailCode">
                <el-input v-model="emailForm.oldEmailCode" placeholder="请输入当前邮箱收到的验证码" maxlength="6">
                  <template #append>
                    <el-button
                      :disabled="oldCodeSending || oldCodeCountdown > 0"
                      @click="handleSendOldCode"
                    >
                      {{ oldCodeCountdown > 0 ? oldCodeCountdown + '秒' : '发送验证码' }}
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="emailStep = 1" :disabled="!emailForm.oldEmailCode">下一步</el-button>
              </el-form-item>
            </el-form>
          </template>
          <template v-if="emailStep === 1">
            <!-- Step 2: 设置新邮箱 -->
            <el-form :model="emailForm" :rules="newEmailRules" ref="newEmailFormRef" label-width="100px">
              <el-form-item label="新邮箱" prop="newEmail">
                <el-input v-model="emailForm.newEmail" placeholder="请输入新邮箱" />
              </el-form-item>
              <el-form-item label="新邮箱验证码" prop="newEmailCode">
                <el-input v-model="emailForm.newEmailCode" placeholder="请输入新邮箱收到的验证码" maxlength="6">
                  <template #append>
                    <el-button
                      :disabled="newCodeSending || newCodeCountdown > 0"
                      @click="handleSendNewCode"
                    >
                      {{ newCodeCountdown > 0 ? newCodeCountdown + '秒' : '获取验证码' }}
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="loadingEmail" @click="handleUpdateEmail">保存</el-button>
                <el-button text @click="emailStep = 0">上一步</el-button>
              </el-form-item>
            </el-form>
          </template>
        </el-tab-pane>

        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px">
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" placeholder="6-20位" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loadingPwd" @click="handleUpdatePassword">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/userStore'
import { sendCode, updateNickname, updateEmail, updatePassword, updateAvatar } from '@/api/login'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('nickname')

// 头像
const loadingAvatar = ref(false)

function beforeAvatarUpload(file) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('仅支持图片格式')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('头像大小不能超过 5MB')
    return false
  }
  return true
}

async function handleAvatarUpload({ file }) {
  loadingAvatar.value = true
  try {
    await updateAvatar(file)
    await userStore.fetchCurrentUser()
    ElMessage.success('头像修改成功')
  } catch (e) {
    ElMessage.error(e.message || '头像上传失败')
  } finally {
    loadingAvatar.value = false
  }
}

// 昵称
const nicknameFormRef = ref(null)
const loadingNickname = ref(false)
const nicknameForm = reactive({ nickname: userStore.userInfo?.nickname || '' })
const nicknameRules = {
  nickname: [
    { required: true, message: '请输入新昵称', trigger: 'blur' },
    { min: 1, max: 20, message: '长度 1-20', trigger: 'blur' }
  ]
}

async function handleUpdateNickname() {
  await nicknameFormRef.value.validate()
  loadingNickname.value = true
  try {
    await updateNickname({ nickname: nicknameForm.nickname })
    ElMessage.success('昵称修改成功')
    await userStore.fetchCurrentUser()
  } catch (e) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    loadingNickname.value = false
  }
}

// 邮箱 — 两步验证
const emailStep = ref(0)
const oldEmailFormRef = ref(null)
const newEmailFormRef = ref(null)
const loadingEmail = ref(false)

// 旧邮箱验证
const oldCodeSending = ref(false)
const oldCodeCountdown = ref(0)
let oldCodeTimer = null

// 新邮箱验证
const newCodeSending = ref(false)
const newCodeCountdown = ref(0)
let newCodeTimer = null

const emailForm = reactive({
  oldEmailCode: '',
  newEmail: '',
  newEmailCode: ''
})

const oldEmailRules = {
  oldEmailCode: [
    { required: true, message: '请输入身份验证码', trigger: 'blur' },
    { len: 6, message: '验证码 6 位', trigger: 'blur' }
  ]
}

const newEmailRules = {
  newEmail: [
    { required: true, message: '请输入新邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  newEmailCode: [
    { required: true, message: '请输入新邮箱验证码', trigger: 'blur' },
    { len: 6, message: '验证码 6 位', trigger: 'blur' }
  ]
}

async function handleSendOldCode() {
  if (!userStore.userInfo?.email) {
    ElMessage.warning('当前账号未绑定邮箱')
    return
  }
  oldCodeSending.value = true
  try {
    await sendCode({ email: userStore.userInfo.email, type: 'UPDATE_EMAIL_OLD_CONFIRM' })
    ElMessage.success('身份验证码已发送到当前邮箱')
    oldCodeCountdown.value = 60
    oldCodeTimer = setInterval(() => {
      oldCodeCountdown.value--
      if (oldCodeCountdown.value <= 0) {
        clearInterval(oldCodeTimer)
        oldCodeTimer = null
      }
    }, 1000)
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  } finally {
    oldCodeSending.value = false
  }
}

async function handleSendNewCode() {
  if (!emailForm.newEmail) {
    ElMessage.warning('请先输入新邮箱')
    return
  }
  newCodeSending.value = true
  try {
    await sendCode({ email: emailForm.newEmail, type: 'UPDATE_EMAIL' })
    ElMessage.success('验证码已发送到新邮箱')
    newCodeCountdown.value = 60
    newCodeTimer = setInterval(() => {
      newCodeCountdown.value--
      if (newCodeCountdown.value <= 0) {
        clearInterval(newCodeTimer)
        newCodeTimer = null
      }
    }, 1000)
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  } finally {
    newCodeSending.value = false
  }
}

async function handleUpdateEmail() {
  if (!emailForm.oldEmailCode || !emailForm.newEmail || !emailForm.newEmailCode) {
    ElMessage.warning('请完整填写信息')
    return
  }
  loadingEmail.value = true
  try {
    await updateEmail({
      oldEmailCode: emailForm.oldEmailCode,
      newEmail: emailForm.newEmail,
      newEmailCode: emailForm.newEmailCode
    })
    ElMessage.success('邮箱修改成功')
    emailForm.oldEmailCode = ''
    emailForm.newEmail = ''
    emailForm.newEmailCode = ''
    emailStep.value = 0
    await userStore.fetchCurrentUser()
  } catch (e) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    loadingEmail.value = false
  }
}

// 密码
const pwdFormRef = ref(null)
const loadingPwd = ref(false)
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

async function handleUpdatePassword() {
  await pwdFormRef.value.validate()
  loadingPwd.value = true
  try {
    await updatePassword({
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    setTimeout(() => {
      userStore.logout()
      router.push('/login')
    }, 1500)
  } catch (e) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    loadingPwd.value = false
  }
}

onBeforeUnmount(() => {
  if (oldCodeTimer) clearInterval(oldCodeTimer)
  if (newCodeTimer) clearInterval(newCodeTimer)
})
</script>

<style scoped>
.settings-container {
  display: flex;
  justify-content: center;
}

.settings-card {
  width: 100%;
  max-width: 720px;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
}

.user-info-desc {
  margin-bottom: 20px;
}

.settings-tabs {
  margin-top: 10px;
}

.avatar-upload-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 20px 0;
}

.avatar-preview {
  background: #f5f7fa;
  color: #c0c4cc;
}

.avatar-tips {
  color: #909399;
  font-size: 12px;
}
</style>