package com.truckingsystem.repository;

import com.truckingsystem.entity.LoadEvent;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface LoadEventRepository extends Repository<LoadEvent, UUID> {
    LoadEvent save(LoadEvent event);
}
