CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uk_organizations_slug UNIQUE (slug)
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    external_subject VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE UNIQUE INDEX uk_users_email_ci ON users (LOWER(email));

ALTER TABLE organizations
    ADD CONSTRAINT fk_organizations_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    ADD CONSTRAINT fk_organizations_updated_by FOREIGN KEY (updated_by) REFERENCES users (id);

ALTER TABLE users
    ADD CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    ADD CONSTRAINT fk_users_updated_by FOREIGN KEY (updated_by) REFERENCES users (id);

CREATE TABLE organization_members (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    user_id UUID NOT NULL REFERENCES users (id),
    role VARCHAR(20) NOT NULL
        CHECK (role IN ('OWNER', 'ADMIN', 'DISPATCHER', 'DRIVER', 'VIEWER')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INVITED', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    CONSTRAINT uk_org_members_org_user UNIQUE (organization_id, user_id)
);

CREATE INDEX ix_org_members_user ON organization_members (user_id);

CREATE TABLE loads (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    reference VARCHAR(200) NOT NULL,
    pickup_location VARCHAR(200) NOT NULL,
    delivery_location VARCHAR(200) NOT NULL,
    rate_amount NUMERIC(19, 4) NOT NULL CHECK (rate_amount >= 0),
    currency_code VARCHAR(3) NOT NULL CHECK (currency_code ~ '^[A-Z]{3}$'),
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN (
            'DRAFT', 'OFFERED', 'ACCEPTED', 'ASSIGNED', 'PRE_TRIP', 'AT_PICKUP',
            'PICKED_UP', 'IN_TRANSIT', 'AT_DELIVERY', 'DELIVERED', 'PENDING_REVIEW',
            'COMPLETED', 'CLOSED'
        )),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    CONSTRAINT uk_loads_org_reference UNIQUE (organization_id, reference),
    CONSTRAINT uk_loads_id_organization UNIQUE (id, organization_id)
);

CREATE INDEX ix_loads_org_status ON loads (organization_id, status);
CREATE INDEX ix_loads_org_created ON loads (organization_id, created_at DESC);

CREATE TABLE load_status_history (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    load_id UUID NOT NULL,
    status VARCHAR(24) NOT NULL
        CHECK (status IN (
            'DRAFT', 'OFFERED', 'ACCEPTED', 'ASSIGNED', 'PRE_TRIP', 'AT_PICKUP',
            'PICKED_UP', 'IN_TRANSIT', 'AT_DELIVERY', 'DELIVERED', 'PENDING_REVIEW',
            'COMPLETED', 'CLOSED'
        )),
    note VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    CONSTRAINT fk_load_history_scoped_load
        FOREIGN KEY (load_id, organization_id) REFERENCES loads (id, organization_id)
);

CREATE INDEX ix_load_history_org_load_created
    ON load_status_history (organization_id, load_id, created_at);

CREATE FUNCTION prevent_load_status_history_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'load status history is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_load_status_history_append_only
    BEFORE UPDATE OR DELETE ON load_status_history
    FOR EACH ROW EXECUTE FUNCTION prevent_load_status_history_mutation();
