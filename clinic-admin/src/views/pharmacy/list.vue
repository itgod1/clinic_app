<template>
  <div class="page-container">
    <el-card class="search-card">
      <TableSearch
        v-model="queryParams"
        :forms="searchForms"
        @search="handleSearch"
        @reset="handleReset"
        @add="handleAdd"
        showAdd
        addText="添加药品"
      />
    </el-card>

    <el-card class="table-card">
      <Table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :operates="operates"
        @page-change="handlePageChange"
        @reload="loadData"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="药品名称" prop="itemName">
              <el-input v-model="form.itemName" placeholder="请输入药品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="药品编码" prop="itemCode">
              <el-input v-model="form.itemCode" placeholder="请输入药品编码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="药品类型" prop="itemType">
              <el-select v-model="form.itemType" placeholder="请选择药品类型">
                <el-option label="西药" :value="1" />
                <el-option label="中成药" :value="2" />
                <el-option label="中药饮片" :value="3" />
                <el-option label="耗材" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-select v-model="form.unit" placeholder="请选择单位" filterable allow-create style="width: 100%">
                <el-option label="盒" value="盒" />
                <el-option label="瓶" value="瓶" />
                <el-option label="支" value="支" />
                <el-option label="片" value="片" />
                <el-option label="粒" value="粒" />
                <el-option label="袋" value="袋" />
                <el-option label="包" value="包" />
                <el-option label="套" value="套" />
                <el-option label="次" value="次" />
                <el-option label="颗" value="颗" />
                <el-option label="只" value="只" />
                <el-option label="副" value="副" />
                <el-option label="件" value="件" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="规格" prop="spec">
              <el-input v-model="form.spec" placeholder="请输入规格" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生产厂家" prop="manufacturer">
              <el-input v-model="form.manufacturer" placeholder="请输入生产厂家" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="进价" prop="purchasePrice">
              <el-input-number v-model="form.purchasePrice" :precision="2" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="零售价" prop="retailPrice">
              <el-input-number v-model="form.retailPrice" :precision="2" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="会员价" prop="memberPrice">
              <el-input-number v-model="form.memberPrice" :precision="2" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存" prop="stock">
              <el-input-number v-model="form.stock" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="条形码" prop="barcode">
          <el-input v-model="form.barcode" placeholder="请输入条形码" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stockDialogVisible" :title="stockDialogTitle" width="600px" destroy-on-close>
      <el-form ref="stockFormRef" :model="stockForm" label-width="100px">
        <el-form-item label="批次号" prop="batchNo">
          <el-input v-model="stockForm.batchNo" placeholder="请输入批次号" />
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="stockForm.quantity" :min="1" />
        </el-form-item>
        <el-form-item label="单价" prop="unitPrice" v-if="stockDialogTitle === '入库'">
          <el-input-number v-model="stockForm.unitPrice" :precision="2" :min="0" />
        </el-form-item>
        <el-form-item label="供应商" prop="supplier" v-if="stockDialogTitle === '入库'">
          <el-input v-model="stockForm.supplier" placeholder="请输入供应商" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="stockForm.remark" type="textarea" rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStockSubmit" :loading="stockLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Table } from '@/components/Table'
import { TableSearch } from '@/components/TableSearch'
import { getStockList, createStockItem, updateStockItem, stockIn, stockOut, type StockItem } from '@/api/stock'
import { getClinicId, getUserInfo } from '@/utils/storage'

const loading = ref(false)
const submitLoading = ref(false)
const stockLoading = ref(false)
const dialogVisible = ref(false)
const stockDialogVisible = ref(false)
const dialogTitle = ref('')
const stockDialogTitle = ref('')
const isEdit = ref(false)
const stockOperationType = ref(1)
const formRef = ref<FormInstance>()
const stockFormRef = ref<FormInstance>()

const queryParams = reactive({
  clinicId: 0,
  keyword: '',
  itemType: undefined as number | undefined,
  status: undefined as number | undefined
})

const tableData = ref<StockItem[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const searchForms = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '名称/编码/条形码' },
  {
    prop: 'itemType',
    label: '药品类型',
    type: 'select',
    options: [
      { label: '西药', value: 1 },
      { label: '中成药', value: 2 },
      { label: '中药饮片', value: 3 },
      { label: '耗材', value: 4 }
    ]
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 }
    ]
  }
]

const form = reactive({
  id: undefined as number | undefined,
  itemName: '',
  itemCode: '',
  itemType: 1,
  unit: '',
  spec: '',
  manufacturer: '',
  barcode: '',
  purchasePrice: 0,
  retailPrice: 0,
  memberPrice: 0,
  stock: 0,
  status: 1
})

