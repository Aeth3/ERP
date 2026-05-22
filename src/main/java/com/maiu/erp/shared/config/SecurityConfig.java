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

import com.maiu.erp.shared.security.CustomAccessDeniedHandler;
import com.maiu.erp.shared.security.CustomAuthenticationEntryPoint;
import com.maiu.erp.shared.security.JwtAuthenticationFilter;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

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
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                                                .anyRequest().authenticated())

                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
