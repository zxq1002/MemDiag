<template>
  <div class="histogram">
    <h2>Heap Histogram</h2>

    <div class="controls">
      <select v-model="selectedConn">
        <option value="">Select connection</option>
        <option v-for="conn in connections" :key="conn.id" :value="conn.id">{{ conn.id }}</option>
      </select>
      <input v-model.number="limit" type="number" min="5" max="100" />
      <button @click="loadHistogram">Refresh</button>
    </div>

    <div v-if="histogram" class="chart-container">
      <div ref="chart" class="chart"></div>
    </div>

    <div v-if="histogram" class="table-container">
      <table>
        <thead>
          <tr>
            <th>Class Name</th>
            <th>Objects</th>
            <th>Shallow Bytes</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="stats in sortedStats" :key="stats.className">
            <td class="class-name">{{ stats.className }}</td>
            <td class="num">{{ formatNumber(stats.objectCount) }}</td>
            <td class="num">{{ formatBytes(stats.shallowBytes) }}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr>
            <td><strong>Total</strong></td>
            <td class="num"><strong>{{ formatNumber(histogram.totalObjects) }}</strong></td>
            <td class="num"><strong>{{ formatBytes(histogram.totalBytes) }}</strong></td>
          </tr>
        </tfoot>
      </table>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import axios from 'axios'

export default {
  name: 'Histogram',
  data() {
    return {
      connections: [],
      selectedConn: '',
      limit: 20,
      histogram: null,
      chart: null
    }
  },
  computed: {
    sortedStats() {
      if (!this.histogram) return []
      return [...this.histogram.classStats].sort((a, b) => b.shallowBytes - a.shallowBytes)
    }
  },
  mounted() {
    this.loadConnections()
  },
  beforeUnmount() {
    if (this.chart) {
      this.chart.dispose()
    }
  },
  methods: {
    async loadConnections() {
      try {
        const response = await axios.get('/api/v1/connections')
        this.connections = Object.entries(response.data).map(([id, status]) => ({ id, status }))
      } catch (e) {
        console.error('Failed to load connections:', e)
      }
    },
    async loadHistogram() {
      if (!this.selectedConn) return
      try {
        const response = await axios.get(`/api/v1/histogram/${this.selectedConn}`, { params: { limit: this.limit } })
        this.histogram = response.data
        this.$nextTick(() => this.renderChart())
      } catch (e) {
        console.error('Failed to load histogram:', e)
      }
    },
    renderChart() {
      if (this.chart) {
        this.chart.dispose()
      }
      const chartDom = this.$refs.chart
      this.chart = echarts.init(chartDom)

      const data = this.sortedStats.slice(0, this.limit).map(s => ({
        name: this.truncate(s.className, 40),
        value: s.shallowBytes
      }))

      const option = {
        title: { text: 'Heap Distribution (Top ' + this.limit + ')', left: 'center' },
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          formatter: (params) => {
            const p = params[0]
            return p.name + '<br/>' + this.formatBytes(p.value)
          }
        },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'value' },
        yAxis: { type: 'category', data: data.map(d => d.name).reverse() },
        series: [{
          type: 'bar',
          data: data.reverse(),
          itemStyle: { color: '#667eea' }
        }]
      }

      this.chart.setOption(option)
    },
    truncate(s, max) {
      if (s.length <= max) return s
      return '...' + s.substring(s.length - max + 3)
    },
    formatNumber(n) {
      return n.toLocaleString()
    },
    formatBytes(bytes) {
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
      if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
      return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
    }
  }
}
</script>

<style scoped>
.histogram {
  width: 100%;
}

h2 {
  margin-bottom: 2rem;
  color: #667eea;
}

.controls {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  background: white;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.controls select,
.controls input {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.controls button {
  padding: 0.5rem 1.5rem;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.controls button:hover {
  background: #5a67d8;
}

.chart-container {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-bottom: 2rem;
}

.chart {
  width: 100%;
  height: 500px;
}

.table-container {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid #eee;
}

th {
  background: #f8f9fa;
  font-weight: 600;
}

.class-name {
  font-family: monospace;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.num {
  text-align: right;
  font-family: monospace;
}
</style>
