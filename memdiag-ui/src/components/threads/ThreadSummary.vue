<script setup>
import { computed } from 'vue'
import Tag from 'primevue/tag'

const props = defineProps({
  totalThreads: Number,
  stateCounts: Object
})

const emit = defineEmits(['filterState'])

const getStateSeverity = (state) => {
  switch (state) {
    case 'RUNNABLE': return 'success'
    case 'BLOCKED': return 'danger'
    case 'WAITING': return 'warn'
    case 'TIMED_WAITING': return 'info'
    default: return 'secondary'
  }
}

const states = computed(() => {
  return Object.keys(props.stateCounts).sort()
})
</script>

<template>
  <div class="flex flex-wrap gap-4">
    <div 
      class="flex-1 min-w-[140px] bg-white p-4 rounded-2xl border border-slate-100 shadow-sm cursor-pointer hover:bg-slate-50 transition-colors"
      @click="emit('filterState', null)"
    >
      <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Total</p>
      <p class="text-2xl font-black text-slate-900">{{ totalThreads }}</p>
    </div>
    <div v-for="state in states" :key="state" 
         class="flex-1 min-w-[140px] bg-white p-4 rounded-2xl border border-slate-100 shadow-sm border-t-4 cursor-pointer hover:bg-slate-50 transition-colors"
         :style="{ borderTopColor: `var(--p-tag-${getStateSeverity(state)}-background)` }"
         @click="emit('filterState', state)">
      <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">{{ state }}</p>
      <p class="text-2xl font-black text-slate-900">{{ stateCounts[state] }}</p>
    </div>
  </div>
</template>
