CREATE TABLE families (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    owner_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    sync_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE family_members (
    user_id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    family_id UUID NOT NULL REFERENCES families (id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE family_invites (
    family_id UUID PRIMARY KEY REFERENCES families (id) ON DELETE CASCADE,
    code TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE family_items (
    family_id UUID NOT NULL REFERENCES families (id) ON DELETE CASCADE,
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
    added_by TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (family_id, id)
);

CREATE INDEX family_members_family_idx ON family_members (family_id);
CREATE INDEX family_items_version_idx ON family_items (family_id, version);
