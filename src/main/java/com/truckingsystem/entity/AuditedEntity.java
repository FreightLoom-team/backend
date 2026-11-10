package com.truckingsystem.entity;

import com.truckingsystem.common.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class AuditedEntity {
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        TenantContext.current().ifPresent(scope -> {
            createdBy = scope.userId();
            updatedBy = scope.userId();
        });
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        TenantContext.current().ifPresent(scope -> updatedBy = scope.userId());
    }
}
