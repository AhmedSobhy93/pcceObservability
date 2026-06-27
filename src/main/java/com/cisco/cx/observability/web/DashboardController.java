package com.cisco.cx.observability.web;

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
        return dashboardView("overview");
    }

    @GetMapping("/business")
    public String business() {
        return dashboardView("business");
    }

    @GetMapping("/agents")
    public String agents() {
        return dashboardView("agents");
    }

    @GetMapping("/calls")
    public String calls() {
        return dashboardView("calls");
    }

    @GetMapping({"/system", "/infra"})
    public String system() {
        return dashboardView("system");
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
    public String plannedPageAliases() {
        return dashboardView("overview");
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:/login.html";
    }

    private String dashboardView(String view) {
        return "redirect:/dashboard/index.html?view=" + view;
    }
}
