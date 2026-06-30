package com.cisco.cx.observability.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

class SecurityConfigTest {

    @Test
    void csrfCookieFilterAccessesDeferredTokenSoCookieRepositoryCanPublishIt() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig(null, null, null);
        OncePerRequestFilter filter = securityConfig.csrfCookieFilter();
        AtomicBoolean tokenAccessed = new AtomicBoolean(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login.html");
        request.setAttribute(CsrfToken.class.getName(), new CsrfToken() {
            @Override
            public String getHeaderName() {
                return "X-XSRF-TOKEN";
            }

            @Override
            public String getParameterName() {
                return "_csrf";
            }

            @Override
            public String getToken() {
                tokenAccessed.set(true);
                return "token";
            }
        });

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(tokenAccessed).isTrue();
    }
}
