package com.cisco.cx.observability.service;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.model.ExternalIntegrationStatus;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdvancedIntegrationService {

    private final PcceProperties pcceProperties;

    public AdvancedIntegrationService(PcceProperties pcceProperties) {
        this.pcceProperties = pcceProperties;
    }

    public List<ExternalIntegrationStatus> jmx() {
        PcceProperties.Jmx jmx = pcceProperties.getJmx();
        if (jmx == null || !jmx.isEnabled()) {
            return List.of(new ExternalIntegrationStatus(
                    "Secure JMX",
                    "CVP / JVM",
                    "disabled",
                    "JMX",
                    "Enable pcce.jmx.enabled after CVP secure JMX truststore and credentials are configured."));
        }
        List<ExternalIntegrationStatus> statuses = new ArrayList<>();
        String trustDetail = jmx.isTrustStoreConfigured()
                ? "Truststore marked configured"
                : "Truststore not marked configured; secure CVP JMX usually requires importing OAMP/CVP certificates.";
        statuses.add(new ExternalIntegrationStatus("Secure JMX", "Readiness", readiness(jmx.getUsername(), jmx.getPassword()),
                "targets=" + safeSize(jmx.getTargets()), trustDetail));
        for (PcceProperties.JmxTarget target : safeList(jmx.getTargets())) {
            statuses.add(new ExternalIntegrationStatus(
                    target.getName(),
                    target.getComponent(),
                    target.isEnabled() && StringUtils.hasText(target.getServiceUrl()) ? "configured" : "not configured",
                    target.getServiceUrl(),
                    "MBeans: " + String.join(", ", safeList(target.getMbeans()))));
        }
        return statuses;
    }

    public List<ExternalIntegrationStatus> appDynamics() {
        PcceProperties.AppDynamics appDynamics = pcceProperties.getAppDynamics();
        if (appDynamics == null || !appDynamics.isEnabled()) {
            return List.of(new ExternalIntegrationStatus(
                    "AppDynamics",
                    "APM",
                    "disabled",
                    "Controller",
                    "Enable pcce.app-dynamics.enabled and configure controller URL, account, application and API user."));
        }
        return List.of(
                new ExternalIntegrationStatus("Controller", "AppDynamics", configured(appDynamics.getControllerUrl()),
                        appDynamics.getControllerUrl(), "Controller endpoint used for PCCE node/application telemetry."),
                new ExternalIntegrationStatus("Application", "AppDynamics", configured(appDynamics.getApplicationName()),
                        appDynamics.getApplicationName(), "Application name expected to contain PCCE/CVP/Finesse tiers."),
                new ExternalIntegrationStatus("Dashboards", "AppDynamics", safeSize(appDynamics.getDashboards()) > 0 ? "configured" : "not configured",
                        "dashboards=" + safeSize(appDynamics.getDashboards()), "Optional embedded deep links for support teams."),
                new ExternalIntegrationStatus("Credentials", "AppDynamics", readiness(appDynamics.getUsername(), appDynamics.getPassword()),
                        userWithAccount(appDynamics.getUsername(), appDynamics.getAccountName()), "Use read-only API user for monitoring."));
    }

    public List<ExternalIntegrationStatus> liveData() {
        PcceProperties.LiveData liveData = pcceProperties.getLiveData();
        if (liveData == null || !liveData.isEnabled()) {
            return List.of(new ExternalIntegrationStatus(
                    "PCCE Live Data",
                    "Realtime",
                    "disabled",
                    "Live Data API/WebSocket",
                    "Enable pcce.live-data.enabled and configure CUIC Live Data host, token path, user and websocket port."));
        }
        String api = liveData.getHost() == null ? "" : "https://" + liveData.getHost() + ":" + liveData.getPort() + "/" + trimSlash(liveData.getTokenPath());
        String websocket = liveData.getHost() == null ? "" : "wss://" + liveData.getHost() + ":" + liveData.getWebsocketPort();
        return List.of(
                new ExternalIntegrationStatus("Token API", "Live Data", configured(liveData.getHost()), api,
                        "Use for realtime token acquisition; keep timeouts short and cache tokens where supported."),
                new ExternalIntegrationStatus("WebSocket", "Live Data", configured(liveData.getHost()), websocket,
                        "Use for low-latency agent, skill group and call type telemetry."),
                new ExternalIntegrationStatus("Credentials", "Live Data", readiness(liveData.getUsername(), liveData.getPassword()),
                        liveData.getUsername(), "Configured CUIC Live Data database/user identity."),
                new ExternalIntegrationStatus("Stock Reports", "CUIC", safeSize(liveData.getStockReports()) > 0 ? "mapped" : "not mapped",
                        String.join(", ", safeList(liveData.getStockReports())),
                        "Use Live Data for realtime screens; use HDS/CUIC SQL for historical dashboards."));
    }

    private static String readiness(String username, String password) {
        return StringUtils.hasText(username) && password != null && !password.isBlank() ? "configured" : "missing credentials";
    }

    private static String configured(String value) {
        return StringUtils.hasText(value) ? "configured" : "not configured";
    }

    private static String userWithAccount(String username, String accountName) {
        if (!StringUtils.hasText(username)) {
            return "";
        }
        return StringUtils.hasText(accountName) ? username + "@" + accountName : username;
    }

    private static String trimSlash(String value) {
        return value == null ? "" : value.replaceAll("^/+", "");
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static int safeSize(List<?> values) {
        return values == null ? 0 : values.size();
    }
}
