package com.truckingsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserAccount extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;

    @Size(max = 255)
    @Column(name = "auth_subject", unique = true, length = 255)
    private String externalSubject;

    @NotBlank
    @Size(max = 80)
    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @NotBlank
    @Size(max = 80)
    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Size(max = 254)
    @Column(length = 254)
    private String email;

    @Size(max = 160)
    @Column(name = "display_name", length = 160)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;
}
