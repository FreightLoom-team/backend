package com.truckingsystem.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantDatabaseContext {
    @PersistenceContext
    private EntityManager entityManager;

    private final boolean rowLevelSecurityEnabled;

    public TenantDatabaseContext(
            @Value("${app.database.row-level-security-enabled:true}") boolean rowLevelSecurityEnabled) {
        this.rowLevelSecurityEnabled = rowLevelSecurityEnabled;
    }

    public void activate(UUID companyId) {
        if (!rowLevelSecurityEnabled) {
            return;
        }
        entityManager.createNativeQuery(
                        "select set_config('app.current_company_id', :companyId, true)")
                .setParameter("companyId", companyId.toString())
                .getSingleResult();
    }
}
