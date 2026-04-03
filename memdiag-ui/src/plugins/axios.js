import axios from 'axios'

export default {
  install: (app) => {
    axios.interceptors.request.use(config => {
      // Log the full URL being called for debugging
      console.log('[API Request]', (config.baseURL || '') + config.url)
      return config
    })

    axios.interceptors.response.use(
      (response) => response,
      (error) => {
        const message = error.response?.data?.error || error.message || 'An unexpected error occurred'
        console.error('[API Error]', message)
        return Promise.reject(error)
      }
    )
    
    app.config.globalProperties.$axios = axios
  }
}
