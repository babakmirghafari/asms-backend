package com.asms.repository;

import com.asms.domain.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA Specifications for AuditLog dynamic queries.
 *
 * <p>Used instead of JPQL with nullable parameters to avoid the PostgreSQL
 * "could not determine data type of parameter" error that occurs when Hibernate
 * generates {@code (? is null or col = ?)} for null-typed bound parameters.
 * With Specifications, null predicates are simply omitted — no null literal
 * is sent to the database.
 */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {}

    public static Specification<AuditLog> belongsToOrg(UUID orgId) {
        return (root, query, cb) -> cb.equal(root.get("orgId"), orgId);
    }

    public static Specification<AuditLog> actorIdEquals(UUID actorId) {
        if (actorId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("actorId"), actorId);
    }

    public static Specification<AuditLog> actionEquals(String action) {
        if (action == null) return null;
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> createdAtAfter(OffsetDateTime from) {
        if (from == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<AuditLog> createdAtBefore(OffsetDateTime to) {
        if (to == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<AuditLog> actionStartsWith(String prefix) {
        if (prefix == null) return null;
        return (root, query, cb) -> cb.like(root.get("action"), prefix + "%");
    }

    public static Specification<AuditLog> targetTypeEquals(String targetType) {
        if (targetType == null) return null;
        return (root, query, cb) -> cb.equal(root.get("targetType"), targetType);
    }

    /**
     * Combines all optional filters with AND. Null specifications are skipped.
     */
    public static Specification<AuditLog> combineAll(Specification<AuditLog>... specs) {
        Specification<AuditLog> result = Specification.where(null);
        for (Specification<AuditLog> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }
}
