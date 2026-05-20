import { useState, useEffect, useRef, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  FileText, Play, Search, Brain, Bell, Mail, UserCheck,
  Check, Loader2, ChevronRight, AlertCircle
} from 'lucide-react'
import { createRfq, acceptRfq, getRfq, getRecommendations, selectSupplier, getPipelineEvents } from '../api'

interface PipelineEvent {
  timestamp: string
  stage: string
  message: string
  level: string
  data: Record<string, unknown>
}

interface Recommendation {
  rfqId: string
  supplierId: string
  name: string | null
  matchType: string | null
  country: string | null
  description: string | null
  supplierTypes: string[] | null
  decisionType: string | null
  notificationStatus: string | null
}

type PipelineStage =
  | 'IDLE'
  | 'CREATING'
  | 'CREATED'
  | 'ACCEPTING'
  | 'ACCEPTED'
  | 'TPP_RECALL'
  | 'INFERENCE'
  | 'RECOMMENDATIONS_STORED'
  | 'CNS_NOTIFICATION'
  | 'CNS_FEEDBACK'
  | 'SUPPLIER_SELECTED'

const STAGES: { key: PipelineStage; label: string; icon: typeof FileText }[] = [
  { key: 'CREATED', label: 'RFQ Created', icon: FileText },
  { key: 'ACCEPTED', label: 'Accepted', icon: Play },
  { key: 'TPP_RECALL', label: 'TPP Recall', icon: Search },
  { key: 'INFERENCE', label: 'RLAB Inference', icon: Brain },
  { key: 'RECOMMENDATIONS_STORED', label: 'Stored', icon: Check },
  { key: 'CNS_NOTIFICATION', label: 'CNS Email', icon: Bell },
  { key: 'CNS_FEEDBACK', label: 'Delivered', icon: Mail },
  { key: 'SUPPLIER_SELECTED', label: 'Selected', icon: UserCheck },
]

const DEFAULT_RFQ = {
  email: 'buyer@example.com',
  fullName: 'Max Mustermann',
  countryCode: 'DE',
  title: 'High-End Leather Card Holders / Wallets',
  description: 'We are looking for a new manufacturing partner to relaunch two existing card holder / wallet models. Products: Slim card holder / wallet (2 models). Quantities: Small series, approx. 20-100 pcs per model per year. Materials: Leather will be supplied by us (box calf and exotic leathers). Quality level: Premium / luxury quality.',
  deliveryLocation: 'FR',
  quantity: '20-100 pieces per model per year',
  supplierTypes: ['PRODUCTION'],
  buyerCountry: 'DE',
  categoryId: 100033,
}

