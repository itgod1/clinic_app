<template>
  <div class="tooth-chart-wrapper">
    <div class="chart-toolbar" v-if="!readOnly">
      <el-button size="small" @click="clearAll" type="warning" plain>清除全部</el-button>
      <span class="hint">点击牙齿标记状况</span>
    </div>
    <svg :viewBox="'0 0 800 400'" class="tooth-chart-svg">
      <!-- 上半部分(上颌) -->
      <text x="400" y="18" text-anchor="middle" class="arch-label">上 颌</text>
      <!-- 下半部分(下颌) -->
      <text x="400" y="398" text-anchor="middle" class="arch-label">下 颌</text>
      <!-- 中线 -->
      <line x1="400" y1="40" x2="400" y2="360" stroke="#ccc" stroke-dasharray="4,4" stroke-width="1" />

      <g v-for="tooth in teethData" :key="tooth.number">
        <!-- 牙根线 -->
        <line
          v-if="tooth.arch === 'upper'"
          :x1="tooth.cx" :y1="tooth.rootY1" :x2="tooth.cxRoot" :y2="tooth.rootY2"
          :stroke="getToothColor(tooth.number)" stroke-width="1.5" opacity="0.5"
        />
        <line
          v-if="tooth.arch === 'lower'"
          :x1="tooth.cx" :y1="tooth.rootY1" :x2="tooth.cxRoot" :y2="tooth.rootY2"
          :stroke="getToothColor(tooth.number)" stroke-width="1.5" opacity="0.5"
        />
        <!-- 牙冠 -->
        <path
          :d="tooth.path"
          :fill="getToothColor(tooth.number)"
          :stroke="isSelected(tooth.number) ? '#409eff' : '#999'"
          :stroke-width="isSelected(tooth.number) ? 2.5 : 1"
          :opacity="getToothColor(tooth.number) === '#ffffff' ? 0.9 : 1"
          :style="{ cursor: readOnly ? 'default' : 'pointer' }"
          @click="onToothClick(tooth.number)"
        />
        <!-- 缺失标记 X -->
        <g v-if="getCondition(tooth.number) === 'MISSING'" :transform="'translate(' + (tooth.cx - 6) + ',' + (tooth.cy - 6) + ')'">
          <line x1="0" y1="0" x2="12" y2="12" stroke="#e74c3c" stroke-width="2" />
          <line x1="12" y1="0" x2="0" y2="12" stroke="#e74c3c" stroke-width="2" />
        </g>
        <!-- 牙位号 -->
        <text
          :x="tooth.textX" :y="tooth.textY"
          text-anchor="middle"
          class="tooth-number"
          :fill="getToothColor(tooth.number) === '#ffffff' ? '#333' : '#fff'"
        >{{ tooth.number }}</text>
      </g>

      <!-- 图例 -->
      <g transform="translate(620, 50)">
        <text x="0" y="0" class="legend-title">图例</text>
        <template v-for="(item, idx) in legendItems" :key="item.type">
          <rect :x="0" :y="14 + idx * 20" width="14" height="14" :fill="item.color" stroke="#999" stroke-width="1" rx="2" />
          <text :x="20" :y="25 + idx * 20" class="legend-text">{{ item.label }}</text>
        </template>
      </g>
    </svg>

    <!-- 状况编辑弹出框 -->
    <el-dialog
      v-model="popoverVisible"
      :title="'牙位 ' + selectedTooth"
      width="420px"
      destroy-on-close
    >
      <el-form label-width="80px">
        <el-form-item label="牙齿状况">
          <el-radio-group v-model="editCondition" class="condition-radio-group">
            <el-radio-button
              v-for="c in conditionOptions"
              :key="c.value"
              :value="c.value"
            >
              {{ c.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="牙面" v-if="showSurface">
          <el-checkbox-group v-model="editSurfaces">
            <el-checkbox label="O">咬合面</el-checkbox>
            <el-checkbox label="B">颊面</el-checkbox>
            <el-checkbox label="L">舌面</el-checkbox>
            <el-checkbox label="M">近中面</el-checkbox>
            <el-checkbox label="D">远中面</el-checkbox>
            <el-checkbox label="P">腭面</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editNote" placeholder="牙位备注" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="removeCondition" type="danger" plain>清除此牙</el-button>
        <el-button @click="popoverVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCondition">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { ToothChartRecord } from '@/api/toothChart'

interface ToothDef {
  number: string
  arch: 'upper' | 'lower'
  path: string
  cx: number
  cy: number
  rootY1: number
  rootY2: number
  cxRoot: number
  textX: number
  textY: number
}

interface ConditionOption {
  value: string
  label: string
}

const props = withDefaults(defineProps<{
  modelValue: ToothChartRecord[]
  readOnly?: boolean
}>(), {
  readOnly: false
})

const emit = defineEmits<{
  'update:modelValue': [records: ToothChartRecord[]]
}>()

const localRecords = ref<ToothChartRecord[]>([...props.modelValue])

watch(() => props.modelValue, (val) => {
  localRecords.value = [...val]
}, { deep: true })

// 状况类型选项
const conditionOptions: ConditionOption[] = [
  { value: '', label: '正常' },
  { value: 'CARIES', label: '龋齿' },
  { value: 'MISSING', label: '缺失' },
  { value: 'FILLED', label: '充填' },
  { value: 'CROWN', label: '牙冠' },
  { value: 'BRIDGE', label: '桥体' },
  { value: 'IMPLANT', label: '种植' },
  { value: 'ROOT_CANAL', label: '根管治疗' },
  { value: 'ABUTMENT', label: '基牙' },
  { value: 'PONTIC', label: '桥体牙' },
  { value: 'FRACTURE', label: '牙折' },
  { value: 'PERIODONTAL', label: '牙周病' },
  { value: 'SEALANT', label: '窝沟封闭' },
]

// 图例
const legendItems = [
  { type: 'CARIES', color: '#e74c3c', label: '龋齿' },
  { type: 'MISSING', color: '#bdc3c7', label: '缺失' },
  { type: 'FILLED', color: '#3498db', label: '充填' },
  { type: 'CROWN', color: '#f39c12', label: '牙冠' },
  { type: 'IMPLANT', color: '#27ae60', label: '种植' },
  { type: 'ROOT_CANAL', color: '#9b59b6', label: '根管' },
]

const conditionColors: Record<string, string> = {
  '': '#ffffff',
  CARIES: '#e74c3c',
  MISSING: '#bdc3c7',
  FILLED: '#3498db',
  CROWN: '#f39c12',
  BRIDGE: '#f39c12',
  IMPLANT: '#27ae60',
  ROOT_CANAL: '#9b59b6',
  ABUTMENT: '#e67e22',
  PONTIC: '#f39c12',
  FRACTURE: '#e74c3c',
  PERIODONTAL: '#e91e63',
  SEALANT: '#00bcd4',
}

// 需显示牙面的状况
const surfaceConditions = ['CARIES', 'FILLED']

// 生成全部32颗牙的SVG坐标
function generateTeethData(): ToothDef[] {
  const teeth: ToothDef[] = []
  // 上颌牙: 18-11 (右半), 21-28 (左半)
  const upperRight = ['18','17','16','15','14','13','12','11']
  const upperLeft  = ['21','22','23','24','25','26','27','28']
  // 下颌牙: 48-41 (右半), 31-38 (左半)
  const lowerRight = ['48','47','46','45','44','43','42','41']
  const lowerLeft  = ['31','32','33','34','35','36','37','38']

  // 上颌牙齿中心线 y=90, 牙根向下到 y=130
  const upperY = 90, upperRootEnd = 135
  // 下颌牙齿中心线 y=270, 牙根向上到 y=230
  const lowerY = 270, lowerRootEnd = 230

  const allUpper = [...upperRight, ...upperLeft]
  const allLower = [...lowerRight, ...lowerLeft]

  // 上颌牙齿宽度: 磨牙更宽
  function getUpperWidth(num: string): number {
    const n = parseInt(num)
    if (n === 18 || n === 17 || n === 27 || n === 28) return 38
    if (n === 16 || n === 15 || n === 25 || n === 26) return 36
    if (n === 14 || n === 24) return 30
    if (n === 13 || n === 23) return 24
    if (n === 12 || n === 22) return 22
    return 20 // incisors 11/21
  }
  function getLowerWidth(num: string): number {
    const n = parseInt(num)
    if (n === 48 || n === 47 || n === 37 || n === 38) return 36
    if (n === 46 || n === 45 || n === 35 || n === 36) return 34
    if (n === 44 || n === 34) return 28
    if (n === 43 || n === 33) return 22
    if (n === 42 || n === 32) return 20
    return 18 // incisors 31/41
  }

  function getUpperRootAngle(num: string): number {
    const n = parseInt(num)
    if (n === 18 || n === 17 || n === 28 || n === 27) return 0.15
    if (n === 16 || n === 15 || n === 26 || n === 25) return 0.08
    return 0
  }
  function getLowerRootAngle(num: string): number {
    const n = parseInt(num)
    if (n === 48 || n === 47 || n === 38 || n === 37) return -0.15
    if (n === 46 || n === 45 || n === 36 || n === 35) return -0.08
    return 0
  }

  // 计算上颌牙齿位置: 从左到右排列 (18在最左, 28在最右)
  let upperX = 48
  for (const num of allUpper) {
    const w = getUpperWidth(num)
    const h = 24
    const x = upperX
    const y = upperY - h / 2
    const cx = x + w / 2
    const cy = upperY
    // 圆角矩形路径
    const rx = 4, ry = 4
    const path = `M${x + rx},${y} L${x + w - rx},${y} Q${x + w},${y} ${x + w},${y + ry} L${x + w},${y + h - ry} Q${x + w},${y + h} ${x + w - rx},${y + h} L${x + rx},${y + h} Q${x},${y + h} ${x},${y + h - ry} L${x},${y + ry} Q${x},${y} ${x + rx},${y} Z`
    const rootAngle = getUpperRootAngle(num)
    teeth.push({
      number: num, arch: 'upper', path,
      cx, cy,
      rootY1: y + h,
      rootY2: upperRootEnd,
      cxRoot: cx + rootAngle * 30,
      textX: cx,
      textY: upperY + 30 + upperRootEnd - (y + h) > 15 ? upperRootEnd - 4 : y + h + 14
    })
    upperX += w + 2
  }

  // 计算下颌牙齿位置: 从左到右排列
  let lowerX = 48
  for (const num of allLower) {
    const w = getLowerWidth(num)
    const h = 24
    const x = lowerX
    const y = lowerY - h / 2
    const cx = x + w / 2
    const cy = lowerY
    const rx = 4, ry = 4
    const path = `M${x + rx},${y} L${x + w - rx},${y} Q${x + w},${y} ${x + w},${y + ry} L${x + w},${y + h - ry} Q${x + w},${y + h} ${x + w - rx},${y + h} L${x + rx},${y + h} Q${x},${y + h} ${x},${y + h - ry} L${x},${y + ry} Q${x},${y} ${x + rx},${y} Z`
    const rootAngle = getLowerRootAngle(num)
    teeth.push({
      number: num, arch: 'lower', path,
      cx, cy,
      rootY1: y,
      rootY2: lowerRootEnd,
      cxRoot: cx + rootAngle * 30,
      textX: cx,
      textY: y - (y - lowerRootEnd) > 12 ? lowerRootEnd + 10 : y - 4
    })
    lowerX += w + 2
  }

  return teeth
}

const teethData: ToothDef[] = generateTeethData()

// 根据牙位号查找牙齿记录
function getRecord(toothNumber: string): ToothChartRecord | undefined {
  return localRecords.value.find(r => r.toothNumber === toothNumber)
}

function getCondition(toothNumber: string): string {
  return getRecord(toothNumber)?.conditionType || ''
}

function getToothColor(toothNumber: string): string {
  return conditionColors[getCondition(toothNumber)] || '#ffffff'
}

function isSelected(toothNumber: string): boolean {
  return selectedTooth.value === toothNumber
}

// 交互状态
const selectedTooth = ref('')
const popoverVisible = ref(false)
const editCondition = ref('')
const editSurfaces = ref<string[]>([])
const editNote = ref('')

const showSurface = computed(() => surfaceConditions.includes(editCondition.value))

function onToothClick(toothNumber: string) {
  if (props.readOnly) return
  selectedTooth.value = toothNumber
  const rec = getRecord(toothNumber)
  editCondition.value = rec?.conditionType || ''
  editSurfaces.value = rec?.surface ? rec.surface.split(',') : []
  editNote.value = rec?.note || ''
  popoverVisible.value = true
}

function confirmCondition() {
  if (!selectedTooth.value) return

  // 从本地记录中移除旧记录
  localRecords.value = localRecords.value.filter(r => r.toothNumber !== selectedTooth.value)

  // 如果有状况，添加新记录
  if (editCondition.value) {
    localRecords.value.push({
      toothNumber: selectedTooth.value,
      conditionType: editCondition.value,
      surface: editSurfaces.value.length > 0 ? editSurfaces.value.join(',') : undefined,
      note: editNote.value || undefined,
      medicalRecordId: 0
    })
  }

  emit('update:modelValue', [...localRecords.value])
  popoverVisible.value = false
}

function removeCondition() {
  localRecords.value = localRecords.value.filter(r => r.toothNumber !== selectedTooth.value)
  emit('update:modelValue', [...localRecords.value])
  popoverVisible.value = false
}

function clearAll() {
  localRecords.value = []
  emit('update:modelValue', [])
}
</script>

<style lang="scss" scoped>
.tooth-chart-wrapper {
  width: 100%;
  .chart-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;
    .hint {
      font-size: 12px;
      color: #909399;
    }
  }
  .tooth-chart-svg {
    width: 100%;
    max-width: 800px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    .arch-label {
      font-size: 11px;
      fill: #909399;
    }
    .tooth-number {
      font-size: 8px;
      font-weight: bold;
    }
    .legend-title {
      font-size: 11px;
      font-weight: bold;
      fill: #303133;
    }
    .legend-text {
      font-size: 10px;
      fill: #606266;
    }
  }
}

.condition-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  :deep(.el-radio-button) {
    margin-bottom: 4px;
  }
}
</style>
