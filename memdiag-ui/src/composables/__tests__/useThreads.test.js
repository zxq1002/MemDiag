import { describe, it, expect, vi } from 'vitest'
import { useThreads } from '../useThreads'
import axios from 'axios'

vi.mock('axios')

describe('useThreads', () => {
  it('should filter threads by state', () => {
    const { threadDump, stateFilter, filteredThreads } = useThreads()
    
    threadDump.value = {
      threadStats: [
        { threadId: 1, state: 'RUNNABLE' },
        { threadId: 2, state: 'BLOCKED' }
      ]
    }

    stateFilter.value = 'RUNNABLE'
    expect(filteredThreads.value).toHaveLength(1)
    expect(filteredThreads.value[0].threadId).toBe(1)
  })

  it('should filter threads by search query', () => {
    const { threadDump, searchQuery, filteredThreads } = useThreads()
    
    threadDump.value = {
      threadStats: [
        { threadId: 1, threadName: 'main' },
        { threadId: 2, threadName: 'worker' }
      ]
    }

    searchQuery.value = 'work'
    expect(filteredThreads.value).toHaveLength(1)
    expect(filteredThreads.value[0].threadName).toBe('worker')
  })

  it('should count states correctly', () => {
    const { threadDump, stateCounts } = useThreads()
    
    threadDump.value = {
      threadStats: [
        { state: 'RUNNABLE' },
        { state: 'RUNNABLE' },
        { state: 'BLOCKED' }
      ]
    }

    expect(stateCounts.value).toEqual({
      'RUNNABLE': 2,
      'BLOCKED': 1
    })
  })
})
