<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'
import axios from 'axios'
import { useConnectionStore } from '../stores/connectionStore'
import { 
  Layers, 
  RefreshCw, 
  PieChart as PieChartIcon, 
  Table as TableIcon,
  AlertTriangle,
  Flag,
  Monitor,
  ListFilter,
  History
} from 'lucide-vue-next'
import Button from 'primevue/button'
import Select from 'primevue/select'
import ToggleSwitch from 'primevue/toggleswitch'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Card from 'primevue/card'
import ProgressBar from 'primevue/progressbar'
import Message from 'primevue/message'
import Tag from 'primevue/tag'

const { t } = useI18n()
const connectionStore = useConnectionStore()

// Global Sync
const selectedConn = computed({
  get: () => connectionStore.currentConnectionId,
  set: (val) => connectionStore.setCurrentConnection(val)
})

const detailMode = ref(false)
const snapshot = ref(null) 
const baseline = ref(null)
const isLoading = ref(false)
const error = ref(null)
const chartRef = ref(null)
const chart = ref(null)

const connections = computed(() => {
  return Object.entries(connectionStore.connections).map(([id, status]) => ({ id, label: id }))
})

const nmtData = computed(() => snapshot.value?.data || snapshot.value)
const baselineData = computed(() => baseline.value?.data || baseline.value)

const usages = computed(() => {
  const data = nmtData.value
  return data?.categories || data?.usages || []
})

const usagesWithDelta = computed(() => {
  const currentUsages = usages.value
  const base = baselineData.value
  const baseUsages = base?.categories || base?.usages || []

  if (!base || baseUsages.length === 0) {
    return currentUsages.map(u => ({ ...u, deltaCommitted: 0 }))
  }

  const baseMap = new Map(baseUsages.map(u => [u.name || u.category?.name || u.category, u]))
  
  return currentUsages.map(u => {
    const key = u.name || u.category?.name || u.category
    const b = baseMap.get(key)
    return {
      ...u,
      deltaCommitted: b ? u.committed - b.committed : 0
    }
  }).sort((a, b) => b.committed - a.committed)
})

const loadNmt = async () => {
  if (!selectedConn.value) return
  isLoading.value = true
  error.value = null
  try {
    const response = await axios.get(`/api/v1/nmt/${selectedConn.value}`, { 
      params: { detail: detailMode.value } 
    })
    snapshot.value = response.data
    await nextTick()
    renderChart()
  } catch (e) {
    console.error('Failed to load NMT:', e)
    error.value = t('nmt.error')
  } finally {
    isLoading.value = false
  }
}

const setBaseline = () => {
  baseline.value = JSON.parse(JSON.stringify(snapshot.value))
}

const clearBaseline = () => {
  baseline.value = null
}

const renderChart = () => {
  if (chart.value) chart.value.dispose()
  if (!chartRef.value || usages.value.length === 0) return

  chart.value = echarts.init(chartRef.value)

  const data = [...usages.value]
    .sort((a, b) => b.committed - a.committed)
    .slice(0, 8)
    .map(u => ({
      name: u.name || u.category?.displayName || u.category,
      value: u.committed
    }))

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        return `<div class="font-sans">
          <div class="text-xs text-slate-500 mb-1">${params.name}</div>
          <div class="font-bold text-indigo-600">${formatBytes(params.value)}</div>
          <div class="text-[10px] text-slate-400 mt-1">${params.percent}% of total</div>
        </div>`
      }
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'middle',
      icon: 'circle',
      textStyle: { fontSize: 11, color: '#64748b' }
    },
    series: [{
      type: 'pie',
      radius: ['50%', '80%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold', formatter: '{b}' } },
      data: data
    }]
  }

  chart.value.setOption(option)
}

