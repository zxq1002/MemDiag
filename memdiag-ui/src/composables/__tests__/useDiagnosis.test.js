import { describe, it, expect, vi } from 'vitest'
import { useDiagnosis } from '../useDiagnosis'
import axios from 'axios'

vi.mock('axios')

describe('useDiagnosis', () => {
  it('should filter issues by severity', async () => {
    const { result, severityFilter, filteredIssues } = useDiagnosis()
    
    result.value = {
      issues: [
        { title: 'I1', severity: 'CRITICAL' },
        { title: 'I2', severity: 'INFO' }
      ]
    }

    expect(filteredIssues.value).toHaveLength(2)

    severityFilter.value = ['CRITICAL']
    expect(filteredIssues.value).toHaveLength(1)
    expect(filteredIssues.value[0].title).toBe('I1')
  })

  it('should load diagnosis data', async () => {
    const { diagnosisData, loadDiagnosis } = useDiagnosis()
    const mockData = { data: { issues: [] } }
    axios.get.mockResolvedValue({ data: mockData })

    await loadDiagnosis('test-id')

    expect(diagnosisData.value).toEqual(mockData.data)
  })
})