export function OnlinePipelinePage() {
  const [stage, setStage] = useState<PipelineStage>('IDLE')
  const [rfqId, setRfqId] = useState<string | null>(null)
  const [rfqData, setRfqData] = useState(DEFAULT_RFQ)
  const [events, setEvents] = useState<PipelineEvent[]>([])
  const [recommendations, setRecommendations] = useState<Recommendation[]>([])
  const [rfqStatus, setRfqStatus] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [polling, setPolling] = useState(false)
  const eventIndexRef = useRef(0)
  const logEndRef = useRef<HTMLDivElement>(null)

  const scrollToBottom = () => {
    logEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [events])

  const pollEvents = useCallback(async (id: string) => {
    try {
      const newEvents = await getPipelineEvents(id, eventIndexRef.current)
      if (newEvents.length > 0) {
        eventIndexRef.current += newEvents.length
        setEvents(prev => [...prev, ...newEvents])
        const lastStage = newEvents[newEvents.length - 1].stage
        if (lastStage && lastStage !== 'IDLE') {
          setStage(lastStage as PipelineStage)
        }
      }
    } catch {
      // ignore
    }
  }, [])

  useEffect(() => {
    if (!polling || !rfqId) return
    const interval = setInterval(() => pollEvents(rfqId), 1000)
    return () => clearInterval(interval)
  }, [polling, rfqId, pollEvents])

  useEffect(() => {
    if (!rfqId || stage === 'IDLE' || stage === 'CREATING') return
    if (stage === 'RECOMMENDATIONS_STORED' || stage === 'CNS_NOTIFICATION' || stage === 'CNS_FEEDBACK' || stage === 'SUPPLIER_SELECTED') {
      loadRecommendations(rfqId)
    }
  }, [stage, rfqId])

  // Keep polling recommendations to pick up notification status changes from DB
  useEffect(() => {
    if (!rfqId || !rfqStatus || rfqStatus !== 'PROCESSED') return
    const hasAllDelivered = recommendations.length > 0 && recommendations.every(r => r.notificationStatus === 'SENT' || r.decisionType)
    if (hasAllDelivered) return
    const interval = setInterval(() => loadRecommendations(rfqId), 2000)
    return () => clearInterval(interval)
  }, [rfqId, rfqStatus, recommendations])

  const loadRecommendations = async (id: string) => {
    try {
      const data = await getRecommendations(id)
      setRecommendations(data.result || [])
    } catch {
      // ignore
    }
  }

  const handleCreateRfq = async () => {
    setError(null)
    setStage('CREATING')
    setEvents([])
    setRecommendations([])
    eventIndexRef.current = 0
    try {
      const result = await createRfq(rfqData)
      setRfqId(result.rfqId)
      setRfqStatus(result.status)
      setStage('CREATED')
      setPolling(true)
    } catch (e) {
      setError((e as Error).message)
      setStage('IDLE')
    }
  }

  const handleAcceptRfq = async () => {
    if (!rfqId) return
    setError(null)
    setStage('ACCEPTING')
    try {
      await acceptRfq(rfqId)
      setStage('ACCEPTED')
      const pollForProcessed = setInterval(async () => {
        try {
          const rfq = await getRfq(rfqId)
          setRfqStatus(rfq.status)
          if (rfq.status === 'PROCESSED') {
            clearInterval(pollForProcessed)
          }
        } catch { /* ignore */ }
      }, 1500)
      setTimeout(() => clearInterval(pollForProcessed), 60000)
    } catch (e) {
      setError((e as Error).message)
      setStage('CREATED')
    }
  }

  const handleSelectSupplier = async (supplierId: string) => {
    if (!rfqId) return
    setError(null)
    try {
      await selectSupplier(rfqId, supplierId, 'Best match for our requirements')
      setStage('SUPPLIER_SELECTED')
      await loadRecommendations(rfqId)
    } catch (e) {
      setError((e as Error).message)
    }
  }

  const handleReset = () => {
    setStage('IDLE')
    setRfqId(null)
    setEvents([])
    setRecommendations([])
    setRfqStatus(null)
    setError(null)
    setPolling(false)
    eventIndexRef.current = 0
  }

  const getStageIndex = (s: PipelineStage) => STAGES.findIndex(st => st.key === s)
  const currentStageIndex = getStageIndex(stage)

  return (
    <div className="online-pipeline">
      <div className="online-layout">
        {/* Left: Form + Controls */}
        <div className="online-panel left-panel">
          <div className="panel-header">
            <h2>Online Recommendation Pipeline</h2>
            {rfqId && (
              <button className="btn-reset" onClick={handleReset}>New RFQ</button>
            )}
          </div>

          {stage === 'IDLE' && (
            <motion.div
              className="rfq-form"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
            >
              <div className="form-group">
                <label>RFQ Title</label>
                <input
                  value={rfqData.title}
                  onChange={e => setRfqData(d => ({ ...d, title: e.target.value }))}
                />
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea
                  rows={3}
                  value={rfqData.description}
                  onChange={e => setRfqData(d => ({ ...d, description: e.target.value }))}
                />
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Delivery</label>
                  <input
                    value={rfqData.deliveryLocation}
                    onChange={e => setRfqData(d => ({ ...d, deliveryLocation: e.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label>Quantity</label>
                  <input
                    value={rfqData.quantity}
                    onChange={e => setRfqData(d => ({ ...d, quantity: e.target.value }))}
                  />
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Buyer Country</label>
                  <input
                    value={rfqData.buyerCountry}
                    onChange={e => setRfqData(d => ({ ...d, buyerCountry: e.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label>Category ID</label>
                  <input
                    type="number"
                    value={rfqData.categoryId}
                    onChange={e => setRfqData(d => ({ ...d, categoryId: Number(e.target.value) }))}
                  />
                </div>
              </div>
              <button className="btn-primary" onClick={handleCreateRfq}>
                <FileText size={16} /> Create RFQ
              </button>
            </motion.div>
          )}

          {stage !== 'IDLE' && (
            <div className="pipeline-controls">
              <div className="rfq-info-card">
                <span className="rfq-label">RFQ ID</span>
                <code className="rfq-id">{rfqId}</code>
                {rfqStatus && <span className={`rfq-status-badge ${rfqStatus.toLowerCase()}`}>{rfqStatus}</span>}
              </div>

              {stage === 'CREATED' && (
                <motion.button
                  className="btn-primary btn-accept"
                  onClick={handleAcceptRfq}
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  whileHover={{ scale: 1.02 }}
                >
                  <Play size={16} /> Accept RFQ (Trigger Pipeline)
                </motion.button>
              )}

              {/* Recommendations list */}
              {recommendations.length > 0 && (
                <motion.div
                  className="recommendations-panel"
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                >
                  <h3>Recommended Suppliers ({recommendations.length})</h3>
                  <div className="recommendation-list">
                    {recommendations.map((rec) => (
                      <div key={rec.supplierId} className={`recommendation-card ${rec.decisionType ? 'selected' : ''}`}>
                        <div className="rec-header">
                          <span className="rec-name">{rec.name || 'Unknown Supplier'}</span>
                          <span className={`match-badge ${rec.matchType?.toLowerCase()}`}>{rec.matchType}</span>
                        </div>
                        <div className="rec-meta">
                          {rec.country && <span>{rec.country}</span>}
                          {rec.supplierTypes && <span>{rec.supplierTypes.join(', ')}</span>}
                        </div>
                        {rec.description && (
                          <p className="rec-desc">{rec.description.substring(0, 120)}...</p>
                        )}
                        {rec.decisionType ? (
                          <div className="rec-selected-badge">
                            <Check size={14} /> Selected
                          </div>
                        ) : rec.notificationStatus === 'SENT' ? (
                          <button
                            className="btn-select"
                            onClick={() => handleSelectSupplier(rec.supplierId)}
                          >
                            <UserCheck size={14} /> Select Supplier
                          </button>
                        ) : rec.notificationStatus === 'ON_WAIT' ? (
                          <div className="rec-waiting-badge">
                            <Mail size={14} /> Awaiting email delivery...
                          </div>
                        ) : null}
                      </div>
                    ))}
                  </div>
                </motion.div>
              )}
            </div>
          )}

          {error && (
            <div className="error-banner">
              <AlertCircle size={16} /> {error}
            </div>
          )}
        </div>

        {/* Right: Pipeline flow + Logs */}
        <div className="online-panel right-panel">
          {/* Pipeline stages */}
          <div className="stage-tracker">
            {STAGES.map((s, i) => {
              const Icon = s.icon
              const isActive = i <= currentStageIndex && stage !== 'IDLE'
              const isCurrent = s.key === stage
              return (
                <div key={s.key} className={`stage-item ${isActive ? 'active' : ''} ${isCurrent ? 'current' : ''}`}>
                  <div className="stage-icon-wrapper">
                    {isCurrent && stage !== 'SUPPLIER_SELECTED' && stage !== 'CREATED' ? (
                      <motion.div animate={{ rotate: 360 }} transition={{ repeat: Infinity, duration: 2, ease: 'linear' }}>
                        <Loader2 size={16} />
                      </motion.div>
                    ) : isActive ? (
                      <Check size={16} />
                    ) : (
                      <Icon size={16} />
                    )}
                  </div>
                  <span className="stage-label">{s.label}</span>
                  {i < STAGES.length - 1 && (
                    <ChevronRight size={12} className="stage-arrow" />
                  )}
                </div>
              )
            })}
          </div>

          {/* Event log */}
          <div className="event-log">
            <div className="log-header">
              <h3>Pipeline Events</h3>
              <span className="event-count">{events.length} events</span>
            </div>
            <div className="log-body">
              <AnimatePresence>
                {events.length === 0 && stage === 'IDLE' && (
                  <div className="log-empty">Create an RFQ to see the pipeline events here</div>
                )}
                {events.map((event, i) => (
                  <motion.div
                    key={i}
                    className={`log-entry ${event.level.toLowerCase()}`}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ duration: 0.2 }}
                  >
                    <span className="log-time">
                      {new Date(event.timestamp).toLocaleTimeString()}
                    </span>
                    <span className={`log-stage ${event.stage.toLowerCase()}`}>
                      {event.stage}
                    </span>
                    <span className="log-message">{event.message}</span>
                  </motion.div>
                ))}
              </AnimatePresence>
              <div ref={logEndRef} />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
