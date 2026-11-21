package com.truckingsystem.service;

import com.truckingsystem.common.ResourceNotFoundException;
import com.truckingsystem.common.TenantContext;
import com.truckingsystem.common.InvalidLoadTransitionException;
import com.truckingsystem.entity.Load;
import com.truckingsystem.entity.LoadEvent;
import com.truckingsystem.entity.LoadStatus;
import com.truckingsystem.pojo.CreateLoadRequest;
import com.truckingsystem.repository.LoadEventRepository;
import com.truckingsystem.repository.LoadRepository;
import com.truckingsystem.security.TenantDatabaseContext;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.time.Instant;
import java.util.UUID;

@Service
@Validated
public class LoadStatusService {
    private static final Map<LoadStatus, Set<LoadStatus>> NEXT_STATUS = Map.ofEntries(
            Map.entry(LoadStatus.DRAFT, Set.of(LoadStatus.OFFERED)),
            Map.entry(LoadStatus.OFFERED, Set.of(LoadStatus.ACCEPTED)),
            Map.entry(LoadStatus.ACCEPTED, Set.of(LoadStatus.ASSIGNED)),
            Map.entry(LoadStatus.ASSIGNED, Set.of(LoadStatus.PRE_TRIP)),
            Map.entry(LoadStatus.PRE_TRIP, Set.of(LoadStatus.AT_PICKUP)),
            Map.entry(LoadStatus.AT_PICKUP, Set.of(LoadStatus.PICKED_UP)),
            Map.entry(LoadStatus.PICKED_UP, Set.of(LoadStatus.IN_TRANSIT)),
            Map.entry(LoadStatus.IN_TRANSIT, Set.of(LoadStatus.AT_DELIVERY)),
            Map.entry(LoadStatus.AT_DELIVERY, Set.of(LoadStatus.DELIVERED)),
            Map.entry(LoadStatus.DELIVERED, Set.of(LoadStatus.PENDING_REVIEW)),
            Map.entry(LoadStatus.PENDING_REVIEW, Set.of(LoadStatus.COMPLETED, LoadStatus.NEEDS_CORRECTION)),
            Map.entry(LoadStatus.NEEDS_CORRECTION, Set.of(LoadStatus.PENDING_REVIEW)));
    private static final Set<LoadStatus> TERMINAL_STATUSES =
            EnumSet.of(LoadStatus.COMPLETED, LoadStatus.CANCELLED, LoadStatus.DELIVERY_EXCEPTION);

    private final LoadRepository loadRepository;
    private final LoadEventRepository eventRepository;
    private final TenantDatabaseContext tenantDatabaseContext;

    public LoadStatusService(
            LoadRepository loadRepository,
            LoadEventRepository eventRepository,
            TenantDatabaseContext tenantDatabaseContext) {
        this.loadRepository = loadRepository;
        this.eventRepository = eventRepository;
        this.tenantDatabaseContext = tenantDatabaseContext;
    }

    @Transactional
    public Load createLoad(@Valid @NotNull CreateLoadRequest request) {
        TenantContext.Scope scope = TenantContext.current()
                .orElseThrow(() -> new IllegalStateException("Tenant scope is not established"));
        tenantDatabaseContext.activate(scope.companyId());
        Load load = new Load();
        load.setCompanyId(scope.companyId());
        load.setLoadNumber(request.loadNumber());
        load.setBrokerName(request.brokerName());
        load.setBrokerReference(request.brokerReference());
        load.setRateAmount(request.rateAmount());
        load.setCurrencyCode(request.currencyCode());
        load.setEquipmentType(request.equipmentType());
        load.setCommodity(request.commodity());
        load.setWeightLbs(request.weightLbs());
        load.setPickupDeadline(request.pickupDeadline());
        load.setDeliveryDeadline(request.deliveryDeadline());
        load.setCreatedByUserId(scope.userId());
        load.setStatus(LoadStatus.DRAFT);
        load = loadRepository.save(load);

        recordStatusEvent(scope.companyId(), scope.userId(), load.getId(), LoadStatus.DRAFT, "Load created");
        return load;
    }

    @Transactional
    public Load changeStatus(UUID loadId, @NotNull LoadStatus newStatus, @Size(max = 1000) String note) {
        TenantContext.Scope scope = TenantContext.current()
                .orElseThrow(() -> new IllegalStateException("Tenant scope is not established"));
        tenantDatabaseContext.activate(scope.companyId());
        Load load = loadRepository.findByIdAndCompanyIdForUpdate(loadId, scope.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        LoadStatus currentStatus = load.getStatus();
        boolean isNextStatus = NEXT_STATUS.getOrDefault(currentStatus, Set.of()).contains(newStatus);
        boolean isClosing = newStatus == LoadStatus.CANCELLED && !TERMINAL_STATUSES.contains(currentStatus);
        if (!isNextStatus && !isClosing) {
            throw new InvalidLoadTransitionException(currentStatus, newStatus);
        }

        recordStatusEvent(scope.companyId(), scope.userId(), load.getId(), newStatus, note);
        load.setStatus(newStatus);
        if (newStatus == LoadStatus.COMPLETED) {
            load.setCompletedAt(Instant.now());
        }
        return loadRepository.save(load);
    }

    private void recordStatusEvent(
            UUID companyId, UUID userId, UUID loadId, LoadStatus status, String description) {
        LoadEvent event = new LoadEvent();
        event.setCompanyId(companyId);
        event.setLoadId(loadId);
        event.setEventType("STATUS_CHANGED");
        event.setEventStatus(status);
        event.setEventDescription(description);
        event.setOccurredAt(Instant.now());
        event.setRecordedByUserId(userId);
        event.setCreatedAt(Instant.now());
        eventRepository.save(event);
    }
}
