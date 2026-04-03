<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useConnectionStore } from '../stores/connectionStore'
import { useHistogram } from '../composables/useHistogram'
import { RefreshCw, Monitor, ListFilter } from 'lucide-vue-next'
import Button from 'primevue/button'
import Select from 'primevue/select'
import InputNumber from 'primevue/inputnumber'
import ProgressBar from 'primevue/progressbar'

// Child components
import HistogramSummary from '../components/histogram/HistogramSummary.vue'
import HistogramChart from '../components/histogram/HistogramChart.vue'
import HistogramTable from '../components/histogram/HistogramTable.vue'

const connectionStore = useConnectionStore()
const { 
  histogram, 
  isLoading, 
  loadHistogram, 
  formatBytes, 
  formatNumber 
} = useHistogram()

// Bind directly to store
const selectedConn = computed({
  get: () => connectionStore.currentConnectionId,
  set: (val) => connectionStore.setCurrentConnection(val)
})

const limit = ref(20)

const connections = computed(() => {
  return Object.entries(connectionStore.connections).map(([id, status]) => ({ id, label: id }))
})

const classStats = computed(() => {
  const data = histogram.value?.data || histogram.value;
  return data?.classes || data?.classStats || [];
})

const totalObjects = computed(() => {
  const data = histogram.value?.data || histogram.value;
  return data?.totalObjects || 0;
})

const totalBytes = computed(() => {
  const data = histogram.value?.data || histogram.value;
  return data?.totalBytes || 0;
})

const refresh = () => {
  if (selectedConn.value) {
    loadHistogram(selectedConn.value, limit.value)
  }
}

onMounted(() => {
  if (selectedConn.value) refresh()
})

// Watch for changes in selectedConn (even from other pages)
watch(selectedConn, (newVal) => {
  if (newVal) {
    refresh()
  } else {
    histogram.value = null
  }
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-3xl font-bold text-slate-900 tracking-tight">Heap Histogram</h1>
        <p class="text-slate-500 mt-1 text-sm">Real-time object distribution and memory usage.</p>
      </div>
    </div>

    <!-- Controls Bar -->
    <div class="bg-white p-3 rounded-2xl shadow-sm border border-slate-100 flex flex-col lg:flex-row gap-4 items-stretch lg:items-center">
      <!-- Target Selection -->
      <div class="flex items-center gap-3 lg:border-r lg:border-slate-100 lg:pr-6">
        <div class="p-2 bg-indigo-50 rounded-lg text-indigo-600">
          <Monitor class="w-4 h-4" />
        </div>
        <Select 
          v-model="selectedConn" 
          :options="connections" 
          optionLabel="label" 
          optionValue="id" 
          placeholder="Select Connection" 
          class="flex-1 lg:w-48 border-0 shadow-none bg-slate-50 rounded-xl"
        />
      </div>

      <!-- Settings Group -->
      <div class="flex-1 flex items-center gap-4 px-2">
        <div class="flex items-center gap-3 flex-1 sm:flex-none">
          <div class="p-2 bg-slate-100 rounded-lg text-slate-500">
            <ListFilter class="w-4 h-4" />
          </div>
          <span class="text-xs font-bold text-slate-400 uppercase tracking-widest hidden sm:inline">Limit</span>
          <InputNumber v-model="limit" :min="1" :max="1000" showButtons class="w-full sm:w-32" inputClass="bg-slate-50 border-0 rounded-l-xl" />
        </div>
      </div>

      <!-- Action Button -->
      <Button 
        size="small" 
        @click="refresh" 
        :loading="isLoading" 
        class="rounded-xl font-bold px-6 min-w-[120px] flex-shrink-0"
      >
        <template #icon><RefreshCw :class="['w-4 h-4 mr-2', isLoading ? 'animate-spin' : '']" /></template>
        Refresh
      </Button>
    </div>

    <!-- Loading State -->
    <ProgressBar v-if="isLoading" mode="indeterminate" style="height: 4px" class="rounded-full overflow-hidden" />

    <!-- Content -->
    <div v-if="histogram" class="space-y-6">
      <HistogramSummary 
        :totalObjects="totalObjects" 
        :totalBytes="totalBytes"
        :formatNumber="formatNumber"
        :formatBytes="formatBytes"
      />

      <div class="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <div class="xl:col-span-1">
          <HistogramChart 
            :classStats="classStats" 
            :formatBytes="formatBytes" 
          />
        </div>
        <div class="xl:col-span-2">
          <HistogramTable 
            :classStats="classStats"
            :formatNumber="formatNumber"
            :formatBytes="formatBytes"
          />
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!isLoading && !selectedConn" class="flex flex-col items-center justify-center py-20 bg-white rounded-3xl border border-dashed border-slate-200">
      <div class="p-4 bg-slate-50 rounded-full mb-4">
        <Monitor class="w-12 h-12 text-slate-300" />
      </div>
      <h3 class="text-xl font-bold text-slate-900">No Connection Selected</h3>
      <p class="text-slate-500 mt-1 max-w-sm text-center">Please select a JVM connection from the Dashboard or the dropdown above.</p>
    </div>
  </div>
</template>
