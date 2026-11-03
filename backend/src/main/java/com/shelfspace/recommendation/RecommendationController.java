package com.shelfspace.recommendation;

import com.shelfspace.auth.AuthUser;
import com.shelfspace.recommendation.dto.RecommendationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Recommendations", description = "\"Readers also borrowed\" and personal suggestions")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "Books often borrowed by readers of this book (falls back to popular in its category)")
    @GetMapping("/books/{id}/recommendations")
    public List<RecommendationResponse> forBook(@PathVariable Long id, @RequestParam(defaultValue = "5") int limit) {
        return recommendationService.forBook(id, limit);
    }

    @Operation(summary = "Suggestions for me, based on my borrowing history")
    @GetMapping("/recommendations/me")
    public List<RecommendationResponse> forMe(@AuthenticationPrincipal AuthUser user,
                                              @RequestParam(defaultValue = "6") int limit) {
        return recommendationService.forMember(user.id(), limit);
    }
}
