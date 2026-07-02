package com.cisco.cx.observability.feature.integration.cvpapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.cisco.cx.observability.feature.integration.cvpapi.CvpApiController;
import com.cisco.cx.observability.feature.integration.cvpapi.CvpApiMonitoringService;
import com.cisco.cx.observability.feature.integration.pcceapi.ApiActionResult;
import com.cisco.cx.observability.feature.integration.pcceapi.ApiActionRequest;
import com.cisco.cx.observability.feature.integration.pcceapi.ApiActionView;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class CvpApiControllerTest {

    @Test
    void executeDelegatesReadActionForOperationsUser() {
        CvpApiMonitoringService service = mock(CvpApiMonitoringService.class);
        when(service.actions()).thenReturn(List.of(action("cvp.syslog.get", false, "GET")));
        ApiActionResult expected = new ApiActionResult(
                "cvp.syslog.get",
                "GET",
                "https://cvp.example.local/cvp/api/syslog",
                200,
                10,
                "<syslog/>",
                Instant.parse("2026-06-30T00:00:00Z"));
        when(service.execute("cvp.syslog.get", null, Map.of(), Map.of())).thenReturn(expected);

        CvpApiController controller = new CvpApiController(service);

        ApiActionResult result = controller.execute("cvp.syslog.get", null, authentication("PERM_OPERATIONS_READ"));

        assertThat(result).isSameAs(expected);
        verify(service).actions();
        verify(service).execute("cvp.syslog.get", null, Map.of(), Map.of());
        verifyNoMoreInteractions(service);
    }

    @Test
    void executeRejectsMutatingActionForNonAdminUserBeforeServiceExecution() {
        CvpApiMonitoringService service = mock(CvpApiMonitoringService.class);
        when(service.actions()).thenReturn(List.of(action("cvp.syslog.update", true, "PUT")));

        CvpApiController controller = new CvpApiController(service);

        assertThatThrownBy(() -> controller.execute("cvp.syslog.update", null, authentication("PERM_OPERATIONS_READ")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only solution administrators");

        verify(service).actions();
        verifyNoMoreInteractions(service);
    }

    @Test
    void executeDelegatesMutatingActionForSolutionAdmin() {
        CvpApiMonitoringService service = mock(CvpApiMonitoringService.class);
        when(service.actions()).thenReturn(List.of(action("cvp.syslog.update", true, "PUT")));
        ApiActionRequest request = new ApiActionRequest("<syslog/>", Map.of(), Map.of());
        ApiActionResult expected = new ApiActionResult(
                "cvp.syslog.update",
                "PUT",
                "https://cvp.example.local/cvp/api/syslog",
                200,
                10,
                "<syslog/>",
                Instant.parse("2026-06-30T00:00:00Z"));
        when(service.execute("cvp.syslog.update", request.body(), request.pathParams(), request.queryParams())).thenReturn(expected);

        CvpApiController controller = new CvpApiController(service);

        ApiActionResult result = controller.execute(
                "cvp.syslog.update",
                request,
                authentication("PERM_OPERATIONS_READ", "PERM_SOLUTION_ADMIN"));

        assertThat(result).isSameAs(expected);
        verify(service).actions();
        verify(service).execute("cvp.syslog.update", request.body(), request.pathParams(), request.queryParams());
        verifyNoMoreInteractions(service);
    }

    @Test
    void executePassesRequestBodyPathParamsAndQueryParamsToService() {
        CvpApiMonitoringService service = mock(CvpApiMonitoringService.class);
        when(service.actions()).thenReturn(List.of(action("cvp.app.get", false, "GET")));
        ApiActionRequest request = new ApiActionRequest(
                "<request><includeRuntime>true</includeRuntime></request>",
                Map.of("name", "OneBank_Main_IVR"),
                Map.of("includeDetails", "true"));
        ApiActionResult expected = new ApiActionResult(
                "cvp.app.get",
                "GET",
                "https://cvp.example.local/cvp/api/applications/OneBank_Main_IVR?includeDetails=true",
                200,
                10,
                "<application/>",
                Instant.parse("2026-06-30T00:00:00Z"));
        when(service.execute("cvp.app.get", request.body(), request.pathParams(), request.queryParams())).thenReturn(expected);

        CvpApiController controller = new CvpApiController(service);

        ApiActionResult result = controller.execute("cvp.app.get", request, authentication("PERM_OPERATIONS_READ"));

        assertThat(result).isSameAs(expected);
        verify(service).actions();
        verify(service).execute("cvp.app.get", request.body(), request.pathParams(), request.queryParams());
        verifyNoMoreInteractions(service);
    }

    private ApiActionView action(String id, boolean adminOnly, String method) {
        return new ApiActionView(id, "Test", "Test action", true, adminOnly, method, "/test", "application/xml");
    }

    private UsernamePasswordAuthenticationToken authentication(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                "operator",
                "n/a",
                java.util.Arrays.stream(authorities)
                        .map(SimpleGrantedAuthority::new)
                        .toList());
    }
}
