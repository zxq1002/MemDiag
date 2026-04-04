<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConnectionStore } from '../stores/connectionStore'
import { useDiagnosis } from '../composables/useDiagnosis'
import { Stethoscope, RefreshCw } from 'lucide-vue-next'
import Button from 'primevue/button'
import Select from 'primevue/select'
import ProgressBar from 'primevue/progressbar'

// Child components
import DiagnosisSummary from '../components/diagnosis/DiagnosisSummary.vue'
import DiagnosisIssues from '../components/diagnosis/DiagnosisIssues.vue'

const { t } = useI18n()
const connectionStore = useConnectionStore()
const { 
  diagnosisData, 
  filteredIssues, 
  severityFilter,
  isLoading, 
  loadDiagnosis 
} = useDiagnosis()

// Global Sync
const selectedConn = computed({
  get: () => connectionStore.currentConnectionId,
  set: (val) => connectionStore.setCurrentConnection(val)
})

const connections = computed(() => {
  return Object.entries(connectionStore.connections).map(([id, status]) => ({ id, label: id }))
})

const refresh = () => {
  if (selectedConn.value) {
    loadDiagnosis(selectedConn.value)
  }
}

onMounted(() => {
  if (selectedConn.value) refresh()
})

watch(selectedConn, (newVal) => {
  if (newVal) {
    refresh()
  } else {
    diagnosisData.value = null
  }
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-3xl font-bold text-slate-900 tracking-tight">{{ t('common.diagnosis') }}</h1>
        <p class="text-slate-500 mt-1 text-sm">{{ t('threads.subtitle') }}</p>
      </div>
    </div>

    <!-- Controls Bar -->
    <div class="bg-white p-3 rounded-2xl shadow-sm border border-slate-100 flex flex-col lg:flex-row gap-4 items-stretch lg:items-center">
      <!-- Target Selection -->
      <div class="flex items-center gap-3 lg:border-r lg:border-slate-100 lg:pr-6">
        <div class="p-2 bg-indigo-50 rounded-lg text-indigo-600">
          <Stethoscope class="w-4 h-4" />
        </div>
        <Select 
          v-model="selectedConn" 
          :options="connections" 
          optionLabel="label" 
          optionValue="id" 
          :placeholder="t('common.selectConnection')" 
          class="flex-1 lg:w-48 border-0 shadow-none bg-slate-50 rounded-xl"
        />
      </div>

      <div class="flex-1"></div>

      <!-- Action Button -->
      <Button 
        size="small" 
        @click="refresh" 
        :loading="isLoading" 
        class="rounded-xl font-bold px-6 min-w-[160px]"
      >
        <template #icon><RefreshCw :class="['w-4 h-4 mr-2', isLoading ? 'animate-spin' : '']" /></template>
        {{ t('common.refresh') }}
      </Button>
    </div>

    <!-- Loading State -->
    <ProgressBar v-if="isLoading" mode="indeterminate" style="height: 4px" class="rounded-full overflow-hidden" />

    <!-- Content -->
    <div v-if="diagnosisData" class="space-y-8">
      <DiagnosisSummary 
        :totalHeapUsed="diagnosisData.totalHeapUsed"
        :totalHeapCommitted="diagnosisData.totalHeapCommitted"
        :threadCount="diagnosisData.threadCount"
        :issueCount="diagnosisData.issues?.length || 0"
      />

      <DiagnosisIssues 
        :issues="filteredIssues"
        v-model:severityFilter="severityFilter"
      />
    </div>

    <!-- Empty State -->
    <div v-else-if="!isLoading && !selectedConn" class="flex flex-col items-center justify-center py-20 bg-white rounded-3xl border border-dashed border-slate-200">
      <div class="p-4 bg-slate-50 rounded-full mb-4">
        <Stethoscope class="w-12 h-12 text-slate-300" />
      </div>
      <h3 class="text-xl font-bold text-slate-900">{{ t('common.diagnosis') }}</h3>
      <p class="text-slate-500 mt-1 max-w-sm text-center">{{ t('diff.selectTwoDesc') }}</p>
    </div>
  </div>
</template>

<style scoped>
:deep(.p-accordion-tab) {
  margin-bottom: 0.75rem;
}
</style>
