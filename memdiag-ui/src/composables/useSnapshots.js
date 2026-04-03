import { ref } from 'vue'
import axios from 'axios'

export function useSnapshots() {
  const snapshots = ref([])
  const isLoading = ref(false)
  const error = ref(null)

  const loadSnapshots = async (connectionId) => {
    if (!connectionId) return
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.get(`/api/v1/snapshots/${connectionId}`)
      snapshots.value = response.data?.data || response.data || []
    } catch (e) {
      console.error('Failed to load snapshots:', e)
      error.value = e.response?.data?.error || e.message || 'Failed to load snapshots'
    } finally {
      isLoading.value = false
    }
  }

  const createSnapshot = async (connectionId, name) => {
    if (!connectionId) return
    isLoading.value = true
    try {
      // Must include /api/v1/ prefix
      const response = await axios.post(`/api/v1/snapshots/${connectionId}`, null, { params: { name } })
      if (response.data?.success === false) {
        throw new Error(response.data?.error || 'Failed to create snapshot')
      }
      await loadSnapshots(connectionId)
      return { success: true }
    } catch (e) {
      const msg = e.response?.data?.error || e.message || 'Failed to create snapshot'
      console.error('Failed to create snapshot:', msg)
      return { success: false, error: msg }
    } finally {
      isLoading.value = false
    }
  }

  const deleteSnapshot = async (connectionId, snapshotId) => {
    if (!connectionId || !snapshotId) return
    try {
      await axios.delete(`/api/v1/snapshots/${connectionId}/${snapshotId}`)
      await loadSnapshots(connectionId)
      return { success: true }
    } catch (e) {
      console.error('Failed to delete snapshot:', e)
      return { success: false, error: e.response?.data?.error || 'Failed to delete snapshot' }
    }
  }

  return {
    snapshots,
    isLoading,
    error,
    loadSnapshots,
    createSnapshot,
    deleteSnapshot
  }
}