const stockForm = reactive({
  itemId: 0,
  batchNo: '',
  quantity: 0,
  unitPrice: 0,
  totalAmount: 0,
  supplier: '',
  remark: ''
})

const rules = {
  itemName: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  itemCode: [{ required: true, message: '请输入药品编码', trigger: 'blur' }],
  itemType: [{ required: true, message: '请选择药品类型', trigger: 'change' }]
}

const itemTypeMap: Record<number, string> = {
  1: '西药',
  2: '中成药',
  3: '中药饮片',
  4: '耗材'
}

const columns = [
  { prop: 'itemCode', label: '编码', width: 120 },
  { prop: 'itemName', label: '名称', minWidth: 150 },
  {
    prop: 'itemType',
    label: '类型',
    width: 100,
    formatter: (row: StockItem) => itemTypeMap[row.itemType] || '-'
  },
  { prop: 'spec', label: '规格', width: 120 },
  { prop: 'unit', label: '单位', width: 60 },
  { prop: 'retailPrice', label: '零售价', width: 100, formatter: (row: StockItem) => `¥${row.retailPrice}` },
  { prop: 'stock', label: '库存', width: 80 },
  {
    prop: 'isLowStock',
    label: '库存预警',
    width: 100,
    formatter: (row: StockItem) => row.isLowStock ? '是' : '否'
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    formatter: (row: StockItem) => row.status === 1 ? '启用' : '禁用'
  }
]

const operates = [
  { label: '编辑', type: 'warning', action: (row: StockItem) => handleEdit(row) },
  { label: '入库', type: 'success', action: (row: StockItem) => handleStockIn(row) },
  { label: '出库', type: 'danger', action: (row: StockItem) => handleStockOut(row) }
]

const loadData = async () => {
  loading.value = true
  try {
    const clinicId = getClinicId()
    if (!clinicId) {
      ElMessage.warning('请先选择诊所')
      return
    }
    const params = {
      clinicId,
      pageNum: pagination.current,
      pageSize: pagination.size,
      ...queryParams
    }
    const res = await getStockList(params)
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  pagination.current = 1
  loadData()
}

const handlePageChange = (page: number, size?: number) => {
  pagination.current = page
  if (size) {
    pagination.size = size
  }
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '添加药品'
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    itemName: '',
    itemCode: '',
    itemType: 1,
    unit: '',
    spec: '',
    manufacturer: '',
    barcode: '',
    purchasePrice: 0,
    retailPrice: 0,
    memberPrice: 0,
    stock: 0,
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row: StockItem) => {
  dialogTitle.value = '编辑药品'
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleStockIn = (row: StockItem) => {
  stockDialogTitle.value = '入库'
  stockOperationType.value = 1
  stockForm.itemId = row.id
  stockForm.batchNo = ''
  stockForm.quantity = 0
  stockForm.unitPrice = row.purchasePrice
  stockForm.totalAmount = 0
  stockForm.supplier = ''
  stockForm.remark = ''
  stockDialogVisible.value = true
}

const handleStockOut = (row: StockItem) => {
  stockDialogTitle.value = '出库'
  stockOperationType.value = 2
  stockForm.itemId = row.id
  stockForm.quantity = 0
  stockForm.remark = ''
  stockDialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    const clinicId = getClinicId()
    if (isEdit.value) {
      await updateStockItem({ ...form, clinicId: clinicId! })
      ElMessage.success('编辑成功')
    } else {
      await createStockItem({ ...form, clinicId: clinicId! })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleStockSubmit = async () => {
  try {
    await stockFormRef.value?.validate()
    stockLoading.value = true
    const userInfo = getUserInfo()
    const clinicId = getClinicId()!

    if (stockOperationType.value === 1) {
      stockForm.totalAmount = stockForm.quantity * stockForm.unitPrice
      await stockIn({
        ...stockForm,
        clinicId,
        operatorId: userInfo?.userId || 0
      })
      ElMessage.success('入库成功')
    } else {
      await stockOut({
        clinicId,
        itemId: stockForm.itemId,
        quantity: stockForm.quantity,
        operationType: 2,
        operatorId: userInfo?.userId || 0,
        remark: stockForm.remark
      })
      ElMessage.success('出库成功')
    }
    stockDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('操作失败:', error)
  } finally {
    stockLoading.value = false
  }
}

onMounted(() => {
  queryParams.clinicId = getClinicId()
  loadData()
})
</script>

<style lang="scss" scoped>
.page-container {
  .search-card {
    margin-bottom: 16px;
  }

  .table-card {
    background: #fff;
  }
}
</style>