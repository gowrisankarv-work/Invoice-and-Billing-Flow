package com.invoiceflow.shared.security;

import com.invoiceflow.shared.tenancy.TenantContext;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets clients see who they are authenticated as and which tenant their requests are scoped to. */
@RestController
class CurrentUserController {

    private final TenantContext tenantContext;

    CurrentUserController(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    @GetMapping("/me")
    CurrentUser me(JwtAuthenticationToken authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .sorted()
                .toList();
        return new CurrentUser(authentication.getName(), tenantContext.currentTenant().value(), roles);
    }

    record CurrentUser(String subject, String tenantId, List<String> roles) {
    }
}
