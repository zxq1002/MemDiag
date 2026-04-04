<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConnectionStore } from '../stores/connectionStore'
import { useSnapshots } from '../composables/useSnapshots'
import { 
  Camera, 
  RefreshCw, 
  Trash2, 
  FileSearch, 
  History,
  Clock,
  HardDrive,
  Plus,
  Monitor
} from 'lucide-vue-next'
import Button from 'primevue/button'
import Select from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import ProgressBar from 'primevue/progressbar'
import { useToast } from 'primevue/usetoast'

const { t } = useI18n()
const connectionStore = useConnectionStore()
const { 
  snapshots, 
  isLoading, 
  loadSnapshots, 
  createSnapshot, 
  deleteSnapshot 
} = useSnapshots()

const toast = useToast()

// Global Sync
const selectedConn = computed({
  get: () => connectionStore.currentConnectionId,
  set: (val) => connectionStore.setCurrentConnection(val)
})

const newSnapshotName = ref('')

const connections = computed(() => {
  return Object.entries(connectionStore.connections).map(([id, status]) => ({ id, label: id }))
})

const refresh = () => {
  if (selectedConn.value) {
    loadSnapshots(selectedConn.value)
  }
}

const handleCreate = async () => {
  if (!selectedConn.value) return
  const result = await createSnapshot(selectedConn.value, newSnapshotName.value)
  if (result.success) {
    toast.add({ severity: 'success', summary: t('common.success'), detail: t('snapshots.captureHistory'), life: 3000 })
    newSnapshotName.value = ''
  } else {
    toast.add({ severity: 'error', summary: t('common.error'), detail: result.error, life: 5000 })
  }
}

