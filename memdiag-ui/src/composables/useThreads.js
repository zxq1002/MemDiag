import { ref, computed } from 'vue'
import axios from 'axios'

export function useThreads() {
  const threadDump = ref(null)
  const isLoading = ref(false)
  const error = ref(null)
  const stateFilter = ref(null)
  const searchQuery = ref('')

  const threads = computed(() => {
    // Dig through layers
    const raw = threadDump.value
    if (!raw) return []
    
    // Support: { success: true, data: { threadStats: [...] } }
    // or: { threadStats: [...] }
    // or: { allThreads: [...] }
    const data = raw.data || raw
    const list = data.threadStats || data.threads || data.allThreads || []
    
    console.log('[useThreads] Extracted thread list', { count: list.length })
    return list
  })

  const filteredThreads = computed(() => {
    let list = [...threads.value]
    
    if (stateFilter.value) {
      list = list.filter(t => t.state === stateFilter.value)
    }
    
    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase()
      list = list.filter(t => 
        t.threadName?.toLowerCase().includes(q) || 
        t.threadId?.toString().includes(q)
      )
    }
    
    return list
  })

  const stateCounts = computed(() => {
    const counts = {}
    threads.value.forEach(t => {
      const s = t.state || 'UNKNOWN'
      counts[s] = (counts[s] || 0) + 1
    })
    return counts
  })

  const loadThreads = async (connectionId) => {
    console.log('[useThreads] Loading threads for', connectionId)
    isLoading.value = true
    error.value = null
    try {
      const response = await axios.get("/api/v1/" + `threads/${connectionId}`)
      console.log('[useThreads] API Response keys:', Object.keys(response.data))
      threadDump.value = response.data
    } catch (e) {
      const msg = e.response?.data?.error || e.message || 'Failed to load threads'
      console.error('[useThreads] Error:', msg)
      error.value = msg
    } finally {
      isLoading.value = false
    }
  }

  return {
    threadDump,
    threads,
    filteredThreads,
    stateCounts,
    stateFilter,
    searchQuery,
    isLoading,
    error,
    loadThreads
  }
}
