import { describe, it, expect, vi } from 'vitest'
import { useHistogram } from '../useHistogram'
import axios from 'axios'

vi.mock('axios')

describe('useHistogram', () => {
  it('should format bytes correctly', () => {
    const { formatBytes } = useHistogram()
    expect(formatBytes(0)).toBe('0 B')
    expect(formatBytes(1024)).toBe('1 KB')
    expect(formatBytes(1024 * 1024)).toBe('1 MB')
    expect(formatBytes(1024 * 1024 * 1024)).toBe('1 GB')
  })

  it('should load histogram data', async () => {
    const { histogram, isLoading, loadHistogram } = useHistogram()
    const mockData = { data: { totalObjects: 100 } }
    axios.get.mockResolvedValue({ data: mockData })

    await loadHistogram('test-id')

    expect(isLoading.value).toBe(false)
    expect(histogram.value).toEqual(mockData)
    expect(axios.get).toHaveBeenCalledWith('/api/v1/histogram/test-id', { params: { limit: 20 } })
  })

  it('should handle errors', async () => {
    const { error, isLoading, loadHistogram } = useHistogram()
    axios.get.mockRejectedValue({ response: { data: { error: 'API Error' } } })

    await loadHistogram('test-id')

    expect(isLoading.value).toBe(false)
    expect(error.value).toBe('API Error')
  })
})
