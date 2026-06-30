package com.cisco.cx.observability.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping({"/", "/dashboard"})
    public String dashboard() {
        return "redirect:/dashboard/index.html";
    }

    @GetMapping({"/overview", "/home"})
    public String overview() {
        return "core/home";
    }

    @GetMapping("/business")
    public String business() {
        return "core/business";
    }

    @GetMapping("/agents")
    public String agents() {
        return "core/agents";
    }

    @GetMapping("/calls")
    public String calls() {
        return "core/calls";
    }

    @GetMapping({"/system", "/infra"})
    public String system() {
        return "core/system";
    }

    @GetMapping({"/integrations", "/pcce"})
    public String integrations() {
        return dashboardView("integration");
    }

    @GetMapping("/cvp")
    public String cvp() {
        return dashboardView("cvp");
    }

    @GetMapping({"/advanced", "/jmx", "/appdynamics", "/live-data"})
    public String advanced() {
        return dashboardView("advanced");
    }

    @GetMapping({"/alerts", "/smtp", "/sms"})
    public String alerts() {
        return dashboardView("smtp");
    }

    @GetMapping({"/spog", "/operations"})
    public String spog() {
        return dashboardView("spog");
    }

    @GetMapping({"/eleveo", "/quality"})
    public String eleveo() {
        return dashboardView("eleveo");
    }

    @GetMapping("/admin")
    public String admin() {
        return dashboardView("admin");
    }

    @GetMapping({"/app", "/support"})
    public String app() {
        return dashboardView("app");
    }

    @GetMapping({"/wallboard", "/supervisor", "/sla-trends", "/ivr", "/forecast", "/adherence", "/cti", "/capacity", "/executive", "/cost"})
    public String plannedPageAliases(jakarta.servlet.http.HttpServletRequest request) {
        String path = request.getRequestURI();
        return switch (path) {
            case "/wallboard" -> "operations/wallboard";
            case "/supervisor" -> "operations/supervisor";
            case "/sla-trends" -> "operations/sla-trends";
            case "/ivr" -> "operations/ivr";
            case "/forecast" -> "operations/forecast";
            case "/adherence" -> "workforce/adherence";
            case "/cti" -> "technical/cti";
            case "/capacity" -> "technical/capacity";
            case "/executive" -> "executive/executive";
            case "/cost" -> "executive/cost";
            default -> dashboardView("overview");
        };
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    private String dashboardView(String view) {
        return "redirect:/dashboard/index.html?view=" + view;
    }
}
