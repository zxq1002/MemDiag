<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import ConnectionIndicator from './ConnectionIndicator.vue'
import { 
  LayoutDashboard, 
  Activity, 
  Stethoscope, 
  ListTree, 
  Layers, 
  Settings,
  Menu,
  X,
  Camera,
  GitCompare,
  Monitor,
  Clock,
  ExternalLink,
  Server,
  Globe
} from 'lucide-vue-next'
import Drawer from 'primevue/drawer'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import SelectButton from 'primevue/selectbutton'
import { useConnectionStore } from '../stores/connectionStore'
import { useSettingsStore } from '../stores/settingsStore'

const { t, locale } = useI18n()
const isSidebarOpen = ref(true)
const isSettingsOpen = ref(false)
const connectionStore = useConnectionStore()
const settingsStore = useSettingsStore()

const languageOptions = [
  { label: '中文', value: 'zh' },
  { label: 'English', value: 'en' }
]

const navItems = [
  { name: 'common.dashboard', path: '/', icon: LayoutDashboard },
  { name: 'common.histogram', path: '/histogram', icon: Activity },
  { name: 'common.snapshots', path: '/snapshots', icon: Camera },
  { name: 'common.comparison', path: '/diff', icon: GitCompare },
  { name: 'common.diagnosis', path: '/diagnose', icon: Stethoscope },
  { name: 'common.threads', path: '/threads', icon: ListTree },
  { name: 'common.nmt', path: '/nmt', icon: Layers },
]

const saveSettings = () => {
  isSettingsOpen.value = false
  connectionStore.fetchConnections()
}
</script>

