CREATE TABLE IF NOT EXISTS recommendations_notifications (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    rfq_id UUID,
    supplier_id UUID,
    unified_supplier_id UUID,
    status TEXT,
    created_at TIMESTAMP,
    modified_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rec_notif_rfq_id ON recommendations_notifications (rfq_id);
CREATE INDEX IF NOT EXISTS idx_rec_notif_supplier_id ON recommendations_notifications (supplier_id);
CREATE INDEX IF NOT EXISTS idx_rec_notif_created_at ON recommendations_notifications (created_at);
CREATE INDEX IF NOT EXISTS idx_rec_notif_unified_supplier_id ON recommendations_notifications (unified_supplier_id);
