package com.truckingsystem.repository;

import com.truckingsystem.entity.Company;
import com.truckingsystem.entity.CompanyStatus;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends Repository<Company, UUID> {
    Company save(Company company);

    Optional<Company> findById(UUID id);

    Optional<Company> findByIdAndStatus(UUID id, CompanyStatus status);
}
