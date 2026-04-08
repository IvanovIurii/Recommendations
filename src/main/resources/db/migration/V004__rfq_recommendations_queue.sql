CREATE TABLE IF NOT EXISTS rfq_recommendations_queue (
    id BIGSERIAL PRIMARY KEY,
    rfq_id UUID NOT NULL,
    queue_type TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processing_attempts INT NOT NULL DEFAULT 0,
    CONSTRAINT rfq_recommendations_queue_rfq_type_uniq UNIQUE (rfq_id, queue_type)
);

CREATE INDEX IF NOT EXISTS idx_rfq_recommendations_queue_created_at
    ON rfq_recommendations_queue (created_at);
