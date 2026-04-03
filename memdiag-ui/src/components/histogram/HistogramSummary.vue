<script setup>
import Card from 'primevue/card'

const props = defineProps({
  totalObjects: Number,
  totalBytes: Number,
  formatNumber: Function,
  formatBytes: Function
})
</script>

<template>
  <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
    <Card class="border-0 shadow-sm bg-gradient-to-br from-indigo-600 to-violet-700 text-white">
      <template #content>
        <p class="text-indigo-100 text-xs font-bold uppercase tracking-wider mb-1">Total Objects</p>
        <p class="text-3xl font-black">{{ formatNumber(totalObjects) }}</p>
      </template>
    </Card>
    <Card class="border-0 shadow-sm bg-gradient-to-br from-indigo-600 to-violet-700 text-white">
      <template #content>
        <p class="text-indigo-100 text-xs font-bold uppercase tracking-wider mb-1">Total Shallow Size</p>
        <p class="text-3xl font-black">{{ formatBytes(totalBytes) }}</p>
      </template>
    </Card>
  </div>
</template>
