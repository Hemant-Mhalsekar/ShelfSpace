package com.shelfspace.wishlist.dto;

import com.shelfspace.catalog.dto.BookResponse;

import java.time.LocalDateTime;

public record WishlistItemResponse(BookResponse book, LocalDateTime addedAt) {
}
