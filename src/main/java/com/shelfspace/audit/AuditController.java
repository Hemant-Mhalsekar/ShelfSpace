package com.shelfspace.audit;

import com.shelfspace.audit.dto.AuditEntryResponse;
import com.shelfspace.audit.dto.AuditFilter;
import com.shelfspace.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "Audit log", description = "Who did what, for librarians")
@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {

    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditQueryService auditQueryService;

    @Operation(summary = "Search the audit log, newest first")
    @GetMapping
    public PageResponse<AuditEntryResponse> search(
            @RequestParam(required = false) AuditCategory category,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return auditQueryService.search(new AuditFilter(category, action, actorId, entityType, entityId, from, to), page, size);
    }

    @Operation(summary = "Every action name that has been recorded, for the filter menu")
    @GetMapping("/actions")
    public List<String> actions() {
        return auditQueryService.actions();
    }

    @Operation(summary = "Download the filtered log as CSV (newest " + AuditQueryService.EXPORT_LIMIT + " rows)")
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) AuditCategory category,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<AuditEntryResponse> rows = auditQueryService.export(new AuditFilter(category, action, actorId, entityType, entityId, from, to));

        StringBuilder csv = new StringBuilder("time,who,action,type,id,about,details\n");
        for (AuditEntryResponse r : rows) {
            csv.append(String.join(",",
                    cell(r.createdAt() == null ? "" : r.createdAt().format(CSV_TIME)),
                    cell(r.actorName() != null ? r.actorName() : r.actorId() == null ? "System" : "User #" + r.actorId()),
                    cell(r.action()),
                    cell(r.entityType()),
                    cell(r.entityId() == null ? "" : r.entityId().toString()),
                    cell(r.entityLabel()),
                    cell(r.details()))).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shelfspace-audit-" + LocalDate.now() + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Quotes a CSV cell. Cells starting with = + - @ get a leading apostrophe so a spreadsheet
     * doesn't run them as formulas (details can contain text a member typed, like a rejection reason).
     */
    static String cell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String v = "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        return '"' + v.replace("\"", "\"\"") + '"';
    }
}
