package com.cisco.cx.observability.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping({"/", "/dashboard"})
    public String dashboard() {
        return "redirect:/dashboard/index.html";
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:/login.html";
    }
}
