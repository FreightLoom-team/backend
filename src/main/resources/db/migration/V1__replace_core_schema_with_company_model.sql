-- Destructive replacement of the initial organization-based schema.
-- Existing organizations, users, memberships, loads, and load history are discarded.

DROP TABLE IF EXISTS load_status_history CASCADE;
DROP TABLE IF EXISTS loads CASCADE;
DROP TABLE IF EXISTS organization_members CASCADE;
DROP TABLE IF EXISTS organizations CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP FUNCTION IF EXISTS prevent_load_status_history_mutation();

CREATE TABLE companies (
    company_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_code VARCHAR(40) NOT NULL UNIQUE,
    legal_name VARCHAR(200) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    tax_identifier VARCHAR(40),
    email VARCHAR(254),
    phone VARCHAR(30),
    website VARCHAR(255),
    address_line1 VARCHAR(160),
    address_line2 VARCHAR(160),
    city VARCHAR(100),
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country_code CHAR(2) NOT NULL DEFAULT 'US',
    time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    company_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (company_status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_subject VARCHAR(255) UNIQUE,
    email VARCHAR(254) UNIQUE,
    phone VARCHAR(30),
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    display_name VARCHAR(160),
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (account_status IN ('INVITED', 'ACTIVE', 'LOCKED', 'DISABLED')),
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_email_or_phone_ck CHECK (email IS NOT NULL OR phone IS NOT NULL)
);

CREATE TABLE company_memberships (
    membership_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    user_id UUID NOT NULL REFERENCES users(user_id),
    role VARCHAR(24) NOT NULL
        CHECK (role IN (
            'COMPANY_ADMIN', 'DISPATCHER', 'DRIVER', 'SAFETY_MANAGER',
            'ACCOUNTANT', 'FLEET_MANAGER'
        )),
    membership_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (membership_status IN ('INVITED', 'ACTIVE', 'SUSPENDED', 'REMOVED')),
    invited_by_user_id UUID REFERENCES users(user_id),
    joined_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, user_id),
    UNIQUE (company_id, membership_id),
    UNIQUE (company_id, user_id, membership_id)
);

