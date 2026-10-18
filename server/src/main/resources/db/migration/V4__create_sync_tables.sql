ALTER TABLE users ADD COLUMN sync_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE categories (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    id TEXT NOT NULL,
    name TEXT NOT NULL,
    is_default BOOLEAN NOT NULL,
    is_deleted BOOLEAN NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (user_id, id)
);

CREATE TABLE items (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    id TEXT NOT NULL,
    name TEXT NOT NULL,
    category_id TEXT,
    barcode TEXT,
    expires_at DATE NOT NULL,
    days_after_opening INTEGER,
    opened_at DATE,
    notify_days_before INTEGER NOT NULL,
    created_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (user_id, id)
);

CREATE TABLE subscriptions (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    id TEXT NOT NULL,
    name TEXT NOT NULL,
    price_minor BIGINT NOT NULL,
    currency TEXT NOT NULL,
    started_at DATE NOT NULL,
    period_type TEXT NOT NULL,
    period_days INTEGER,
    notify_days_before INTEGER NOT NULL,
    is_cancelled BOOLEAN NOT NULL,
    is_deleted BOOLEAN NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (user_id, id)
);

CREATE TABLE products (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    barcode TEXT NOT NULL,
    name TEXT NOT NULL,
    category_id TEXT,
    default_days_after_opening INTEGER,
    default_notify_days_before INTEGER,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (user_id, barcode)
);

CREATE INDEX categories_version_idx ON categories (user_id, version);
CREATE INDEX items_version_idx ON items (user_id, version);
CREATE INDEX subscriptions_version_idx ON subscriptions (user_id, version);
CREATE INDEX products_version_idx ON products (user_id, version);
