import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'
import { useConnectionStore } from './connectionStore'

export const useDashboardStore = defineStore('dashboard', () => {
  const connections = ref([])
  const isLoading = ref(false)
  const connectionStore = useConnectionStore()

  async function loadConnections() {
    isLoading.value = true
    try {
      const response = await axios.get('/api/v1/connections')
      const mapped = Object.entries(response.data).map(([id, status]) => {
        const [type, state] = status.split(':')
        return { id, type, state }
      })
      connections.value = mapped
      connectionStore.setConnections(response.data)
    } catch (e) {
      console.error('Failed to load connections:', e)
    } finally {
      isLoading.value = false
    }
  }

  async function connect(id, target) {
    isLoading.value = true
    try {
      const params = target ? { target } : {}
      await axios.post(`/api/v1/connections/${id}`, null, { params })
      await loadConnections()
      return { success: true }
    } catch (e) {
      console.error('Connection failed:', e)
      return { success: false, error: e.response?.data?.error || 'Unknown error' }
    } finally {
      isLoading.value = false
    }
  }

  async function disconnect(id) {
    try {
      await axios.delete(`/api/v1/connections/${id}`)
      await loadConnections()
      if (connectionStore.currentConnectionId === id) {
        connectionStore.setCurrentConnection(null)
      }
    } catch (e) {
      console.error('Disconnect failed:', e)
    }
  }

  return {
    connections,
    isLoading,
    loadConnections,
    connect,
    disconnect
  }
})
