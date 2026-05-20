import type { PipelineRun } from '../types'
import { PipelineFlow } from './PipelineFlow'
import { PipelineHistory } from './PipelineHistory'
import { EmptyState } from './EmptyState'

interface Props {
  runs: PipelineRun[]
  loading: boolean
}

export function PipelineDashboard({ runs, loading }: Props) {
  if (loading) {
    return (
      <div className="dashboard-loading">
        <div className="spinner" />
        <p>Connecting to pipeline service...</p>
      </div>
    )
  }

  if (runs.length === 0) {
    return <EmptyState />
  }

  const latestRun = runs[0]
  const previousRuns = runs.slice(1)

  return (
    <div className="dashboard">
      <section className="dashboard-section">
        <div className="section-header">
          <h2>Current Pipeline Run</h2>
          <div className="run-meta">
            <span className="model-badge">{latestRun.modelVersion}</span>
            <span className="time-badge">
              {new Date(latestRun.startedAt).toLocaleTimeString()}
            </span>
          </div>
        </div>
        <PipelineFlow run={latestRun} />
      </section>

      {previousRuns.length > 0 && (
        <section className="dashboard-section">
          <div className="section-header">
            <h2>Pipeline History</h2>
            <span className="history-count">{previousRuns.length} previous run{previousRuns.length > 1 ? 's' : ''}</span>
          </div>
          <PipelineHistory runs={previousRuns} />
        </section>
      )}
    </div>
  )
}
