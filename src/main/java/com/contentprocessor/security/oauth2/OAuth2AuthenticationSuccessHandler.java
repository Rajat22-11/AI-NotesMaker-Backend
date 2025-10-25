package com.contentprocessor.security.oauth2;

import com.contentprocessor.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handles successful OAuth2/OIDC authentication.
 * Generates an application-specific JWT using JwtTokenProvider and redirects the user
 * back to the frontend application, passing the token as a URL parameter.
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final JwtTokenProvider tokenProvider;
    private final String frontendUrl;

    /**
     * Constructor injecting the JwtTokenProvider and the frontend URL from properties.
     * @param tokenProvider Utility to generate JWTs.
     * @param frontendUrl The base URL of the frontend application (from app.cors.allowed-origins).
     */
    public OAuth2AuthenticationSuccessHandler(JwtTokenProvider tokenProvider,
                                              @Value("${app.cors.allowed-origins}") String frontendUrl) {
        this.tokenProvider = tokenProvider;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        logger.info("Configured frontend redirect URL base: {}", this.frontendUrl);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            logger.error("Authentication principal is not an instance of UserPrincipal: {}", authentication.getPrincipal().getClass());
            // Handle error - maybe redirect to a generic error page or use default behavior
            super.onAuthenticationSuccess(request, response, authentication); // Or throw exception
            return;
        }

        String userId = userPrincipal.getId(); // Get the DB ID from our UserPrincipal
        String token = tokenProvider.generateTokenFromUserId(userId);

        if (token == null) {
            logger.error("Failed to generate JWT token for user ID: {}", userId);
            // Handle error - redirect to error page?
            getRedirectStrategy().sendRedirect(request, response, "/error?message=token_generation_failed");
            return;
        }

        // Example: http://localhost:3000/auth/callback?token=xxx.yyy.zzz
        String targetPath = "/auth/callback"; // Path on the frontend that handles the token
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + targetPath)
                .queryParam("token", token)
                .build().toUriString();

        clearAuthenticationAttributes(request);

        logger.info("Redirecting user {} to frontend: {}", userId, targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
