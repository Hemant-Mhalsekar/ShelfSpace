package com.shelfspace.audit;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Looks up readable names for a page of audit rows: one query per entity type, never one per row.
 *
 * Uses plain SQL on purpose: Book has a soft-delete filter, and a deleted book should still show its title here.
 */
@Component
@RequiredArgsConstructor
class AuditLabels {

    /** Name to show, plus ids the UI can link to. */
    record Label(String text, Long bookId, Long memberId) {
    }

    private final EntityManager em;

    Map<Long, String> userNames(Collection<Long> ids) {
        Map<Long, String> names = new HashMap<>();
        for (Object[] r : rows("SELECT id, name FROM users WHERE id IN (:ids)", ids)) {
            names.put(id(r[0]), (String) r[1]);
        }
        return names;
    }

    /** entityType -> (entityId -> label) */
    Map<String, Map<Long, Label>> forEntities(List<AuditLog> logs) {
        Map<String, Set<Long>> idsByType = logs.stream()
                .filter(l -> l.getEntityId() != null)
                .collect(Collectors.groupingBy(AuditLog::getEntityType,
                        Collectors.mapping(AuditLog::getEntityId, Collectors.toSet())));

        Map<String, Map<Long, Label>> out = new HashMap<>();
        idsByType.forEach((type, ids) -> out.put(type, switch (type) {
            case "Book" -> collect("SELECT id, title, id, NULL FROM books WHERE id IN (:ids)", ids, false);
            case "User" -> collect("SELECT id, name, NULL, id FROM users WHERE id IN (:ids)", ids, false);
            case "BookCopy" -> collect("""
                    SELECT c.id, CONCAT(c.barcode, ' · ', b.title), b.id, NULL
                    FROM book_copies c JOIN books b ON b.id = c.book_id WHERE c.id IN (:ids)""", ids, false);
            case "Loan" -> collect("""
                    SELECT l.id, CONCAT(b.title, ' · ', u.name), b.id, u.id
                    FROM loans l JOIN book_copies c ON c.id = l.copy_id JOIN books b ON b.id = c.book_id
                    JOIN users u ON u.id = l.member_id WHERE l.id IN (:ids)""", ids, false);
            case "Fine" -> collect("""
                    SELECT f.id, CONCAT(b.title, ' · ', u.name), b.id, u.id
                    FROM fines f JOIN loans l ON l.id = f.loan_id JOIN book_copies c ON c.id = l.copy_id
                    JOIN books b ON b.id = c.book_id JOIN users u ON u.id = l.member_id WHERE f.id IN (:ids)""", ids, false);
            case "BorrowingPolicy" -> collect("SELECT id, membership_tier, NULL, NULL FROM borrowing_policies WHERE id IN (:ids)", ids, true);
            default -> Map.of();
        }));
        return out;
    }

    private Map<Long, Label> collect(String sql, Collection<Long> ids, boolean tierName) {
        Map<Long, Label> labels = new HashMap<>();
        for (Object[] r : rows(sql, ids)) {
            String text = (String) r[1];
            if (tierName && text != null) {
                text = text.charAt(0) + text.substring(1).toLowerCase() + " tier";
            }
            labels.put(id(r[0]), new Label(text, r[2] == null ? null : id(r[2]), r[3] == null ? null : id(r[3])));
        }
        return labels;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return em.createNativeQuery(sql).setParameter("ids", ids).getResultList();
    }

    private static Long id(Object value) {
        return ((Number) value).longValue();
    }
}
