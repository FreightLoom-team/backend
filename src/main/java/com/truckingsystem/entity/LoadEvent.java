package com.truckingsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "load_events")
public class LoadEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id")
    private UUID id;

    @NotNull
    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @NotNull
    @Column(name = "load_id", nullable = false, updatable = false)
    private UUID loadId;

    @NotNull
    @Size(max = 40)
    @Column(name = "event_type", nullable = false, length = 40, updatable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", length = 30, updatable = false)
    private LoadStatus eventStatus;

    @Size(max = 4000)
    @Column(name = "event_description", updatable = false)
    private String eventDescription;

    @NotNull
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @NotNull
    @Column(name = "recorded_by_user_id", nullable = false, updatable = false)
    private UUID recordedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
