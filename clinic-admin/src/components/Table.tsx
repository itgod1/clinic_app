import { defineComponent, ref, PropType, watch } from 'vue'
import { Delete, Edit, View, Refresh } from '@element-plus/icons-vue'
import './TableOperate.scss'

export interface TableColumn {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right'
  sortable?: boolean
  formatter?: (row: any, column: any, cellValue: any, index: number) => any
  showOverflowTooltip?: boolean
}

export interface TableOperate {
  label: string
  type?: string
  icon?: boolean
  condition?: (row: any) => boolean
  action: (row: any) => void
}

export interface Pagination {
  current: number
  size: number
  total: number
}

export interface TableProps {
  columns: TableColumn[]
  data: any[]
  loading?: boolean
  selection?: boolean
  index?: boolean
  operates?: TableOperate[]
  pagination?: Pagination
  showOverflowTooltip?: boolean
}

export const Table = defineComponent({
  name: 'Table',
  props: {
    columns: {
      type: Array as PropType<TableColumn[]>,
      required: true
    },
    data: {
      type: Array as PropType<any[]>,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    selection: {
      type: Boolean,
      default: false
    },
    index: {
      type: Boolean,
      default: false
    },
    operates: {
      type: Array as PropType<TableOperate[]>,
      default: () => []
    },
    pagination: {
      type: Object as PropType<Pagination>,
      default: () => ({ current: 1, size: 10, total: 0 })
    },
    showOverflowTooltip: {
      type: Boolean,
      default: true
    }
  },
  emits: ['selection-change', 'page-change', 'reload'],
  setup(props, { emit }) {
    const tableRef = ref()

    const handleSelectionChange = (selection: any[]) => {
      emit('selection-change', selection)
    }

    const handlePageChange = (page: number) => {
      emit('page-change', page)
    }

    const handleSizeChange = (size: number) => {
      emit('page-change', 1, size)
    }

    const handleReload = () => {
      emit('reload')
    }

    const getCellStyle = (column: TableColumn) => {
      const style: Record<string, any> = {
        textAlign: column.align || 'left'
      }
      if (column.width) {
        style.width = typeof column.width === 'number' ? `${column.width}px` : column.width
      }
      return style
    }

    const formatCellValue = (row: any, col: TableColumn, cellValue: any, index: number) => {
      if (col.formatter) {
        const result = col.formatter(row, col, cellValue, index)
        // 如果结果是字符串且包含 HTML 标签，使用 v-html 渲染
        if (typeof result === 'string' && /<[^>]+>/.test(result)) {
          return <span v-html={result}></span>
        }
        return result
      }
      return cellValue ?? '-'
    }

    return () => (
      <div class="table-container">
        <div class="table-toolbar">
          <slot name="toolbar" />
          <el-button
            v-if={props.loading}
            class="reload-btn"
            onClick={handleReload}
            icon={<Refresh />}
            circle
          />
        </div>
        <el-table
          ref={tableRef}
          data={props.data}
          loading={props.loading}
          selection={props.selection}
          onSelection-change={handleSelectionChange}
          border
          stripe
          style={{ width: '100%' }}
          table-layout="auto"
          class="full-width-table"
        >
          {props.selection && (
            <el-table-column type="selection" width="50" fixed="left" />
          )}
          {props.index && (
            <el-table-column type="index" label="序号" width="60" align="center" fixed="left" />
          )}
          {props.columns.map((col, colIndex) => (
            <el-table-column
              key={col.prop || colIndex}
              prop={col.prop}
              label={col.label}
              width={col.width}
              min-width={col.minWidth || (col.width ? undefined : '100')}
              align={col.align || 'left'}
              fixed={col.fixed}
              sortable={col.sortable}
              showOverflowTooltip={col.showOverflowTooltip ?? props.showOverflowTooltip}
              cell-style={getCellStyle(col)}
            >{{
              default: (scope: any) => formatCellValue(scope.row, col, scope.row[col.prop], scope.$index)
            }}</el-table-column>
          ))}
          {props.operates && props.operates.length > 0 && (
            <el-table-column
              label="操作"
              width="180"
              fixed="right"
              align="center"
            >
              {{
                default: (scope: any) => (
                  <div class="table-operate">
                    {props.operates!.map((operate) => {
                      if (operate.condition && !operate.condition(scope.row)) {
                        return null
                      }
                      return (
                        <el-button
                          key={operate.label}
                          type={operate.type || 'primary'}
                          size="small"
                          text={operate.type === 'text'}
                          onClick={() => operate.action(scope.row)}
                        >
                          {operate.icon && <el-icon class="operate-icon"><Edit /></el-icon>}
                          {operate.label}
                        </el-button>
                      )
                    })}
                  </div>
                )
              }}
            </el-table-column>
          )}
        </el-table>
        {props.pagination && props.pagination.total >= 0 && (
          <div class="table-pagination">
            <el-pagination
              v-model:current-page={props.pagination.current}
              v-model:page-size={props.pagination.size}
              page-sizes={[10, 20, 50, 100]}
              total={props.pagination.total}
              layout="total, sizes, prev, pager, next, jumper"
              background
              onCurrentChange={handlePageChange}
              onSizeChange={handleSizeChange}
            />
          </div>
        )}
      </div>
    )
  }
})
