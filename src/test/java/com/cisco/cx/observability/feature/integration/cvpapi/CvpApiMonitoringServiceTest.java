package com.cisco.cx.observability.feature.integration.cvpapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.config.PcceProperties.ApiAction;
import com.cisco.cx.observability.feature.integration.cvpapi.CvpApiMonitoringService;
import com.cisco.cx.observability.feature.integration.pcceapi.ApiActionView;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.junit.jupiter.api.Test;

class CvpApiMonitoringServiceTest {

    @Test
    void actionsKeepBuiltInOperationsReadActionsEnabledWhenConfigOverridesThem() {
        PcceProperties properties = new PcceProperties();
        properties.getCvpApi().setActions(List.of(disabledAction(
                "cvp.syslog.get",
                "GET",
                "/wrong/syslog/path")));

        CvpApiMonitoringService service = new CvpApiMonitoringService(
                properties,
                HttpsURLConnection.getDefaultSSLSocketFactory(),
                HttpsURLConnection.getDefaultHostnameVerifier());

        ApiActionView syslog = action(service.actions(), "cvp.syslog.get");
        ApiActionView snmp = action(service.actions(), "cvp.snmp.get");

        assertThat(syslog.enabled()).isTrue();
        assertThat(syslog.adminOnly()).isFalse();
        assertThat(syslog.method()).isEqualTo("GET");
        assertThat(syslog.path()).isEqualTo("/cvp/api/syslog");
        assertThat(syslog.contentType()).isEqualTo("application/xml");

        assertThat(snmp.enabled()).isTrue();
        assertThat(snmp.path()).isEqualTo("/cvp/api/snmp");
    }

    @Test
    void executeRejectsActionWhenRequiredPathParamIsMissingBeforeNetworkCall() {
        PcceProperties properties = new PcceProperties();
        properties.getCvpApi().setEnabled(true);
        properties.getCvpApi().setBaseUrl("cvp.example.local");

        CvpApiMonitoringService service = new CvpApiMonitoringService(
                properties,
                HttpsURLConnection.getDefaultSSLSocketFactory(),
                HttpsURLConnection.getDefaultHostnameVerifier());

        assertThatThrownBy(() -> service.execute("cvp.app.get", null, java.util.Map.of(), java.util.Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing path params")
                .hasMessageContaining("/cvp/api/applications/{name}");
    }

    @Test
    void executeRejectsDisabledMutatingActionBeforeNetworkCall() {
        PcceProperties properties = new PcceProperties();
        properties.getCvpApi().setEnabled(true);
        properties.getCvpApi().setBaseUrl("cvp.example.local");

        CvpApiMonitoringService service = new CvpApiMonitoringService(
                properties,
                HttpsURLConnection.getDefaultSSLSocketFactory(),
                HttpsURLConnection.getDefaultHostnameVerifier());

        assertThatThrownBy(() -> service.execute("cvp.syslog.update", "{}", java.util.Map.of(), java.util.Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CVP API action is disabled: cvp.syslog.update");
    }

    private ApiActionView action(List<ApiActionView> actions, String id) {
        return actions.stream()
                .filter(action -> action.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private ApiAction disabledAction(String id, String method, String path) {
        ApiAction action = new ApiAction();
        action.setId(id);
        action.setCategory("Configured");
        action.setName("Configured action");
        action.setMethod(method);
        action.setPath(path);
        action.setEnabled(false);
        action.setAdminOnly(true);
        action.setContentType("application/json");
        return action;
    }
}
