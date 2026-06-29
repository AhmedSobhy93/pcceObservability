package com.cisco.cx.observability.security;

import static org.springframework.security.config.Customizer.withDefaults;

import com.cisco.cx.observability.config.PcceProperties;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final LdapAuthenticationProvider ldapAuthenticationProvider;
    private final ConfiguredUserDetailsService configuredUserDetailsService;
    private final PcceProperties pcceProperties;

    public SecurityConfig(
            LdapAuthenticationProvider ldapAuthenticationProvider,
            ConfiguredUserDetailsService configuredUserDetailsService,
            PcceProperties pcceProperties) {
        this.ldapAuthenticationProvider = ldapAuthenticationProvider;
        this.configuredUserDetailsService = configuredUserDetailsService;
        this.pcceProperties = pcceProperties;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/v1/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/login", "/login.html", "/logout.html", "/login.css").permitAll()
                        .requestMatchers("/h2-console/**").hasAuthority("PERM_SOLUTION_ADMIN")
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").hasAuthority("PERM_SOLUTION_ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard/index.html", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                        .logoutSuccessUrl("/logout.html")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .authenticationProvider(daoAuthenticationProvider(passwordEncoder()))
                .authenticationProvider(ldapAuthenticationProvider)
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                contentSecurityPolicy()))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "geolocation=(), microphone=(), camera=()")))
                .httpBasic(withDefaults());
        return http.build();
    }

    private String contentSecurityPolicy() {
        return "default-src 'self'; "
                + "script-src 'self' 'unsafe-inline'; "
                + "style-src 'self' 'unsafe-inline'; "
                + "img-src 'self' data:; "
                + "connect-src 'self'; "
                + "frame-src 'self' " + grafanaFrameSources() + "; "
                + "frame-ancestors 'self'";
    }

    private String grafanaFrameSources() {
        Set<String> sources = new LinkedHashSet<>();
        if (pcceProperties.getGrafana() != null && pcceProperties.getGrafana().getDashboards() != null) {
            pcceProperties.getGrafana().getDashboards().stream()
                    .filter(PcceProperties.GrafanaDashboard::isEnabled)
                    .map(PcceProperties.GrafanaDashboard::getUrl)
                    .map(this::originOf)
                    .filter(origin -> !origin.isBlank())
                    .forEach(sources::add);
        }
        return sources.isEmpty() ? "" : String.join(" ", sources);
    }

    private String originOf(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(url.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "";
            }
            int port = uri.getPort();
            return uri.getScheme() + "://" + uri.getHost() + (port > -1 ? ":" + port : "");
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(configuredUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