CREATE TABLE drivers (
    driver_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    user_id UUID,
    driver_code VARCHAR(40) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(254),
    license_number VARCHAR(60),
    license_state VARCHAR(50),
    license_country CHAR(2) DEFAULT 'US',
    license_expiry_date DATE,
    date_of_birth DATE,
    employment_type VARCHAR(20) NOT NULL DEFAULT 'COMPANY'
        CHECK (employment_type IN ('COMPANY', 'INDEPENDENT', 'CONTRACTOR')),
    driver_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (driver_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    emergency_contact VARCHAR(160),
    emergency_phone VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, driver_id),
    UNIQUE (company_id, driver_code),
    CONSTRAINT drivers_membership_fk FOREIGN KEY (company_id, user_id)
        REFERENCES company_memberships(company_id, user_id)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE trucks (
    truck_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    truck_number VARCHAR(40) NOT NULL,
    vin VARCHAR(17),
    make VARCHAR(80),
    model VARCHAR(80),
    model_year SMALLINT CHECK (model_year BETWEEN 1900 AND 2200),
    license_plate VARCHAR(20),
    plate_state VARCHAR(50),
    truck_type VARCHAR(40),
    capacity_lbs NUMERIC(12, 2) CHECK (capacity_lbs IS NULL OR capacity_lbs >= 0),
    current_status VARCHAR(24) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (current_status IN ('AVAILABLE', 'ASSIGNED', 'IN_MAINTENANCE', 'OUT_OF_SERVICE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, truck_id),
    UNIQUE (company_id, truck_number)
);

CREATE TABLE trailers (
    trailer_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    trailer_number VARCHAR(40) NOT NULL,
    vin VARCHAR(17),
    trailer_type VARCHAR(40),
    license_plate VARCHAR(20),
    plate_state VARCHAR(50),
    capacity_lbs NUMERIC(12, 2) CHECK (capacity_lbs IS NULL OR capacity_lbs >= 0),
    trailer_status VARCHAR(24) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (trailer_status IN ('AVAILABLE', 'ASSIGNED', 'IN_MAINTENANCE', 'OUT_OF_SERVICE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, trailer_id),
    UNIQUE (company_id, trailer_number)
);

CREATE TABLE loads (
    load_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    load_number VARCHAR(50) NOT NULL,
    broker_name VARCHAR(160),
    broker_reference VARCHAR(80),
    broker_contact_name VARCHAR(120),
    broker_phone VARCHAR(30),
    broker_email VARCHAR(254),
    rate_amount NUMERIC(12, 2) CHECK (rate_amount IS NULL OR rate_amount >= 0),
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN (
            'DRAFT', 'OFFERED', 'ACCEPTED', 'ASSIGNED', 'PRE_TRIP', 'AT_PICKUP',
            'PICKED_UP', 'IN_TRANSIT', 'AT_DELIVERY', 'DELIVERED', 'PENDING_REVIEW',
            'NEEDS_CORRECTION', 'COMPLETED', 'CANCELLED', 'DELIVERY_EXCEPTION'
        )),
    equipment_type VARCHAR(50),
    commodity VARCHAR(160),
    weight_lbs NUMERIC(12, 2) CHECK (weight_lbs IS NULL OR weight_lbs >= 0),
    pickup_deadline TIMESTAMPTZ,
    delivery_deadline TIMESTAMPTZ,
    dispatcher_notes TEXT,
    special_instructions TEXT,
    created_by_user_id UUID NOT NULL REFERENCES users(user_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    UNIQUE (company_id, load_id),
    UNIQUE (company_id, load_number)
);

CREATE TABLE load_stops (
    stop_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    load_id UUID NOT NULL,
    stop_number SMALLINT NOT NULL CHECK (stop_number > 0),
    stop_type VARCHAR(20) NOT NULL CHECK (stop_type IN ('PICKUP', 'DELIVERY', 'STOP')),
    facility_name VARCHAR(160),
    contact_name VARCHAR(120),
    contact_phone VARCHAR(30),
    address_line1 VARCHAR(160) NOT NULL,
    address_line2 VARCHAR(160),
    city VARCHAR(100) NOT NULL,
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country_code CHAR(2) NOT NULL DEFAULT 'US',
    latitude NUMERIC(9, 6) CHECK (latitude BETWEEN -90 AND 90),
    longitude NUMERIC(9, 6) CHECK (longitude BETWEEN -180 AND 180),
    appointment_at TIMESTAMPTZ,
    arrived_at TIMESTAMPTZ,
    departed_at TIMESTAMPTZ,
    stop_instructions TEXT,
    stop_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (stop_status IN ('PENDING', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED', 'EXCEPTION')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, stop_id),
    UNIQUE (company_id, load_id, stop_number),
    FOREIGN KEY (company_id, load_id) REFERENCES loads(company_id, load_id)
);

CREATE TABLE load_offers (
    offer_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    load_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    offered_by_user_id UUID NOT NULL REFERENCES users(user_id),
    offer_status VARCHAR(20) NOT NULL DEFAULT 'SENT'
        CHECK (offer_status IN ('SENT', 'VIEWED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'EXPIRED', 'SUPERSEDED')),
    offered_rate NUMERIC(12, 2) CHECK (offered_rate IS NULL OR offered_rate >= 0),
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    viewed_at TIMESTAMPTZ,
    responded_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    driver_response_note TEXT,
    UNIQUE (company_id, offer_id),
    FOREIGN KEY (company_id, load_id) REFERENCES loads(company_id, load_id),
    FOREIGN KEY (company_id, driver_id) REFERENCES drivers(company_id, driver_id)
);

CREATE TABLE load_assignments (
    assignment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    load_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    truck_id UUID NOT NULL,
    trailer_id UUID,
    offer_id UUID,
    assigned_by_user_id UUID NOT NULL REFERENCES users(user_id),
    assignment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (assignment_status IN ('ACTIVE', 'REASSIGNED', 'CANCELLED', 'COMPLETED')),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unassigned_at TIMESTAMPTZ,
    assignment_notes TEXT,
    UNIQUE (company_id, assignment_id),
    FOREIGN KEY (company_id, load_id) REFERENCES loads(company_id, load_id),
    FOREIGN KEY (company_id, driver_id) REFERENCES drivers(company_id, driver_id),
    FOREIGN KEY (company_id, truck_id) REFERENCES trucks(company_id, truck_id),
    FOREIGN KEY (company_id, trailer_id) REFERENCES trailers(company_id, trailer_id),
    FOREIGN KEY (company_id, offer_id) REFERENCES load_offers(company_id, offer_id)
);

CREATE UNIQUE INDEX uq_load_one_active_assignment
    ON load_assignments(company_id, load_id)
    WHERE assignment_status = 'ACTIVE';

CREATE TABLE load_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    load_id UUID NOT NULL,
    stop_id UUID,
    event_type VARCHAR(40) NOT NULL,
    event_status VARCHAR(30),
    event_description TEXT,
    event_location_text VARCHAR(255),
    latitude NUMERIC(9, 6) CHECK (latitude BETWEEN -90 AND 90),
    longitude NUMERIC(9, 6) CHECK (longitude BETWEEN -180 AND 180),
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_by_user_id UUID NOT NULL REFERENCES users(user_id),
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL'
        CHECK (source IN ('MANUAL', 'MOBILE_APP', 'SYSTEM', 'GPS', 'API')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, event_id),
    FOREIGN KEY (company_id, load_id) REFERENCES loads(company_id, load_id),
    FOREIGN KEY (company_id, stop_id) REFERENCES load_stops(company_id, stop_id)
);

CREATE FUNCTION prevent_load_event_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'load events are append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER load_events_are_append_only
    BEFORE UPDATE OR DELETE ON load_events
    FOR EACH ROW EXECUTE FUNCTION prevent_load_event_mutation();

CREATE TABLE load_documents (
    document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    load_id UUID NOT NULL,
    stop_id UUID,
    uploaded_by_user_id UUID NOT NULL REFERENCES users(user_id),
    document_type VARCHAR(30) NOT NULL
        CHECK (document_type IN (
            'RATE_CONFIRMATION', 'BOL', 'POD', 'RECEIPT', 'TRUCK_PHOTO',
            'INSPECTION_PHOTO', 'OTHER'
        )),
    original_file_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(700) NOT NULL,
    mime_type VARCHAR(120),
    file_size_bytes BIGINT CHECK (file_size_bytes IS NULL OR file_size_bytes >= 0),
    checksum_sha256 CHAR(64),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    document_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (document_status IN ('ACTIVE', 'SUPERSEDED', 'DELETED')),
    notes TEXT,
    UNIQUE (company_id, document_id),
    FOREIGN KEY (company_id, load_id) REFERENCES loads(company_id, load_id),
    FOREIGN KEY (company_id, stop_id) REFERENCES load_stops(company_id, stop_id)
);

CREATE TABLE trip_expenses (
    expense_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    load_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    expense_type VARCHAR(30) NOT NULL
        CHECK (expense_type IN ('TOLL', 'DETENTION', 'LUMPER', 'FUEL', 'PARKING', 'REPAIR', 'OTHER')),
    description VARCHAR(500),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    incurred_at TIMESTAMPTZ NOT NULL,
    submitted_by_user_id UUID NOT NULL REFERENCES users(user_id),
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'NEEDS_INFO')),
    reviewed_by_user_id UUID REFERENCES users(user_id),
    reviewed_at TIMESTAMPTZ,
    review_note TEXT,
    receipt_document_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, expense_id),
    FOREIGN KEY (company_id, load_id) REFERENCES loads(company_id, load_id),
    FOREIGN KEY (company_id, driver_id) REFERENCES drivers(company_id, driver_id),
    FOREIGN KEY (company_id, receipt_document_id) REFERENCES load_documents(company_id, document_id)
);

CREATE TABLE vehicle_inspections (
    inspection_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    driver_id UUID NOT NULL,
    truck_id UUID NOT NULL,
    trailer_id UUID,
    load_id UUID,
    inspection_type VARCHAR(20) NOT NULL
        CHECK (inspection_type IN ('PRE_TRIP', 'POST_TRIP', 'ROADSIDE', 'PERIODIC')),
    inspection_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
        CHECK (inspection_status IN ('IN_PROGRESS', 'PASSED', 'FAILED', 'SUBMITTED')),
    inspected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_by_user_id UUID NOT NULL REFERENCES users(user_id),
    odometer_miles NUMERIC(12, 1) CHECK (odometer_miles IS NULL OR odometer_miles >= 0),
    general_notes TEXT,
    reviewed_by_user_id UUID REFERENCES users(user_id),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, inspection_id),
    FOREIGN KEY (company_id, driver_id) REFERENCES drivers(company_id, driver_id),
    FOREIGN KEY (company_id, truck_id) REFERENCES trucks(company_id, truck_id),
    FOREIGN KEY (company_id, trailer_id) REFERENCES trailers(company_id, trailer_id),
    FOREIGN KEY (company_id, load_id) REFERENCES loads(company_id, load_id)
);

CREATE TABLE inspection_defects (
    defect_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    inspection_id UUID NOT NULL,
    component VARCHAR(100) NOT NULL,
    defect_description TEXT NOT NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'MINOR'
        CHECK (severity IN ('MINOR', 'MAJOR', 'CRITICAL')),
    out_of_service BOOLEAN NOT NULL DEFAULT FALSE,
    resolution_status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (resolution_status IN ('OPEN', 'IN_REPAIR', 'RESOLVED', 'ACCEPTED_RISK')),
    resolved_at TIMESTAMPTZ,
    resolution_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, defect_id),
    FOREIGN KEY (company_id, inspection_id) REFERENCES vehicle_inspections(company_id, inspection_id)
);

CREATE TABLE safety_incidents (
    incident_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    driver_id UUID,
    truck_id UUID,
    trailer_id UUID,
    load_id UUID,
    inspection_id UUID,
    incident_type VARCHAR(30) NOT NULL
        CHECK (incident_type IN (
            'POLICE_CITATION', 'INSPECTION_FAILURE', 'ACCIDENT', 'VEHICLE_DEFECT', 'DAMAGE', 'OTHER'
        )),
    severity VARCHAR(16) NOT NULL DEFAULT 'MINOR'
        CHECK (severity IN ('MINOR', 'MODERATE', 'MAJOR', 'CRITICAL')),
    incident_at TIMESTAMPTZ NOT NULL,
    location_description VARCHAR(255),
    description TEXT NOT NULL,
    citation_number VARCHAR(80),
    fine_amount NUMERIC(12, 2) CHECK (fine_amount IS NULL OR fine_amount >= 0),
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    fine_status VARCHAR(20)
        CHECK (fine_status IS NULL OR fine_status IN ('NOT_APPLICABLE', 'UNPAID', 'PAID', 'DISPUTED', 'WAIVED')),
    reported_by_user_id UUID NOT NULL REFERENCES users(user_id),
    assigned_to_user_id UUID REFERENCES users(user_id),
    resolution_status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (resolution_status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'CLOSED')),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, incident_id),
    FOREIGN KEY (company_id, driver_id) REFERENCES drivers(company_id, driver_id),
    FOREIGN KEY (company_id, truck_id) REFERENCES trucks(company_id, truck_id),
    FOREIGN KEY (company_id, trailer_id) REFERENCES trailers(company_id, trailer_id),
    FOREIGN KEY (company_id, load_id) REFERENCES loads(company_id, load_id),
    FOREIGN KEY (company_id, inspection_id) REFERENCES vehicle_inspections(company_id, inspection_id)
);

CREATE TABLE maintenance_records (
    maintenance_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    truck_id UUID,
    trailer_id UUID,
    maintenance_type VARCHAR(30) NOT NULL
        CHECK (maintenance_type IN ('PREVENTIVE', 'REPAIR', 'INSPECTION', 'TIRE', 'OTHER')),
    description TEXT NOT NULL,
    service_provider VARCHAR(160),
    scheduled_at TIMESTAMPTZ,
    performed_at TIMESTAMPTZ,
    odometer_miles NUMERIC(12, 1) CHECK (odometer_miles IS NULL OR odometer_miles >= 0),
    cost_amount NUMERIC(12, 2) CHECK (cost_amount IS NULL OR cost_amount >= 0),
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    maintenance_status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
        CHECK (maintenance_status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    created_by_user_id UUID NOT NULL REFERENCES users(user_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, maintenance_id),
    FOREIGN KEY (company_id, truck_id) REFERENCES trucks(company_id, truck_id),
    FOREIGN KEY (company_id, trailer_id) REFERENCES trailers(company_id, trailer_id),
    CHECK (truck_id IS NOT NULL OR trailer_id IS NOT NULL)
);

CREATE TABLE location_updates (
    location_update_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    driver_id UUID NOT NULL,
    load_id UUID,
    truck_id UUID,
    latitude NUMERIC(9, 6) NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude NUMERIC(9, 6) NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    accuracy_meters NUMERIC(8, 2) CHECK (accuracy_meters IS NULL OR accuracy_meters >= 0),
    speed_kph NUMERIC(7, 2) CHECK (speed_kph IS NULL OR speed_kph >= 0),
    heading_degrees NUMERIC(5, 2) CHECK (heading_degrees IS NULL OR heading_degrees BETWEEN 0 AND 360),
    captured_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(16) NOT NULL DEFAULT 'GPS' CHECK (source IN ('GPS', 'MANUAL')),
    UNIQUE (company_id, location_update_id),
    FOREIGN KEY (company_id, driver_id) REFERENCES drivers(company_id, driver_id),
    FOREIGN KEY (company_id, load_id) REFERENCES loads(company_id, load_id),
    FOREIGN KEY (company_id, truck_id) REFERENCES trucks(company_id, truck_id)
);

CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    recipient_user_id UUID NOT NULL REFERENCES users(user_id),
    notification_type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    related_load_id UUID,
    channel VARCHAR(16) NOT NULL DEFAULT 'IN_APP'
        CHECK (channel IN ('IN_APP', 'PUSH', 'EMAIL', 'SMS')),
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (delivery_status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'READ')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    FOREIGN KEY (company_id, related_load_id) REFERENCES loads(company_id, load_id)
);

CREATE TABLE conversations (
    conversation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    load_id UUID,
    conversation_type VARCHAR(20) NOT NULL DEFAULT 'LOAD'
        CHECK (conversation_type IN ('LOAD', 'GENERAL', 'SUPPORT')),
    subject VARCHAR(160),
    created_by_user_id UUID NOT NULL REFERENCES users(user_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, conversation_id),
    FOREIGN KEY (company_id, load_id) REFERENCES loads(company_id, load_id)
);

CREATE TABLE conversation_members (
    company_id UUID NOT NULL REFERENCES companies(company_id),
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users(user_id),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (company_id, conversation_id, user_id),
    FOREIGN KEY (company_id, conversation_id) REFERENCES conversations(company_id, conversation_id)
);

CREATE TABLE messages (
    message_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    conversation_id UUID NOT NULL,
    sender_user_id UUID NOT NULL REFERENCES users(user_id),
    message_body TEXT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    FOREIGN KEY (company_id, conversation_id) REFERENCES conversations(company_id, conversation_id)
);

CREATE TABLE shift_handoffs (
    handoff_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(company_id),
    outgoing_user_id UUID NOT NULL REFERENCES users(user_id),
    incoming_user_id UUID REFERENCES users(user_id),
    handoff_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    summary TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'CLOSED')),
    acknowledged_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, handoff_id)
);

