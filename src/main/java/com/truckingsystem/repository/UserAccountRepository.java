package com.truckingsystem.repository;

import com.truckingsystem.entity.UserAccount;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends Repository<UserAccount, UUID> {
    Optional<UserAccount> findByExternalSubjectAndActiveTrue(String externalSubject);
}
