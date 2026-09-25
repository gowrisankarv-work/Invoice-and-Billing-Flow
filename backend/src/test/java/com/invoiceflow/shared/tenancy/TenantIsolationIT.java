package com.invoiceflow.shared.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.invoiceflow.shared.money.Money;
import com.invoiceflow.support.IntegrationTest;
import com.invoiceflow.support.TestJwt;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.context.SecurityContextHolder;

/** Proves, against a real MongoDB, that one tenant can never read or change another tenant's data. */
@IntegrationTest
class TenantIsolationIT {

    @Autowired
    TenantScopedMongoOperations operations;

    @Autowired
    MongoTemplate rawMongo;

    @BeforeEach
    void clean() {
        rawMongo.dropCollection(IsolationProbe.class);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void savingStampsTheAuthenticatedTenant() {
        actAs("acme");
        IsolationProbe saved = operations.save(new IsolationProbe("p-1", "Consulting", Money.inr("1500.00")));

        assertThat(saved.tenant()).isEqualTo(TenantId.of("acme"));
        Document raw = rawMongo.getCollection("isolation_probes").find().first();
        assertThat(raw.getString("tenantId")).isEqualTo("acme");
    }

    @Test
    void anotherTenantCannotReadCountOrDelete() {
        actAs("acme");
        operations.save(new IsolationProbe("p-1", "Consulting", Money.inr("1500.00")));

        actAs("globex");
        assertThat(operations.findById("p-1", IsolationProbe.class)).isEmpty();
        assertThat(operations.findAll(IsolationProbe.class)).isEmpty();
        assertThat(operations.find(Query.query(where("name").is("Consulting")), IsolationProbe.class)).isEmpty();
        assertThat(operations.count(new Query(), IsolationProbe.class)).isZero();
        assertThat(operations.deleteById("p-1", IsolationProbe.class)).isFalse();

        actAs("acme");
        assertThat(operations.findById("p-1", IsolationProbe.class)).isPresent();
        assertThat(operations.findAll(IsolationProbe.class)).hasSize(1);
    }

    @Test
    void eachTenantOnlySeesItsOwnDocuments() {
        actAs("acme");
        operations.save(new IsolationProbe("a-1", "Acme work", Money.inr("10.00")));
        actAs("globex");
        operations.save(new IsolationProbe("g-1", "Globex work", Money.inr("20.00")));

        assertThat(operations.findAll(IsolationProbe.class)).extracting(IsolationProbe::name)
                .containsExactly("Globex work");
        actAs("acme");
        assertThat(operations.findAll(IsolationProbe.class)).extracting(IsolationProbe::name)
                .containsExactly("Acme work");
    }

    @Test
    void anotherTenantCannotOverwriteADocumentByReusingItsId() {
        actAs("acme");
        operations.save(new IsolationProbe("p-1", "Consulting", Money.inr("1500.00")));

        actAs("globex");
        assertThatThrownBy(() -> operations.save(new IsolationProbe("p-1", "Hijacked", Money.inr("0.00"))))
                .isInstanceOf(TenantAccessViolationException.class);

        actAs("acme");
        assertThat(operations.findById("p-1", IsolationProbe.class)).get()
                .extracting(IsolationProbe::name).isEqualTo("Consulting");
    }

    @Test
    void aDocumentLoadedByOneTenantCannotBeSavedByAnother() {
        actAs("acme");
        IsolationProbe acmeDocument = operations.save(new IsolationProbe("p-1", "Consulting", Money.inr("1.00")));

        actAs("globex");
        assertThatThrownBy(() -> operations.save(acmeDocument)).isInstanceOf(TenantAccessViolationException.class);
    }

    @Test
    void queriesCannotWidenOrRedirectTheTenantScope() {
        actAs("acme");
        operations.save(new IsolationProbe("p-1", "Consulting", Money.inr("1.00")));

        actAs("globex");
        Query redirect = Query.query(where(TenantOwnedDocument.TENANT_FIELD).is("acme"));
        Query widen = Query.query(new Criteria().orOperator(
                where(TenantOwnedDocument.TENANT_FIELD).exists(true), where("name").is("Consulting")));
        assertThat(operations.find(redirect, IsolationProbe.class)).isEmpty();
        assertThat(operations.find(widen, IsolationProbe.class)).isEmpty();
        assertThat(operations.count(widen, IsolationProbe.class)).isZero();

        actAs("acme");
        assertThat(operations.find(widen, IsolationProbe.class)).hasSize(1);
    }

    @Test
    void refusesToWorkWithoutAnAuthenticatedTenant() {
        assertThatThrownBy(() -> operations.findAll(IsolationProbe.class)).isInstanceOf(MissingTenantException.class);
        assertThatThrownBy(() -> operations.save(new IsolationProbe("p-1", "Consulting", Money.inr("1.00"))))
                .isInstanceOf(MissingTenantException.class);
    }

    @Test
    void storesMoneyAsExactDecimal128() {
        actAs("acme");
        operations.save(new IsolationProbe("p-1", "Consulting", Money.inr("1234.56")));

        Document price = rawMongo.getCollection("isolation_probes").find().first().get("price", Document.class);
        assertThat(price.get("amount")).isEqualTo(Decimal128.parse("1234.56"));
        assertThat(price.getString("currency")).isEqualTo("INR");
        assertThat(operations.findById("p-1", IsolationProbe.class)).get()
                .extracting(IsolationProbe::price).isEqualTo(Money.inr("1234.56"));
    }

    private static void actAs(String tenant) {
        SecurityContextHolder.getContext().setAuthentication(TestJwt.authentication("user-" + tenant, tenant));
    }
}
