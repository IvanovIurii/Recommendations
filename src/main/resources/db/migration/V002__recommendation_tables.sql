CREATE TABLE IF NOT EXISTS recommendation (
    rfq_id UUID NOT NULL REFERENCES rfq_core (rfq_id),
    supplier_id UUID NOT NULL,
    unified_supplier_id UUID,
    match_type TEXT,
    model_version TEXT,
    customer_in_need BOOLEAN,
    is_customer BOOLEAN DEFAULT FALSE,
    raw_recommendation_json JSONB,
    recommended_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (rfq_id, supplier_id)
);

CREATE INDEX IF NOT EXISTS idx_recommendation_rfq_id ON recommendation (rfq_id);
CREATE INDEX IF NOT EXISTS idx_recommendation_supplier_id ON recommendation (supplier_id);
CREATE INDEX IF NOT EXISTS idx_recommendation_unified_supplier_id ON recommendation (unified_supplier_id);
CREATE INDEX IF NOT EXISTS idx_recommendation_recommended_at ON recommendation (recommended_at);
CREATE INDEX IF NOT EXISTS idx_recommendation_match_type ON recommendation (match_type);

CREATE TABLE IF NOT EXISTS supplier_profile_snapshot (
    rfq_id UUID NOT NULL,
    supplier_id UUID NOT NULL,
    name TEXT,
    website TEXT,
    profile_url TEXT,
    country TEXT,
    distribution_area TEXT,
    description TEXT,
    description_de TEXT,
    description_en TEXT,
    supplier_types TEXT[],
    products TEXT[],
    keywords TEXT[],
    product_categories TEXT[],
    snapshot_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (rfq_id, supplier_id),
    FOREIGN KEY (rfq_id, supplier_id) REFERENCES recommendation (rfq_id, supplier_id)
);

CREATE TABLE IF NOT EXISTS decision (
    rfq_id UUID NOT NULL,
    supplier_id UUID NOT NULL,
    decision_type TEXT NOT NULL,
    reason TEXT,
    decided_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (rfq_id, supplier_id),
    FOREIGN KEY (rfq_id, supplier_id) REFERENCES recommendation (rfq_id, supplier_id)
);

CREATE INDEX IF NOT EXISTS idx_decision_decided_at ON decision (decided_at);
