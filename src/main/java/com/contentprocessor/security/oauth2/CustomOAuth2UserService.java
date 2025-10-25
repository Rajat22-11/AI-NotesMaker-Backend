package com.contentprocessor.security.oauth2;

import com.contentprocessor.model.entities.User;
import com.contentprocessor.model.enums.AuthProvider;
import com.contentprocessor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

/**
 * Service responsible for processing user information obtained from an OIDC provider (like Microsoft)
 * after successful authentication. Finds or registers the user in the local database.
 * Extends OidcUserService to integrate correctly with Spring Security's OIDC flow.
 */

@Service
public class CustomOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    public CustomOAuth2UserService(UserRepository userRepository) {
        super();
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser;

        try {
            // Try to load user info using the parent class
            oidcUser = super.loadUser(userRequest);
        } catch (OAuth2AuthenticationException e) {
            logger.warn("Failed to load user info from userinfo endpoint: {}. Creating OidcUser from ID token only.", e.getMessage());

            // Fallback: Create OidcUser from ID token claims only (skip userinfo endpoint)
            // This works because Microsoft includes most user info in the ID token
            oidcUser = new DefaultOidcUser(
                    userRequest.getIdToken().getClaims().containsKey("roles")
                            ? (java.util.Collection) userRequest.getIdToken().getClaim("roles")
                            : java.util.Collections.emptyList(),
                    userRequest.getIdToken()
            );
        }

        if (oidcUser.getIdToken() != null) {
            logger.info("Decoded ID Token Claims: {}", oidcUser.getIdToken().getClaims());
        }

        Map<String, Object> attributes = oidcUser.getAttributes();
        logger.info("Successfully loaded OIDC User. Attributes/Claims: {}", attributes);

        String email = findEmail(oidcUser);
        String microsoftId = oidcUser.getName(); // This is the 'sub' claim

        if (!StringUtils.hasText(microsoftId)) {
            logger.error("Microsoft ID ('sub' claim) is missing from OIDC user info. Cannot proceed. Attributes: {}", attributes);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "Microsoft ID ('sub' claim) not found in user info."
            );
        }

        Optional<User> userOptional = userRepository.findByMicrosoftId(microsoftId);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            logger.debug("Found existing user {} by Microsoft ID {}", user.getId(), microsoftId);

            if (StringUtils.hasText(email) && !email.equals(user.getEmail())) {
                logger.warn("Updating email for user {} from {} to {}", user.getId(), user.getEmail(), email);
                user.setEmail(email);
            }

            user = updateExistingUser(user, oidcUser);
        } else {
            if (StringUtils.hasText(email)) {
                userOptional = userRepository.findByEmail(email);
                if (userOptional.isPresent()) {
                    // Found by Email
                    user = userOptional.get();
                    logger.warn("Found existing user {} by email {}, linking Microsoft ID {}", user.getId(), email, microsoftId);
                    user.setMicrosoftId(microsoftId);
                    user.setProvider(AuthProvider.MICROSOFT);
                    user = updateExistingUser(user, oidcUser);
                } else {
                    logger.info("Registering new user with Microsoft ID {} and email {}", microsoftId, email);
                    user = registerNewUser(oidcUser, email);
                }
            } else {
                logger.warn("Registering new user with Microsoft ID {} but no email found.", microsoftId);
                user = registerNewUser(oidcUser, email);
            }
        }

        return new UserPrincipal(user, attributes, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private String findEmail(OidcUser oidcUser) {
        if (oidcUser == null) return null;

        // Check ID token claims first (most reliable for Microsoft)
        if (oidcUser.getIdToken() != null) {
            Map<String, Object> idTokenClaims = oidcUser.getIdToken().getClaims();

            String email = (String) idTokenClaims.get("email");
            if (StringUtils.hasText(email)) {
                logger.debug("Found email in ID token 'email' claim: {}", email);
                return email;
            }

            String upn = (String) idTokenClaims.get("upn");
            if (StringUtils.hasText(upn) && upn.contains("@")) {
                logger.debug("Found email in ID token 'upn' claim: {}", upn);
                return upn;
            }

            String preferredUsername = (String) idTokenClaims.get("preferred_username");
            if (StringUtils.hasText(preferredUsername) && preferredUsername.contains("@")) {
                logger.debug("Found email in ID token 'preferred_username' claim: {}", preferredUsername);
                return preferredUsername;
            }
        }

        // Fallback to attributes (from userinfo endpoint)
        String email = oidcUser.getAttribute("email");
        if (StringUtils.hasText(email)) {
            logger.debug("Found email using 'email' attribute: {}", email);
            return email;
        }

        String upn = oidcUser.getAttribute("upn");
        if (StringUtils.hasText(upn) && upn.contains("@")) {
            logger.debug("Found email using 'upn' attribute: {}", upn);
            return upn;
        }

        String preferredUsername = oidcUser.getAttribute("preferred_username");
        if (StringUtils.hasText(preferredUsername) && preferredUsername.contains("@")) {
            logger.debug("Found email using 'preferred_username' attribute: {}", preferredUsername);
            return preferredUsername;
        }

        logger.warn("Could not find email attribute in user info.");
        return null;
    }

    private User registerNewUser(OidcUser oidcUser, String email) {
        User newUser = User.builder()
                .provider(AuthProvider.MICROSOFT)
                .microsoftId(oidcUser.getName())
                .email(email)
                .enabled(true)
                .build();

        return userRepository.save(newUser);
    }

    private User updateExistingUser(User existingUser, OidcUser oidcUser) {
        existingUser.setMicrosoftId(oidcUser.getName());
        existingUser.setProvider(AuthProvider.MICROSOFT);

        return userRepository.save(existingUser);
    }
}