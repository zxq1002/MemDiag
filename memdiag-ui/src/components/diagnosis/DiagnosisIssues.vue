<script setup>
import { 
  ShieldAlert, 
  AlertTriangle, 
  Info, 
  CheckCircle2,
  Lightbulb,
  ArrowRight
} from 'lucide-vue-next'
import Tag from 'primevue/tag'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'
import SelectButton from 'primevue/selectbutton'

const props = defineProps({
  issues: Array,
  severityFilter: Array
})

const emit = defineEmits(['update:severityFilter'])

const severityOptions = [
  { label: 'Critical', value: 'CRITICAL', icon: ShieldAlert },
  { label: 'Warning', value: 'WARNING', icon: AlertTriangle },
  { label: 'Info', value: 'INFO', icon: Info }
]

const getSeverityInfo = (severity) => {
  const s = severity?.toUpperCase() || 'INFO'
  switch (s) {
    case 'CRITICAL': return { color: 'danger' }
    case 'WARNING': return { color: 'warn' }
    default: return { color: 'info' }
  }
}

const onFilterChange = (val) => {
  emit('update:severityFilter', val || [])
}
</script>

<template>
  <div class="space-y-4">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 px-2">
      <h3 class="text-lg font-bold text-slate-800 flex items-center gap-2">
        <ShieldAlert class="w-5 h-5 text-indigo-600" />
        Detected Issues
      </h3>
      
      <SelectButton 
        :modelValue="severityFilter" 
        @update:modelValue="onFilterChange"
        :options="severityOptions" 
        multiple 
        optionLabel="label" 
        optionValue="value"
        class="p-selectbutton-sm"
      >
        <template #option="slotProps">
          <div class="flex items-center gap-2 px-1">
            <component :is="slotProps.option.icon" class="w-3.5 h-3.5" />
            <span>{{ slotProps.option.label }}</span>
          </div>
        </template>
      </SelectButton>
    </div>
    
    <div v-if="issues.length > 0">
      <Accordion :multiple="true" :activeIndex="[0]">
        <AccordionTab v-for="(issue, idx) in issues" :key="idx">
          <template #header>
            <div class="flex items-center gap-3 w-full">
              <Tag :value="issue.severity" :severity="getSeverityInfo(issue.severity).color" />
              <span class="font-bold text-slate-700">{{ issue.title }}</span>
              <span class="ml-auto text-xs text-slate-400 font-medium mr-4">{{ issue.type }}</span>
            </div>
          </template>
          
          <div class="space-y-4 py-2">
            <p class="text-slate-600 leading-relaxed">{{ issue.description }}</p>
            
            <div v-if="issue.recommendations?.length" class="bg-slate-50 rounded-2xl p-6 border border-slate-100">
              <h4 class="font-bold text-slate-800 flex items-center gap-2 mb-4 text-sm uppercase tracking-wider">
                <Lightbulb class="w-4 h-4 text-amber-500" />
                Actionable Recommendations
              </h4>
              <div class="space-y-3">
                <div v-for="(rec, ridx) in issue.recommendations" :key="ridx" class="flex gap-3">
                  <div class="mt-1 flex-shrink-0 w-5 h-5 rounded-full bg-white border border-slate-200 flex items-center justify-center text-[10px] font-bold text-slate-400">
                    {{ ridx + 1 }}
                  </div>
                  <div>
                    <p class="font-bold text-slate-800 text-sm">{{ rec.title }}</p>
                    <p class="text-slate-500 text-sm mt-0.5">{{ rec.description }}</p>
                    <div v-if="rec.action" class="mt-2 inline-flex items-center gap-1 text-xs font-bold text-indigo-600 hover:text-indigo-700 cursor-pointer group">
                      Execute Suggested Action <ArrowRight class="w-3 h-3 transition-transform group-hover:translate-x-1" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </AccordionTab>
      </Accordion>
    </div>

    <!-- Success State -->
    <div v-else class="flex flex-col items-center justify-center py-20 bg-white rounded-3xl border border-dashed border-slate-200">
      <div class="p-4 bg-emerald-50 rounded-full mb-4 text-emerald-500">
        <CheckCircle2 class="w-12 h-12" />
      </div>
      <h3 class="text-xl font-bold text-slate-900">No matching issues</h3>
      <p class="text-slate-500 mt-1 max-w-sm text-center">Adjust your severity filters or enjoy a healthy system!</p>
    </div>
  </div>
</template>