const formatBytes = (bytes) => {
  if (!bytes && bytes !== 0) return '-'
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const sign = bytes < 0 ? '-' : ''
  const absBytes = Math.abs(bytes)
  const i = Math.floor(Math.log(absBytes) / Math.log(k))
  return sign + parseFloat((absBytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

onMounted(() => {
  if (selectedConn.value) loadNmt()
  window.addEventListener('resize', () => chart.value?.resize())
})

onBeforeUnmount(() => {
  chart.value?.dispose()
})

watch(selectedConn, (newVal) => {
  if (newVal) {
    loadNmt()
    baseline.value = null
  }
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-3xl font-bold text-slate-900 tracking-tight">{{ t('nmt.title') }}</h1>
        <p class="text-slate-500 mt-1 text-sm">{{ t('nmt.subtitle') }}</p>
      </div>
      
      <div class="flex items-center gap-3 bg-white p-2 rounded-2xl shadow-sm border border-slate-100">
        <Select 
          v-model="selectedConn" 
          :options="connections" 
          optionLabel="label" 
          optionValue="id" 
          :placeholder="t('common.selectConnection')" 
          class="w-48 border-0 shadow-none bg-slate-50 rounded-xl font-medium"
        />
        <Button 
          size="small" 
          @click="loadNmt" 
          :loading="isLoading" 
          class="rounded-xl font-bold px-6 min-w-[120px]"
        >
          <template #icon><RefreshCw :class="['w-4 h-4 mr-2', isLoading ? 'animate-spin' : '']" /></template>
          {{ t('common.refresh') }}
        </Button>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="flex flex-wrap items-center gap-4 bg-slate-100/50 p-2 rounded-2xl border border-slate-200/50 min-h-[64px]">
      <div class="flex items-center gap-3 bg-white px-4 h-12 rounded-xl border border-slate-200 shadow-sm">
        <ListFilter class="w-4 h-4 text-slate-400" />
        <span class="text-[10px] font-black text-slate-500 uppercase tracking-widest">{{ t('nmt.detailMode') }}</span>
        <ToggleSwitch v-model="detailMode" @change="loadNmt" />
      </div>

      <div class="flex-1"></div>

      <div v-if="nmtData" class="flex items-center h-12 gap-2">
        <div v-if="!baseline" class="flex items-center bg-white h-full px-2 rounded-xl border border-slate-200 shadow-sm">
          <Button 
            size="small" 
            severity="primary" 
            text 
            @click="setBaseline"
            class="font-bold px-4 hover:bg-indigo-50"
          >
            <template #icon><Flag class="w-4 h-4 mr-2" /></template>
            {{ t('nmt.setBaseline') }}
          </Button>
        </div>
        <div v-else class="flex items-center bg-amber-50 h-full px-4 rounded-xl border border-amber-100 shadow-sm gap-3">
          <div class="flex items-center gap-2">
            <History class="w-4 h-4 text-amber-600" />
            <span class="text-[10px] font-black text-amber-700 uppercase tracking-widest">{{ t('nmt.baselineActive') }}</span>
          </div>
          <div class="w-px h-4 bg-amber-200"></div>
          <Button 
            size="small" 
            severity="warn" 
            text 
            @click="clearBaseline"
            class="font-bold px-2 py-1 text-[11px] hover:bg-white rounded-lg"
          >
            {{ t('nmt.clearBaseline') }}
          </Button>
        </div>
      </div>
    </div>

    <ProgressBar v-if="isLoading" mode="indeterminate" style="height: 4px" class="rounded-full overflow-hidden" />

    <Message v-if="error" severity="error" variant="simple" class="rounded-2xl">
      <template #message>
        <div class="flex items-center gap-3">
          <AlertTriangle class="w-5 h-5" />
          <span>{{ error }}</span>
        </div>
      </template>
    </Message>

    <div v-if="nmtData" class="space-y-6">
      <!-- Summary Cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card class="border-0 shadow-sm border-l-4 border-l-indigo-500 rounded-3xl">
          <template #content>
            <p class="text-xs font-bold text-slate-400 uppercase mb-1">{{ t('nmt.totalCommitted') }}</p>
            <p class="text-2xl font-black text-slate-900">{{ formatBytes(nmtData.totalCommitted) }}</p>
            <p v-if="baselineData" class="text-xs font-bold mt-1" :class="nmtData.totalCommitted - baselineData.totalCommitted > 0 ? 'text-red-500' : 'text-emerald-500'">
              {{ formatBytes(nmtData.totalCommitted - baselineData.totalCommitted) }}
            </p>
          </template>
        </Card>
        <Card class="border-0 shadow-sm border-l-4 border-l-emerald-500 rounded-3xl">
          <template #content>
            <p class="text-xs font-bold text-slate-400 uppercase mb-1">{{ t('nmt.totalReserved') }}</p>
            <p class="text-2xl font-black text-slate-900">{{ formatBytes(nmtData.totalReserved) }}</p>
          </template>
        </Card>
        <Card class="border-0 shadow-sm border-l-4 border-l-amber-500 rounded-3xl">
          <template #content>
            <p class="text-xs font-bold text-slate-400 uppercase mb-1">{{ t('nmt.established') }}</p>
            <p class="text-sm font-bold text-slate-600 flex items-center gap-2">
              <span v-if="baselineData" class="text-amber-600 flex items-center gap-1">
                <Flag class="w-3.5 h-3.5" /> {{ t('nmt.baselineActive') }}
              </span>
              <span v-else class="text-slate-400 italic">{{ t('nmt.notSet') }}</span>
            </p>
          </template>
        </Card>
        <Card class="border-0 shadow-sm border-l-4 border-l-slate-400 rounded-3xl">
          <template #content>
            <p class="text-xs font-bold text-slate-400 uppercase mb-1">{{ t('nmt.lastUpdate') }}</p>
            <p class="text-lg font-bold text-slate-600">{{ new Date().toLocaleTimeString() }}</p>
          </template>
        </Card>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-5 gap-6">
        <!-- Chart -->
        <Card class="lg:col-span-2 border-0 shadow-sm rounded-3xl overflow-hidden">
          <template #title>
            <div class="flex items-center gap-2 text-lg">
              <PieChartIcon class="w-5 h-5 text-indigo-600" />
              <span>{{ t('nmt.categoryBreakdown') }}</span>
            </div>
          </template>
          <template #content>
            <div ref="chartRef" class="w-full h-[350px]"></div>
          </template>
        </Card>

        <!-- Categories Table -->
        <Card class="lg:col-span-3 border-0 shadow-sm overflow-hidden rounded-3xl">
          <template #title>
            <div class="flex items-center gap-2 text-lg">
              <TableIcon class="w-5 h-5 text-indigo-600" />
              <span>{{ t('nmt.usageDetails') }}</span>
            </div>
          </template>
          <template #content>
            <DataTable 
              :value="usagesWithDelta" 
              class="p-datatable-sm" 
              removableSort
              paginator :rows="8"
            >
              <Column field="name" :header="t('common.type')" sortable>
                <template #body="slotProps">
                  <span class="font-semibold text-slate-700">
                    {{ slotProps.data.name || slotProps.data.category?.displayName || slotProps.data.category }}
                  </span>
                </template>
              </Column>
              <Column field="committed" :header="t('common.status')" sortable class="text-right">
                <template #body="slotProps">
                  <span class="font-mono text-sm font-bold text-slate-700">{{ formatBytes(slotProps.data.committed) }}</span>
                </template>
              </Column>
              <Column field="deltaCommitted" :header="t('nmt.deltaBaseline')" sortable class="text-right" v-if="baselineData">
                <template #body="slotProps">
                  <Tag 
                    v-if="Math.abs(slotProps.data.deltaCommitted) > 1024"
                    :value="formatBytes(slotProps.data.deltaCommitted)" 
                    :severity="slotProps.data.deltaCommitted > 0 ? 'danger' : 'success'"
                    class="font-mono text-[10px] rounded-lg"
                  />
                  <span v-else class="text-slate-300">-</span>
                </template>
              </Column>
            </DataTable>
          </template>
        </Card>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!isLoading && !selectedConn" class="flex flex-col items-center justify-center py-20 bg-white rounded-3xl border border-dashed border-slate-200">
      <div class="p-4 bg-slate-50 rounded-full mb-4">
        <Layers class="w-12 h-12 text-slate-300" />
      </div>
      <h3 class="text-xl font-bold text-slate-900">{{ t('common.nmt') }}</h3>
      <p class="text-slate-500 mt-1 max-w-sm text-center">{{ t('diff.selectTwoDesc') }}</p>
    </div>
  </div>
</template>

<style scoped>
</style>
