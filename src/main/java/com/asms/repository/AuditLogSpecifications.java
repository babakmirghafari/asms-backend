package com.asms.repository;

import com.asms.domain.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
     *
     * <p>Avoids {@code Specification.where(null)} entirely to sidestep the
     * ambiguous overload between {@code Specification.where(Specification)}
     * and {@code Specification.where(PredicateSpecification)} in Spring Data 3.x.
     * Instead, non-null specs are collected into a list and reduced with .and().
     * If all specs are null, returns a match-all (no predicate added).
     */
    @SuppressWarnings("unchecked")
    public static Specification<AuditLog> combineAll(Specification<AuditLog>... specs) {
        List<Specification<AuditLog>> nonNull = Arrays.stream(specs)
                .filter(Objects::nonNull)
                .toList();
        if (nonNull.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        Specification<AuditLog> result = nonNull.get(0);
        for (int i = 1; i < nonNull.size(); i++) {
            result = result.and(nonNull.get(i));
        }
        return result;
    }
}
