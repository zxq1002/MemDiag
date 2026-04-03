import { ref, computed } from 'vue'
import axios from 'axios'

export function useDiff() {
  const baseSnapshot = ref(null)
  const compareSnapshot = ref(null)
  const isLoading = ref(false)
  const error = ref(null)

  const diffData = computed(() => {
    if (!baseSnapshot.value || !compareSnapshot.value) {
      console.log('[useDiff] Missing snapshots for computation', { base: !!baseSnapshot.value, compare: !!compareSnapshot.value })
      return []
    }

    const getSnapshotData = (val) => val?.data || val
    const base = getSnapshotData(baseSnapshot.value)
    const compare = getSnapshotData(compareSnapshot.value)

    // Log the structure we found
    console.log('[useDiff] Analyzing data structures', { 
      baseKeys: Object.keys(base || {}), 
      compareKeys: Object.keys(compare || {}) 
    })

    const baseClasses = base?.heapHistogram?.classStats || base?.classes || []
    const compareClasses = compare?.heapHistogram?.classStats || compare?.classes || []

    console.log('[useDiff] Class counts', { base: baseClasses.length, compare: compareClasses.length })

    const baseMap = new Map(baseClasses.map(c => [c.className, c]))
    const compareMap = new Map(compareClasses.map(c => [c.className, c]))

    const allClassNames = new Set([
      ...baseClasses.map(c => c.className),
      ...compareClasses.map(c => c.className)
    ])

    const result = Array.from(allClassNames).map(name => {
      const b = baseMap.get("" + name) || { objectCount: 0, shallowBytes: 0 }
      const c = compareMap.get("" + name) || { objectCount: 0, shallowBytes: 0 }

      const bObjects = b.objectCount || 0
      const cObjects = c.objectCount || 0
      const bBytes = b.shallowBytes || 0
      const cBytes = c.shallowBytes || 0

      return {
        className: name,
        baseObjects: bObjects,
        compareObjects: cObjects,
        deltaObjects: cObjects - bObjects,
        baseBytes: bBytes,
        compareBytes: cBytes,
        deltaBytes: cBytes - bBytes,
        growthRate: bBytes > 0 ? ((cBytes - bBytes) / bBytes) * 100 : (cBytes > 0 ? 100 : 0)
      }
    })
    
    // Only show items with actual differences to reduce noise
    const finalDiff = result.filter(d => d.deltaBytes !== 0 || d.deltaObjects !== 0)
      .sort((a, b) => Math.abs(b.deltaBytes) - Math.abs(a.deltaBytes))

    console.log('[useDiff] Computation finished', { totalClasses: result.length, differentClasses: finalDiff.length })
    return finalDiff
  })

  const loadSnapshotsForDiff = async (connectionId, baseId, compareId) => {
    console.log('[useDiff] Fetching snapshots', { connectionId, baseId, compareId })
    isLoading.value = true
    error.value = null
    try {
      const [baseRes, compareRes] = await Promise.all([
        axios.get("/api/v1/" + `snapshots/${connectionId}/${baseId}`),
        axios.get("/api/v1/" + `snapshots/${connectionId}/${compareId}`)
      ])
      console.log('[useDiff] Fetching success', { baseStatus: baseRes.status, compareStatus: compareRes.status })
      baseSnapshot.value = baseRes.data
      compareSnapshot.value = compareRes.data
      return { success: true }
    } catch (e) {
      const msg = e.response?.data?.error || e.message || 'Failed to load snapshots'
      console.error('[useDiff] Fetching error', msg)
      error.value = msg
      return { success: false, error: msg }
    } finally {
      isLoading.value = false
    }
  }

  return {
    baseSnapshot,
    compareSnapshot,
    diffData,
    isLoading,
    error,
    loadSnapshotsForDiff
  }
}
