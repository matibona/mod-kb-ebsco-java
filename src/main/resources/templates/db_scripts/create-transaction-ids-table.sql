ALTER TABLE transaction_ids
DROP COLUMN IF EXISTS jsonb,
DROP CONSTRAINT IF EXISTS transaction_ids_pkey,
DROP COLUMN IF EXISTS id,
ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(50),
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
