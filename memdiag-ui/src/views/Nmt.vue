<template>
  <div class="nmt">
    <h2>Native Memory Tracking</h2>

    <div class="controls">
      <select v-model="selectedConn">
        <option value="">Select connection</option>
        <option v-for="conn in connections" :key="conn.id" :value="conn.id">{{ conn.id }}</option>
      </select>
      <label>
        <input type="checkbox" v-model="detailMode" />
        Detail Mode
      </label>
      <button @click="loadNmt">Refresh</button>
    </div>

    <div v-if="snapshot" class="nmt-results">
      <div class="summary-card">
        <h3>Summary</h3>
        <div class="summary-grid">
          <div class="summary-item">
            <span class="label">Generated</span>
            <span class="value">{{ formatTimestamp(snapshot.timestamp) }}</span>
          </div>
          <div class="summary-item">
            <span class="label">Total Reserved</span>
            <span class="value">{{ formatBytes(snapshot.totalReserved) }}</span>
          </div>
          <div class="summary-item">
            <span class="label">Total Committed</span>
            <span class="value">{{ formatBytes(snapshot.totalCommitted) }}</span>
          </div>
          <div class="summary-item">
            <span class="label">Total Malloc'd</span>
            <span class="value">{{ formatBytes(snapshot.totalMalloced) }}</span>
          </div>
        </div>
      </div>

      <div class="categories-card">
        <h3>Categories</h3>
        <table>
          <thead>
            <tr>
              <th>Category</th>
              <th>Reserved</th>
              <th>Committed</th>
              <th>Malloc'd</th>
              <th>Malloc Count</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="usage in sortedUsages" :key="usage.category">
              <td class="category">{{ usage.category?.displayName || usage.category }}</td>
              <td class="num">{{ formatBytes(usage.reserved) }}</td>
              <td class="num">{{ formatBytes(usage.committed) }}</td>
              <td class="num">{{ formatBytes(usage.malloced) }}</td>
              <td class="num">{{ formatNumber(usage.mallocCount) }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <td><strong>Total</strong></td>
              <td class="num"><strong>{{ formatBytes(snapshot.totalReserved) }}</strong></td>
              <td class="num"><strong>{{ formatBytes(snapshot.totalCommitted) }}</strong></td>
              <td class="num"><strong>{{ formatBytes(snapshot.totalMalloced) }}</strong></td>
              <td></td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>

    <div v-if="error" class="error">
      {{ error }}
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'Nmt',
  data() {
    return {
      connections: [],
      selectedConn: '',
      detailMode: false,
      snapshot: null,
      error: null
    }
  },
  computed: {
    sortedUsages() {
      if (!this.snapshot || !this.snapshot.usages) return []
      return [...this.snapshot.usages].sort((a, b) => b.committed - a.committed)
    }
  },
  mounted() {
    this.loadConnections()
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
    async loadNmt() {
      if (!this.selectedConn) return
      this.error = null
      try {
        const response = await axios.get(`/api/v1/nmt/${this.selectedConn}`, { params: { detail: this.detailMode } })
        this.snapshot = response.data
      } catch (e) {
        console.error('Failed to load NMT:', e)
        this.error = 'Failed to load NMT data. Make sure NativeMemoryTracking is enabled with -XX:NativeMemoryTracking=summary or =detail.'
      }
    },
    formatTimestamp(ts) {
      if (!ts) return '-'
      return new Date(ts).toLocaleString()
    },
    formatNumber(n) {
      return n?.toLocaleString() || '0'
    },
    formatBytes(bytes) {
      if (!bytes || bytes === 0) return '0 B'
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
      if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
      return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
    }
  }
}
</script>

<style scoped>
.nmt {
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
  align-items: center;
}

.controls select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.controls label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
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

.nmt-results {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.summary-card,
.categories-card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

h3 {
  color: #555;
  margin-bottom: 1rem;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.summary-item {
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 6px;
  text-align: center;
}

.summary-item .label {
  display: block;
  color: #666;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}

.summary-item .value {
  display: block;
  font-size: 1.25rem;
  font-weight: 700;
  color: #667eea;
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

.category {
  font-weight: 500;
}

.num {
  text-align: right;
  font-family: monospace;
}

.error {
  background: #fff5f5;
  border: 1px solid #feb2b2;
  color: #c53030;
  padding: 1rem;
  border-radius: 8px;
  line-height: 1.6;
}
</style>
