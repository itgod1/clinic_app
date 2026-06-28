<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="compare-header">
          <span>影像对比</span>
          <el-button size="small" type="primary" @click="openImagePicker('left')">选择左侧影像</el-button>
          <el-button size="small" type="success" @click="openImagePicker('right')">选择右侧影像</el-button>
          <el-checkbox v-model="syncZoom" style="margin-left: 12px">同步缩放</el-checkbox>
        </div>
      </template>
      <div class="compare-container" v-if="leftImage || rightImage">
        <div class="compare-pane">
          <div class="pane-label">治疗前</div>
          <div v-if="leftImage" class="pane-image-wrap">
            <img :src="leftImage.imageUrl" class="compare-image" />
          </div>
          <el-empty v-else description="未选择" />
        </div>
        <div class="compare-divider"></div>
        <div class="compare-pane">
          <div class="pane-label">治疗后</div>
          <div v-if="rightImage" class="pane-image-wrap">
            <img :src="rightImage.imageUrl" class="compare-image" />
          </div>
          <el-empty v-else description="未选择" />
        </div>
      </div>
      <el-empty v-else description="请选择左右两侧影像进行对比" />
    </el-card>

    <!-- 选择影像对话框 -->
    <el-dialog v-model="pickerVisible" title="选择影像" width="800px" destroy-on-close>
      <el-form inline>
        <el-form-item label="患者">
          <el-select v-model="pickerPatientId" placeholder="选择患者" filterable style="width: 200px" @change="loadPickerImages">
            <el-option v-for="p in patientOptions" :key="p.id" :label="p.patientName" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="picker-grid" v-if="pickerImages.length > 0">
        <div
          v-for="img in pickerImages"
          :key="img.id"
          class="picker-card"
          :class="{ selected: pickerSelected?.id === img.id }"
          @click="pickerSelected = img"
        >
          <el-image :src="img.thumbnailUrl || img.imageUrl" fit="cover" class="picker-thumb" />
          <div class="picker-name">{{ img.fileName || img.imageUrl }}</div>
          <div class="picker-type">{{ imageTypeLabel(img.imageType) }}</div>
        </div>
      </div>
      <el-empty v-else description="请先选择患者" />
      <template #footer>
        <el-button @click="pickerVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPicker">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPatientList, type Patient } from '@/api/patient'
import { getDentalImageList, type DentalImage } from '@/api/dentalImage'
import { getClinicId } from '@/utils/storage'

const leftImage = ref<DentalImage | null>(null)
const rightImage = ref<DentalImage | null>(null)
const syncZoom = ref(false)

const pickerVisible = ref(false)
const pickerSide = ref<'left' | 'right'>('left')
const pickerPatientId = ref<number | null>(null)
const pickerImages = ref<DentalImage[]>([])
const pickerSelected = ref<DentalImage | null>(null)
const patientOptions = ref<Patient[]>([])

function imageTypeLabel(type: string): string {
  const map: Record<string, string> = {
    PANORAMIC: '全景片', INTRAORAL: '口内照', CBCT: 'CBCT',
    CEPHALOMETRIC: '头颅侧位', PERIAPICAL: '根尖片', PHOTO: '面像照'
  }
  return map[type] || type
}

function openImagePicker(side: 'left' | 'right') {
  pickerSide.value = side
  pickerSelected.value = null
  pickerVisible.value = true
}

async function loadPickerImages() {
  if (!pickerPatientId.value) return
  const res = await getDentalImageList({
    clinicId: getClinicId(),
    patientId: pickerPatientId.value,
    pageNum: 1,
    pageSize: 50
  })
  pickerImages.value = res.data.list || []
}

function confirmPicker() {
  if (!pickerSelected.value) return
  if (pickerSide.value === 'left') {
    leftImage.value = pickerSelected.value
  } else {
    rightImage.value = pickerSelected.value
  }
  pickerVisible.value = false
}

onMounted(async () => {
  const res = await getPatientList({ clinicId: getClinicId(), pageNum: 1, pageSize: 200 })
  patientOptions.value = res.data.list || []
})
</script>

<style lang="scss" scoped>
.page-container { padding: 0; }
.compare-header {
  display: flex;
  align-items: center;
  gap: 12px;
  span { font-weight: bold; }
}
.compare-container {
  display: flex;
  min-height: 400px;
}
.compare-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.pane-label {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 12px;
}
.pane-image-wrap {
  width: 100%;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
  border-radius: 4px;
}
.compare-image {
  max-width: 100%;
  max-height: 500px;
  object-fit: contain;
}
.compare-divider {
  width: 2px;
  background: #dcdfe6;
  margin: 0 16px;
}
.picker-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-top: 12px;
}
.picker-card {
  border: 2px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  &.selected { border-color: #409eff; }
}
.picker-thumb { width: 100%; height: 120px; }
.picker-name { font-size: 12px; padding: 4px 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.picker-type { font-size: 11px; color: #909399; padding: 0 8px 4px; }
</style>
