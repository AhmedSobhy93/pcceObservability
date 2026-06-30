package com.cisco.cx.observability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.config.PcceProperties.ApiAction;
import com.cisco.cx.observability.model.ApiActionView;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.junit.jupiter.api.Test;

class PcceApiMonitoringServiceTest {

    @Test
    void actionsKeepBuiltInReadActionsEnabledWhenConfigOverridesThem() {
        PcceProperties properties = new PcceProperties();
        properties.getPcceApi().setActions(List.of(disabledAction(
                "users.list",
                "GET",
                "/wrong/user/path")));

        PcceApiMonitoringService service = new PcceApiMonitoringService(
                properties,
                HttpsURLConnection.getDefaultSSLSocketFactory(),
                HttpsURLConnection.getDefaultHostnameVerifier());

        ApiActionView usersList = action(service.actions(), "users.list");
        ApiActionView machineInventory = action(service.actions(), "machineInventory.list");

        assertThat(usersList.enabled()).isTrue();
        assertThat(usersList.adminOnly()).isFalse();
        assertThat(usersList.method()).isEqualTo("GET");
        assertThat(usersList.path()).isEqualTo("/unifiedconfig/config/user");
        assertThat(usersList.contentType()).isEqualTo("application/xml");

        assertThat(machineInventory.enabled()).isTrue();
        assertThat(machineInventory.path()).isEqualTo("/unifiedconfig/config/machineinventory");
    }

    @Test
    void executeRejectsActionWhenRequiredPathParamIsMissingBeforeNetworkCall() {
        PcceProperties properties = new PcceProperties();
        properties.getPcceApi().setEnabled(true);
        properties.getPcceApi().setBaseUrl("pcce.example.local");

        PcceApiMonitoringService service = new PcceApiMonitoringService(
                properties,
                HttpsURLConnection.getDefaultSSLSocketFactory(),
                HttpsURLConnection.getDefaultHostnameVerifier());

        assertThatThrownBy(() -> service.execute("agent.get", null, java.util.Map.of(), java.util.Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing path params")
                .hasMessageContaining("/unifiedconfig/config/agent/{id}");
    }

    @Test
    void executeRejectsDisabledMutatingActionBeforeNetworkCall() {
        PcceProperties properties = new PcceProperties();
        properties.getPcceApi().setEnabled(true);
        properties.getPcceApi().setBaseUrl("pcce.example.local");

        PcceApiMonitoringService service = new PcceApiMonitoringService(
                properties,
                HttpsURLConnection.getDefaultSSLSocketFactory(),
                HttpsURLConnection.getDefaultHostnameVerifier());

        assertThatThrownBy(() -> service.execute("skill.create", "{}", java.util.Map.of(), java.util.Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PCCE API action is disabled: skill.create");
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
