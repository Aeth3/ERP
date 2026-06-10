package com.maiu.erp.shared.config;

import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.maiu.erp.shared.security.CustomAccessDeniedHandler;
import com.maiu.erp.shared.security.CustomAuthenticationEntryPoint;
import com.maiu.erp.shared.security.JwtAuthenticationFilter;


import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {
        private static final String[] READ_ROLES = {
                        "USER",
                        "VIEWER",
                        "PROCUREMENT",
                        "WAREHOUSE",
                        "PROJECT_MANAGER",
                        "ADMIN"
        };
        private static final String[] PROCUREMENT_WRITE_ROLES = {
                        "PROCUREMENT",
                        "ADMIN"
        };
        private static final String[] WAREHOUSE_WRITE_ROLES = {
                        "WAREHOUSE",
                        "ADMIN"
        };
        private static final String[] PROJECT_WRITE_ROLES = {
                        "PROJECT_MANAGER",
                        "ADMIN"
        };

        private final JwtAuthenticationFilter jwtFilter;

        public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
                this.jwtFilter = jwtFilter;
                System.out.println("JWT FILTER LOADED ✅");
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults())
                                .csrf(csrf -> csrf.disable())

                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(
                                                                org.springframework.security.config.http.SessionCreationPolicy.STATELESS))

                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())

                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                                                .accessDeniedHandler(new CustomAccessDeniedHandler()))

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/auth/**").permitAll()
                                                .requestMatchers("/error").permitAll()
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/approvals/**").hasAnyRole(READ_ROLES)
                                                .requestMatchers("/approvals/**").hasRole("ADMIN")
                                                .requestMatchers("/roles/**").hasRole("ADMIN")
                                                .requestMatchers("/users/**").hasRole("ADMIN")
                                                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/inventory/**").hasAnyRole(READ_ROLES)
                                                .requestMatchers(HttpMethod.GET, "/projects/**").hasAnyRole(READ_ROLES)
                                                .requestMatchers(HttpMethod.GET, "/orders/**").hasAnyRole(READ_ROLES)
                                                .requestMatchers("/inventory/purchase-orders/**")
                                                .hasAnyRole(PROCUREMENT_WRITE_ROLES)
                                                .requestMatchers("/inventory/suppliers/**")
                                                .hasAnyRole(PROCUREMENT_WRITE_ROLES)
                                                .requestMatchers("/inventory/customers/**")
                                                .hasAnyRole(PROCUREMENT_WRITE_ROLES)
                                                .requestMatchers("/inventory/categories/**")
                                                .hasAnyRole(PROCUREMENT_WRITE_ROLES)
                                                .requestMatchers("/inventory/products/**")
                                                .hasAnyRole(PROCUREMENT_WRITE_ROLES)
                                                .requestMatchers("/inventory/units/**")
                                                .hasAnyRole(PROCUREMENT_WRITE_ROLES)
                                                .requestMatchers("/inventory/material-issues/**")
                                                .hasAnyRole(WAREHOUSE_WRITE_ROLES)
                                                .requestMatchers("/inventory/warehouses/**")
                                                .hasAnyRole(WAREHOUSE_WRITE_ROLES)
                                                .requestMatchers("/inventory/stocks/**")
                                                .hasAnyRole(WAREHOUSE_WRITE_ROLES)
                                                .requestMatchers("/inventory/sales-orders/**")
                                                .hasAnyRole(WAREHOUSE_WRITE_ROLES)
                                                .requestMatchers("/projects/**").hasAnyRole(PROJECT_WRITE_ROLES)
                                                .requestMatchers("/orders/**").hasRole("ADMIN")
                                                .requestMatchers("/inventory/**").hasRole("ADMIN")
                                                .anyRequest().authenticated())

                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOrigins(List.of(
                                "http://localhost:3000",
                                "https://erp.maiu.top"));

                configuration.setAllowedMethods(List.of(
                                "GET",
                                "POST",
                                "PUT",
                                "PATCH",
                                "DELETE",
                                "OPTIONS"));

                configuration.setAllowedHeaders(List.of("*"));
                configuration.setExposedHeaders(List.of("Authorization"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}
