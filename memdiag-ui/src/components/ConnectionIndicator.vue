<script setup>
import { useConnectionStore } from '../stores/connectionStore'
import { computed } from 'vue'
import { Wifi, WifiOff, Globe, Zap } from 'lucide-vue-next'

const store = useConnectionStore()

const hasConnections = computed(() => Object.keys(store.connections).length > 0)

const statusColor = computed(() => {
  if (store.status === 'OPEN') return 'text-emerald-500'
  if (hasConnections.value) return 'text-indigo-500'
  return 'text-slate-400'
})

const statusLabel = computed(() => {
  if (store.status === 'OPEN') return 'Live Sync'
  if (hasConnections.value) return 'Ready'
  return 'No Target'
})
</script>

<template>
  <div class="flex items-center gap-2 px-3 py-1.5 rounded-full bg-slate-50 border border-slate-200">
    <div :class="['w-2 h-2 rounded-full bg-current', statusColor, store.status === 'OPEN' ? 'animate-pulse' : '']"></div>
    <span class="text-[10px] font-bold uppercase tracking-wider text-slate-600">{{ statusLabel }}</span>
    
    <div class="w-px h-3 bg-slate-200 mx-1"></div>
    
    <div class="flex items-center gap-1.5" v-tooltip="store.status === 'OPEN' ? 'WebSocket Connected' : 'REST API Only'">
      <Zap v-if="store.status === 'OPEN'" class="w-3 h-3 text-amber-400 fill-amber-400" />
      <component :is="store.isOnline ? Wifi : WifiOff" :class="['w-3.5 h-3.5', store.isOnline ? 'text-indigo-400' : 'text-slate-300']" />
    </div>
  </div>
</template>
