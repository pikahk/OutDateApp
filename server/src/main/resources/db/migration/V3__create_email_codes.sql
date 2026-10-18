CREATE TABLE email_codes (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose TEXT NOT NULL,
    code TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, purpose)
);
