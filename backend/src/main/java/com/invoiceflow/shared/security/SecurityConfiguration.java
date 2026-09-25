package com.invoiceflow.shared.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Stateless JWT resource server. Every {@code /api/**} call needs a bearer token carrying a tenant;
 * only health and info probes are public. Authentication and authorization failures are rendered as
 * RFC 9457 problem details by the MVC exception handler.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(
            HttpSecurity http, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver)
            throws Exception {
        var problemHandler = new ProblemDetailSecurityHandler(resolver);
        http.authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/error")
                        .permitAll()
                        .requestMatchers("/api/**")
                        .authenticated()
                        .anyRequest()
                        .denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new TenantJwtAuthenticationConverter()))
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * OWNER implies ADMIN; ADMIN implies ACCOUNTANT and SALES; both imply STAFF; STAFF implies VIEWER.
     * So {@code hasRole('VIEWER')} admits every member and {@code hasRole('ACCOUNTANT')} admits
     * accountants, admins and owners.
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(Role.OWNER.name()).implies(Role.ADMIN.name())
                .role(Role.ADMIN.name()).implies(Role.ACCOUNTANT.name(), Role.SALES.name())
                .role(Role.ACCOUNTANT.name()).implies(Role.STAFF.name())
                .role(Role.SALES.name()).implies(Role.STAFF.name())
                .role(Role.STAFF.name()).implies(Role.VIEWER.name())
                .build();
    }
}
