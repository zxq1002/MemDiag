import { ref } from 'vue'
import axios from 'axios'

export function useHistogram() {
  const histogram = ref(null)
  const isLoading = ref(false)
  const error = ref(null)

  const loadHistogram = async (connectionId, limit = 20) => {
    if (!connectionId) return
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.get("/api/v1/" + `histogram/${connectionId}`, { 
        params: { limit } 
      })
      histogram.value = response.data
    } catch (e) {
      console.error('Failed to load histogram:', e)
      error.value = e.response?.data?.error || e.message || 'Failed to load histogram'
    } finally {
      isLoading.value = false
    }
  }

  const formatBytes = (bytes) => {
    if (!bytes || bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  const formatNumber = (n) => n?.toLocaleString() || '0'

  return {
    histogram,
    isLoading,
    error,
    loadHistogram,
    formatBytes,
    formatNumber
  }
}
