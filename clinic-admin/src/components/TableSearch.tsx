import { defineComponent, ref, PropType, watch } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import './TableSearch.scss'

export interface SearchFormItem {
  prop: string
  label: string
  type: string
  placeholder?: string
  options?: { label: string; value: any }[]
  defaultValue?: any
  attrs?: Record<string, any>
  onChange?: (val: any) => void
}

export interface TableSearchProps {
  modelValue: Record<string, any>
  forms: SearchFormItem[]
  showSearch?: boolean
  showReset?: boolean
  showAdd?: boolean
  addText?: string
  searchText?: string
  loading?: boolean
}

export const TableSearch = defineComponent({
  name: 'TableSearch',
  props: {
    modelValue: {
      type: Object as PropType<Record<string, any>>,
      required: true
    },
    forms: {
      type: Array as PropType<SearchFormItem[]>,
      required: true
    },
    showSearch: {
      type: Boolean,
      default: true
    },
    showReset: {
      type: Boolean,
      default: true
    },
    showAdd: {
      type: Boolean,
      default: false
    },
    addText: {
      type: String,
      default: '新增'
    },
    searchText: {
      type: String,
      default: '搜索'
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'search', 'reset', 'add'],
  setup(props, { emit }) {
    const formData = ref<Record<string, any>>({ ...props.modelValue })

    watch(() => props.modelValue, (val) => {
      formData.value = { ...val }
    }, { deep: true })

    const handleSearch = () => {
      emit('update:modelValue', { ...formData.value })
      emit('search')
    }

    const handleReset = () => {
      const defaultData: Record<string, any> = {}
      props.forms.forEach(item => {
        if (item.defaultValue !== undefined) {
          defaultData[item.prop] = item.defaultValue
        } else {
          defaultData[item.prop] = ''
        }
      })
      formData.value = defaultData
      emit('update:modelValue', { ...formData.value })
      emit('reset')
    }

    const handleAdd = () => {
      emit('add')
    }

    const updateValue = (prop: string, value: any) => {
      formData.value[prop] = value
    }

    return () => (
      <div class="table-search">
        <div class="search-forms">
          {props.forms.map(item => (
            <div class="search-item" key={item.prop}>
              <span class="search-label">{item.label}</span>
              {item.type === 'input' && (
                <el-input
                  modelValue={formData.value[item.prop]}
                  onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                  placeholder={item.placeholder || `请输入${item.label}`}
                  clearable
                  {...item.attrs}
                />
              )}
              {item.type === 'select' && (
                <el-select
                  modelValue={formData.value[item.prop]}
                  onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                  placeholder={item.placeholder || `请选择${item.label}`}
                  clearable
                  {...item.attrs}
                >
                  {item.options?.map(opt => (
                    <el-option key={opt.value} label={opt.label} value={opt.value} />
                  ))}
                </el-select>
              )}
              {item.type === 'date' && (
                <el-date-picker
                  modelValue={formData.value[item.prop]}
                  onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                  type="date"
                  placeholder={item.placeholder || `请选择${item.label}`}
                  value-format="YYYY-MM-DD"
                  {...item.attrs}
                />
              )}
              {item.type === 'daterange' && (
                <el-date-picker
                  modelValue={formData.value[item.prop]}
                  onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  {...item.attrs}
                />
              )}
            </div>
          ))}
        </div>
        <div class="search-buttons">
          {props.showSearch && (
            <el-button type="primary" onClick={handleSearch} loading={props.loading}>
              <el-icon><Search /></el-icon>
              {props.searchText}
            </el-button>
          )}
          {props.showReset && (
            <el-button onClick={handleReset}>
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          )}
          {props.showAdd && (
            <el-button type="primary" onClick={handleAdd}>
              <el-icon><Plus /></el-icon>
              {props.addText}
            </el-button>
          )}
        </div>
      </div>
    )
  }
})