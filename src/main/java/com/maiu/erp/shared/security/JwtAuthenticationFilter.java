package com.maiu.erp.shared.security;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.maiu.erp.modules.identity.domain.model.User;
import com.maiu.erp.modules.identity.domain.repository.UserRepository;
import com.maiu.erp.modules.identity.infrastructure.security.CustomUserDetails;
import com.maiu.erp.shared.utils.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {

        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if (path.startsWith("/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtUtil.extractEmail(token);
        Set<String> tokenRoles = jwtUtil.extractRoles(token);
        Set<String> tokenPermissions = jwtUtil.extractPermissions(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getPassword(),
                buildAuthorities(tokenRoles, tokenPermissions));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private java.util.List<SimpleGrantedAuthority> buildAuthorities(Set<String> roles, Set<String> permissions) {
        Set<String> authorities = new LinkedHashSet<>();

        authorities.addAll(roles);
        authorities.addAll(permissions);

        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
