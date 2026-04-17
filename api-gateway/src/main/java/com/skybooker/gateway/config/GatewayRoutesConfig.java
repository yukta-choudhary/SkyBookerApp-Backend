package com.skybooker.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .uri("http://127.0.0.1:8081"))

                .route("airline-service", r -> r
                        .path("/api/v1/airlines/**")
                        .uri("http://127.0.0.1:8082"))

                .route("airport-service", r -> r
                        .path("/api/v1/airports/**")
                        .uri("http://127.0.0.1:8082"))

                .route("flight-service", r -> r
                        .path("/api/v1/flights/**")
                        .uri("http://127.0.0.1:8083"))

                .route("booking-service", r -> r
                        .path("/api/v1/bookings/**")
                        .uri("http://127.0.0.1:8084"))

                .route("passenger-service", r -> r
                        .path("/api/v1/passengers/**")
                        .uri("http://127.0.0.1:8085"))

                .route("seat-service", r -> r
                        .path("/api/v1/seats/**")
                        .uri("http://127.0.0.1:8086"))

                .build();
    }
}