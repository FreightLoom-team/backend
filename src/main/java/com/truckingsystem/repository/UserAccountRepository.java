package com.truckingsystem.repository;

import com.truckingsystem.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByExternalSubjectAndStatus(
            String externalSubject, com.truckingsystem.entity.UserStatus status);

    boolean existsByEmailIgnoreCase(String email);

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByExternalSubject(String externalSubject);
}
