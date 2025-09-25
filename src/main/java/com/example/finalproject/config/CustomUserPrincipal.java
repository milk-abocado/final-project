package com.example.finalproject.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserPrincipal implements UserDetails {
    private final Long userId;
    private final String userEmail;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;

    public CustomUserPrincipal(Long userId,
                               String userEmail,
                               String password,
                               Collection<? extends GrantedAuthority> authorities,
                               boolean enabled,
                               boolean accountNonExpired,
                               boolean accountNonLocked,
                               boolean credentialsNonExpired) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
    }

    // 컨트롤러에서 사용할 expression
    public Long getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return userEmail; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public boolean isAccountNonExpired() { return accountNonExpired; }
    @Override public boolean isAccountNonLocked() { return accountNonLocked; }
    @Override public boolean isCredentialsNonExpired() { return credentialsNonExpired; }
}