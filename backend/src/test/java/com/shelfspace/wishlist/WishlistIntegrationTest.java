package com.shelfspace.wishlist;

import com.fasterxml.jackson.databind.JsonNode;
import com.shelfspace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WishlistIntegrationTest extends IntegrationTest {

    @Test
    void addListAndRemove() throws Exception {
        TestMember member = newMember();
        long bookId = newBook(2);

        mockMvc.perform(add(member, bookId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.book.id").value(bookId))
                .andExpect(jsonPath("$.book.availableCopies").value(2));

        mockMvc.perform(get("/api/v1/wishlist").header("Authorization", member.token()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].book.id").value(bookId));

        mockMvc.perform(delete("/api/v1/wishlist/{bookId}", bookId).header("Authorization", member.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/wishlist").header("Authorization", member.token()))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(delete("/api/v1/wishlist/{bookId}", bookId).header("Authorization", member.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void sameBookTwiceIsAConflict() throws Exception {
        TestMember member = newMember();
        long bookId = newBook(1);
        mockMvc.perform(add(member, bookId)).andExpect(status().isCreated());
        mockMvc.perform(add(member, bookId)).andExpect(status().isConflict());
    }

    @Test
    void wishlistersAreToldWhenABookComesBack() throws Exception {
        long bookId = newBook(1);
        long loanId = issueLoan(newMember(), bookId);
        TestMember fan = newMember();
        mockMvc.perform(add(fan, bookId)).andExpect(status().isCreated());

        returnLoan(loanId);

        assertThat(countOfType(fan, "WISHLIST_AVAILABLE")).isEqualTo(1);
    }

    @Test
    void noAlertWhenTheCopyGoesToSomeoneInTheQueue() throws Exception {
        long bookId = newBook(1);
        long loanId = issueLoan(newMember(), bookId);
        TestMember fan = newMember();
        mockMvc.perform(add(fan, bookId)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/reservations")
                        .header("Authorization", newMember().token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": " + bookId + "}"))
                .andExpect(status().isCreated());

        returnLoan(loanId);

        assertThat(countOfType(fan, "WISHLIST_AVAILABLE")).isZero();
    }

    @Test
    void addingCopiesToAnAllOutBookAlertsWishlisters() throws Exception {
        long bookId = newBook(1);
        issueLoan(newMember(), bookId);
        TestMember fan = newMember();
        mockMvc.perform(add(fan, bookId)).andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/books/{id}/copies", bookId)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\": 2}"))
                .andExpect(status().isCreated());

        assertThat(countOfType(fan, "WISHLIST_AVAILABLE")).isEqualTo(1);
    }

    private RequestBuilder add(TestMember member, long bookId) {
        return post("/api/v1/wishlist")
                .header("Authorization", member.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\": " + bookId + "}");
    }

    private long countOfType(TestMember member, String type) throws Exception {
        String body = mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", member.token()))
                .andReturn().getResponse().getContentAsString();
        long n = 0;
        for (JsonNode node : json.readTree(body).get("content")) {
            if (type.equals(node.get("type").asText())) {
                n++;
            }
        }
        return n;
    }
}
