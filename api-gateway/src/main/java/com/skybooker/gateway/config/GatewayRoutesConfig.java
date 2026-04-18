package com.skybooker.gateway.config;

/**
 * Routes are configured via application.yml using Eureka load-balanced (lb://) URIs.
 * Programmatic route configuration here is intentionally disabled to avoid
 * duplicate routes. See application.yml spring.cloud.gateway.routes for the active config.
 *
 * For local development without Eureka, set environment variables like:
 *   AUTH_SERVICE_URI=http://localhost:8081
 * or use the local profile: application-local.yml
 */
public class GatewayRoutesConfig {
    // Routes defined in application.yml — no programmatic bean needed.
}