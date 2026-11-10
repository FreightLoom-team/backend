package com.truckingsystem.repository;

import com.truckingsystem.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByExternalSubjectAndActiveTrue(String externalSubject);

    boolean existsByEmailIgnoreCase(String email);
}