CREATE TABLE shift_handoff_loads (
    company_id UUID NOT NULL REFERENCES companies(company_id),
    handoff_id UUID NOT NULL,
    load_id UUID NOT NULL,
    handoff_note TEXT,
    PRIMARY KEY (company_id, handoff_id, load_id),
    FOREIGN KEY (company_id, handoff_id) REFERENCES shift_handoffs(company_id, handoff_id),
    FOREIGN KEY (company_id, load_id) REFERENCES loads(company_id, load_id)
);

CREATE TABLE audit_logs (
    audit_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(company_id),
    actor_user_id UUID REFERENCES users(user_id),
    action VARCHAR(60) NOT NULL,
    entity_name VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent VARCHAR(500),
    old_values JSONB,
    new_values JSONB,
    correlation_id UUID
);

CREATE INDEX idx_memberships_user_status ON company_memberships(user_id, membership_status);
CREATE INDEX idx_drivers_company_status ON drivers(company_id, driver_status);
CREATE INDEX idx_trucks_company_status ON trucks(company_id, current_status);
CREATE INDEX idx_loads_company_status_created ON loads(company_id, status, created_at DESC);
CREATE INDEX idx_loads_company_delivery_deadline ON loads(company_id, delivery_deadline);
CREATE INDEX idx_stops_load_sequence ON load_stops(company_id, load_id, stop_number);
CREATE INDEX idx_offers_driver_status ON load_offers(company_id, driver_id, offer_status, sent_at DESC);
CREATE INDEX idx_assignments_driver_status ON load_assignments(company_id, driver_id, assignment_status);
CREATE INDEX idx_events_load_time ON load_events(company_id, load_id, occurred_at);
CREATE INDEX idx_documents_load_type ON load_documents(company_id, load_id, document_type);
CREATE INDEX idx_expenses_company_status_date ON trip_expenses(company_id, approval_status, incurred_at DESC);
CREATE INDEX idx_inspections_driver_date ON vehicle_inspections(company_id, driver_id, inspected_at DESC);
CREATE INDEX idx_incidents_company_date ON safety_incidents(company_id, incident_at DESC);
CREATE INDEX idx_maintenance_truck_status ON maintenance_records(company_id, truck_id, maintenance_status);
CREATE INDEX idx_locations_driver_time ON location_updates(company_id, driver_id, captured_at DESC);
CREATE INDEX idx_locations_load_time ON location_updates(company_id, load_id, captured_at DESC);
CREATE INDEX idx_notifications_recipient_status
    ON notifications(company_id, recipient_user_id, delivery_status, created_at DESC);
CREATE INDEX idx_messages_conversation_time ON messages(company_id, conversation_id, sent_at);
CREATE INDEX idx_audit_company_entity_time
    ON audit_logs(company_id, entity_name, entity_id, occurred_at DESC);

DO $$
DECLARE
    table_name TEXT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'company_memberships', 'drivers', 'trucks', 'trailers', 'loads', 'load_stops',
        'load_offers', 'load_assignments', 'load_events', 'load_documents',
        'trip_expenses', 'vehicle_inspections', 'inspection_defects', 'safety_incidents',
        'maintenance_records', 'location_updates', 'notifications', 'conversations',
        'conversation_members', 'messages', 'shift_handoffs', 'shift_handoff_loads', 'audit_logs'
    ]
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
        EXECUTE format(
            'CREATE POLICY company_isolation ON %I USING (company_id = NULLIF(current_setting(''app.current_company_id'', true), '''')::uuid) WITH CHECK (company_id = NULLIF(current_setting(''app.current_company_id'', true), '''')::uuid)',
            table_name
        );
    END LOOP;
END $$;
