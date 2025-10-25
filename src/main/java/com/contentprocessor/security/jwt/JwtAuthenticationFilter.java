package com.contentprocessor.security.jwt;

import com.contentprocessor.repository.UserRepository;
import com.contentprocessor.security.oauth2.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Extract JWT from Auth header
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {


                String userId = tokenProvider.getUserIdFromJWT(jwt);

                if (userId != null) {
                    userRepository.findById(userId).ifPresent(user -> {
                        if (user.isEnabled()) {
                            UserPrincipal userPrincipal = new UserPrincipal(user);

                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userPrincipal, null, userPrincipal.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            logger.debug("Successfully authenticated user {} via JWT", userId);
                        } else {
                            logger.warn("JWT validation succeeded for disabled user ID: {}", userId);
                        }
                    });
                } else {
                    logger.warn("JWT validation passed but could not extract user ID");
                }
            } else {
                if (StringUtils.hasText(jwt)) {
                    logger.debug("Invalid or expired JWT received.");
                } else {
                    logger.trace("No JWT found in request header.");
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the Authorization header (Bearer scheme).
     * @param request The incoming HTTP request.
     * @return The JWT string or null if not found or invalid format.
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Return token without "Bearer " prefix
            return bearerToken.substring(7);
        }
        return null;
    }
}