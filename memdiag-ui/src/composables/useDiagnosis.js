import { ref, computed } from 'vue'
import axios from 'axios'

export function useDiagnosis() {
  const result = ref(null)
  const isLoading = ref(false)
  const error = ref(null)
  const severityFilter = ref([])

  const diagnosisData = computed(() => result.value?.data || result.value)
  
  const filteredIssues = computed(() => {
    const issues = diagnosisData.value?.issues || []
    if (severityFilter.value.length === 0) return issues
    return issues.filter(i => severityFilter.value.includes(i.severity?.toUpperCase()))
  })

  const loadDiagnosis = async (connectionId) => {
    if (!connectionId) return
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.get("/api/v1/" + `diagnose/${connectionId}`)
      result.value = response.data
    } catch (e) {
      console.error('Failed to load diagnosis:', e)
      error.value = e.response?.data?.error || e.message || 'Failed to load diagnosis'
    } finally {
      isLoading.value = false
    }
  }

  return {
    result,
    diagnosisData,
    filteredIssues,
    severityFilter,
    isLoading,
    error,
    loadDiagnosis
  }
}
