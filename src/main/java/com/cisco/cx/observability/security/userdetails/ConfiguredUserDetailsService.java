package com.cisco.cx.observability.security.userdetails;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.config.PcceProperties.AppRole;
import com.cisco.cx.observability.config.PcceProperties.AppUser;
import com.cisco.cx.observability.config.PcceProperties.Permission;
import com.cisco.cx.observability.security.access.PermissionCatalog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ConfiguredUserDetailsService implements UserDetailsService {

    private final PcceProperties pcceProperties;

    public ConfiguredUserDetailsService(PcceProperties pcceProperties) {
        this.pcceProperties = pcceProperties;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = pcceProperties.getSecurity().getUsers().stream()
                .filter(candidate -> candidate.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<AppRole> roles = safeList(user.getRoles());
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        roles.forEach(role -> permissions.addAll(rolePermissions(role)));
        permissions.addAll(safeList(user.getExtraPermissions()));

        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name())));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority("PERM_" + permission.name())));

        return new AppUserDetails(
                user.getUsername(),
                user.getPassword(),
                user.getDisplayName(),
                user.getAgentId(),
                safeList(user.getAllowedTeams()),
                roles,
                List.copyOf(permissions),
                user.isEnabled(),
                authorities);
    }

    private <T> List<T> safeList(Collection<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private List<Permission> rolePermissions(AppRole role) {
        var configuredRoles = pcceProperties.getSecurity().getRolePermissions();
        List<Permission> configured = configuredRoles == null ? null : configuredRoles.get(role);
        return configured == null ? List.copyOf(PermissionCatalog.permissionsFor(role)) : configured;
    }
}
