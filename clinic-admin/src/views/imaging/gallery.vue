<template>
  <div class="page-container">
    <!-- 顶部筛选区 -->
    <el-card class="filter-card">
      <div class="filter-bar">
        <div class="filter-left">
          <span class="filter-label">患者选择：</span>
          <el-select
            v-model="selectedPatientId"
            placeholder="请选择患者"
            filterable
            clearable
            style="width: 220px"
            @change="onPatientChange"
          >
            <el-option
              v-for="p in patientOptions"
              :key="p.id"
              :label="p.patientName + (p.phone ? ' - ' + p.phone : '')"
              :value="p.id"
            />
          </el-select>
          <el-divider direction="vertical" />
          <span class="filter-label">影像类型：</span>
          <el-radio-group v-model="filterImageType" @change="loadImages">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="PANORAMIC">全景片</el-radio-button>
            <el-radio-button label="INTRAORAL">口内照</el-radio-button>
            <el-radio-button label="CBCT">CBCT</el-radio-button>
            <el-radio-button label="CEPHALOMETRIC">头颅侧位</el-radio-button>
            <el-radio-button label="PERIAPICAL">根尖片</el-radio-button>
            <el-radio-button label="PHOTO">面像照</el-radio-button>
          </el-radio-group>
        </div>
        <div class="filter-right">
          <el-button type="primary" @click="uploadVisible = true" :disabled="!selectedPatientId">
            <el-icon><Upload /></el-icon>上传影像
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 影像列表 -->
    <el-card class="gallery-card" v-if="selectedPatientId">
      <div v-if="images.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无影像，请上传" />
      </div>
      <div v-else class="image-grid">
        <div
          v-for="img in images"
          :key="img.id"
          class="image-card"
          @click="viewImage(img)"
        >
          <div class="image-thumb">
            <el-image
              :src="img.thumbnailUrl || img.imageUrl"
              fit="cover"
              class="thumb-img"
              lazy
            >
              <template #error>
                <div class="image-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <div class="image-type-badge">{{ imageTypeLabel(img.imageType) }}</div>
          </div>
          <div class="image-info">
            <div class="image-name" :title="img.fileName">{{ img.fileName || '未知' }}</div>
            <div class="image-meta">
              <span v-if="img.shotDate">{{ img.shotDate }}</span>
              <span v-if="img.description">{{ img.description }}</span>
            </div>
          </div>
          <div class="image-actions" @click.stop>
            <el-button size="small" text @click="viewImage(img)">查看</el-button>
            <el-button size="small" text @click="openLinkTeeth(img)">关联牙位</el-button>
            <el-popconfirm title="确定删除此影像？" @confirm="handleDelete(img.id!)">
              <template #reference>
                <el-button size="small" text type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>
      <div class="pagination-wrap" v-if="pagination.total > 0">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @change="loadImages"
        />
      </div>
    </el-card>

    <!-- 未选择患者时的提示 -->
    <el-card class="gallery-card" v-else>
      <el-empty description="请先选择患者" />
    </el-card>

    <!-- 上传影像对话框 -->
    <el-dialog v-model="uploadVisible" title="上传影像" width="550px" destroy-on-close>
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="上传文件" required>
          <el-upload
            ref="uploadRef"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="onUploadSuccess"
            :on-error="onUploadError"
            :before-upload="beforeUpload"
            :limit="1"
            list-type="picture"
            accept="image/*"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="upload-tip">支持 JPG/PNG 格式，大小不超过 10MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="影像类型">
          <el-select v-model="uploadForm.imageType" placeholder="选择类型" style="width: 100%">
            <el-option label="全景片" value="PANORAMIC" />
            <el-option label="口内照" value="INTRAORAL" />
            <el-option label="CBCT" value="CBCT" />
            <el-option label="头颅侧位片" value="CEPHALOMETRIC" />
            <el-option label="根尖片" value="PERIAPICAL" />
            <el-option label="咬翼片" value="BITEWING" />
            <el-option label="面像照" value="PHOTO" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="拍摄日期">
          <el-date-picker
            v-model="uploadForm.shotDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部位">
          <el-select v-model="uploadForm.bodyPart" placeholder="选择部位" style="width: 100%" clearable>
            <el-option label="上颌" value="UPPER" />
            <el-option label="下颌" value="LOWER" />
            <el-option label="全口" value="FULL" />
            <el-option label="左侧" value="LEFT" />
            <el-option label="右侧" value="RIGHT" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="uploadForm.description" type="textarea" rows="2" placeholder="影像描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmUpload" :disabled="!uploadedUrl">确定保存</el-button>
      </template>
    </el-dialog>

    <!-- 影像查看器 -->
    <ImageViewer v-model:visible="viewerVisible" :url="viewerUrl" />

    <!-- 关联牙位对话框 -->
    <el-dialog v-model="linkTeethVisible" title="关联牙位" width="700px" destroy-on-close>
      <ToothChart v-model="linkToothRecords" />
      <template #footer>
        <el-button @click="linkTeethVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmLinkTeeth">确定关联</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Picture } from '@element-plus/icons-vue'
import { getPatientList, type Patient } from '@/api/patient'
import {
  getDentalImageList,
  createDentalImage,
  deleteDentalImage,
  linkTeethToImage,
  type DentalImage
} from '@/api/dentalImage'
import { getToothChart, type ToothChartRecord } from '@/api/toothChart'
import { getClinicId, getToken } from '@/utils/storage'
import ImageViewer from '@/components/ImageViewer.vue'
import ToothChart from '@/components/ToothChart.vue'

