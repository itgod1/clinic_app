<template>
  <teleport to="body">
    <div class="image-viewer-overlay" v-if="visible" @click.self="close">
      <div class="image-viewer-toolbar">
        <el-button-group>
          <el-button size="small" @click="zoomIn" :disabled="scale >= 5">放大</el-button>
          <el-button size="small" @click="zoomOut" :disabled="scale <= 0.2">缩小</el-button>
          <el-button size="small" @click="reset">重置</el-button>
          <el-button size="small" @click="rotateLeft">左旋</el-button>
          <el-button size="small" @click="rotateRight">右旋</el-button>
          <el-button size="small" @click="flipH">水平翻转</el-button>
          <el-button size="small" @click="flipV">垂直翻转</el-button>
        </el-button-group>
        <span class="scale-info">{{ Math.round(scale * 100) }}%</span>
        <el-button size="small" @click="close" type="danger" plain>关闭</el-button>
      </div>
      <div
        class="image-viewer-canvas"
        ref="canvasRef"
        @wheel.prevent="onWheel"
        @mousedown="onMouseDown"
        @mousemove="onMouseMove"
        @mouseup="onMouseUp"
        @mouseleave="onMouseUp"
      >
        <img
          ref="imgRef"
          :src="url"
          :style="imgStyle"
          class="viewer-image"
          draggable="false"
          @load="onLoad"
        />
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{
  visible: boolean
  url: string
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
}>()

const canvasRef = ref<HTMLElement>()
const imgRef = ref<HTMLImageElement>()

const scale = ref(1)
const rotate = ref(0)
const flipHValue = ref(1)
const flipVValue = ref(1)
const posX = ref(0)
const posY = ref(0)

const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)
const posStartX = ref(0)
const posStartY = ref(0)

const imgStyle = computed(() => ({
  transform: `translate(${posX.value}px, ${posY.value}px) scale(${scale.value}) rotate(${rotate.value}deg) scaleX(${flipHValue.value}) scaleY(${flipVValue.value})`,
  transition: 'none'
}))

function close() {
  emit('update:visible', false)
}

function reset() {
  scale.value = 1
  rotate.value = 0
  flipHValue.value = 1
  flipVValue.value = 1
  posX.value = 0
  posY.value = 0
}

function zoomIn() {
  scale.value = Math.min(5, scale.value + 0.25)
}

function zoomOut() {
  scale.value = Math.max(0.2, scale.value - 0.25)
}

function rotateLeft() {
  rotate.value -= 90
}

function rotateRight() {
  rotate.value += 90
}

function flipH() {
  flipHValue.value *= -1
}

function flipV() {
  flipVValue.value *= -1
}

function onWheel(e: WheelEvent) {
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  const newScale = Math.max(0.2, Math.min(5, scale.value + delta))
  // Zoom toward cursor position
  if (canvasRef.value) {
    const rect = canvasRef.value.getBoundingClientRect()
    const cx = e.clientX - rect.left - rect.width / 2
    const cy = e.clientY - rect.top - rect.height / 2
    const ratio = newScale / scale.value
    posX.value = cx - ratio * (cx - posX.value)
    posY.value = cy - ratio * (cy - posY.value)
  }
  scale.value = newScale
}

function onMouseDown(e: MouseEvent) {
  isDragging.value = true
  dragStartX.value = e.clientX
  dragStartY.value = e.clientY
  posStartX.value = posX.value
  posStartY.value = posY.value
}

function onMouseMove(e: MouseEvent) {
  if (!isDragging.value) return
  posX.value = posStartX.value + (e.clientX - dragStartX.value)
  posY.value = posStartY.value + (e.clientY - dragStartY.value)
}

function onMouseUp() {
  isDragging.value = false
}

function onLoad() {
  reset()
}

watch(() => props.visible, (val) => {
  if (!val) reset()
})
</script>

<style lang="scss" scoped>
.image-viewer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.9);
  z-index: 9999;
  display: flex;
  flex-direction: column;
}
.image-viewer-toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.6);
  .scale-info {
    color: #fff;
    font-size: 13px;
    min-width: 50px;
    text-align: center;
  }
}
.image-viewer-canvas {
  flex: 1;
  position: relative;
  overflow: hidden;
  cursor: grab;
  &:active {
    cursor: grabbing;
  }
}
.viewer-image {
  position: absolute;
  top: 50%;
  left: 50%;
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
  transform-origin: 0 0;
  user-select: none;
}
</style>
