package com.cisco.cx.observability.security.access;

import com.cisco.cx.observability.security.userdetails.AppUserDetails;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AccessControlService {

    public boolean isAdmin() {
        AppUserDetails user = currentUser();
        return user.roles().stream().anyMatch(role -> role.name().equals("ADMIN"));
    }

    public String scopedAgentId(String requestedAgentId) {
        AppUserDetails user = currentUser();
        if (isAdmin()) {
            return requestedAgentId;
        }
        if (hasRole(user, "AGENT")) {
            if (!StringUtils.hasText(user.agentId())) {
                throw new AccessDeniedException("Agent user has no configured agentId");
            }
            if (StringUtils.hasText(requestedAgentId) && !requestedAgentId.equalsIgnoreCase(user.agentId())) {
                throw new AccessDeniedException("Agents can only read their own statistics");
            }
            return user.agentId();
        }
        return requestedAgentId;
    }

    public String scopedTeam(String requestedTeam) {
        AppUserDetails user = currentUser();
        if (isAdmin()) {
            return requestedTeam;
        }
        List<String> allowedTeams = user.allowedTeams();
        if (allowedTeams == null || allowedTeams.isEmpty()) {
            return requestedTeam;
        }
        if (StringUtils.hasText(requestedTeam)) {
            boolean allowed = allowedTeams.stream().anyMatch(team -> team.equalsIgnoreCase(requestedTeam));
            if (!allowed) {
                throw new AccessDeniedException("User is not allowed to read team: " + requestedTeam);
            }
            return requestedTeam;
        }
        if (allowedTeams.size() == 1) {
            return allowedTeams.get(0);
        }
        throw new AccessDeniedException("Select one of the allowed teams: " + String.join(", ", allowedTeams));
    }

    public AppUserDetails currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails user)) {
            throw new AccessDeniedException("Authenticated user context is missing");
        }
        return user;
    }

    private boolean hasRole(AppUserDetails user, String role) {
        return user.roles().stream().anyMatch(candidate -> candidate.name().equals(role));
    }
}
