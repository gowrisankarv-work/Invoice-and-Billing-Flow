package com.invoiceflow.shared.tenancy;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

/**
 * The only gateway business modules use to reach MongoDB.
 *
 * <p>Every read, count and delete is restricted to the current tenant, and every write stamps or
 * verifies the current tenant. An architecture test forbids other code from using
 * {@link MongoOperations} or Spring Data repositories directly.
 */
@Component
public class TenantScopedMongoOperations {

    private final MongoOperations mongo;
    private final TenantContext tenantContext;

    public TenantScopedMongoOperations(MongoOperations mongo, TenantContext tenantContext) {
        this.mongo = mongo;
        this.tenantContext = tenantContext;
    }

    public <T extends TenantOwnedDocument> T save(T document) {
        TenantId tenant = tenantContext.currentTenant();
        if (document.tenant() == null) {
            document.assignTenant(tenant);
        } else if (!document.tenant().equals(tenant)) {
            throw new TenantAccessViolationException("Document belongs to another tenant");
        }
        Object id = document.id();
        if (id != null && mongo.exists(ownedByAnotherTenant(id, tenant), document.getClass())) {
            throw new TenantAccessViolationException("Document id is already used by another tenant");
        }
        return mongo.save(document);
    }

    public <T extends TenantOwnedDocument> Optional<T> findById(Object id, Class<T> type) {
        return Optional.ofNullable(mongo.findOne(scoped(Query.query(where("_id").is(id))), type));
    }

    public <T extends TenantOwnedDocument> List<T> find(Query query, Class<T> type) {
        return mongo.find(scoped(query), type);
    }

    public <T extends TenantOwnedDocument> List<T> findAll(Class<T> type) {
        return mongo.find(scoped(new Query()), type);
    }

    public long count(Query query, Class<? extends TenantOwnedDocument> type) {
        return mongo.count(scoped(query), type);
    }

    public boolean deleteById(Object id, Class<? extends TenantOwnedDocument> type) {
        return mongo.remove(scoped(Query.query(where("_id").is(id))), type).getDeletedCount() > 0;
    }

    /**
     * Returns a copy of {@code query} whose filter is {@code $and: [<caller filter>, {tenantId: <current>}]}.
     * Wrapping rather than merging means nothing in the caller's filter (a tenantId condition, an
     * {@code $or}, ...) can widen or redirect the scope: at worst it matches nothing.
     */
    private Query scoped(Query query) {
        Document tenantFilter = new Document(TenantOwnedDocument.TENANT_FIELD, tenantContext.currentTenant().value());
        Document filter = new Document("$and", List.of(query.getQueryObject(), tenantFilter));
        BasicQuery scoped = new BasicQuery(filter, query.getFieldsObject());
        scoped.setSortObject(query.getSortObject());
        scoped.skip(query.getSkip());
        scoped.limit(query.getLimit());
        query.getCollation().ifPresent(scoped::collation);
        return scoped;
    }

    private static Query ownedByAnotherTenant(Object id, TenantId tenant) {
        return Query.query(where("_id").is(id).and(TenantOwnedDocument.TENANT_FIELD).ne(tenant.value()));
    }
}
