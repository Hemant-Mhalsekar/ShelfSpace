package com.shelfspace.wishlist;

import com.shelfspace.auth.AuthUser;
import com.shelfspace.wishlist.dto.WishlistItemResponse;
import com.shelfspace.wishlist.dto.WishlistRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Wishlist", description = "Books saved for later")
@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @Operation(summary = "My wishlist, newest first, with live availability")
    @GetMapping
    public List<WishlistItemResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return wishlistService.forMember(user.id());
    }

    @Operation(summary = "Save a book to my wishlist")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WishlistItemResponse add(@Valid @RequestBody WishlistRequest request, @AuthenticationPrincipal AuthUser user) {
        return wishlistService.add(user.id(), request.bookId());
    }

    @Operation(summary = "Remove a book from my wishlist")
    @DeleteMapping("/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long bookId, @AuthenticationPrincipal AuthUser user) {
        wishlistService.remove(user.id(), bookId);
    }
}
