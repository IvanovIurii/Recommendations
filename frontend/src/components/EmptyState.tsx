import { Workflow } from 'lucide-react'

export function EmptyState() {
  return (
    <div className="empty-state">
      <div className="empty-icon">
        <Workflow size={48} strokeWidth={1.5} />
      </div>
      <h2>No Pipeline Runs Yet</h2>
      <p>
        The offline training pipeline runs automatically after the Spring Boot service starts.
        <br />
        Pipeline status will appear here once the first run begins.
      </p>
      <div className="empty-hint">
        <code>./gradlew bootRun</code>
        <span>to start the backend service</span>
      </div>
    </div>
  )
}
