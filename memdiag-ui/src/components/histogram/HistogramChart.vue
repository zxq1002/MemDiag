<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'
import Card from 'primevue/card'
import { BarChart3 } from 'lucide-vue-next'

const props = defineProps({
  classStats: Array,
  formatBytes: Function
})

const chartRef = ref(null)
const chart = ref(null)

/**
 * Direct length-based truncator for UI stability.
 */
const getTruncatedName = (raw) => {
  if (!raw || typeof raw !== 'string') return 'Unknown'
  
  // Strip rank prefix if still present
  let clean = raw.replace(/^\s*\d+:\s+/, '').trim()
  
  // Format standard JVM arrays for readability
  if (clean === '[C') return 'char[]'
  if (clean === '[B') return 'byte[]'
  if (clean === '[I') return 'int[]'
  if (clean.startsWith('[L')) clean = clean.substring(2).replace(/;$/, '') + '[]'

  const MAX_LENGTH = 22
  return clean.length > MAX_LENGTH 
    ? clean.substring(0, MAX_LENGTH) + '...' 
    : clean
}

const renderChart = () => {
  if (chart.value) chart.value.dispose()
  if (!chartRef.value || !props.classStats?.length) return

  chart.value = echarts.init(chartRef.value)

  const data = [...props.classStats]
    .map(s => {
      const rawName = s.className || s.name || 'Unknown'
      return {
        fullName: rawName,
        name: getTruncatedName(rawName),
        value: s.shallowBytes || s.bytes || 0
      }
    })
    .sort((a, b) => b.value - a.value)
    .slice(0, 10)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = params[0]
        return `<div class="font-sans px-1 py-0.5">
          <div class="text-[10px] text-slate-400 mb-1 break-all max-w-[280px]">${data[p.dataIndex].fullName}</div>
          <div class="flex items-center justify-between gap-4">
            <span class="text-xs font-medium text-slate-600">Size:</span>
            <span class="text-sm font-black text-indigo-600">${props.formatBytes(p.value)}</span>
          </div>
        </div>`
      }
    },
    grid: { left: '4%', right: '20%', bottom: '4%', top: '4%', containLabel: true },
    xAxis: { 
      type: 'value',
      axisLabel: { show: false },
      splitLine: { show: false },
      axisLine: { show: false }
    },
    yAxis: { 
      type: 'category', 
      data: data.map(d => d.name).reverse(),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { 
        color: '#475569',
        fontSize: 10,
        fontWeight: 500,
        margin: 12,
        // ECharts native truncation if needed
        overflow: 'truncate'
      }
    },
    series: [{
      type: 'bar',
      data: data.map(d => d.value).reverse(),
      itemStyle: { 
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#818cf8' },
          { offset: 1, color: '#6366f1' }
        ]),
        borderRadius: [0, 6, 6, 0]
      },
      label: {
        show: true,
        position: 'right',
        formatter: (params) => props.formatBytes(params.value),
        fontSize: 10,
        color: '#4f46e5',
        fontWeight: 'bold',
        distance: 8
      },
      barWidth: '55%'
    }]
  }

  chart.value.setOption(option)
}

onMounted(() => {
  renderChart()
  window.addEventListener('resize', () => chart.value?.resize())
})

onBeforeUnmount(() => {
  chart.value?.dispose()
})

watch(() => props.classStats, renderChart, { deep: true })
</script>

<template>
  <Card class="border-0 shadow-sm overflow-hidden h-full rounded-3xl">
    <template #title>
      <div class="flex items-center gap-2 text-lg">
        <BarChart3 class="w-5 h-5 text-indigo-600" />
        <span class="font-bold text-slate-800 text-base">Top 10 Hotspots</span>
      </div>
    </template>
    <template #content>
      <div ref="chartRef" class="w-full h-[320px]"></div>
    </template>
  </Card>
</template>