const loading = ref(false)
const selectedPatientId = ref<number | null>(null)
const patientOptions = ref<Patient[]>([])
const filterImageType = ref('')
const images = ref<DentalImage[]>([])

const pagination = reactive({ current: 1, size: 12, total: 0 })

const uploadVisible = ref(false)
const uploadedUrl = ref('')
const uploadedFileName = ref('')
const uploadRef = ref()

const uploadUrl = '/api/upload/image'
const uploadHeaders = { Authorization: `Bearer ${getToken()}` }

const uploadForm = reactive({
  imageType: 'PANORAMIC',
  shotDate: '',
  bodyPart: '',
  description: ''
})

const viewerVisible = ref(false)
const viewerUrl = ref('')

const linkTeethVisible = ref(false)
const linkToothRecords = ref<ToothChartRecord[]>([])
const linkingImageId = ref<number | null>(null)

function imageTypeLabel(type: string): string {
  const map: Record<string, string> = {
    PANORAMIC: '全景片', INTRAORAL: '口内照', CBCT: 'CBCT',
    CEPHALOMETRIC: '头颅侧位', PERIAPICAL: '根尖片', BITEWING: '咬翼片',
    PHOTO: '面像照', OTHER: '其他'
  }
  return map[type] || type
}

async function loadPatients() {
  try {
    const res = await getPatientList({ clinicId: getClinicId(), pageNum: 1, pageSize: 200 })
    patientOptions.value = res.data.list || []
  } catch (e) {
    console.error('加载患者失败:', e)
  }
}

function onPatientChange() {
  pagination.current = 1
  loadImages()
}

async function loadImages() {
  if (!selectedPatientId.value) return
  loading.value = true
  try {
    const res = await getDentalImageList({
      clinicId: getClinicId(),
      patientId: selectedPatientId.value,
      imageType: filterImageType.value || undefined,
      pageNum: pagination.current,
      pageSize: pagination.size
    })
    images.value = res.data.list || []
    pagination.total = res.data.total || 0
  } catch (e) {
    console.error('加载影像失败:', e)
  } finally {
    loading.value = false
  }
}

function viewImage(img: DentalImage) {
  viewerUrl.value = img.imageUrl
  viewerVisible.value = true
}

async function handleDelete(id: number) {
  try {
    await deleteDentalImage(id, getClinicId())
    ElMessage.success('删除成功')
    loadImages()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

function beforeUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  return true
}

function onUploadSuccess(res: any) {
  if (res.code === 200 && res.data) {
    // UploadController 返回 {url, filename, path} 对象
    uploadedUrl.value = res.data.url || res.data
    uploadedFileName.value = res.data.filename || ''
    ElMessage.success('上传成功')
  } else {
    ElMessage.error('上传失败')
  }
}

function onUploadError() {
  ElMessage.error('上传失败，请重试')
}

async function confirmUpload() {
  if (!uploadedUrl.value || !selectedPatientId.value) return
  try {
    await createDentalImage({
      clinicId: getClinicId(),
      patientId: selectedPatientId.value,
      imageType: uploadForm.imageType,
      imageUrl: uploadedUrl.value,
      fileName: uploadedFileName.value || undefined,
      shotDate: uploadForm.shotDate || undefined,
      bodyPart: uploadForm.bodyPart || undefined,
      description: uploadForm.description || undefined
    })
    ElMessage.success('影像保存成功')
    uploadVisible.value = false
    uploadedUrl.value = ''
    uploadedFileName.value = ''
    uploadForm.imageType = 'PANORAMIC'
    uploadForm.shotDate = ''
    uploadForm.bodyPart = ''
    uploadForm.description = ''
    loadImages()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

function openLinkTeeth(img: DentalImage) {
  linkingImageId.value = img.id!
  linkToothRecords.value = img.linkedTeeth?.map(t => ({
    toothNumber: t,
    conditionType: '',
    medicalRecordId: 0
  })) || []
  linkTeethVisible.value = true
}

async function confirmLinkTeeth() {
  if (!linkingImageId.value) return
  try {
    const teethNumbers = linkToothRecords.value.map(r => r.toothNumber)
    await linkTeethToImage({ imageId: linkingImageId.value, toothNumbers: teethNumbers })
    ElMessage.success('关联成功')
    linkTeethVisible.value = false
    loadImages()
  } catch (e) {
    ElMessage.error('关联失败')
  }
}

onMounted(() => {
  loadPatients()
})
</script>

<style lang="scss" scoped>
.page-container {
  padding: 0;
}
.filter-card {
  margin-bottom: 16px;
}
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.filter-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.filter-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}
.gallery-card {
  min-height: 300px;
}
.empty-state {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.image-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s;
  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  }
}
.image-thumb {
  height: 160px;
  position: relative;
  overflow: hidden;
  background: #f5f7fa;
  .thumb-img {
    width: 100%;
    height: 100%;
  }
  .image-type-badge {
    position: absolute;
    top: 8px;
    right: 8px;
    background: rgba(64, 158, 255, 0.85);
    color: #fff;
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 4px;
  }
}
.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  color: #c0c4cc;
}
.image-info {
  padding: 8px 12px;
  .image-name {
    font-size: 13px;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .image-meta {
    font-size: 11px;
    color: #909399;
    margin-top: 4px;
    display: flex;
    gap: 8px;
  }
}
.image-actions {
  padding: 4px 12px 8px;
  display: flex;
  gap: 4px;
  border-top: 1px solid #f2f3f5;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
