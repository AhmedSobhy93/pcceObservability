package com.cisco.cx.observability.security;

import com.cisco.cx.observability.config.PcceProperties.AppRole;
import com.cisco.cx.observability.config.PcceProperties.Permission;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AppUserDetails(
        String username,
        String password,
        String displayName,
        String agentId,
        List<String> allowedTeams,
        List<AppRole> roles,
        List<Permission> permissions,
        boolean enabled,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
