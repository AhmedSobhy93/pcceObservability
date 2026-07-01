package com.cisco.cx.observability.security.ldap;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.config.PcceProperties.AppRole;
import com.cisco.cx.observability.config.PcceProperties.Ldap;
import com.cisco.cx.observability.config.PcceProperties.Permission;
import com.cisco.cx.observability.security.access.PermissionCatalog;
import com.cisco.cx.observability.security.userdetails.AppUserDetails;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LdapAuthenticationProvider implements AuthenticationProvider {

    private final PcceProperties pcceProperties;
    private final String urls;
    private final String baseDn;
    private final String managerDn;
    private final String managerPassword;

    public LdapAuthenticationProvider(
            PcceProperties pcceProperties,
            @Value("${spring.ldap.urls:}") String urls,
            @Value("${spring.ldap.base:}") String baseDn,
            @Value("${spring.ldap.username:}") String managerDn,
            @Value("${spring.ldap.password:}") String managerPassword) {
        this.pcceProperties = pcceProperties;
        this.urls = urls;
        this.baseDn = baseDn;
        this.managerDn = managerDn;
        this.managerPassword = managerPassword;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Ldap ldap = pcceProperties.getSecurity().getLdap();
        String username = authentication.getName();
        String password = String.valueOf(authentication.getCredentials());
        if (ldap == null || !ldap.isEnabled() || isLocalUser(username)) {
            return null;
        }
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BadCredentialsException("LDAP username/password is required");
        }
        if (!StringUtils.hasText(urls) || !StringUtils.hasText(baseDn) || !StringUtils.hasText(managerDn)) {
            throw new BadCredentialsException("LDAP is enabled but APP_LDAP_URLS, APP_LDAP_BASE, and APP_LDAP_MANAGER_DN are not configured");
        }

        LdapUser ldapUser = findUser(ldap, username);
        bind(ldapUser.dn(), password);
        List<AppRole> roles = rolesFor(ldap, ldapUser.groups());
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        roles.forEach(role -> permissions.addAll(rolePermissions(role)));
        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name())));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority("PERM_" + permission.name())));
        AppUserDetails principal = new AppUserDetails(
                username,
                "",
                username,
                null,
                List.of(),
                roles,
                List.copyOf(permissions),
                true,
                authorities);
        return new UsernamePasswordAuthenticationToken(principal, password, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private boolean isLocalUser(String username) {
        return pcceProperties.getSecurity().getUsers().stream()
                .anyMatch(user -> user.getUsername().equalsIgnoreCase(username));
    }

    private LdapUser findUser(Ldap ldap, String username) {
        DirContext context = null;
        try {
            context = context(managerDn, managerPassword);
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(new String[] { "distinguishedName", "memberOf", "cn" });
            String searchBase = joinDn(ldap.getUserSearchBase(), baseDn);
            String filter = ldap.getUserSearchFilter().replace("{0}", escape(username));
            NamingEnumeration<SearchResult> results = context.search(searchBase, filter, controls);
            if (!results.hasMore()) {
                throw new BadCredentialsException("LDAP user not found");
            }
            SearchResult result = results.next();
            String dn = result.getNameInNamespace();
            List<String> groups = groups(result.getAttributes());
            return new LdapUser(dn, groups);
        } catch (BadCredentialsException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadCredentialsException("LDAP lookup failed: " + ex.getMessage(), ex);
        } finally {
            close(context);
        }
    }

    private void bind(String userDn, String password) {
        DirContext context = null;
        try {
            context = context(userDn, password);
            // Successful bind proves the credentials.
        } catch (Exception ex) {
            throw new BadCredentialsException("LDAP bind failed", ex);
        } finally {
            close(context);
        }
    }

    private DirContext context(String principal, String credentials) throws javax.naming.NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, urls);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, credentials == null ? "" : credentials);
        return new InitialDirContext(env);
    }

    private List<String> groups(Attributes attributes) throws javax.naming.NamingException {
        Attribute memberOf = attributes.get("memberOf");
        if (memberOf == null) {
            return List.of();
        }
        List<String> groups = new ArrayList<>();
        NamingEnumeration<?> values = memberOf.getAll();
        while (values.hasMore()) {
            groups.add(groupName(String.valueOf(values.next())));
        }
        return groups;
    }

    private List<AppRole> rolesFor(Ldap ldap, List<String> groups) {
        List<AppRole> roles = new ArrayList<>();
        addRole(roles, groups, ldap.getAdminGroup(), AppRole.ADMIN);
        addRole(roles, groups, ldap.getWorkforceManagerGroup(), AppRole.WORKFORCE_MANAGER);
        addRole(roles, groups, ldap.getSupervisorGroup(), AppRole.SUPERVISOR);
        addRole(roles, groups, ldap.getViewerGroup(), AppRole.VIEWER);
        if (roles.isEmpty()) {
            roles.add(AppRole.VIEWER);
        }
        return roles;
    }

    private void addRole(List<AppRole> roles, List<String> groups, String group, AppRole role) {
        if (StringUtils.hasText(group) && groups.stream().anyMatch(value -> value.equalsIgnoreCase(group))) {
            roles.add(role);
        }
    }

    private List<Permission> rolePermissions(AppRole role) {
        List<Permission> configured = pcceProperties.getSecurity().getRolePermissions() == null
                ? null
                : pcceProperties.getSecurity().getRolePermissions().get(role);
        return configured == null ? List.copyOf(PermissionCatalog.permissionsFor(role)) : configured;
    }

    private String groupName(String dn) {
        for (String part : dn.split(",")) {
            String trimmed = part.trim();
            if (trimmed.regionMatches(true, 0, "CN=", 0, 3)) {
                return trimmed.substring(3);
            }
        }
        return dn;
    }

    private String joinDn(String child, String parent) {
        if (!StringUtils.hasText(child)) {
            return parent;
        }
        return child + "," + parent;
    }

    private String escape(String value) {
        return value.replace("\\", "\\5c")
                .replace("*", "\\2a")
                .replace("(", "\\28")
                .replace(")", "\\29")
                .replace("\u0000", "\\00");
    }

    private void close(DirContext context) {
        if (context == null) {
            return;
        }
        try {
            context.close();
        } catch (Exception ignored) {
            // Nothing useful to do during authentication cleanup.
        }
    }

    private record LdapUser(String dn, List<String> groups) {
    }
}
