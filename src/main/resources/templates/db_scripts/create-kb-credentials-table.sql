ALTER TABLE kb_credentials
DROP COLUMN IF EXISTS jsonb,
ADD COLUMN IF NOT EXISTS name VARCHAR (255) NOT NULL UNIQUE,
ADD COLUMN IF NOT EXISTS customer_id VARCHAR (100) NOT NULL,
ADD COLUMN IF NOT EXISTS api_key VARCHAR (100) NOT NULL,
ADD COLUMN IF NOT EXISTS url VARCHAR (100) NOT NULL,
ADD COLUMN IF NOT EXISTS created_date TIMESTAMP WITH TIME ZONE NOT NULL,
ADD COLUMN IF NOT EXISTS created_by_user_id UUID NOT NULL,
ADD COLUMN IF NOT EXISTS created_by_user_name VARCHAR (100) NOT NULL,
ADD COLUMN IF NOT EXISTS updated_date TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS updated_by_user_id UUID,
ADD COLUMN IF NOT EXISTS updated_by_user_name VARCHAR (100);
