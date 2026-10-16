<template>
  <div class="admin-page">
    <el-tabs v-model="activeTab">
      <!-- OSS配置 -->
      <el-tab-pane label="OSS存储配置" name="oss">
        <div class="tab-header">
          <el-button type="primary" @click="showOssDialog()">新增OSS配置</el-button>
        </div>

        <el-table :data="ossConfigs" border stripe>
          <el-table-column prop="name" label="配置名称" width="140" />
          <el-table-column prop="provider" label="提供商" width="100">
            <template #default="{ row }">
              <el-tag :type="providerTagType(row.provider)">{{ providerLabel(row.provider) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="endpoint" label="Endpoint" min-width="200" show-overflow-tooltip />
          <el-table-column prop="bucketName" label="Bucket" width="140" />
          <el-table-column prop="prefix" label="前缀" width="100" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isActive ? 'success' : 'info'">
                {{ row.isActive ? '使用中' : '未启用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button v-if="!row.isActive" type="success" size="small" @click="handleActivate(row)">启用</el-button>
              <el-button type="warning" size="small" @click="handleTestConnection(row)">测试</el-button>
              <el-button type="primary" size="small" @click="showOssDialog(row)">编辑</el-button>
              <el-button type="danger" size="small" :disabled="row.isActive" @click="handleDeleteOss(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- APP发布管理 -->
      <el-tab-pane label="APP发布管理" name="release">
        <div class="tab-header">
          <el-button type="primary" @click="showReleaseDialog()">新增版本发布</el-button>
          <el-select v-model="releasePlatform" placeholder="筛选平台" size="default" style="width: 140px; margin-left: 12px;" clearable @change="loadReleases">
            <el-option label="Android" value="android" />
            <el-option label="iOS" value="ios" />
          </el-select>
        </div>

        <el-table :data="releases" border stripe>
          <el-table-column prop="versionName" label="版本号" width="100" />
          <el-table-column prop="versionCode" label="版本编号" width="90" />
          <el-table-column prop="platform" label="平台" width="90">
            <template #default="{ row }">
              <el-tag>{{ row.platform === 'ios' ? 'iOS' : 'Android' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="releaseNotes" label="更新说明" min-width="200" show-overflow-tooltip />
          <el-table-column label="文件大小" width="110">
            <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column label="强制更新" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isForceUpdate ? 'danger' : 'info'" size="small">
                {{ row.isForceUpdate ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isPublished ? 'success' : 'warning'" size="small">
                {{ row.isPublished ? '已发布' : '未发布' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button v-if="!row.isPublished" type="success" size="small" @click="handlePublish(row)">发布</el-button>
              <el-button v-else type="warning" size="small" @click="handleUnpublish(row)">取消发布</el-button>
              <el-button type="danger" size="small" @click="handleDeleteRelease(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- OSS配置弹窗 -->
    <el-dialog v-model="ossDialogVisible" :title="ossForm.id ? '编辑OSS配置' : '新增OSS配置'" width="600px" destroy-on-close>
      <el-form :model="ossForm" label-width="120px" :rules="ossRules" ref="ossFormRef">
        <el-form-item label="配置名称" prop="name">
          <el-input v-model="ossForm.name" placeholder="如：阿里云OSS生产环境" />
        </el-form-item>
        <el-form-item label="OSS提供商" prop="provider">
          <el-select v-model="ossForm.provider" placeholder="选择OSS提供商" style="width: 100%;" @change="handleProviderChange">
            <el-option label="阿里云 OSS" value="ALIYUN" />
            <el-option label="腾讯云 COS" value="TENCENT" />
            <el-option label="七牛云" value="QINIU" />
            <el-option label="Cloudflare R2" value="CLOUDFLARE" />
            <el-option label="MinIO" value="MINIO" />
            <el-option label="AWS S3" value="AWS" />
          </el-select>
        </el-form-item>
        <el-form-item label="Endpoint" prop="endpoint">
          <el-input v-model="ossForm.endpoint" :placeholder="endpointPlaceholder" />
        </el-form-item>
        <el-form-item label="Region" prop="region">
          <el-input v-model="ossForm.region" :placeholder="regionPlaceholder" :disabled="ossForm.provider === 'CLOUDFLARE'" />
        </el-form-item>
        <el-form-item label="AccessKey" prop="accessKey">
          <el-input v-model="ossForm.accessKey" :placeholder="accessKeyPlaceholder" />
        </el-form-item>
        <el-form-item label="SecretKey" prop="secretKey">
          <el-input v-model="ossForm.secretKey" placeholder="AccessKey Secret" type="password" show-password />
        </el-form-item>
        <el-form-item label="Bucket名称" prop="bucketName">
          <el-input v-model="ossForm.bucketName" placeholder="存储桶名称" />
        </el-form-item>
        <el-form-item v-if="ossForm.provider !== 'CLOUDFLARE'" label="路径样式访问">
          <el-switch v-model="ossForm.pathStyleAccess" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px;">阿里云OSS/MinIO需开启</span>
        </el-form-item>
        <el-form-item label="公开访问域名">
          <el-input v-model="ossForm.publicDomain" :placeholder="publicDomainPlaceholder" />
        </el-form-item>
        <el-form-item label="存储路径前缀">
          <el-input v-model="ossForm.prefix" placeholder="如：app（留空则无前缀）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ossDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveOss" :loading="ossSaving">保存</el-button>
      </template>
    </el-dialog>

    <!-- APP发布弹窗 -->
    <el-dialog v-model="releaseDialogVisible" title="新增版本发布" width="600px" destroy-on-close @close="resetUploadState">
      <el-form :model="releaseForm" label-width="100px" :rules="releaseRules" ref="releaseFormRef">
        <el-form-item label="版本号" prop="versionName">
          <el-input v-model="releaseForm.versionName" placeholder="如：1.0.0" :disabled="uploadState === 'uploading'" @input="onVersionNameChange" />
        </el-form-item>
        <el-form-item label="版本编号">
          <el-input :model-value="releaseForm.versionCode" disabled style="width: 120px;" />
          <span style="margin-left: 12px; color: #909399; font-size: 13px;">根据版本号自动计算，无需手动输入</span>
        </el-form-item>
        <el-form-item label="平台" prop="platform">
          <el-select v-model="releaseForm.platform" style="width: 100%;" :disabled="uploadState === 'uploading'">
            <el-option label="Android" value="android" />
            <el-option label="iOS" value="ios" />
          </el-select>
        </el-form-item>
        <el-form-item label="更新说明" prop="releaseNotes">
          <el-input v-model="releaseForm.releaseNotes" type="textarea" :rows="3" placeholder="本次更新内容" :disabled="uploadState === 'uploading'" />
        </el-form-item>
        <el-form-item label="强制更新">
          <el-switch v-model="releaseForm.isForceUpdate" :disabled="uploadState === 'uploading'" />
        </el-form-item>

        <!-- 文件选择与上传 -->
        <el-form-item label="APP文件" required>
          <!-- 未选择文件：显示选择按钮 -->
          <div v-if="!selectedFile" style="width: 100%;">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="1"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              accept=".apk,.ipa"
              :disabled="uploadState === 'uploading'"
            >
              <el-button type="primary" :disabled="uploadState === 'uploading'">选择文件</el-button>
              <template #tip>
                <div class="el-upload__tip">支持 .apk / .ipa 文件</div>
              </template>
            </el-upload>
          </div>

          <!-- 已选择文件：显示文件信息 + 进度 -->
          <div v-else style="width: 100%;">
            <div class="upload-file-info">
              <div class="file-detail">
                <el-icon size="20" color="#409eff"><Document /></el-icon>
                <span class="file-name">{{ selectedFile.name }}</span>
                <span class="file-size">({{ formatFileSize(selectedFile.size) }})</span>
              </div>
              <el-button
                v-if="uploadState === 'idle' || uploadState === 'error'"
                type="danger"
                size="small"
                text
                @click="handleFileRemove"
              >移除</el-button>
            </div>

            <!-- 进度条 -->
            <el-progress
              v-if="uploadState === 'uploading'"
              :percentage="uploadProgress"
              :stroke-width="20"
              :text-inside="true"
              status="success"
              style="margin-top: 10px;"
            />

            <!-- 上传成功 -->
            <div v-if="uploadState === 'success'" class="upload-status success">
              <el-icon><CircleCheck /></el-icon>
              <span>文件上传成功</span>
            </div>

            <!-- 上传失败 -->
            <div v-if="uploadState === 'error'" class="upload-status error">
              <el-icon><CircleClose /></el-icon>
              <span>{{ uploadError }}</span>
              <el-button type="primary" size="small" text @click="doUploadFile">重试</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <template v-if="uploadState === 'idle' && selectedFile">
          <el-button @click="releaseDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="doUploadFile" :loading="false">上传文件</el-button>
        </template>
        <template v-else-if="uploadState === 'uploading'">
          <el-button @click="releaseDialogVisible = false" disabled>取消</el-button>
          <el-button type="primary" loading>上传中 {{ uploadProgress }}%</el-button>
        </template>
        <template v-else-if="uploadState === 'success'">
          <el-button @click="releaseDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveRelease" :loading="releaseSaving">创建发布</el-button>
        </template>
        <template v-else-if="uploadState === 'error'">
          <el-button @click="releaseDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="doUploadFile">重新上传</el-button>
        </template>
        <template v-else>
          <el-button @click="releaseDialogVisible = false">取消</el-button>
          <el-button type="primary" disabled>请先选择文件</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import {
  getOssConfigs, createOssConfig, updateOssConfig, deleteOssConfig,
  activateOssConfig, testOssConnection,
  getAppReleases, uploadAppFile, createAppRelease, publishAppRelease, unpublishAppRelease, deleteAppRelease
} from '@/api/admin'

const activeTab = ref('oss')

// 当前已发布版本的最大versionCode（创建新版本时必须大于此值，否则APP检测不到更新）
const currentAppVersionCode = computed(() => {
  const published = releases.value.filter(r => r.isPublished)
  if (published.length === 0) return 0
  return Math.max(...published.map(r => r.versionCode))
})

// ========== OSS配置 ==========
const ossConfigs = ref([])
const ossDialogVisible = ref(false)
const ossSaving = ref(false)
const ossFormRef = ref(null)
const ossForm = ref({})
const ossRules = {
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择OSS提供商', trigger: 'change' }],
  endpoint: [{ required: true, message: '请输入Endpoint', trigger: 'blur' }],
  accessKey: [{ required: true, message: '请输入AccessKey', trigger: 'blur' }],
  secretKey: [{ required: true, message: '请输入SecretKey', trigger: 'blur' }],
  bucketName: [{ required: true, message: '请输入Bucket名称', trigger: 'blur' }]
}

function providerLabel(provider) {
  const map = { ALIYUN: '阿里云', TENCENT: '腾讯云', QINIU: '七牛云', CLOUDFLARE: 'Cloudflare', MINIO: 'MinIO', AWS: 'AWS' }
  return map[provider] || provider
}

function providerTagType(provider) {
  const map = { ALIYUN: '', TENCENT: 'success', QINIU: 'warning', CLOUDFLARE: 'danger', MINIO: 'info', AWS: 'danger' }
  return map[provider] || 'info'
}

// 根据提供商动态显示表单提示
const endpointPlaceholder = computed(() => {
  const map = {
    ALIYUN: '如：https://oss-cn-hangzhou.aliyuncs.com',
    TENCENT: '如：https://cos.ap-guangzhou.myqcloud.com',
    QINIU: '如：https://s3-z0.qiniucs.com',
    CLOUDFLARE: '如：https://<accountid>.r2.cloudflarestorage.com',
    MINIO: '如：http://192.168.1.10:9000',
    AWS: '如：https://s3.us-east-1.amazonaws.com'
  }
  return map[ossForm.value.provider] || '请输入Endpoint'
})

const regionPlaceholder = computed(() => {
  const map = {
    ALIYUN: '如：cn-hangzhou',
    TENCENT: '如：ap-guangzhou',
    QINIU: '如：z0',
    CLOUDFLARE: 'auto（自动填充，实际签名使用us-east-1）',
    MINIO: '如：us-east-1',
    AWS: '如：us-east-1'
  }
  return map[ossForm.value.provider] || '如：us-east-1'
})

const accessKeyPlaceholder = computed(() => {
  return ossForm.value.provider === 'CLOUDFLARE' ? 'Access Key ID' : 'AccessKey ID'
})

const publicDomainPlaceholder = computed(() => {
  if (ossForm.value.provider === 'CLOUDFLARE') {
    return '如：https://files.yourdomain.com（R2自定义域名）'
  }
  return '如：https://cdn.example.com（留空则使用endpoint拼接）'
})

function handleProviderChange(provider) {
  const defaults = {
    ALIYUN: { pathStyleAccess: true, region: 'cn-hangzhou', endpoint: '' },
    TENCENT: { pathStyleAccess: false, region: 'ap-guangzhou', endpoint: '' },
    QINIU: { pathStyleAccess: true, region: 'z0', endpoint: '' },
    CLOUDFLARE: { pathStyleAccess: true, region: 'auto', endpoint: '' },
    MINIO: { pathStyleAccess: true, region: 'us-east-1', endpoint: '' },
    AWS: { pathStyleAccess: false, region: 'us-east-1', endpoint: '' }
  }
  const d = defaults[provider] || {}
  ossForm.value.pathStyleAccess = d.pathStyleAccess ?? true
  ossForm.value.region = d.region || 'us-east-1'
  // 切换提供商时清空 endpoint 让用户重新输入
  ossForm.value.endpoint = d.endpoint ?? ''
}

function showOssDialog(row) {
  if (row) {
    // 只提取 OSS 配置需要的字段，避免发送 id、isActive、createdAt 等多余字段
    ossForm.value = {
      id: row.id,
      provider: row.provider,
      name: row.name,
      endpoint: row.endpoint,
      region: row.region,
      accessKey: row.accessKey,
      secretKey: row.secretKey,
      bucketName: row.bucketName,
      pathStyleAccess: row.pathStyleAccess,
      publicDomain: row.publicDomain,
      prefix: row.prefix
    }
  } else {
    ossForm.value = {
      provider: 'ALIYUN', name: '', endpoint: '', region: 'cn-hangzhou',
      accessKey: '', secretKey: '', bucketName: '',
      pathStyleAccess: true, publicDomain: '', prefix: 'app'
    }
  }
  ossDialogVisible.value = true
}

async function handleSaveOss() {
  await ossFormRef.value.validate()
  ossSaving.value = true
  try {
    // 构建请求体，排除 id 等非请求字段
    const payload = { ...ossForm.value }
    delete payload.id
    if (ossForm.value.id) {
      await updateOssConfig(ossForm.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createOssConfig(payload)
      ElMessage.success('创建成功')
    }
    ossDialogVisible.value = false
    loadOssConfigs()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    ossSaving.value = false
  }
}

async function handleActivate(row) {
  try {
    await activateOssConfig(row.id)
    ElMessage.success('已启用该OSS配置')
    loadOssConfigs()
  } catch (e) {
    ElMessage.error(e.message || '启用失败')
  }
}

async function handleTestConnection(row) {
  try {
    await testOssConnection(row.id)
    ElMessage.success('连接测试成功')
  } catch (e) {
    ElMessage.error(e.message || '连接测试失败，请检查配置')
  }
}

async function handleDeleteOss(row) {
  try {
    await ElMessageBox.confirm('确定要删除该OSS配置吗？', '提示', { type: 'warning' })
    await deleteOssConfig(row.id)
    ElMessage.success('删除成功')
    loadOssConfigs()
  } catch { }
}

async function loadOssConfigs() {
  try {
    ossConfigs.value = await getOssConfigs()
  } catch (e) {
    console.error('加载OSS配置失败', e)
  }
}

// ========== APP发布 ==========
const releases = ref([])
const releasePlatform = ref('')
const releaseDialogVisible = ref(false)
const releaseSaving = ref(false)
const releaseFormRef = ref(null)
const uploadRef = ref(null)
const releaseForm = ref({})
const selectedFile = ref(null)
const releaseRules = {
  versionName: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  platform: [{ required: true, message: '请选择平台', trigger: 'change' }]
}

// 上传状态: idle / uploading / success / error
const uploadState = ref('idle')
const uploadProgress = ref(0)
const uploadError = ref('')
const uploadedFileInfo = ref(null) // 文件上传成功后返回的信息

/** 根据版本号自动计算 versionCode，如 1.0.1 → 101, 1.2.10 → 1210 */
function onVersionNameChange(val) {
  const parts = (val || '').trim().split('.').filter(p => /^\d+$/.test(p))
  if (parts.length >= 2) {
    releaseForm.value.versionCode = Number(parts.join(''))
  } else {
    releaseForm.value.versionCode = null
  }
}

function showReleaseDialog() {
  releaseForm.value = {
    versionName: '', versionCode: null, platform: 'android',
    releaseNotes: '', isForceUpdate: false
  }
  selectedFile.value = null
  uploadState.value = 'idle'
  uploadProgress.value = 0
  uploadError.value = ''
  uploadedFileInfo.value = null
  releaseDialogVisible.value = true
}

function resetUploadState() {
  selectedFile.value = null
  uploadState.value = 'idle'
  uploadProgress.value = 0
  uploadError.value = ''
  uploadedFileInfo.value = null
}

function handleFileChange(file) {
  selectedFile.value = file.raw
  uploadState.value = 'idle'
  uploadProgress.value = 0
  uploadError.value = ''
  uploadedFileInfo.value = null
}

function handleFileRemove() {
  selectedFile.value = null
  uploadState.value = 'idle'
  uploadProgress.value = 0
  uploadError.value = ''
  uploadedFileInfo.value = null
}

async function doUploadFile() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploadState.value = 'uploading'
  uploadProgress.value = 0
  uploadError.value = ''

  try {
    const result = await uploadAppFile(selectedFile.value, releaseForm.value.platform, (e) => {
      uploadProgress.value = Math.round((e.loaded / e.total) * 100)
    })
    uploadedFileInfo.value = result
    uploadState.value = 'success'
    ElMessage.success('文件上传成功')
  } catch (e) {
    uploadState.value = 'error'
    uploadError.value = e.message || '上传失败'
    ElMessage.error(uploadError.value)
  }
}

async function handleSaveRelease() {
  await releaseFormRef.value.validate()

  if (!uploadedFileInfo.value) {
    ElMessage.warning('请先上传APP文件')
    return
  }

  releaseSaving.value = true
  try {
    const data = {
      versionName: releaseForm.value.versionName,
      versionCode: releaseForm.value.versionCode,
      platform: releaseForm.value.platform,
      releaseNotes: releaseForm.value.releaseNotes,
      isForceUpdate: releaseForm.value.isForceUpdate,
      downloadUrl: uploadedFileInfo.value.downloadUrl,
      fileKey: uploadedFileInfo.value.fileKey,
      fileSize: uploadedFileInfo.value.fileSize,
      fileName: uploadedFileInfo.value.fileName
    }
    await createAppRelease(data)
    ElMessage.success('版本发布创建成功')
    releaseDialogVisible.value = false
    loadReleases()
  } catch (e) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    releaseSaving.value = false
  }
}

async function handlePublish(row) {
  try {
    await publishAppRelease(row.id)
    ElMessage.success('发布成功')
    loadReleases()
  } catch (e) {
    ElMessage.error(e.message || '发布失败')
  }
}

async function handleUnpublish(row) {
  try {
    await unpublishAppRelease(row.id)
    ElMessage.success('已取消发布')
    loadReleases()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleDeleteRelease(row) {
  try {
    await ElMessageBox.confirm('确定要删除该版本吗？OSS上的文件也会被删除', '提示', { type: 'warning' })
    await deleteAppRelease(row.id)
    ElMessage.success('删除成功')
    loadReleases()
  } catch { }
}

async function loadReleases() {
  try {
    releases.value = await getAppReleases(releasePlatform.value || undefined)
  } catch (e) {
    console.error('加载发布列表失败', e)
  }
}

function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

onMounted(() => {
  loadOssConfigs()
  loadReleases()
})
</script>

<style scoped>
.admin-page {
  padding: 0;
}

.tab-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.upload-file-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  width: 100%;
}

.file-detail {
  display: flex;
  align-items: center;
  gap: 6px;
}

.file-name {
  font-weight: 500;
  color: #303133;
}

.file-size {
  color: #909399;
  font-size: 12px;
}

.upload-status {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 14px;
}

.upload-status.success {
  color: #67c23a;
}

.upload-status.error {
  color: #f56c6c;
}
</style>
