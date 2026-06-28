export { Table, TableSearch, DialogForm, Chart } from './components'

import { Table } from './components/Table'
import { TableSearch } from './components/TableSearch'
import { DialogForm } from './components/DialogForm'
import { Chart } from './components/Chart'

export const components = [
  Table,
  TableSearch,
  DialogForm,
  Chart
]

export default {
  install(app: any) {
    components.forEach(component => {
      app.component(component.name, component)
    })
  }
}