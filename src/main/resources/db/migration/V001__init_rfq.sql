CREATE TABLE IF NOT EXISTS rfq_user (
    rfq_user_id BIGSERIAL NOT NULL PRIMARY KEY,
    user_profile_id UUID NULL,
    email TEXT NOT NULL,
    full_name TEXT,
    country_code TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_rfq_user_email ON rfq_user (email);
CREATE INDEX IF NOT EXISTS idx_rfq_user_profile_id ON rfq_user (user_profile_id);

CREATE TABLE IF NOT EXISTS rfq_core (
    rfq_id UUID NOT NULL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES rfq_user (rfq_user_id),
    title TEXT,
    description TEXT,
    delivery_location TEXT,
    quantity TEXT,
    supplier_types TEXT[],
    status TEXT NOT NULL,
    buyer_country TEXT,
    category_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_rfq_core_sender_id ON rfq_core (sender_id);
CREATE INDEX IF NOT EXISTS idx_rfq_core_status ON rfq_core (status);
CREATE INDEX IF NOT EXISTS idx_rfq_core_created_at ON rfq_core (created_at);
