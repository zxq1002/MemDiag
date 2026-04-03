<script setup>
import { ref, computed } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import { Table as TableIcon, FileDown, Search, Filter } from 'lucide-vue-next'

const props = defineProps({
  classStats: Array,
  formatNumber: Function,
  formatBytes: Function
})

const dt = ref()

const filters = ref({
  global: { value: null, matchMode: 'contains' },
  className: { operator: 'and', constraints: [{ value: null, matchMode: 'startsWith' }] },
  objectCount: { operator: 'and', constraints: [{ value: null, matchMode: 'gt' }] },
  shallowBytes: { operator: 'and', constraints: [{ value: null, matchMode: 'gt' }] }
})

const maxBytes = computed(() => {
  if (!props.classStats?.length) return 1
  return Math.max(...props.classStats.map(s => s.shallowBytes || 0))
})

const exportCSV = () => {
  dt.value.exportCSV()
}

const exportJSON = () => {
  const dataStr = JSON.stringify(props.classStats, null, 2)
  const dataBlob = new Blob([dataStr], { type: 'application/json' })
  const url = URL.createObjectURL(dataBlob)
  const link = document.createElement('a')
  link.href = url
  link.download = `histogram-${new Date().getTime()}.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
</script>

<template>
  <Card class="border-0 shadow-sm overflow-hidden rounded-3xl">
    <template #title>
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div class="flex items-center gap-2 text-lg">
          <TableIcon class="w-5 h-5 text-indigo-600" />
          <span>Class Statistics</span>
        </div>
        <div class="flex items-center gap-3">
          <div class="relative">
            <Search class="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 z-10" />
            <InputText v-model="filters['global'].value" placeholder="Search..." class="p-inputtext-sm pl-10 w-40 sm:w-64 bg-slate-50 border-slate-100 rounded-xl" />
          </div>
          
          <div class="flex items-center p-1 bg-slate-50 rounded-xl border border-slate-100">
            <Button icon="FileDown" severity="secondary" text rounded size="small" @click="exportCSV">
              <template #icon><FileDown class="w-4 h-4" /></template>
            </Button>
            <Button icon="Code" severity="secondary" text rounded size="small" @click="exportJSON">
              <template #icon><span class="text-[10px] font-bold">JSON</span></template>
            </Button>
          </div>
        </div>
      </div>
    </template>
    
    <template #content>
      <DataTable 
        ref="dt"
        :value="classStats" 
        :filters="filters"
        filterDisplay="menu"
        paginator 
        :rows="10" 
        class="p-datatable-sm"
        removableSort
        :globalFilterFields="['className']"
      >
        <Column field="className" header="Class Name" sortable filterField="className" :showFilterMatchModes="true">
          <template #body="slotProps">
            <code class="text-[11px] text-indigo-600 font-medium break-all">{{ slotProps.data.className }}</code>
          </template>
          <template #filter="{ filterModel }">
            <InputText v-model="filterModel.value" type="text" class="p-column-filter rounded-lg" placeholder="Search by name" />
          </template>
        </Column>
        
        <Column field="objectCount" header="Objects" sortable class="text-right" dataType="numeric">
          <template #body="slotProps">
            <span class="font-mono text-sm">{{ formatNumber(slotProps.data.objectCount) }}</span>
          </template>
          <template #filter="{ filterModel }">
            <InputText v-model="filterModel.value" type="number" class="p-column-filter rounded-lg" placeholder="Min objects" />
          </template>
        </Column>
        
        <Column field="shallowBytes" header="Shallow Size" sortable class="text-right" dataType="numeric">
          <template #body="slotProps">
            <div class="relative w-full h-8 flex items-center justify-end px-2 group">
              <!-- Background Data Bar - Using plain inline style for dynamic width, static color via class -->
              <div 
                class="absolute right-0 top-1 bottom-1 bg-indigo-50 rounded-l-md transition-all duration-500"
                :style="{ width: `${(slotProps.data.shallowBytes / maxBytes) * 100}%` }"
              ></div>
              <!-- Text -->
              <span class="relative z-10 font-mono text-sm font-semibold text-slate-700">
                {{ formatBytes(slotProps.data.shallowBytes) }}
              </span>
            </div>
          </template>
          <template #filter="{ filterModel }">
            <InputText v-model="filterModel.value" type="number" class="p-column-filter rounded-lg" placeholder="Min bytes" />
          </template>
        </Column>
      </DataTable>
    </template>
  </Card>
</template>

<style scoped>
/* No @apply used here */
:deep(.p-column-filter-menu-button) {
  color: #94a3b8;
}
:deep(.p-column-filter-menu-button-active) {
  color: #4f46e5;
  background-color: #eef2ff;
}
</style>
