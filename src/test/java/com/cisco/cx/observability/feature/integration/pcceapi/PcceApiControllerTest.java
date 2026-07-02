package com.cisco.cx.observability.feature.integration.pcceapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.cisco.cx.observability.feature.integration.pcceapi.ApiActionResult;
import com.cisco.cx.observability.feature.integration.pcceapi.ApiActionRequest;
import com.cisco.cx.observability.feature.integration.pcceapi.ApiActionView;
import com.cisco.cx.observability.feature.integration.pcceapi.PcceApiController;
import com.cisco.cx.observability.feature.integration.pcceapi.PcceApiMonitoringService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class PcceApiControllerTest {

    @Test
    void executeDelegatesReadActionForOperationsUser() {
        PcceApiMonitoringService service = mock(PcceApiMonitoringService.class);
        when(service.actions()).thenReturn(List.of(action("users.list", false, "GET")));
        ApiActionResult expected = new ApiActionResult(
                "users.list",
                "GET",
                "https://pcce.example.local/unifiedconfig/config/user",
                200,
                10,
                "<users/>",
                Instant.parse("2026-06-30T00:00:00Z"));
        when(service.execute("users.list", null, Map.of(), Map.of())).thenReturn(expected);

        PcceApiController controller = new PcceApiController(service);

        ApiActionResult result = controller.execute("users.list", null, authentication("PERM_OPERATIONS_READ"));

        assertThat(result).isSameAs(expected);
        verify(service).actions();
        verify(service).execute("users.list", null, Map.of(), Map.of());
        verifyNoMoreInteractions(service);
    }

    @Test
    void executeRejectsMutatingActionForNonAdminUserBeforeServiceExecution() {
        PcceApiMonitoringService service = mock(PcceApiMonitoringService.class);
        when(service.actions()).thenReturn(List.of(action("skill.create", true, "POST")));

        PcceApiController controller = new PcceApiController(service);

        assertThatThrownBy(() -> controller.execute("skill.create", null, authentication("PERM_OPERATIONS_READ")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only solution administrators");

        verify(service).actions();
        verifyNoMoreInteractions(service);
    }

    @Test
    void executeDelegatesMutatingActionForSolutionAdmin() {
        PcceApiMonitoringService service = mock(PcceApiMonitoringService.class);
        when(service.actions()).thenReturn(List.of(action("skill.create", true, "POST")));
        ApiActionRequest request = new ApiActionRequest("<skill/>", Map.of(), Map.of());
        ApiActionResult expected = new ApiActionResult(
                "skill.create",
                "POST",
                "https://pcce.example.local/unifiedconfig/config/skillgroup",
                201,
                10,
                "<skill/>",
                Instant.parse("2026-06-30T00:00:00Z"));
        when(service.execute("skill.create", request.body(), request.pathParams(), request.queryParams())).thenReturn(expected);

        PcceApiController controller = new PcceApiController(service);

        ApiActionResult result = controller.execute(
                "skill.create",
                request,
                authentication("PERM_OPERATIONS_READ", "PERM_SOLUTION_ADMIN"));

        assertThat(result).isSameAs(expected);
        verify(service).actions();
        verify(service).execute("skill.create", request.body(), request.pathParams(), request.queryParams());
        verifyNoMoreInteractions(service);
    }

    @Test
    void executePassesRequestBodyPathParamsAndQueryParamsToService() {
        PcceApiMonitoringService service = mock(PcceApiMonitoringService.class);
        when(service.actions()).thenReturn(List.of(action("agent.get", false, "GET")));
        ApiActionRequest request = new ApiActionRequest(
                "<request><trace>true</trace></request>",
                Map.of("id", "5001"),
                Map.of("resultsPerPage", "100"));
        ApiActionResult expected = new ApiActionResult(
                "agent.get",
                "GET",
                "https://pcce.example.local/unifiedconfig/config/agent/5001?resultsPerPage=100",
                200,
                10,
                "<agent/>",
                Instant.parse("2026-06-30T00:00:00Z"));
        when(service.execute("agent.get", request.body(), request.pathParams(), request.queryParams())).thenReturn(expected);

        PcceApiController controller = new PcceApiController(service);

        ApiActionResult result = controller.execute("agent.get", request, authentication("PERM_OPERATIONS_READ"));

        assertThat(result).isSameAs(expected);
        verify(service).actions();
        verify(service).execute("agent.get", request.body(), request.pathParams(), request.queryParams());
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
