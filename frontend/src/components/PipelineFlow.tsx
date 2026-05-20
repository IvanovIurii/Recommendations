import { motion } from 'framer-motion'
import { Database, Upload, Brain, Archive, RefreshCw, Check, X, Loader2 } from 'lucide-react'
import type { PipelineRun, PipelineStep, StepStatus } from '../types'

interface Props {
  run: PipelineRun
}

const stepIcons = [Database, Upload, Brain, Archive, RefreshCw]

function StepIcon({ index, status }: { index: number; status: StepStatus }) {
  const Icon = stepIcons[index]

  if (status === 'RUNNING') {
    return (
      <motion.div
        className="step-icon running"
        animate={{ rotate: 360 }}
        transition={{ repeat: Infinity, duration: 2, ease: 'linear' }}
      >
        <Loader2 size={22} />
      </motion.div>
    )
  }

  if (status === 'COMPLETED') {
    return (
      <motion.div
        className="step-icon completed"
        initial={{ scale: 0 }}
        animate={{ scale: 1 }}
        transition={{ type: 'spring', stiffness: 300, damping: 20 }}
      >
        <Check size={22} />
      </motion.div>
    )
  }

  if (status === 'FAILED') {
    return (
      <motion.div
        className="step-icon failed"
        initial={{ scale: 0 }}
        animate={{ scale: 1 }}
        transition={{ type: 'spring', stiffness: 300, damping: 20 }}
      >
        <X size={22} />
      </motion.div>
    )
  }

  return (
    <div className="step-icon pending">
      <Icon size={22} />
    </div>
  )
}

function getDuration(step: PipelineStep): string | null {
  if (!step.startedAt || !step.completedAt) return null
  const ms = new Date(step.completedAt).getTime() - new Date(step.startedAt).getTime()
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

export function PipelineFlow({ run }: Props) {
  return (
    <div className="pipeline-flow">
      <div className="flow-progress-track">
        <motion.div
          className={`flow-progress-bar ${run.status === 'FAILED' ? 'failed' : ''}`}
          initial={{ width: '0%' }}
          animate={{
            width: `${(run.steps.filter(s => s.status === 'COMPLETED' || s.status === 'FAILED').length / run.steps.length) * 100}%`,
          }}
          transition={{ duration: 0.6, ease: 'easeOut' }}
        />
      </div>

      <div className="flow-steps">
        {run.steps.map((step, i) => (
          <motion.div
            key={step.index}
            className={`flow-step ${step.status.toLowerCase()}`}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.1 }}
          >
            <div className="step-connector-line">
              {i < run.steps.length - 1 && (
                <div className={`connector ${run.steps[i + 1].status !== 'PENDING' ? 'active' : ''}`} />
              )}
            </div>

            <div className="step-card">
              <div className="step-card-header">
                <StepIcon index={step.index} status={step.status} />
                <div className="step-info">
                  <span className="step-number">Step {step.index + 1}</span>
                  <h3 className="step-name">{step.name}</h3>
                </div>
                {getDuration(step) && (
                  <span className="step-duration">{getDuration(step)}</span>
                )}
              </div>

              <p className="step-description">{step.description}</p>

              {step.detail && (
                <motion.div
                  className="step-detail"
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  transition={{ duration: 0.3 }}
                >
                  <code>{step.detail}</code>
                </motion.div>
              )}
            </div>
          </motion.div>
        ))}
      </div>

      <div className="run-summary">
        <div className={`summary-badge ${run.status.toLowerCase()}`}>
          {run.status === 'RUNNING' && 'Pipeline Running...'}
          {run.status === 'COMPLETED' && 'Pipeline Completed Successfully'}
          {run.status === 'FAILED' && 'Pipeline Failed'}
          {run.status === 'PENDING' && 'Pipeline Pending'}
        </div>
        {run.completedAt && (
          <span className="summary-time">
            Total: {((new Date(run.completedAt).getTime() - new Date(run.startedAt).getTime()) / 1000).toFixed(1)}s
          </span>
        )}
      </div>
    </div>
  )
}
