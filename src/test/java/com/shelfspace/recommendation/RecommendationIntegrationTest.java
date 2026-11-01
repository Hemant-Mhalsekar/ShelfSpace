package com.shelfspace.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.shelfspace.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Each test uses its own random category so other tests' loans can't affect the ranking. */
class RecommendationIntegrationTest extends IntegrationTest {

    @Test
    void readersAlsoBorrowedIsRankedBySharedReaders() throws Exception {
        String cat = uniqueCategory();
        long a = newBook(3, cat);
        long b = newBook(3, cat);
        long c = newBook(3, cat);

        // Two readers of A also read B; one reader of A also read C
        readBoth(a, b);
        readBoth(a, b);
        readBoth(a, c);

        List<Long> ids = bookIds(recommendationsFor(a));
        assertThat(ids).startsWith(b, c).doesNotContain(a);
        assertThat(recommendationsFor(a).get(0).get("reason").asText()).contains("also borrowed");
    }

    @Test
    void newBookFallsBackToItsCategory() throws Exception {
        String cat = uniqueCategory();
        long popular = newBook(1, cat);
        long fresh = newBook(1, cat);
        returnLoan(issueLoan(newMember(), popular));

        JsonNode first = recommendationsFor(fresh).get(0);
        assertThat(first.get("book").get("id").asLong()).isEqualTo(popular);
        assertThat(first.get("reason").asText()).isEqualTo("Popular in " + cat);
    }

    @Test
    void deletedBooksAreNeverRecommended() throws Exception {
        String cat = uniqueCategory();
        long a = newBook(2, cat);
        long gone = newBook(2, cat);
        readBoth(a, gone);
        mockMvc.perform(delete("/api/v1/books/{id}", gone).header("Authorization", adminToken()))
                .andExpect(status().isNoContent());

        assertThat(bookIds(recommendationsFor(a))).doesNotContain(gone);
    }

    @Test
    void personalSuggestionsSkipBooksYouAlreadyHad() throws Exception {
        String cat = uniqueCategory();
        long a = newBook(3, cat);
        long b = newBook(3, cat);
        readBoth(a, b);   // someone else read A and B

        TestMember me = newMember();
        returnLoan(issueLoan(me, a));

        List<JsonNode> mine = new ArrayList<>();
        json.readTree(mockMvc.perform(get("/api/v1/recommendations/me").header("Authorization", me.token()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).forEach(mine::add);

        assertThat(bookIds(mine)).contains(b).doesNotContain(a);
        assertThat(bookIds(mine).get(0)).isEqualTo(b);
    }

    @Test
    void newMemberStillGetsSuggestions() throws Exception {
        newBook(1);   // make sure the library isn't empty
        mockMvc.perform(get("/api/v1/recommendations/me").header("Authorization", newMember().token()))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(json.readTree(result.getResponse().getContentAsString()).size())
                        .isGreaterThan(0));
    }

    // ---------- helpers ----------

    /** A fresh member borrows and returns both books. */
    private void readBoth(long first, long second) throws Exception {
        TestMember m = newMember();
        returnLoan(issueLoan(m, first));
        returnLoan(issueLoan(m, second));
    }

    private List<JsonNode> recommendationsFor(long bookId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/books/{id}/recommendations", bookId)
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<JsonNode> out = new ArrayList<>();
        json.readTree(body).forEach(out::add);
        return out;
    }

    private static List<Long> bookIds(List<JsonNode> recs) {
        return recs.stream().map(r -> r.get("book").get("id").asLong()).toList();
    }

    private static String uniqueCategory() {
        return "Cat-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
