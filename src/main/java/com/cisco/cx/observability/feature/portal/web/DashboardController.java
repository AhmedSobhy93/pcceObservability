package com.cisco.cx.observability.feature.portal.web;

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
        return "feature/reporting/overview";
    }

    @GetMapping("/business")
    public String business() {
        return "feature/reporting/business";
    }

    @GetMapping("/agents")
    public String agents() {
        return "feature/reporting/agents";
    }

    @GetMapping("/calls")
    public String calls() {
        return "feature/reporting/calls";
    }

    @GetMapping({"/system", "/infra"})
    public String system() {
        return "feature/components/system";
    }

    @GetMapping({"/integrations", "/pcce"})
    public String integrations() {
        return "feature/integration/integration";
    }

    @GetMapping("/cvp")
    public String cvp() {
        return "feature/integration/cvp";
    }

    @GetMapping({"/advanced", "/jmx", "/appdynamics", "/live-data"})
    public String advanced() {
        return "feature/integration/advanced";
    }

    @GetMapping({"/alerts", "/smtp", "/sms"})
    public String alerts() {
        return "feature/operations/alerts";
    }

    @GetMapping({"/spog", "/operations"})
    public String spog() {
        return "feature/operations/spog";
    }

    @GetMapping({"/eleveo", "/quality"})
    public String eleveo() {
        return dashboardView("eleveo");
    }

    @GetMapping("/admin")
    public String admin() {
        return "feature/usermgmt/admin";
    }

    @GetMapping("/servers")
    public String servers() {
        return "feature/components/servers";
    }

    @GetMapping({"/config", "/agent-skills"})
    public String config() {
        return "feature/workforce/config";
    }

    @GetMapping({"/app", "/support"})
    public String app() {
        return dashboardView("app");
    }

    @GetMapping({"/wallboard", "/supervisor", "/sla-trends", "/ivr", "/forecast", "/adherence", "/cti", "/capacity", "/executive", "/cost"})
    public String plannedPageAliases(jakarta.servlet.http.HttpServletRequest request) {
        String path = request.getRequestURI();
        return switch (path) {
            case "/wallboard" -> "feature/reporting/wallboard";
            case "/supervisor" -> "feature/operations/supervisor";
            case "/sla-trends" -> "feature/operations/sla-trends";
            case "/ivr" -> "feature/operations/ivr";
            case "/forecast" -> "feature/operations/forecast";
            case "/adherence" -> "feature/workforce/adherence";
            case "/cti" -> "feature/monitoring/cti";
            case "/capacity" -> "feature/monitoring/capacity";
            case "/executive" -> "feature/reporting/executive";
            case "/cost" -> "feature/reporting/cost";
            default -> dashboardView("overview");
        };
    }

    @GetMapping({"/login", "/profile/login"})
    public String login() {
        return "auth/login";
    }

    private String dashboardView(String view) {
        return "redirect:/dashboard/index.html?view=" + view;
    }
}
