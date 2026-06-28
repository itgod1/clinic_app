import { defineComponent, PropType, ref, watch } from 'vue'
import { Close } from '@element-plus/icons-vue'
import './DialogForm.scss'

export interface DialogFormColumn {
  prop: string
  label: string
  type: 'input' | 'textarea' | 'select' | 'radio' | 'checkbox' | 'date' | 'datetime' | 'daterange' | 'number' | 'switch' | 'cascader' | 'slot'
  placeholder?: string
  rules?: any[]
  options?: { label: string; value: any }[]
  attrs?: Record<string, any>
  span?: number
  show?: boolean
  disabled?: boolean
  onChange?: (val: any) => void
}

export interface DialogFormProps {
  modelValue: boolean
  title: string
  columns: DialogFormColumn[]
  formData: Record<string, any>
  width?: string
  labelWidth?: string
  loading?: boolean
  destroyOnClose?: boolean
}

export const DialogForm = defineComponent({
  name: 'DialogForm',
  props: {
    modelValue: {
      type: Boolean,
      required: true
    },
    title: {
      type: String,
      required: true
    },
    columns: {
      type: Array as PropType<DialogFormColumn[]>,
      required: true
    },
    formData: {
      type: Object as PropType<Record<string, any>>,
      required: true
    },
    width: {
      type: String,
      default: '600px'
    },
    labelWidth: {
      type: String,
      default: '120px'
    },
    loading: {
      type: Boolean,
      default: false
    },
    destroyOnClose: {
      type: Boolean,
      default: true
    }
  },
  emits: ['update:modelValue', 'confirm', 'cancel'],
  setup(props, { emit, slots }) {
    const dialogRef = ref()
    const formRef = ref()

    watch(() => props.modelValue, (val) => {
      if (val) {
        Object.keys(props.formData).forEach(key => {
          ;(props.formData as any)[key] = (props.formData as any)[key] ?? ''
        })
      }
    })

    const handleClose = () => {
      emit('update:modelValue', false)
      emit('cancel')
    }

    const handleConfirm = async () => {
      try {
        await (formRef.value as any)?.validate()
        emit('confirm', { ...props.formData })
      } catch (error) {
        console.log('表单校验失败')
      }
    }

    const updateValue = (prop: string, value: any) => {
      ;(props.formData as any)[prop] = value
    }

    const renderFormItem = (item: DialogFormColumn) => {
      if (item.show === false) return null

      const formItemContent = () => {
        switch (item.type) {
          case 'input':
            return (
              <el-input
                modelValue={(props.formData as any)[item.prop]}
                onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                placeholder={item.placeholder || `请输入${item.label}`}
                disabled={item.disabled}
                {...item.attrs}
              />
            )
          case 'textarea':
            return (
              <el-input
                modelValue={(props.formData as any)[item.prop]}
                onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                type="textarea"
                placeholder={item.placeholder || `请输入${item.label}`}
                rows={3}
                disabled={item.disabled}
                {...item.attrs}
              />
            )
          case 'number':
            return (
              <el-input-number
                modelValue={(props.formData as any)[item.prop]}
                onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                placeholder={item.placeholder || `请输入${item.label}`}
                disabled={item.disabled}
                {...item.attrs}
              />
            )
          case 'select':
            return (
              <el-select
                modelValue={(props.formData as any)[item.prop]}
                onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                placeholder={item.placeholder || `请选择${item.label}`}
                disabled={item.disabled}
                {...item.attrs}
              >
                {item.options?.map((opt) => (
                  <el-option key={opt.value} label={opt.label} value={opt.value} />
                ))}
              </el-select>
            )
          case 'radio':
            return (
              <el-radio-group
                modelValue={(props.formData as any)[item.prop]}
                onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                disabled={item.disabled}
              >
                {item.options?.map((opt) => (
                  <el-radio key={opt.value} value={opt.value}>
                    {opt.label}
                  </el-radio>
                ))}
              </el-radio-group>
            )
          case 'checkbox':
            return (
              <el-checkbox-group
                modelValue={(props.formData as any)[item.prop]}
                onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                disabled={item.disabled}
              >
                {item.options?.map((opt) => (
                  <el-checkbox key={opt.value} value={opt.value}>
                    {opt.label}
                  </el-checkbox>
                ))}
              </el-checkbox-group>
            )
          case 'date':
            return (
              <el-date-picker
                modelValue={(props.formData as any)[item.prop]}
                onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                type="date"
                placeholder={item.placeholder || `请选择${item.label}`}
                value-format="YYYY-MM-DD"
                disabled={item.disabled}
                {...item.attrs}
              />
            )
          case 'datetime':
            return (
              <el-date-picker
                modelValue={(props.formData as any)[item.prop]}
                onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                type="datetime"
                placeholder={item.placeholder || `请选择${item.label}`}
                value-format="YYYY-MM-DD HH:mm:ss"
                disabled={item.disabled}
                {...item.attrs}
              />
            )
          case 'daterange':
            return (
              <el-date-picker
                modelValue={(props.formData as any)[item.prop]}
                onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                disabled={item.disabled}
                {...item.attrs}
              />
            )
          case 'switch':
            return (
              <el-switch
                modelValue={(props.formData as any)[item.prop]}
                onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                disabled={item.disabled}
                {...item.attrs}
              />
            )
          case 'slot':
            return slots[item.prop]?.({ row: props.formData })
          default:
            return (
              <el-input
                modelValue={(props.formData as any)[item.prop]}
                onUpdate:modelValue={(val) => updateValue(item.prop, val)}
                placeholder={item.placeholder}
                disabled={item.disabled}
              />
            )
        }
      }

      return (
        <el-form-item
          key={item.prop}
          label={item.label}
          prop={item.prop}
          rules={item.rules}
          style={{ width: item.span === 2 ? '100%' : '50%' }}
        >
          {formItemContent()}
        </el-form-item>
      )
    }

    return () => (
      <el-dialog
        ref={dialogRef}
        modelValue={props.modelValue}
        title={props.title}
        width={props.width}
        close-on-click-modal={false}
        destroy-on-close={props.destroyOnClose}
        onClose={handleClose}
        class="dialog-form"
      >
        <el-form
          ref={formRef}
          model={props.formData}
          label-width={props.labelWidth}
          class="dialog-form-content"
        >
          {props.columns.map((item) => renderFormItem(item))}
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button onClick={handleClose}>取消</el-button>
            <el-button type="primary" loading={props.loading} onClick={handleConfirm}>
              确定
            </el-button>
          </div>
        </template>
      </el-dialog>
    )
  }
})