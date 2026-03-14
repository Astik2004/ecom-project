package com.astik.user_service.repository;
import com.astik.user_service.entity.RefreshToken;
import com.astik.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    // Revoke ALL tokens for a user (used on logout)
    @Modifying
    @Query("""
        UPDATE RefreshToken r SET r.revoked = true, r.expired = true
        WHERE r.user = :user AND r.revoked = false
        """)
    void revokeAllUserTokens(User user);
}