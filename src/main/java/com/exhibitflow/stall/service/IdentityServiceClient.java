package com.exhibitflow.stall.service;

import com.exhibitflow.stall.config.IdentityServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Service for interacting with the Identity Service OAuth endpoints.
 * 
 * <p>Provides methods to:
 * <ul>
 *   <li>Introspect tokens using RFC 7662 standard</li>
 *   <li>Validate tokens using simple validation endpoint</li>
 * </ul>
 * 
 * <p>This service is optional and enabled only when identity-service.base-url is configured.
 * JWT validation still works without this service using the shared secret.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "identity-service.base-url")
public class IdentityServiceClient {

    private final IdentityServiceProperties properties;
    private final RestClient restClient;

    /**
     * Introspect a token using RFC 7662 Token Introspection.
     * 
     * <p>Endpoint: POST /api/v1/oauth/introspect?token={token}
     * 
     * <p>Returns detailed information about the token including:
     * <ul>
     *   <li>active - whether the token is currently active</li>
     *   <li>username - the username of the token owner</li>
     *   <li>roles - list of roles assigned to the user</li>
     *   <li>permissions - list of permissions assigned to the user</li>
     *   <li>exp, iat - expiration and issued-at timestamps</li>
     * </ul>
     * 
     * @param token the access token to introspect
     * @return introspection response as a map
     * @throws RestClientException if the request fails
     */
    public Map<String, Object> introspectToken(String token) {
        try {
            log.debug("Introspecting token with Identity Service");
            
            Map<String, Object> response = restClient.post()
                    .uri(properties.getOauth().getIntrospectUrl() + "?token={token}", token)
                    .retrieve()
                    .body(Map.class);
            
            log.debug("Token introspection successful, active: {}", response.get("active"));
            return response;
            
        } catch (RestClientException e) {
            log.error("Failed to introspect token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Validate a token using simple validation endpoint.
     * 
     * <p>Endpoint: POST /api/v1/oauth/validate?token={token}
     * 
     * <p>Returns a simple response indicating if the token is valid:
     * <pre>
     * {
     *   "valid": true
     * }
     * </pre>
     * 
     * @param token the access token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            log.debug("Validating token with Identity Service");
            
            Map<String, Object> response = restClient.post()
                    .uri(properties.getOauth().getValidateUrl() + "?token={token}", token)
                    .retrieve()
                    .body(Map.class);
            
            boolean valid = response != null && Boolean.TRUE.equals(response.get("valid"));
            log.debug("Token validation result: {}", valid);
            return valid;
            
        } catch (RestClientException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract roles from token introspection response.
     * 
     * @param introspection the introspection response
     * @return list of role names (without ROLE_ prefix)
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Map<String, Object> introspection) {
        Object roles = introspection.get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return List.of();
    }

    /**
     * Extract permissions from token introspection response.
     * 
     * @param introspection the introspection response
     * @return list of permission strings
     */
    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(Map<String, Object> introspection) {
        Object permissions = introspection.get("permissions");
        if (permissions instanceof List) {
            return (List<String>) permissions;
        }
        return List.of();
    }

    /**
     * Check if a token is active based on introspection response.
     * 
     * @param introspection the introspection response
     * @return true if the token is active
     */
    public boolean isTokenActive(Map<String, Object> introspection) {
        return Boolean.TRUE.equals(introspection.get("active"));
    }
}
