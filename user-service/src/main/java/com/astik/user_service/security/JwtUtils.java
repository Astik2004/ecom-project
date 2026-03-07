package com.astik.user_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtUtils {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long accessExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    @Value("${spring.application.name:user-service}")
    private String issuer;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    public String generateAccessToken(UserPrincipal userPrincipal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role",      userPrincipal.getUser().getRole());
        claims.put("userId",    userPrincipal.getUser().getId());
        claims.put("tokenType", "ACCESS");
        return buildToken(claims, userPrincipal.getUsername(), accessExpiration);
    }

    public String generateRefreshToken(UserPrincipal userPrincipal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",    userPrincipal.getUser().getId());
        claims.put("tokenType", "REFRESH");
        return buildToken(claims, userPrincipal.getUsername(), refreshExpiration);
    }

    private String buildToken(Map<String, Object> claims,String subject,long expiration) {
            return Jwts.builder()
                    .claims(claims)
                    .subject(subject)
                    .issuer(issuer)
                    .issuedAt(new Date(System.currentTimeMillis()))
                    .expiration(new Date(System.currentTimeMillis()+expiration))
                    .signWith(signingKey, Jwts.SIG.HS512)
                    .compact();
    }



}
