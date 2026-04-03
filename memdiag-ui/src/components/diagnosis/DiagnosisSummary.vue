<script setup>
import Card from 'primevue/card'

const props = defineProps({
  totalHeapUsed: Number,
  totalHeapCommitted: Number,
  threadCount: Number,
  issueCount: Number
})
</script>

<template>
  <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
    <Card class="border-0 shadow-sm">
      <template #content>
        <p class="text-xs font-bold text-slate-400 uppercase mb-1">Heap Usage</p>
        <p class="text-2xl font-black text-slate-900">
          {{ (totalHeapUsed / (1024 * 1024)).toFixed(1) }} MB 
          <span class="text-sm font-normal text-slate-400">/ {{ (totalHeapCommitted / (1024 * 1024)).toFixed(1) }} MB</span>
        </p>
      </template>
    </Card>
    <Card class="border-0 shadow-sm">
      <template #content>
        <p class="text-xs font-bold text-slate-400 uppercase mb-1">Threads</p>
        <p class="text-2xl font-black text-slate-900">{{ threadCount }}</p>
      </template>
    </Card>
    <Card class="border-0 shadow-sm">
      <template #content>
        <p class="text-xs font-bold text-slate-400 uppercase mb-1">Issues Found</p>
        <p class="text-2xl font-black" :class="issueCount > 0 ? 'text-amber-600' : 'text-emerald-600'">
          {{ issueCount }}
        </p>
      </template>
    </Card>
  </div>
</template>
