package com.maiu.erp.shared.utils;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.maiu.erp.shared.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final Key key;

    public JwtUtil(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes());
    }

    public String generateToken(String email, Set<String> roles, Set<String> permissions) {
        return Jwts.builder()
                .setSubject(email)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    public Set<String> extractRoles(String token) {
    Claims claims = getClaims(token);

    Object rolesObj = claims.get("roles");

    if (rolesObj instanceof List<?> list) {
        return list.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    return Set.of();
}

    public Set<String> extractPermissions(String token) {
        Claims claims = getClaims(token);

        Object permissionsObj = claims.get("permissions");

        if (permissionsObj instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }

        return Set.of();
    }
}
