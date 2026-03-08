package com.astik.user_service.repository;

import com.astik.user_service.entity.User;
import com.astik.user_service.enums.Role;
import com.astik.user_service.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByEmailVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Page<User> findAllByRole(Role role, Pageable pageable);

    Page<User> findAllByStatus(UserStatus status, Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
        UPDATE User u SET
            u.lastLoginAt      = :loginAt,
            u.failedLoginAttempts = 0,
            u.accountLockedUntil  = null
        WHERE u.id = :id
    """)
    void updateOnSuccessfulLogin(
            @Param("id") UUID id,
            @Param("loginAt") LocalDateTime loginAt);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :id")
    void incrementFailedAttempts(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.accountLockedUntil = :until, u.status = 'SUSPENDED' WHERE u.id = :id")
    void lockAccount(@Param("id") UUID id, @Param("until") LocalDateTime until);

    @Modifying
    @Transactional
    @Query("""
        UPDATE User u SET
            u.accountLockedUntil  = null,
            u.failedLoginAttempts = 0,
            u.status              = 'ACTIVE'
        WHERE u.id = :id
    """)
    void unlockAccount(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("""
        UPDATE User u SET
            u.emailVerificationToken       = :token,
            u.emailVerificationTokenExpiry = :expiry
        WHERE u.id = :id
    """)
    void setEmailVerificationToken(
            @Param("id") Long id,
            @Param("token") String token,
            @Param("expiry") LocalDateTime expiry);

    @Modifying
    @Transactional
    @Query("""
        UPDATE User u SET
            u.isEmailVerified              = true,
            u.emailVerificationToken       = null,
            u.emailVerificationTokenExpiry = null,
            u.status                       = 'ACTIVE'
        WHERE u.id = :id
    """)
    void markEmailVerified(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("""
        UPDATE User u SET
            u.passwordResetToken       = :token,
            u.passwordResetTokenExpiry = :expiry
        WHERE u.id = :id
    """)
    void setPasswordResetToken(
            @Param("id") Long id,
            @Param("token") String token,
            @Param("expiry") LocalDateTime expiry);

    @Modifying
    @Transactional
    @Query("""
        UPDATE User u SET
            u.password                 = :password,
            u.passwordChangedAt        = :changedAt,
            u.passwordResetToken       = null,
            u.passwordResetTokenExpiry = null
        WHERE u.id = :id
    """)
    void updatePassword(
            @Param("id") Long id,
            @Param("password") String password,
            @Param("changedAt") LocalDateTime changedAt);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") UserStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.role = :role WHERE u.id = :id")
    void updateRole(@Param("id") Long id, @Param("role") Role role);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isDeleted = true, u.status = 'DELETED' WHERE u.id = :id")
    void softDelete(@Param("id") Long id);
}