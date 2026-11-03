package com.shelfspace.recommendation.dto;

import com.shelfspace.catalog.dto.BookResponse;

/** A suggested book and a short, human-readable reason for suggesting it. */
public record RecommendationResponse(BookResponse book, String reason) {
}
