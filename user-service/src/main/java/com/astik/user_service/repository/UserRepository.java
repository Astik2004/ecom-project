package com.astik.user_service.repository;

import com.astik.user_service.entity.User;
import com.astik.user_service.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findByEmailVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);

    // Unlock accounts whose lock has expired
    @Modifying
    @Query("""
        UPDATE User u SET u.failedLoginAttempts = 0,
        u.accountLockedUntil = NULL
        WHERE u.accountLockedUntil < :now
        """)
    void unlockExpiredAccounts(LocalDateTime now);

    // Update last login without loading the full entity
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :now WHERE u.id = :id")
    void updateLastLogin(UUID id, LocalDateTime now);
}