package com.skybooker.airline.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health"
                        ).permitAll()

                        // public airport search for guest/passenger flight search
                        .requestMatchers(HttpMethod.GET, "/api/v1/airports/search/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/airports/iata/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/airports/city/**").permitAll()

                        // read-only airline/airport listing for authenticated staff/admin/passenger if needed
                        .requestMatchers(HttpMethod.GET, "/api/v1/airlines/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/airports/**").permitAll()

                        // admin only write access
                        .requestMatchers(HttpMethod.POST, "/api/v1/airlines/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/airlines/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/airlines/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/airports/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/airports/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/airports/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}