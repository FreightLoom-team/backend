package com.truckingsystem.service;

import com.truckingsystem.common.ResourceNotFoundException;
import com.truckingsystem.common.TenantContext;
import com.truckingsystem.common.InvalidLoadTransitionException;
import com.truckingsystem.entity.Load;
import com.truckingsystem.entity.LoadStatus;
import com.truckingsystem.entity.LoadStatusHistory;
import com.truckingsystem.pojo.CreateLoadRequest;
import com.truckingsystem.repository.LoadRepository;
import com.truckingsystem.repository.LoadStatusHistoryRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Validated
public class LoadStatusService {
    private static final Map<LoadStatus, LoadStatus> NEXT_STATUS = Map.ofEntries(
            Map.entry(LoadStatus.DRAFT, LoadStatus.OFFERED),
            Map.entry(LoadStatus.OFFERED, LoadStatus.ACCEPTED),
            Map.entry(LoadStatus.ACCEPTED, LoadStatus.ASSIGNED),
            Map.entry(LoadStatus.ASSIGNED, LoadStatus.PRE_TRIP),
            Map.entry(LoadStatus.PRE_TRIP, LoadStatus.AT_PICKUP),
            Map.entry(LoadStatus.AT_PICKUP, LoadStatus.PICKED_UP),
            Map.entry(LoadStatus.PICKED_UP, LoadStatus.IN_TRANSIT),
            Map.entry(LoadStatus.IN_TRANSIT, LoadStatus.AT_DELIVERY),
            Map.entry(LoadStatus.AT_DELIVERY, LoadStatus.DELIVERED),
            Map.entry(LoadStatus.DELIVERED, LoadStatus.PENDING_REVIEW),
            Map.entry(LoadStatus.PENDING_REVIEW, LoadStatus.COMPLETED));
    private static final Set<LoadStatus> TERMINAL_STATUSES =
            EnumSet.of(LoadStatus.COMPLETED, LoadStatus.CLOSED);

    private final LoadRepository loadRepository;
    private final LoadStatusHistoryRepository historyRepository;

    public LoadStatusService(LoadRepository loadRepository, LoadStatusHistoryRepository historyRepository) {
        this.loadRepository = loadRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public Load createLoad(@Valid @NotNull CreateLoadRequest request) {
        TenantContext.Scope scope = TenantContext.current()
                .orElseThrow(() -> new IllegalStateException("Tenant scope is not established"));
        Load load = new Load();
        load.setOrganizationId(scope.organizationId());
        load.setReference(request.reference());
        load.setPickupLocation(request.pickupLocation());
        load.setDeliveryLocation(request.deliveryLocation());
        load.setRateAmount(request.rateAmount());
        load.setCurrencyCode(request.currencyCode());
        load.setStatus(LoadStatus.DRAFT);
        load = loadRepository.save(load);

        LoadStatusHistory initialHistory = new LoadStatusHistory();
        initialHistory.setOrganizationId(scope.organizationId());
        initialHistory.setLoadId(load.getId());
        initialHistory.setStatus(LoadStatus.DRAFT);
        historyRepository.save(initialHistory);
        return load;
    }

    @Transactional
    public Load changeStatus(UUID loadId, @NotNull LoadStatus newStatus, @Size(max = 1000) String note) {
        TenantContext.Scope scope = TenantContext.current()
                .orElseThrow(() -> new IllegalStateException("Tenant scope is not established"));
        Load load = loadRepository.findByIdAndOrganizationIdForUpdate(loadId, scope.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        LoadStatus currentStatus = load.getStatus();
        boolean isNextStatus = NEXT_STATUS.get(currentStatus) == newStatus;
        boolean isClosing = newStatus == LoadStatus.CLOSED && !TERMINAL_STATUSES.contains(currentStatus);
        if (!isNextStatus && !isClosing) {
            throw new InvalidLoadTransitionException(currentStatus, newStatus);
        }

        LoadStatusHistory history = new LoadStatusHistory();
        history.setOrganizationId(scope.organizationId());
        history.setLoadId(load.getId());
        history.setStatus(newStatus);
        history.setNote(note);
        historyRepository.save(history);

        load.setStatus(newStatus);
        return loadRepository.save(load);
    }
}