const handleDelete = async (id) => {
  if (!selectedConn.value) return
  const result = await deleteSnapshot(selectedConn.value, id)
  if (result.success) {
    toast.add({ severity: 'info', summary: t('common.success'), detail: t('common.delete'), life: 3000 })
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

onMounted(() => {
  if (selectedConn.value) refresh()
})

watch(selectedConn, (newVal) => {
  if (newVal) {
    refresh()
  } else {
    snapshots.value = []
  }
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-3xl font-bold text-slate-900 tracking-tight">{{ t('snapshots.title') }}</h1>
        <p class="text-slate-500 mt-1 text-sm">{{ t('snapshots.subtitle') }}</p>
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
        class="rounded-xl font-bold px-6 min-w-[120px]"
      >
        <template #icon><RefreshCw :class="['w-4 h-4 mr-2', isLoading ? 'animate-spin' : '']" /></template>
        {{ t('common.refresh') }}
      </Button>
    </div>

    <ProgressBar v-if="isLoading" mode="indeterminate" style="height: 4px" class="rounded-full overflow-hidden" />

    <div v-if="selectedConn" class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <!-- Snapshot List -->
      <div class="lg:col-span-2">
        <Card class="border-0 shadow-sm overflow-hidden rounded-3xl">
          <template #title>
            <div class="flex items-center gap-2 text-lg">
              <History class="w-5 h-5 text-indigo-600" />
              <span>{{ t('snapshots.captureHistory') }}</span>
            </div>
          </template>
          <template #content>
            <DataTable 
              :value="snapshots" 
              paginator :rows="10" 
              class="p-datatable-sm"
              removableSort
            >
              <template #empty>
                <div class="text-center py-12">
                  <Camera class="w-12 h-12 text-slate-200 mx-auto mb-3" />
                  <p class="text-slate-400">{{ t('dashboard.noConnections') }}</p>
                </div>
              </template>

              <Column field="id" :header="t('common.id')" sortable>
                <template #body="slotProps">
                  <code class="text-xs font-bold text-indigo-600 bg-indigo-50 px-2 py-1 rounded">{{ slotProps.data.id }}</code>
                </template>
              </Column>
              <Column field="name" :header="t('common.name')" sortable>
                <template #body="slotProps">
                  <span class="font-medium text-slate-700">{{ slotProps.data.name || slotProps.data.id }}</span>
                </template>
              </Column>
              <Column field="createdAt" :header="t('common.createdAt')" sortable>
                <template #body="slotProps">
                  <div class="flex items-center gap-2 text-slate-500 text-sm">
                    <Clock class="w-3.5 h-3.5" />
                    {{ new Date(slotProps.data.createdAt).toLocaleString() }}
                  </div>
                </template>
              </Column>
              <Column field="size" :header="t('common.size')" sortable class="text-right">
                <template #body="slotProps">
                  <span class="font-mono text-sm">{{ formatSize(slotProps.data.size) }}</span>
                </template>
              </Column>
              <Column :header="t('common.actions')" class="text-right">
                <template #body="slotProps">
                  <div class="flex justify-end gap-2">
                    <Button 
                      size="small" 
                      icon="FileSearch" 
                      severity="primary" 
                      text 
                      rounded 
                      v-tooltip="t('snapshots.viewAnalysis')"
                    >
                      <template #icon><FileSearch class="w-4 h-4" /></template>
                    </Button>
                    <Button 
                      size="small" 
                      icon="Trash2" 
                      severity="danger" 
                      text 
                      rounded 
                      @click="handleDelete(slotProps.data.id)"
                    >
                      <template #icon><Trash2 class="w-4 h-4" /></template>
                    </Button>
                  </div>
                </template>
              </Column>
            </DataTable>
          </template>
        </Card>
      </div>

      <!-- Create Snapshot Form -->
      <div class="space-y-6">
        <Card class="border-0 shadow-sm border-t-4 border-t-indigo-600 rounded-3xl">
          <template #title>
            <div class="flex items-center gap-2 text-lg">
              <Plus class="w-5 h-5 text-indigo-600" />
              <span>{{ t('snapshots.takeNew') }}</span>
            </div>
          </template>
          <template #content>
            <div class="space-y-4">
              <p class="text-xs text-slate-500 leading-relaxed">
                {{ t('snapshots.subtitle') }}
              </p>
              <div class="flex flex-col gap-2">
                <label class="text-sm font-semibold text-slate-700">{{ t('snapshots.optionalName') }}</label>
                <InputText v-model="newSnapshotName" :placeholder="t('snapshots.optionalName')" class="rounded-xl bg-slate-50 border-slate-100" />
              </div>
              <Button 
                :label="t('snapshots.captureNow')" 
                class="w-full mt-2 rounded-xl font-bold shadow-md" 
                icon="Camera"
                @click="handleCreate"
                :loading="isLoading"
              >
                <template #icon><Camera class="w-4 h-4 mr-2" /></template>
              </Button>
            </div>
          </template>
        </Card>

        <!-- Stats Card -->
        <div class="p-6 bg-slate-900 rounded-3xl text-white relative overflow-hidden">
          <div class="relative z-10">
            <h4 class="font-bold text-lg mb-4 flex items-center gap-2">
              <HardDrive class="w-5 h-5 text-indigo-400" />
              {{ t('snapshots.storage') }}
            </h4>
            <div class="space-y-4">
              <div class="flex justify-between items-end">
                <span class="text-sm text-slate-400">{{ t('snapshots.usedSpace') }}</span>
                <span class="text-xl font-bold">{{ formatSize(snapshots.reduce((acc, s) => acc + (s.size || 0), 0)) }}</span>
              </div>
              <div class="w-full h-2 bg-white/10 rounded-full overflow-hidden">
                <div class="h-full bg-indigo-500 rounded-full" style="width: 15%"></div>
              </div>
              <p class="text-[10px] text-slate-500">{{ t('snapshots.storageHint') }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="flex flex-col items-center justify-center py-20 bg-white rounded-3xl border border-dashed border-slate-200">
      <div class="p-4 bg-slate-50 rounded-full mb-4">
        <Camera class="w-12 h-12 text-slate-300" />
      </div>
      <h3 class="text-xl font-bold text-slate-900">{{ t('snapshots.title') }}</h3>
      <p class="text-slate-500 mt-1 max-w-sm text-center">{{ t('diff.selectTwoDesc') }}</p>
    </div>
  </div>
</template>
