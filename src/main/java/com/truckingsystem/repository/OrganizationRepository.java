package com.truckingsystem.repository;

import com.truckingsystem.entity.Organization;
import com.truckingsystem.entity.OrganizationStatus;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends Repository<Organization, UUID> {
    Organization save(Organization organization);

    Optional<Organization> findByIdAndStatus(UUID id, OrganizationStatus status);
}
