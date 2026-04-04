<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConnectionStore } from '../stores/connectionStore'
import { useSnapshots } from '../composables/useSnapshots'
import { useDiff } from '../composables/useDiff'
import { 
  GitCompare, 
  RefreshCw, 
  Layers,
  Search,
  CheckCircle2,
  AlertTriangle
} from 'lucide-vue-next'
import Button from 'primevue/button'
import Select from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Card from 'primevue/card'
import ProgressBar from 'primevue/progressbar'
import Tag from 'primevue/tag'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const connectionStore = useConnectionStore()
const toast = useToast()
const { snapshots, loadSnapshots } = useSnapshots()
const { 
  diffData, 
  isLoading, 
  loadSnapshotsForDiff,
  baseSnapshot,
  compareSnapshot 
} = useDiff()

// Global Sync
const selectedConn = computed({
  get: () => connectionStore.currentConnectionId,
  set: (val) => connectionStore.setCurrentConnection(val)
})

const baseId = ref(null)
const compareId = ref(null)
const hasCompared = ref(false)

const connections = computed(() => {
  return Object.entries(connectionStore.connections).map(([id, status]) => ({ id, label: id }))
})

const snapshotOptions = computed(() => {
  return snapshots.value.map(s => ({ id: s.id, label: `${s.name || s.id} (${new Date(s.createdAt).toLocaleTimeString()})` }))
})

const refreshSnapshots = () => {
  if (selectedConn.value) loadSnapshots(selectedConn.value)
}

const compare = async () => {
  if (selectedConn.value && baseId.value && compareId.value) {
    hasCompared.value = false
    const res = await loadSnapshotsForDiff(selectedConn.value, baseId.value, compareId.value)
    if (res.success) {
      hasCompared.value = true
      if (diffData.value.length === 0) {
        toast.add({ severity: 'info', summary: t('diff.noDiff'), detail: t('diff.noDiffDesc'), life: 5000 })
      } else {
        toast.add({ severity: 'success', summary: t('common.success'), detail: `${diffData.value.length} diffs`, life: 3000 })
      }
    } else {
      toast.add({ severity: 'error', summary: t('common.error'), detail: res.error, life: 5000 })
    }
  }
}

