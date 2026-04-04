<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDashboardStore } from '../stores/dashboardStore'
import { useConnectionStore } from '../stores/connectionStore'
import { 
  Plus, 
  RefreshCw, 
  Trash2, 
  Monitor, 
  Cpu, 
  Network,
  CheckCircle2,
  AlertCircle,
  Check
} from 'lucide-vue-next'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Card from 'primevue/card'
import Tag from 'primevue/tag'

const { t } = useI18n()
const dashboardStore = useDashboardStore()
const connectionStore = useConnectionStore()

const newConnId = ref('')
const newConnTarget = ref('')

onMounted(() => {
  dashboardStore.loadConnections()
})

const handleConnect = async () => {
  if (!newConnId.value) return
  const result = await dashboardStore.connect(newConnId.value, newConnTarget.value)
  if (result.success) {
    newConnId.value = ''
    newConnTarget.value = ''
  }
}

const getSeverity = (state) => {
  switch (state) {
    case 'connected': return 'success'
    case 'error': return 'danger'
    default: return 'info'
  }
}

const selectConnection = (id) => {
  connectionStore.setCurrentConnection(id)
}
</script>

<template>
  <div class="space-y-8">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-3xl font-bold text-slate-900 tracking-tight">{{ t('dashboard.title') }}</h1>
        <p class="text-slate-500 mt-1">{{ t('dashboard.subtitle') }}</p>
      </div>
      <Button 
        icon="RefreshCw" 
        severity="secondary" 
        rounded 
        text 
        @click="dashboardStore.loadConnections"
        :loading="dashboardStore.isLoading"
      >
        <template #icon>
          <RefreshCw :class="['w-5 h-5', dashboardStore.isLoading ? 'animate-spin' : '']" />
        </template>
      </Button>
    </div>

    <!-- Quick Stats -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <Card class="border-0 shadow-sm overflow-hidden rounded-3xl">
        <template #content>
          <div class="flex items-center gap-4">
            <div class="p-3 bg-indigo-50 rounded-xl text-indigo-600">
              <Monitor class="w-6 h-6" />
            </div>
            <div>
              <p class="text-sm font-medium text-slate-500 uppercase tracking-wider">{{ t('dashboard.activeConnections') }}</p>
              <p class="text-2xl font-bold text-slate-900">{{ dashboardStore.connections.length }}</p>
            </div>
          </div>
        </template>
      </Card>
      
      <Card class="border-0 shadow-sm overflow-hidden rounded-3xl">
        <template #content>
          <div class="flex items-center gap-4">
            <div class="p-3 bg-emerald-50 rounded-xl text-emerald-600">
              <CheckCircle2 class="w-6 h-6" />
            </div>
            <div>
              <p class="text-sm font-medium text-slate-500 uppercase tracking-wider">{{ t('dashboard.systemStatus') }}</p>
              <p class="text-2xl font-bold text-slate-900">{{ t('dashboard.healthy') }}</p>
            </div>
          </div>
        </template>
      </Card>

      <Card class="border-0 shadow-sm overflow-hidden rounded-3xl">
        <template #content>
          <div class="flex items-center gap-4">
            <div class="p-3 bg-amber-50 rounded-xl text-amber-600">
              <Cpu class="w-6 h-6" />
            </div>
            <div>
              <p class="text-sm font-medium text-slate-500 uppercase tracking-wider">{{ t('dashboard.agentNodes') }}</p>
              <p class="text-2xl font-bold text-slate-900">
                {{ dashboardStore.connections.filter(c => c.type === 'agent').length }}
              </p>
            </div>
          </div>
        </template>
      </Card>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <!-- Connection List -->
      <div class="lg:col-span-2 space-y-4">
        <Card class="border-0 shadow-sm h-full rounded-3xl overflow-hidden">
          <template #title>
            <div class="flex items-center gap-2 text-lg">
              <Network class="w-5 h-5 text-indigo-600" />
              <span>{{ t('dashboard.liveConnections') }}</span>
            </div>
          </template>
          <template #content>
            <DataTable 
              :value="dashboardStore.connections" 
              class="p-datatable-sm" 
              responsiveLayout="stack"
              breakpoint="960px"
              rowHover
            >
              <template #empty>
                <div class="text-center py-8">
                  <AlertCircle class="w-12 h-12 text-slate-300 mx-auto mb-3" />
                  <p class="text-slate-500">{{ t('dashboard.noConnections') }}</p>
                </div>
              </template>
              
              <Column field="id" :header="t('common.id')">
                <template #body="slotProps">
                  <div class="flex items-center gap-2">
                    <span :class="['font-semibold', connectionStore.currentConnectionId === slotProps.data.id ? 'text-indigo-600' : 'text-slate-700']">
                      {{ slotProps.data.id }}
                    </span>
                    <Check v-if="connectionStore.currentConnectionId === slotProps.data.id" class="w-4 h-4 text-emerald-500" />
                  </div>
                </template>
              </Column>
              <Column field="type" :header="t('common.type')">
                <template #body="slotProps">
                  <span class="capitalize text-sm text-slate-500">{{ slotProps.data.type }}</span>
                </template>
              </Column>
              <Column field="state" :header="t('common.status')">
                <template #body="slotProps">
                  <Tag :value="slotProps.data.state" :severity="getSeverity(slotProps.data.state)" />
                </template>
              </Column>
              <Column :header="t('common.actions')" class="text-right">
                <template #body="slotProps">
                  <div class="flex justify-end gap-2">
                    <Button 
                      v-if="connectionStore.currentConnectionId !== slotProps.data.id"
                      size="small" 
                      :label="t('dashboard.selectBtn')" 
                      severity="primary" 
                      text 
                      outlined
                      class="rounded-lg py-1 px-3"
                      @click="selectConnection(slotProps.data.id)"
                    />
                    <Button 
                      v-else
                      size="small" 
                      :label="t('dashboard.activeBtn')" 
                      severity="success" 
                      class="rounded-lg py-1 px-3 shadow-sm"
                      disabled
                    />
                    <Button 
                      size="small" 
                      icon="Trash2" 
                      severity="danger" 
                      text 
                      rounded 
                      @click="dashboardStore.disconnect(slotProps.data.id)"
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

      <!-- Connect Form -->
      <div class="space-y-4">
        <Card class="border-0 shadow-sm border-t-4 border-t-indigo-600 rounded-3xl">
          <template #title>
            <div class="flex items-center gap-2 text-lg">
              <Plus class="w-5 h-5 text-indigo-600" />
              <span>{{ t('dashboard.newConnection') }}</span>
            </div>
          </template>
          <template #content>
            <div class="space-y-4">
              <div class="flex flex-col gap-2">
                <label for="conn-id" class="text-sm font-semibold text-slate-700">{{ t('dashboard.displayId') }}</label>
                <InputText id="conn-id" v-model="newConnId" :placeholder="t('dashboard.displayId')" class="rounded-xl bg-slate-50 border-slate-100" />
              </div>
              <div class="flex flex-col gap-2">
                <label for="conn-target" class="text-sm font-semibold text-slate-700">{{ t('dashboard.targetPidOrAgent') }}</label>
                <InputText id="conn-target" v-model="newConnTarget" :placeholder="t('dashboard.targetPidOrAgent')" class="rounded-xl bg-slate-50 border-slate-100" />
                <p class="text-[10px] text-slate-400 italic">{{ t('dashboard.currentJvmHint') }}</p>
              </div>
              <Button 
                :label="t('dashboard.connectBtn')" 
                class="w-full mt-2 rounded-xl font-bold shadow-md" 
                @click="handleConnect"
                :loading="dashboardStore.isLoading"
              />
            </div>
          </template>
        </Card>

        <div class="p-6 bg-slate-900 rounded-3xl text-white overflow-hidden relative group">
          <div class="relative z-10">
            <h4 class="font-bold text-lg mb-2">{{ t('dashboard.needHelp') }}</h4>
            <p class="text-slate-400 text-sm mb-4 leading-relaxed">
              {{ t('dashboard.helpDesc') }}
            </p>
            <a href="https://github.com/zxq1002/MemDiag" target="_blank" class="text-indigo-400 text-sm font-bold flex items-center gap-1 group-hover:gap-2 transition-all">
              {{ t('dashboard.readDocs') }} <Plus class="w-4 h-4 rotate-45" />
            </a>
          </div>
          <Activity class="absolute -bottom-4 -right-4 w-32 h-32 text-white/5 -rotate-12" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.p-datatable-thead > tr > th) {
  background-color: #f8f9fa;
  color: #64748b;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 1rem;
}

:deep(.p-datatable-tbody > tr.p-highlight) {
  background-color: #f0f7ff;
}
</style>
