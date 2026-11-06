package com.truckingsystem.repository;

import com.truckingsystem.entity.Load;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadRepository extends Repository<Load, UUID> {
    Load save(Load load);

    Optional<Load> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select load from Load load where load.id = :loadId and load.organizationId = :organizationId")
    Optional<Load> findByIdAndOrganizationIdForUpdate(
            @Param("loadId") UUID loadId, @Param("organizationId") UUID organizationId);

    List<Load> findAllByOrganizationId(UUID organizationId);
}