<template>
  <div class="flex h-screen bg-slate-50 font-sans antialiased text-slate-900">
    <!-- Sidebar -->
    <aside 
      :class="[
        'fixed inset-y-0 left-0 z-50 w-64 bg-white border-r border-slate-200 transition-transform duration-300 ease-in-out lg:translate-x-0 lg:static lg:inset-0',
        isSidebarOpen ? 'translate-x-0' : '-translate-x-full'
      ]"
    >
      <div class="flex flex-col h-full">
        <!-- Logo -->
        <div class="flex items-center gap-3 px-6 py-8">
          <div class="flex items-center justify-center w-10 h-10 rounded-xl bg-indigo-600 text-white shadow-lg shadow-indigo-200">
            <Activity class="w-6 h-6" />
          </div>
          <span class="text-xl font-bold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-indigo-600 to-violet-600">
            MemDiag
          </span>
        </div>

        <!-- Nav -->
        <nav class="flex-1 px-4 space-y-1 overflow-y-auto">
          <router-link 
            v-for="item in navItems" 
            :key="item.path"
            :to="item.path"
            v-slot="{ isActive }"
            class="group"
          >
            <div 
              :class="[
                'flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200',
                isActive 
                  ? 'bg-indigo-50 text-indigo-700 shadow-sm' 
                  : 'text-slate-500 hover:bg-slate-50 hover:text-slate-900'
              ]"
            >
              <component :is="item.icon" :class="['w-5 h-5', isActive ? 'text-indigo-600' : 'text-slate-400 group-hover:text-slate-600']" />
              <span class="font-medium">{{ t(item.name) }}</span>
            </div>
          </router-link>
        </nav>

        <!-- Footer -->
        <div class="p-4 border-t border-slate-100">
          <div class="p-4 rounded-2xl bg-slate-50 border border-slate-100">
            <p class="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">{{ t('settings.version') }}</p>
            <p class="text-sm font-medium text-slate-600">v1.0.0-beta</p>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="flex-1 flex flex-col min-w-0 overflow-hidden">
      <!-- Topbar -->
      <header class="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-4 lg:px-8">
        <button 
          @click="isSidebarOpen = !isSidebarOpen"
          class="p-2 rounded-lg hover:bg-slate-100 lg:hidden text-slate-500"
        >
          <Menu v-if="!isSidebarOpen" class="w-6 h-6" />
          <X v-else class="w-6 h-6" />
        </button>

        <div class="flex items-center gap-4 ml-auto">
          <ConnectionIndicator />
          <div class="w-px h-6 bg-slate-200"></div>
          <button 
            @click="isSettingsOpen = true"
            class="p-2 rounded-full hover:bg-slate-100 text-slate-500 transition-colors"
          >
            <Settings class="w-5 h-5" />
          </button>
        </div>
      </header>

      <!-- Page Area -->
      <main class="flex-1 overflow-y-auto p-4 lg:p-8">
        <div class="max-w-7xl mx-auto">
          <slot />
        </div>
      </main>
    </div>

    <!-- Settings Drawer -->
    <Drawer v-model:visible="isSettingsOpen" :header="t('settings.title')" position="right" class="w-[400px]">
      <div class="p-2 space-y-8">
        <!-- Language -->
        <section class="space-y-4">
          <h3 class="flex items-center gap-2 text-xs font-black text-slate-400 uppercase tracking-widest">
            <Globe class="w-3.5 h-3.5" />
            Language / 语言
          </h3>
          <SelectButton 
            v-model="locale" 
            :options="languageOptions" 
            optionLabel="label" 
            optionValue="value" 
            aria-labelledby="basic"
            class="w-full"
          />
        </section>

        <!-- Backend Info -->
        <section class="space-y-4">
          <h3 class="flex items-center gap-2 text-xs font-black text-slate-400 uppercase tracking-widest">
            <Server class="w-3.5 h-3.5" />
            {{ t('settings.infrastructure') }}
          </h3>
          <div class="space-y-4">
            <div class="flex flex-col gap-2">
              <label class="text-xs font-bold text-slate-700 uppercase">{{ t('settings.apiBaseUrl') }}</label>
              <InputText 
                v-model="settingsStore.apiEndpoint" 
                @blur="settingsStore.setApiEndpoint($event.target.value)"
                placeholder="e.g. /api/v1" 
                class="w-full bg-slate-50 border-slate-200 rounded-xl text-sm font-mono" 
              />
              <p class="text-[10px] text-slate-400 leading-relaxed italic">
                {{ t('settings.apiHint') }}
              </p>
            </div>
            
            <div class="p-4 bg-slate-50 rounded-2xl border border-slate-100 flex items-center justify-between">
              <span class="text-xs text-slate-600 font-medium">{{ t('settings.connectivity') }}</span>
              <span class="flex items-center gap-1.5 text-xs font-bold text-emerald-500">
                <div class="w-1.5 h-1.5 rounded-full bg-current animate-pulse"></div>
                {{ t('settings.backendActive') }}
              </span>
            </div>
          </div>
        </section>

        <!-- Preferences -->
        <section class="space-y-4">
          <h3 class="flex items-center gap-2 text-xs font-black text-slate-400 uppercase tracking-widest">
            <Clock class="w-3.5 h-3.5" />
            {{ t('settings.performance') }}
          </h3>
          <div class="space-y-4">
            <div class="flex flex-col gap-2">
              <label class="text-xs font-bold text-slate-700 uppercase">{{ t('settings.pollingRate') }}</label>
              <InputNumber 
                v-model="settingsStore.refreshRate" 
                @update:modelValue="settingsStore.setRefreshRate"
                :min="1000" :max="60000" :step="1000" 
                showButtons 
                class="w-full" 
                inputClass="rounded-xl border-slate-200" 
              />
              <p class="text-[10px] text-slate-400 italic">{{ t('settings.pollingDesc') }}</p>
            </div>
          </div>
        </section>

        <!-- Support -->
        <section class="space-y-4 pt-4">
          <div class="p-5 bg-gradient-to-br from-indigo-600 to-violet-700 rounded-3xl text-white shadow-xl shadow-indigo-100">
            <h4 class="font-bold text-sm mb-1 flex items-center gap-2">
              {{ t('dashboard.needHelp') }}
            </h4>
            <p class="text-indigo-100 text-xs leading-relaxed mb-4">
              {{ t('dashboard.helpDesc') }}
            </p>
            <a href="https://github.com/zxq1002/MemDiag" target="_blank" class="inline-flex items-center gap-2 text-[11px] font-bold bg-white text-indigo-600 px-4 py-2 rounded-xl hover:bg-indigo-50 transition-colors">
              {{ t('dashboard.readDocs') }} <ExternalLink class="w-3.5 h-3.5" />
            </a>
          </div>
        </section>
      </div>
      
      <template #footer>
        <div class="p-6 border-t border-slate-100 flex flex-col items-center gap-2">
          <Button :label="t('settings.closeSync')" class="w-full rounded-xl font-bold py-3" @click="saveSettings" />
          <p class="text-[10px] text-slate-400 mt-2 tracking-widest">{{ t('settings.version') }} 1.0.0-BETA</p>
        </div>
      </template>
    </Drawer>
  </div>
</template>

<style scoped>
</style>
