package com.example.pcceobservability.web;

import com.example.pcceobservability.model.CurrentUserView;
import com.example.pcceobservability.security.AccessControlService;
import com.example.pcceobservability.security.AppUserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AccessControlService accessControlService;

    public AuthController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @GetMapping("/me")
    public CurrentUserView me() {
        AppUserDetails user = accessControlService.currentUser();
        return new CurrentUserView(
                user.username(),
                user.displayName(),
                user.agentId(),
                user.allowedTeams(),
                user.roles(),
                user.permissions());
    }
}
