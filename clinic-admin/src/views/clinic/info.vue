<template>
  <div class="page-container">
    <el-card class="info-card">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
          <el-button type="primary" @click="handleEdit" v-if="!isEditing">编辑信息</el-button>
        </div>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        :disabled="!isEditing"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="诊所名称" prop="clinicName">
              <el-input v-model="form.clinicName" placeholder="请输入诊所名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="诊所编码" prop="clinicCode">
              <el-input v-model="form.clinicCode" placeholder="诊所编码" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="所在地区" prop="region">
          <el-cascader
            v-model="regionValue"
            :options="regionOptions"
            placeholder="请选择省/市/区"
            @change="handleRegionChange"
            style="width: 400px"
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入详细地址" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="营业开始时间" prop="serviceStart">
              <el-time-picker
                v-model="form.serviceStart"
                placeholder="选择开始时间"
                format="HH:mm:ss"
                value-format="HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="营业结束时间" prop="serviceEnd">
              <el-time-picker
                v-model="form.serviceEnd"
                placeholder="选择结束时间"
                format="HH:mm:ss"
                value-format="HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="诊所简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入诊所简介" />
        </el-form-item>
        <el-form-item label="营业状态" prop="businessStatus">
          <el-switch
            v-model="form.businessStatus"
            :active-value="1"
            :inactive-value="0"
            active-text="营业中"
            inactive-text="休息中"
          />
        </el-form-item>
        <el-form-item v-if="isEditing">
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">保存</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="info-card">
      <template #header>
        <div class="card-header">
          <span>其他信息</span>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="创建时间">{{ clinicInfo?.createdAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ clinicInfo?.updatedAt || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { updateClinicInfo, type ClinicInfo } from '@/api/clinic'
import { getClinicId } from '@/utils/storage'

const loading = ref(false)
const submitLoading = ref(false)
const isEditing = ref(false)
const formRef = ref<FormInstance>()
const clinicInfo = ref<ClinicInfo | null>(null)

const regionValue = ref<string[]>([])
const regionOptions = [
  {
    value: '北京市',
    label: '北京市',
    children: [
      { value: '市辖区', label: '市辖区' }
    ]
  },
  {
    value: '上海市',
    label: '上海市',
    children: [
      { value: '市辖区', label: '市辖区' }
    ]
  },
  {
    value: '广东省',
    label: '广东省',
    children: [
      { value: '广州市', label: '广州市' },
      { value: '深圳市', label: '深圳市' },
      { value: '珠海市', label: '珠海市' }
    ]
  },
  {
    value: '浙江省',
    label: '浙江省',
    children: [
      { value: '杭州市', label: '杭州市' },
      { value: '宁波市', label: '宁波市' },
      { value: '温州市', label: '温州市' }
    ]
  }
]

const form = reactive({
  clinicName: '',
  clinicCode: '',
  contactPerson: '',
  contactPhone: '',
  province: '',
  city: '',
  district: '',
  address: '',
  serviceStart: '',
  serviceEnd: '',
  businessStatus: 1,
  description: ''
})

const rules = {
  clinicName: [{ required: true, message: '请输入诊所名称', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

const loadData = async () => {
  loading.value = true
  try {
    const clinicId = getClinicId()
    if (!clinicId) {
      ElMessage.error('未找到诊所信息')
      return
    }
    // TODO: 加载诊所信息
    loading.value = false
  } catch (error) {
    console.error('加载诊所信息失败:', error)
    loading.value = false
  }
}

const handleRegionChange = (value: string[]) => {
  if (value && value.length >= 3) {
    form.province = value[0]
    form.city = value[1]
    form.district = value[2]
  }
}

const handleEdit = () => {
  isEditing.value = true
}

const handleCancel = () => {
  isEditing.value = false
  loadData()
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    await updateClinicInfo(form)
    ElMessage.success('保存成功')
    isEditing.value = false
    loadData()
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.page-container {
  .info-card {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
}
</style>