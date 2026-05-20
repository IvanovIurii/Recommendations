export type StepStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'

export interface PipelineStep {
  index: number
  name: string
  description: string
  status: StepStatus
  startedAt: string | null
  completedAt: string | null
  detail: string | null
}

export interface PipelineRun {
  id: string
  modelVersion: string
  startedAt: string
  completedAt: string | null
  status: StepStatus
  steps: PipelineStep[]
}
