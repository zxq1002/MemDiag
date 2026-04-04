<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import InputText from 'primevue/inputtext'
import { ListTree, Search, X } from 'lucide-vue-next'
import StackTraceViewer from './StackTraceViewer.vue'

const { t } = useI18n()
const props = defineProps({
  threads: Array,
  stateFilter: String,
  searchQuery: String
})

const emit = defineEmits(['update:searchQuery', 'clearFilter'])

const expandedRows = ref([])

const getStateSeverity = (state) => {
  switch (state) {
    case 'RUNNABLE': return 'success'
    case 'BLOCKED': return 'danger'
    case 'WAITING': return 'warn'
    case 'TIMED_WAITING': return 'info'
    default: return 'secondary'
  }
}
</script>

<template>
  <Card class="border-0 shadow-sm overflow-hidden rounded-3xl">
    <template #title>
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div class="flex items-center gap-2 text-lg">
          <ListTree class="w-5 h-5 text-indigo-600" />
          <span>{{ t('threads.activeThreads') }}</span>
          <Tag v-if="stateFilter" :value="stateFilter" :severity="getStateSeverity(stateFilter)" closable @close="emit('clearFilter')" />
        </div>
        <div class="flex items-center gap-3">
          <div class="relative">
            <Search class="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 z-10" />
            <InputText 
              :modelValue="searchQuery" 
              @update:modelValue="val => emit('update:searchQuery', val)"
              :placeholder="t('threads.searchPlaceholder')" 
              class="p-inputtext-sm pl-10 w-48 sm:w-64 bg-slate-50 border-slate-100 rounded-xl" 
            />
          </div>
        </div>
      </div>
    </template>
    
    <template #content>
      <DataTable 
        :value="threads" 
        v-model:expandedRows="expandedRows"
        paginator :rows="10" 
        class="p-datatable-sm"
        dataKey="threadId"
        removableSort
      >
        <Column expander style="width: 3rem" />
        <Column field="threadId" :header="t('common.id')" sortable>
          <template #body="slotProps">
            <code class="text-xs font-bold text-slate-400">#{{ slotProps.data.threadId }}</code>
          </template>
        </Column>
        <Column field="threadName" :header="t('common.name')" sortable>
          <template #body="slotProps">
            <span class="font-bold text-slate-700 text-sm">{{ slotProps.data.threadName }}</span>
          </template>
        </Column>
        <Column field="state" :header="t('common.status')" sortable>
          <template #body="slotProps">
            <Tag :value="slotProps.data.state" :severity="getStateSeverity(slotProps.data.state)" />
          </template>
        </Column>
        <Column field="blockedCount" :header="t('threads.blocked')" sortable class="text-right">
          <template #body="slotProps">
            <span class="text-xs font-mono" :class="slotProps.data.blockedCount > 0 ? 'text-red-500 font-bold' : 'text-slate-400'">
              {{ slotProps.data.blockedCount }}
            </span>
          </template>
        </Column>
        
        <template #expansion="slotProps">
          <StackTraceViewer :stackTrace="slotProps.data.stackTrace" />
        </template>
      </DataTable>
    </template>
  </Card>
</template>
