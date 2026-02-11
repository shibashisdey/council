package com.council.appointmentservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class InternalJwtService {

    private final SecretKey key;
    private final long expirationMs;
    private final String serviceName;

    public InternalJwtService(
            @Value("${internal.jwt.secret}") String secret,
            @Value("${internal.jwt.expiration.ms:300000}") long expirationMs,
            @Value("${spring.application.name}") String serviceName
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.serviceName = serviceName;
    }

    public String generateToken() {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(serviceName)
                .claim("service", serviceName)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public void validate(String token) {
        Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
}
