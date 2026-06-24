package com.example.pcceobservability.security;

import static org.springframework.security.config.Customizer.withDefaults;

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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final LdapAuthenticationProvider ldapAuthenticationProvider;
    private final ConfiguredUserDetailsService configuredUserDetailsService;

    public SecurityConfig(
            LdapAuthenticationProvider ldapAuthenticationProvider,
            ConfiguredUserDetailsService configuredUserDetailsService) {
        this.ldapAuthenticationProvider = ldapAuthenticationProvider;
        this.configuredUserDetailsService = configuredUserDetailsService;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
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
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .httpBasic(withDefaults());
        return http.build();
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
