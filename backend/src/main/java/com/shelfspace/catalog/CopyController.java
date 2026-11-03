package com.shelfspace.catalog;

import com.shelfspace.auth.AuthUser;
import com.shelfspace.catalog.dto.CopyResponse;
import com.shelfspace.catalog.dto.CopyStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Copies", description = "Manage individual physical copies (admins)")
@RestController
@RequestMapping("/api/v1/copies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CopyController {

    private final BookService bookService;

    @Operation(summary = "Mark a copy AVAILABLE, DAMAGED or LOST")
    @PatchMapping("/{id}/status")
    public CopyResponse updateStatus(@PathVariable Long id, @Valid @RequestBody CopyStatusRequest request,
                                     @AuthenticationPrincipal AuthUser admin) {
        return bookService.updateCopyStatus(id, request.status(), request.note(), admin.id());
    }
}
