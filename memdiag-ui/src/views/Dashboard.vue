<template>
  <div class="dashboard">
    <h2>Dashboard</h2>

    <div class="connections">
      <h3>Connections</h3>
      <div v-if="connections.length === 0" class="empty">
        No connections. Connect to a JVM to get started.
      </div>
      <div v-else class="connection-list">
        <div v-for="conn in connections" :key="conn.id" class="connection-card">
          <span class="conn-id">{{ conn.id }}</span>
          <span class="conn-status" :class="conn.status">{{ conn.status }}</span>
        </div>
      </div>
    </div>

    <div class="connect-form">
      <h3>New Connection</h3>
      <input v-model="newConnId" placeholder="Connection ID" />
      <input v-model="newConnPid" placeholder="PID (optional for current JVM)" />
      <button @click="connect">Connect</button>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'Dashboard',
  data() {
    return {
      connections: [],
      newConnId: '',
      newConnPid: ''
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
    async connect() {
      if (!this.newConnId) return
      try {
        const params = this.newConnPid ? { pid: this.newConnPid } : {}
        await axios.post(`/api/v1/connections/${this.newConnId}`, null, { params })
        this.newConnId = ''
        this.newConnPid = ''
        this.loadConnections()
      } catch (e) {
        console.error('Failed to connect:', e)
      }
    }
  }
}
</script>

<style scoped>
.dashboard {
  max-width: 800px;
}

h2 {
  margin-bottom: 2rem;
  color: #667eea;
}

h3 {
  margin-bottom: 1rem;
  color: #555;
}

.connections {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-bottom: 2rem;
}

.empty {
  color: #999;
  padding: 2rem;
  text-align: center;
}

.connection-list {
  display: grid;
  gap: 1rem;
}

.connection-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 6px;
}

.conn-id {
  font-weight: 600;
}

.conn-status {
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  font-size: 0.875rem;
  background: #48bb78;
  color: white;
}

.connect-form {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.connect-form input {
  width: 100%;
  padding: 0.75rem;
  margin-bottom: 1rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.connect-form button {
  padding: 0.75rem 2rem;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s;
}

.connect-form button:hover {
  background: #5a67d8;
}
</style>
