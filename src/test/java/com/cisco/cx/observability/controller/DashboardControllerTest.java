package com.cisco.cx.observability.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;

class DashboardControllerTest {

    @Test
    void loginReturnsThymeleafLoginTemplateInsteadOfRedirectingToStaticPage() {
        DashboardController controller = new DashboardController();

        assertThat(controller.login()).isEqualTo("auth/login");
    }

    @Test
    @DisplayName("login page is also available under profile login path")
    void loginRouteIncludesProfileLoginAlias() throws NoSuchMethodException {
        GetMapping mapping = DashboardController.class.getDeclaredMethod("login").getAnnotation(GetMapping.class);

        assertThat(mapping.value()).contains("/login", "/profile/login");
    }

    @Test
    void dashboardRoutesReturnRequestedTemplateTree() {
        DashboardController controller = new DashboardController();

        assertThat(controller.overview()).isEqualTo("analytics/overview");
        assertThat(controller.business()).isEqualTo("analytics/business");
        assertThat(controller.agents()).isEqualTo("analytics/agents");
        assertThat(controller.calls()).isEqualTo("analytics/calls");
        assertThat(controller.system()).isEqualTo("operations/system");
        assertThat(controller.integrations()).isEqualTo("integrations/integration");
        assertThat(controller.advanced()).isEqualTo("integrations/advanced");
        assertThat(controller.cvp()).isEqualTo("configuration/cvp");
        assertThat(controller.alerts()).isEqualTo("operations/alerts");
        assertThat(controller.spog()).isEqualTo("operations/spog");
        assertThat(controller.admin()).isEqualTo("admin/admin");
        assertThat(controller.servers()).isEqualTo("configuration/servers");
        assertThat(controller.config()).isEqualTo("configuration/config");
    }

    @Test
    void wallboardAliasUsesAnalyticsTemplate() {
        DashboardController controller = new DashboardController();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wallboard");

        assertThat(controller.plannedPageAliases(request)).isEqualTo("analytics/wallboard");
    }
}
