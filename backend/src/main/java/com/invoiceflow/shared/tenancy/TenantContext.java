package com.invoiceflow.shared.tenancy;

/**
 * Resolves the tenant of the current request from the security context.
 *
 * <p>There is deliberately no way to set the tenant from request data: the client is never trusted to
 * choose a tenant.
 */
public interface TenantContext {

    /**
     * Returns the current tenant.
     *
     * @throws MissingTenantException if there is no authenticated principal carrying a tenant
     */
    TenantId currentTenant();
}