const formatBytes = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const sign = bytes < 0 ? '-' : ''
  const absBytes = Math.abs(bytes)
  const i = Math.floor(Math.log(absBytes) / Math.log(k))
  return sign + parseFloat((absBytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatNumber = (n) => {
  const sign = n < 0 ? '-' : n > 0 ? '+' : ''
  return sign + Math.abs(n).toLocaleString()
}

onMounted(() => {
  refreshSnapshots()
})

watch(selectedConn, (newVal) => {
  if (newVal) {
    refreshSnapshots()
    baseId.value = null
    compareId.value = null
    hasCompared.value = false
  }
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-3xl font-bold text-slate-900 tracking-tight">{{ t('diff.title') }}</h1>
        <p class="text-slate-500 mt-1 text-sm text-balance">{{ t('diff.subtitle') }}</p>
      </div>
    </div>
    
    <!-- Controls Bar -->
    <div class="bg-white p-3 rounded-3xl shadow-sm border border-slate-100 flex flex-col lg:flex-row gap-4 items-stretch">
      <!-- Target Section -->
      <div class="flex items-center gap-3 lg:border-r lg:border-slate-100 lg:pr-6 shrink-0">
        <div class="p-2 bg-indigo-50 rounded-lg text-indigo-600">
          <RefreshCw class="w-4 h-4" />
        </div>
        <Select 
          v-model="selectedConn" 
          :options="connections" 
          optionLabel="label" 
          optionValue="id" 
          :placeholder="t('common.selectConnection')" 
          class="w-full lg:w-36 border-0 shadow-none bg-slate-50 rounded-xl font-medium"
        />
      </div>

      <!-- Snapshots Selection Group -->
      <div class="flex-1 flex flex-col sm:flex-row items-center gap-3 min-w-0">
        <Select 
          v-model="baseId" 
          :options="snapshotOptions" 
          optionLabel="label" 
          optionValue="id" 
          :placeholder="t('diff.baseline')" 
          class="w-full sm:w-1/2 max-w-[280px] border-0 shadow-none bg-slate-50 rounded-xl text-sm overflow-hidden"
        />
        <div class="hidden sm:block shrink-0 text-slate-300">
          <GitCompare class="w-4 h-4 rotate-90" />
        </div>
        <Select 
          v-model="compareId" 
          :options="snapshotOptions" 
          optionLabel="label" 
          optionValue="id" 
          :placeholder="t('diff.comparison')" 
          class="w-full sm:w-1/2 max-w-[280px] border-0 shadow-none bg-slate-50 rounded-xl text-sm overflow-hidden"
        />
      </div>

      <!-- Action Button -->
      <Button 
        @click="compare" 
        :loading="isLoading" 
        :disabled="!baseId || !compareId" 
        class="rounded-xl font-bold px-8 h-12 lg:h-auto min-w-[140px] shrink-0 shadow-lg shadow-indigo-100 whitespace-nowrap"
      >
        <template #icon><GitCompare class="w-4 h-4 mr-2" /></template>
        {{ t('diff.compareBtn') }}
      </Button>
    </div>

    <ProgressBar v-if="isLoading" mode="indeterminate" style="height: 4px" class="rounded-full overflow-hidden" />

    <div v-if="hasCompared && diffData.length" class="space-y-6">
      <!-- Summary Cards -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card class="border-0 shadow-sm border-l-4 border-l-slate-400 rounded-3xl overflow-hidden">
          <template #content>
            <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">{{ t('diff.baseline') }}</p>
            <p class="text-base font-bold text-slate-700 truncate" :title="baseSnapshot?.name || baseId">{{ baseSnapshot?.name || baseId }}</p>
            <p class="text-[10px] text-slate-400">{{ new Date(baseSnapshot?.createdAt || Date.now()).toLocaleString() }}</p>
          </template>
        </Card>
        <Card class="border-0 shadow-sm border-l-4 border-l-indigo-500 rounded-3xl overflow-hidden">
          <template #content>
            <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">{{ t('diff.comparison') }}</p>
            <p class="text-base font-bold text-slate-700 truncate" :title="compareSnapshot?.name || compareId">{{ compareSnapshot?.name || compareId }}</p>
            <p class="text-[10px] text-slate-400">{{ new Date(compareSnapshot?.createdAt || Date.now()).toLocaleString() }}</p>
          </template>
        </Card>
        <Card class="border-0 shadow-sm border-l-4 rounded-3xl" :class="diffData.reduce((acc, d) => acc + d.deltaBytes, 0) > 0 ? 'border-l-red-500' : 'border-l-emerald-500'">
          <template #content>
            <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">{{ t('diff.netChange') }}</p>
            <p class="text-2xl font-black text-slate-900">
              {{ formatBytes(diffData.reduce((acc, d) => acc + d.deltaBytes, 0)) }}
            </p>
          </template>
        </Card>
      </div>

      <!-- Diff Table -->
      <Card class="border-0 shadow-sm overflow-hidden rounded-3xl">
        <template #content>
          <DataTable 
            :value="diffData" 
            paginator :rows="15" 
            class="p-datatable-sm"
            removableSort
            sortField="deltaBytes"
            :sortOrder="-1"
          >
            <Column field="className" :header="t('histogram.className')" sortable>
              <template #body="slotProps">
                <code class="text-[11px] text-indigo-600 font-medium break-all">{{ slotProps.data.className }}</code>
              </template>
            </Column>
            <Column field="deltaObjects" :header="t('diff.deltaObjects')" sortable class="text-right">
              <template #body="slotProps">
                <span :class="['font-mono text-sm', slotProps.data.deltaObjects > 0 ? 'text-red-500 font-bold' : slotProps.data.deltaObjects < 0 ? 'text-emerald-500' : 'text-slate-400']">
                  {{ formatNumber(slotProps.data.deltaObjects) }}
                </span>
              </template>
            </Column>
            <Column field="deltaBytes" :header="t('diff.deltaSize')" sortable class="text-right">
              <template #body="slotProps">
                <span :class="['font-mono text-sm', slotProps.data.deltaBytes > 0 ? 'text-red-500 font-bold' : slotProps.data.deltaBytes < 0 ? 'text-emerald-500' : 'text-slate-400']">
                  {{ formatBytes(slotProps.data.deltaBytes) }}
                </span>
              </template>
            </Column>
            <Column field="growthRate" :header="t('diff.growth')" sortable class="text-right">
              <template #body="slotProps">
                <Tag 
                  v-if="Math.abs(slotProps.data.growthRate) > 0.01"
                  :value="slotProps.data.growthRate.toFixed(1) + '%'" 
                  :severity="slotProps.data.growthRate > 0 ? 'danger' : 'success'"
                  class="font-mono text-[10px] rounded-lg"
                />
                <span v-else class="text-slate-300">-</span>
              </template>
            </Column>
          </DataTable>
        </template>
      </Card>
    </div>

    <!-- Results empty (No differences found) -->
    <div v-else-if="hasCompared && diffData.length === 0" class="flex flex-col items-center justify-center py-20 bg-white rounded-3xl border border-dashed border-slate-200">
      <div class="p-4 bg-emerald-50 rounded-full mb-4 text-emerald-500">
        <CheckCircle2 class="w-12 h-12" />
      </div>
      <h3 class="text-xl font-bold text-slate-900">{{ t('diff.noDiff') }}</h3>
      <p class="text-slate-500 mt-1 max-w-sm text-center">{{ t('diff.noDiffDesc') }}</p>
    </div>

    <!-- Selection Guide -->
    <div v-else-if="!isLoading && !selectedConn" class="flex flex-col items-center justify-center py-20 bg-white rounded-3xl border border-dashed border-slate-200">
      <div class="p-4 bg-slate-50 rounded-full mb-4">
        <GitCompare class="w-12 h-12 text-slate-300" />
      </div>
      <h3 class="text-xl font-bold text-slate-900">{{ t('diff.selectTwo') }}</h3>
      <p class="text-slate-500 mt-1 max-w-sm text-center">{{ t('diff.selectTwoDesc') }}</p>
    </div>
    
    <div v-else-if="!isLoading && selectedConn && !hasCompared" class="flex flex-col items-center justify-center py-20 bg-white rounded-3xl border border-dashed border-slate-200">
      <div class="p-4 bg-slate-50 rounded-full mb-4 text-indigo-500">
        <Layers class="w-12 h-12" />
      </div>
      <h3 class="text-xl font-bold text-slate-900">{{ t('diff.selectTwo') }}</h3>
      <p class="text-slate-500 mt-1 max-w-sm text-center">{{ t('diff.selectTwoDesc') }}</p>
    </div>
  </div>
</template>

<style scoped>
:deep(.p-select-label) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
