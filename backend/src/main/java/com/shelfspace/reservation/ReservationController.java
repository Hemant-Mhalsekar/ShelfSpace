package com.shelfspace.reservation;

import com.shelfspace.auth.AuthUser;
import com.shelfspace.reservation.dto.ReservationRequest;
import com.shelfspace.reservation.dto.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Reservations", description = "Join the waiting list for a book with no free copies")
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "Reserve a book that has no copies available right now")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@Valid @RequestBody ReservationRequest request,
                                       @AuthenticationPrincipal AuthUser user) {
        return reservationService.reserve(user.id(), request.bookId());
    }

    @Operation(summary = "My reservations, with queue position and estimated date")
    @GetMapping("/me")
    public List<ReservationResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return reservationService.mine(user.id());
    }

    @Operation(summary = "One reservation (your own, or any for admins)")
    @GetMapping("/{id}")
    public ReservationResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return reservationService.get(id, user);
    }

    @Operation(summary = "Leave the queue, or give up a held copy")
    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return reservationService.cancel(id, user);
    }
}
