<script setup>
import { useI18n } from 'vue-i18n'
import { Terminal } from 'lucide-vue-next'

const { t } = useI18n()
const props = defineProps({
  stackTrace: {
    type: Array,
    default: () => []
  }
})

const parseFrame = (frame) => {
  const className = frame.className || ''
  const parts = className.split('.')
  const simpleName = parts.pop()
  const packageName = parts.join('.')
  
  return {
    packageName,
    simpleName,
    methodName: frame.methodName,
    fileName: frame.fileName,
    lineNumber: frame.lineNumber
  }
}
</script>

<template>
  <div class="p-6 bg-slate-900 rounded-2xl mx-4 my-2 border border-white/5 shadow-inner">
    <div class="flex items-center gap-2 mb-4 text-indigo-400">
      <Terminal class="w-4 h-4" />
      <span class="text-xs font-bold uppercase tracking-widest">{{ t('threads.stackTrace') }}</span>
    </div>
    
    <div v-if="stackTrace.length" class="space-y-1 font-mono text-[11px] leading-relaxed overflow-x-auto">
      <div v-for="(frame, idx) in stackTrace" :key="idx" class="whitespace-nowrap group">
        <span class="text-slate-600 mr-2 selection:bg-indigo-500/30">{{ (idx + 1).toString().padStart(2, ' ') }}</span>
        <span class="text-slate-500">at </span>
        
        <template v-if="frame.className">
          <span class="text-slate-500 group-hover:text-slate-400 transition-colors">{{ parseFrame(frame).packageName }}.</span>
          <span class="text-indigo-300 font-bold group-hover:text-indigo-200 transition-colors">{{ parseFrame(frame).simpleName }}</span>
          <span class="text-white">.{{ frame.methodName }}</span>
          
          <span class="text-slate-400 ml-2 italic" v-if="frame.lineNumber > 0">
            (<span class="text-slate-500">{{ frame.fileName }}:</span><span class="text-amber-400 font-bold">{{ frame.lineNumber }}</span>)
          </span>
          <span class="text-slate-600 ml-2" v-else-if="frame.fileName">
            ({{ frame.fileName }})
          </span>
          <span class="text-slate-600 ml-2" v-else>
            (Native Method)
          </span>
        </template>
        
        <span v-else class="text-slate-400 italic">Unknown Source</span>
      </div>
    </div>
    
    <div v-else class="text-center py-8">
      <p class="text-slate-500 italic text-sm">{{ t('threads.noStackTrace') }}</p>
    </div>
  </div>
</template>

<style scoped>
div::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.1);
}
div::-webkit-scrollbar-thumb:hover {
  background-color: rgba(255, 255, 255, 0.2);
}
</style>
