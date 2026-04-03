import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSettingsStore = defineStore('settings', () => {
  const savedEndpoint = localStorage.getItem('memdiag_endpoint')
  const apiEndpoint = ref(savedEndpoint || '/api/v1')
  const refreshRate = ref(Number(localStorage.getItem('memdiag_refresh_rate')) || 5000)

  function setApiEndpoint(newUrl) {
    apiEndpoint.value = newUrl
    if (newUrl === '/api/v1' || !newUrl) {
      localStorage.removeItem('memdiag_endpoint')
    } else {
      localStorage.setItem('memdiag_endpoint', newUrl)
    }
    // We intentionally DON'T touch axios.defaults.baseURL here anymore
    // to keep things predictable. Advanced cross-origin would need interceptor logic.
  }

  function setRefreshRate(rate) {
    refreshRate.value = rate
    localStorage.setItem('memdiag_refresh_rate', rate.toString())
  }

  return {
    apiEndpoint,
    refreshRate,
    setApiEndpoint,
    setRefreshRate
  }
})
