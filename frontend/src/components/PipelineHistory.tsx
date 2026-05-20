import type { PipelineRun } from '../types'
import { Check, X, Clock } from 'lucide-react'

interface Props {
  runs: PipelineRun[]
}

export function PipelineHistory({ runs }: Props) {
  return (
    <div className="pipeline-history">
      {runs.map((run) => (
        <div key={run.id} className={`history-card ${run.status.toLowerCase()}`}>
          <div className="history-icon">
            {run.status === 'COMPLETED' ? (
              <Check size={16} />
            ) : run.status === 'FAILED' ? (
              <X size={16} />
            ) : (
              <Clock size={16} />
            )}
          </div>
          <div className="history-info">
            <span className="history-model">{run.modelVersion}</span>
            <span className="history-time">
              {new Date(run.startedAt).toLocaleString()}
            </span>
          </div>
          <div className="history-steps">
            {run.steps.map((step) => (
              <div
                key={step.index}
                className={`history-step-dot ${step.status.toLowerCase()}`}
                title={`${step.name}: ${step.status}`}
              />
            ))}
          </div>
          {run.completedAt && (
            <span className="history-duration">
              {((new Date(run.completedAt).getTime() - new Date(run.startedAt).getTime()) / 1000).toFixed(1)}s
            </span>
          )}
        </div>
      ))}
    </div>
  )
}
