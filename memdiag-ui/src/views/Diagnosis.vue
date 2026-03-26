<template>
  <div class="diagnosis">
    <h2>Diagnosis</h2>

    <div class="controls">
      <select v-model="selectedConn">
        <option value="">Select connection</option>
        <option v-for="conn in connections" :key="conn.id" :value="conn.id">{{ conn.id }}</option>
      </select>
      <button @click="loadDiagnosis">Diagnose</button>
    </div>

    <div v-if="result" class="results">
      <div v-if="result.issues && result.issues.length > 0" class="issues">
        <h3>Issues Found</h3>
        <div v-for="(issue, idx) in result.issues" :key="idx" class="issue-card" :class="issue.severity?.toLowerCase()">
          <div class="issue-header">
            <span class="severity-badge" :class="issue.severity?.toLowerCase()">{{ issue.severity || 'INFO' }}</span>
            <span class="issue-title">{{ issue.title }}</span>
          </div>
          <p class="issue-desc">{{ issue.description }}</p>
          <div v-if="issue.recommendations && issue.recommendations.length > 0" class="recommendations">
            <strong>Recommendations:</strong>
            <ul>
              <li v-for="(rec, ridx) in issue.recommendations" :key="ridx">{{ rec.description }}</li>
            </ul>
          </div>
        </div>
      </div>

      <div v-else class="no-issues">
        <div class="success-icon">✓</div>
        <p>No issues found!</p>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'Diagnosis',
  data() {
    return {
      connections: [],
      selectedConn: '',
      result: null
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
    async loadDiagnosis() {
      if (!this.selectedConn) return
      try {
        const response = await axios.get(`/api/v1/diagnose/${this.selectedConn}`)
        this.result = response.data
      } catch (e) {
        console.error('Failed to load diagnosis:', e)
      }
    }
  }
}
</script>

<style scoped>
.diagnosis {
  max-width: 1000px;
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

.controls select {
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

.results {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.issues {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

h3 {
  color: #555;
  margin-bottom: 1rem;
}

.issue-card {
  padding: 1.5rem;
  border-radius: 8px;
  border-left: 4px solid #ccc;
  background: #f8f9fa;
}

.issue-card.critical {
  border-left-color: #e53e3e;
  background: #fff5f5;
}

.issue-card.warning {
  border-left-color: #ed8936;
  background: #fffaf0;
}

.issue-card.info {
  border-left-color: #4299e1;
  background: #ebf8ff;
}

.issue-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.severity-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  background: #ccc;
  color: white;
}

.severity-badge.critical {
  background: #e53e3e;
}

.severity-badge.warning {
  background: #ed8936;
}

.severity-badge.info {
  background: #4299e1;
}

.issue-title {
  font-weight: 600;
  font-size: 1.1rem;
}

.issue-desc {
  color: #666;
  line-height: 1.6;
}

.recommendations {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #ddd;
}

.recommendations ul {
  margin-top: 0.5rem;
  padding-left: 1.5rem;
}

.recommendations li {
  margin: 0.25rem 0;
}

.no-issues {
  text-align: center;
  padding: 3rem;
}

.success-icon {
  font-size: 4rem;
  color: #48bb78;
  margin-bottom: 1rem;
}

.no-issues p {
  color: #666;
  font-size: 1.2rem;
}
</style>
