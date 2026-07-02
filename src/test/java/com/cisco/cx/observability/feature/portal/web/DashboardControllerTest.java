package com.cisco.cx.observability.feature.portal.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.cisco.cx.observability.feature.portal.web.DashboardController;
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

        assertThat(controller.overview()).isEqualTo("feature/reporting/overview");
        assertThat(controller.business()).isEqualTo("feature/reporting/business");
        assertThat(controller.agents()).isEqualTo("feature/reporting/agents");
        assertThat(controller.calls()).isEqualTo("feature/reporting/calls");
        assertThat(controller.system()).isEqualTo("feature/components/system");
        assertThat(controller.integrations()).isEqualTo("feature/integration/integration");
        assertThat(controller.advanced()).isEqualTo("feature/integration/advanced");
        assertThat(controller.cvp()).isEqualTo("feature/integration/cvp");
        assertThat(controller.alerts()).isEqualTo("feature/operations/alerts");
        assertThat(controller.spog()).isEqualTo("feature/operations/spog");
        assertThat(controller.admin()).isEqualTo("feature/usermgmt/admin");
        assertThat(controller.servers()).isEqualTo("feature/components/servers");
        assertThat(controller.config()).isEqualTo("feature/workforce/config");
    }

    @Test
    void wallboardAliasUsesAnalyticsTemplate() {
        DashboardController controller = new DashboardController();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wallboard");

        assertThat(controller.plannedPageAliases(request)).isEqualTo("feature/reporting/wallboard");
    }
}
