CREATE UNIQUE INDEX IF NOT EXISTS uq_recommendations_notifications_rfq_supplier
    ON recommendations_notifications (rfq_id, supplier_id);
