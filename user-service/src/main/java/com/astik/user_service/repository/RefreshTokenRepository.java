package com.astik.user_service.repository;

import com.astik.user_service.entity.RefreshToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken t SET t.revoked = true, t.expired = true WHERE t.user.id = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken t WHERE (t.revoked = true OR t.expired = true) AND t.createdAt < :before")
    void deleteExpiredBefore(@Param("before") LocalDateTime before);

    long countByUserIdAndRevokedFalseAndExpiredFalse(UUID userId);
}
