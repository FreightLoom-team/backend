package com.truckingsystem.repository;

import com.truckingsystem.entity.LoadStatusHistory;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface LoadStatusHistoryRepository extends Repository<LoadStatusHistory, UUID> {
    LoadStatusHistory save(LoadStatusHistory history);

    List<LoadStatusHistory> findAllByOrganizationIdAndLoadIdOrderByCreatedAtAsc(
            UUID organizationId, UUID loadId);
}
