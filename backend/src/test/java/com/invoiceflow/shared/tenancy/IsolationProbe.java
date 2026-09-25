package com.invoiceflow.shared.tenancy;

import com.invoiceflow.shared.money.Money;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Minimal tenant-owned document used to prove isolation before real aggregates exist. */
@Document("isolation_probes")
class IsolationProbe extends TenantOwnedDocument {

    @Id
    private final String id;
    private final String name;
    private final Money price;

    IsolationProbe(String id, String name, Money price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    @Override
    public Object id() {
        return id;
    }

    String name() {
        return name;
    }

    Money price() {
        return price;
    }
}
