package com.invoiceflow.shared.security;

import com.invoiceflow.shared.tenancy.MissingTenantException;
import com.invoiceflow.shared.tenancy.TenantContext;
import com.invoiceflow.shared.tenancy.TenantId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Reads the tenant from the {@value JwtClaims#TENANT_ID} claim of the authenticated JWT. */
@Component
class JwtTenantContext implements TenantContext {

    @Override
    public TenantId currentTenant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token && token.isAuthenticated()) {
            String tenant = token.getToken().getClaimAsString(JwtClaims.TENANT_ID);
            if (tenant != null) {
                return TenantId.of(tenant);
            }
        }
        throw new MissingTenantException();
    }
}
