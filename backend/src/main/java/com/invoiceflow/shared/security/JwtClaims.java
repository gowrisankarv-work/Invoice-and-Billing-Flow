package com.invoiceflow.shared.security;

/** Custom claims InvoiceFlow expects in access tokens issued by the identity provider. */
public final class JwtClaims {

    /** The tenant (organization) the token was issued for. Required. */
    public static final String TENANT_ID = "tenant_id";

    /** Array of {@link Role} names held by the user in that tenant. */
    public static final String ROLES = "roles";

    private JwtClaims() {
    }
}
