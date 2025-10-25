package com.contentprocessor.security.oauth2;

import com.contentprocessor.model.entities.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser; // Implement OidcUser for OIDC providers

import java.util.Collection;
import java.util.Collections;
import java.util.Map;


public class UserPrincipal implements OidcUser {

    @Getter
    private final User user; // Your application's User entity
    private final Map<String, Object> attributes; // Combined attributes from ID token and UserInfo endpoint
    private final OidcIdToken idToken; // ID Token from OIDC provider (can be null if authenticated via JWT)
    private final OidcUserInfo userInfo; // UserInfo from OIDC provider (can be null if authenticated via JWT)


    public UserPrincipal(User user, Map<String, Object> attributes, OidcIdToken idToken, OidcUserInfo userInfo) {
        this.user = user;
        this.attributes = attributes != null ? Collections.unmodifiableMap(attributes) : Collections.emptyMap();
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    /**
     * Constructor used when re-authenticating via your application's JWT.
     * OIDC details are not available in this flow.
     * @param user Your application's User entity.
     */
    public UserPrincipal(User user) {
        this.user = user;
        // Attributes might not be needed or can be minimal for JWT flow
        this.attributes = Collections.singletonMap("sub", user.getId()); // Provide at least 'sub' using DB ID
        this.idToken = null;
        this.userInfo = null;
    }

    // --- Application-specific Getters ---
    public String getId() {
        return user.getId(); // Internal Database ID
    }

    // --- OidcUser / OAuth2User Methods ---
    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        // The standard OIDC 'sub' (subject) claim is the primary identifier.
        // OidcUser interface requires this method to return the subject.
        return (this.idToken != null) ? this.idToken.getSubject() : this.user.getId();
    }

    @Override
    public Map<String, Object> getClaims() {
        // Return ID token claims if available, otherwise return basic attributes
        return (this.idToken != null) ? this.idToken.getClaims() : this.attributes;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return this.userInfo; // May be null
    }

    @Override
    public OidcIdToken getIdToken() {
        return this.idToken; // May be null
    }

    public String getUsername() {
        return user.getEmail(); // Use email for display or lookup
    }

    public boolean isEnabled() {
        return user.isEnabled();
    }
}
