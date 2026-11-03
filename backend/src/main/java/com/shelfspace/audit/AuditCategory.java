package com.shelfspace.audit;

import java.util.Set;

/** Groups the entity types into the filters a librarian thinks in. */
public enum AuditCategory {
    BOOKS(Set.of("Book", "BookCopy")),
    LOANS(Set.of("Loan")),
    FINES(Set.of("Fine")),
    MEMBERS(Set.of("User")),
    RULES(Set.of("BorrowingPolicy"));

    private final Set<String> entityTypes;

    AuditCategory(Set<String> entityTypes) {
        this.entityTypes = entityTypes;
    }

    public Set<String> entityTypes() {
        return entityTypes;
    }
}
