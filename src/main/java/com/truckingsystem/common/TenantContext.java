package com.truckingsystem.common;

import java.util.Optional;
import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID organizationId, UUID userId) {
        CURRENT.set(new Scope(organizationId, userId));
    }

    public static Optional<Scope> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record Scope(UUID organizationId, UUID userId) {
    }
}
