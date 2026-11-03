package com.shelfspace.dashboard;

import com.shelfspace.auth.AuthUser;
import com.shelfspace.dashboard.dto.AdminDashboardResponse;
import com.shelfspace.dashboard.dto.MemberDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboards", description = "Summary numbers for admins and members")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Library-wide stats: inventory, circulation, fines, top books, monthly borrowing (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public AdminDashboardResponse admin() {
        return dashboardService.admin();
    }

    @Operation(summary = "My dashboard: loans, due dates, fines, reservations, alerts")
    @GetMapping("/me")
    public MemberDashboardResponse me(@AuthenticationPrincipal AuthUser user) {
        return dashboardService.member(user.id());
    }
}
