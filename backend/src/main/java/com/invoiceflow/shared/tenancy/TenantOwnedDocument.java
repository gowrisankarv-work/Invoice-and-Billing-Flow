package com.invoiceflow.shared.tenancy;

import org.springframework.data.mongodb.core.index.Indexed;

/**
 * Base class for every MongoDB document owned by a tenant.
 *
 * <p>The tenant is assigned by {@link TenantScopedMongoOperations} on first save and cannot be set by
 * application code, so a document can never be moved to, or created for, another tenant.
 */
public abstract class TenantOwnedDocument {

    public static final String TENANT_FIELD = "tenantId";

    @Indexed
    private String tenantId;

    public TenantId tenant() {
        return tenantId == null ? null : TenantId.of(tenantId);
    }

    /** Document identifier, used to guard writes against another tenant's record. */
    public abstract Object id();

    void assignTenant(TenantId tenant) {
        this.tenantId = tenant.value();
    }
}
