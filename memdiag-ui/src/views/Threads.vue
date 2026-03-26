<template>
  <div class="threads">
    <h2>Threads</h2>

    <div class="controls">
      <select v-model="selectedConn">
        <option value="">Select connection</option>
        <option v-for="conn in connections" :key="conn.id" :value="conn.id">{{ conn.id }}</option>
      </select>
      <button @click="loadThreads">Refresh</button>
    </div>

    <div v-if="threadDump" class="thread-list">
      <div v-for="thread in sortedThreads" :key="thread.threadId" class="thread-card" :class="thread.state?.toLowerCase()">
        <div class="thread-header">
          <span class="thread-id">#{{ thread.threadId }}</span>
          <span class="thread-name">{{ thread.threadName }}</span>
          <span class="thread-state" :class="thread.state?.toLowerCase()">{{ thread.state || 'UNKNOWN' }}</span>
        </div>
        <div v-if="thread.stackTrace && thread.stackTrace.length > 0" class="stack-trace">
          <div v-for="(frame, idx) in thread.stackTrace" :key="idx" class="stack-frame">
            <span class="frame-class">{{ frame.className }}</span>
            <span class="frame-method">.{{ frame.methodName }}</span>
            <span v-if="frame.lineNumber > 0" class="frame-line">({{ frame.fileName }}:{{ frame.lineNumber }})</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="threadDump" class="summary">
      <div class="summary-card">
        <span class="summary-label">Total Threads</span>
        <span class="summary-value">{{ threadDump.threadStats?.length || 0 }}</span>
      </div>
      <div v-for="(count, state) in stateCounts" :key="state" class="summary-card" :class="state.toLowerCase()">
        <span class="summary-label">{{ state }}</span>
        <span class="summary-value">{{ count }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'Threads',
  data() {
    return {
      connections: [],
      selectedConn: '',
      threadDump: null
    }
  },
  computed: {
    sortedThreads() {
      if (!this.threadDump || !this.threadDump.threadStats) return []
      return [...this.threadDump.threadStats].sort((a, b) => a.threadId - b.threadId)
    },
    stateCounts() {
      const counts = {}
      if (this.threadDump && this.threadDump.threadStats) {
        for (const thread of this.threadDump.threadStats) {
          const state = thread.state || 'UNKNOWN'
          counts[state] = (counts[state] || 0) + 1
        }
      }
      return counts
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
    async loadThreads() {
      if (!this.selectedConn) return
      try {
        const response = await axios.get(`/api/v1/threads/${this.selectedConn}`)
        this.threadDump = response.data
      } catch (e) {
        console.error('Failed to load threads:', e)
      }
    }
  }
}
</script>

<style scoped>
.threads {
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

.summary {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  flex-wrap: wrap;
}

.summary-card {
  flex: 1;
  min-width: 120px;
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  text-align: center;
}

.summary-card.runnable {
  border-top: 4px solid #48bb78;
}

.summary-card.blocked {
  border-top: 4px solid #e53e3e;
}

.summary-card.waiting {
  border-top: 4px solid #ed8936;
}

.summary-card.timed_waiting {
  border-top: 4px solid #4299e1;
}

.summary-label {
  display: block;
  color: #666;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}

.summary-value {
  display: block;
  font-size: 2rem;
  font-weight: 700;
  color: #333;
}

.thread-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.thread-card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  border-left: 4px solid #ccc;
}

.thread-card.runnable {
  border-left-color: #48bb78;
}

.thread-card.blocked {
  border-left-color: #e53e3e;
}

.thread-card.waiting {
  border-left-color: #ed8936;
}

.thread-card.timed_waiting {
  border-left-color: #4299e1;
}

.thread-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.thread-id {
  background: #667eea;
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.875rem;
}

.thread-name {
  font-weight: 600;
  font-size: 1.1rem;
}

.thread-state {
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  background: #ccc;
  color: white;
}

.thread-state.runnable {
  background: #48bb78;
}

.thread-state.blocked {
  background: #e53e3e;
}

.thread-state.waiting {
  background: #ed8936;
}

.thread-state.timed_waiting {
  background: #4299e1;
}

.stack-trace {
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.875rem;
}

.stack-frame {
  padding: 0.25rem 0;
}

.frame-class {
  color: #667eea;
}

.frame-method {
  color: #333;
}

.frame-line {
  color: #999;
}
</style>
