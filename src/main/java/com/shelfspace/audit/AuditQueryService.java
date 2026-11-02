package com.shelfspace.audit;

import com.shelfspace.audit.dto.AuditEntryResponse;
import com.shelfspace.audit.dto.AuditFilter;
import com.shelfspace.common.dto.PageResponse;
import com.shelfspace.common.exception.InvalidRequestException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Read side of the audit log, for librarians. */
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    /** A CSV export holds at most this many rows (newest first). */
    public static final int EXPORT_LIMIT = 10_000;

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final AuditLogRepository repository;
    private final AuditLabels labels;

    @Transactional(readOnly = true)
    public PageResponse<AuditEntryResponse> search(AuditFilter filter, int page, int size) {
        Page<AuditLog> logs = repository.findAll(spec(filter),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), NEWEST_FIRST));
        return PageResponse.from(logs.map(toResponse(logs.getContent())::convert));
    }

    @Transactional(readOnly = true)
    public List<AuditEntryResponse> export(AuditFilter filter) {
        List<AuditLog> logs = repository.findAll(spec(filter), PageRequest.of(0, EXPORT_LIMIT, NEWEST_FIRST)).getContent();
        Converter c = toResponse(logs);
        return logs.stream().map(c::convert).toList();
    }

    @Transactional(readOnly = true)
    public List<String> actions() {
        return repository.findDistinctActions();
    }

    // ---------- helpers ----------

    private interface Converter {
        AuditEntryResponse convert(AuditLog log);
    }

    /** Resolves names for the whole batch up front, then maps each row. */
    private Converter toResponse(List<AuditLog> logs) {
        Map<Long, String> actors = labels.userNames(logs.stream().map(AuditLog::getActorId).filter(Objects::nonNull).distinct().toList());
        Map<String, Map<Long, AuditLabels.Label>> entities = labels.forEntities(logs);
        return log -> {
            AuditLabels.Label label = log.getEntityId() == null ? null
                    : entities.getOrDefault(log.getEntityType(), Map.of()).get(log.getEntityId());
            return new AuditEntryResponse(log.getId(), log.getCreatedAt(), log.getAction(),
                    log.getActorId(), log.getActorId() == null ? null : actors.get(log.getActorId()),
                    log.getEntityType(), log.getEntityId(),
                    label == null ? null : label.text(),
                    label == null ? null : label.bookId(),
                    label == null ? null : label.memberId(),
                    log.getDetails());
        };
    }

    private static Specification<AuditLog> spec(AuditFilter f) {
        if (f.from() != null && f.to() != null && f.from().isAfter(f.to())) {
            throw new InvalidRequestException("'from' must be on or before 'to'");
        }
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (f.category() != null) {
                where.add(root.get("entityType").in(f.category().entityTypes()));
            }
            if (f.action() != null && !f.action().isBlank()) {
                where.add(cb.equal(root.get("action"), f.action().trim()));
            }
            if (f.actorId() != null) {
                where.add(cb.equal(root.get("actorId"), f.actorId()));
            }
            if (f.entityType() != null && !f.entityType().isBlank()) {
                where.add(cb.equal(root.get("entityType"), f.entityType().trim()));
            }
            if (f.entityId() != null) {
                where.add(cb.equal(root.get("entityId"), f.entityId()));
            }
            if (f.from() != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.from().atStartOfDay()));
            }
            if (f.to() != null) {
                where.add(cb.lessThan(root.get("createdAt"), f.to().plusDays(1).atStartOfDay()));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }
}
