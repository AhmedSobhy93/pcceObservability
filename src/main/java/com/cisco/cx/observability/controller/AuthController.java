package com.cisco.cx.observability.controller;

import com.cisco.cx.observability.model.CurrentUserView;
import com.cisco.cx.observability.security.access.AccessControlService;
import com.cisco.cx.observability.security.userdetails.AppUserDetails;
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
