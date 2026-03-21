package com.astik.user_service.repository;

import com.astik.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Modifying
    @Transactional
    @Query("""
        UPDATE User u SET
            u.failedLoginAttempts = 0,
            u.accountLockedUntil  = NULL
        WHERE u.accountLockedUntil < :now
        """)
    void unlockExpiredAccounts(LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLoginAt = :now WHERE u.id = :id")
    void updateLastLogin(UUID id, LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
        UPDATE User u SET
            u.failedLoginAttempts = :attempts,
            u.accountLockedUntil  = :lockedUntil
        WHERE u.id = :id
        """)
    void updateFailedAttempts(UUID id, int attempts, LocalDateTime lockedUntil);

    @Modifying
    @Transactional
    @Query("""
        UPDATE User u SET
            u.failedLoginAttempts = u.failedLoginAttempts + 1,
            u.accountLockedUntil = CASE
                WHEN (u.failedLoginAttempts + 1) >= :maxAttempts
                THEN :lockedUntil
                ELSE u.accountLockedUntil
            END
        WHERE u.id = :id
        """)
    void incrementFailedAttempts(UUID id, int maxAttempts, LocalDateTime lockedUntil);

    @Query("SELECT u.failedLoginAttempts FROM User u WHERE u.id = :id")
    int getFailedAttempts(UUID id);
}