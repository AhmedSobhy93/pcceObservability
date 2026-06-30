package com.cisco.cx.observability.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DashboardControllerTest {

    @Test
    void loginReturnsThymeleafLoginTemplateInsteadOfRedirectingToStaticPage() {
        DashboardController controller = new DashboardController();

        assertThat(controller.login()).isEqualTo("auth/login");
    }
}
