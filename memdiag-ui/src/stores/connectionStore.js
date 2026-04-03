import { defineStore } from 'pinia'
import { useWebSocket } from '@vueuse/core'
import { ref, computed } from 'vue'
import axios from 'axios'

export const useConnectionStore = defineStore('connection', () => {
  const connections = ref({})
  const currentConnectionId = ref(null)
  const isInitialLoading = ref(false)
  
  const { status } = useWebSocket('ws://' + window.location.host + '/ws', {
    autoReconnect: true,
    heartbeat: true,
  })

  const isOnline = computed(() => {
    return status.value === 'OPEN' || Object.keys(connections.value).length > 0
  })

  async function fetchConnections() {
    isInitialLoading.value = true
    try {
      // Use full relative path explicitly
      const response = await axios.get('/api/v1/connections')
      connections.value = response.data
      console.log('[connectionStore] Connections initialized:', Object.keys(connections.value).length)
    } catch (e) {
      console.error('[connectionStore] Failed to fetch connections:', e)
    } finally {
      isInitialLoading.value = false
    }
  }

  function setConnections(newConnections) {
    connections.value = newConnections
  }

  function setCurrentConnection(id) {
    currentConnectionId.value = id
  }

  return {
    connections,
    currentConnectionId,
    status,
    isOnline,
    isInitialLoading,
    fetchConnections,
    setConnections,
    setCurrentConnection
  }
})
