import { useState, useEffect } from 'react'
import { PipelineDashboard } from '../components/PipelineDashboard'
import type { PipelineRun } from '../types'
import { getPipelineRuns } from '../api'

export function OfflinePipelinePage() {
  const [runs, setRuns] = useState<PipelineRun[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchRuns = async () => {
      try {
        const data = await getPipelineRuns()
        setRuns(data)
      } catch {
        // Backend not available
      } finally {
        setLoading(false)
      }
    }

    fetchRuns()
    const interval = setInterval(fetchRuns, 2000)
    return () => clearInterval(interval)
  }, [])

  return <PipelineDashboard runs={runs} loading={loading} />
}
