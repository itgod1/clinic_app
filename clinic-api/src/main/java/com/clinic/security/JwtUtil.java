package com.clinic.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    private static final String SECRET_KEY = "clinicSaasSecretKey2024ForJWTTokenGenerationMustBeLongEnough";
    private static final long EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;

    private SecretKey getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String username, Integer clinicId, String realName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("clinicId", clinicId);
        claims.put("realName", realName);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.error("JWT Token已过期: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.error("JWT Token解析失败: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        return claims != null ? Long.valueOf(claims.get("userId").toString()) : null;
    }

    public String getUsername(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public Integer getClinicId(String token) {
        Claims claims = getClaims(token);
        return claims != null ? Integer.valueOf(claims.get("clinicId").toString()) : null;
    }

    public String getRealName(String token) {
        Claims claims = getClaims(token);
        return claims != null ? (String) claims.get("realName") : null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            log.error("JWT Token无效: {}", e.getMessage());
            return false;
        }
    }
}