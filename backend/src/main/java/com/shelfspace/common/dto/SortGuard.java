package com.shelfspace.common.dto;

import com.shelfspace.common.exception.InvalidRequestException;
import org.springframework.data.domain.Pageable;

import java.util.Set;

/** Rejects ?sort= values outside an allowed list, so they become a 400 instead of a query error. */
public final class SortGuard {

    private SortGuard() {
    }

    public static void check(Pageable pageable, Set<String> allowed) {
        pageable.getSort().forEach(order -> {
            if (!allowed.contains(order.getProperty())) {
                throw new InvalidRequestException(
                        "Cannot sort by '" + order.getProperty() + "'. Allowed: " + allowed);
            }
        });
    }
}
