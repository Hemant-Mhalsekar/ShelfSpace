package com.shelfspace.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.shelfspace.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogIntegrationTest extends IntegrationTest {

    @Autowired
    private BookCopyRepository copyRepository;

    private String admin;
    private String member;

    @BeforeEach
    void login() throws Exception {
        admin = adminToken();
        member = memberToken();
    }

    @Test
    void adminAddsBookAndMemberFindsIt() throws Exception {
        String title = uniqueTitle();
        createBook(title, randomIsbn13(), 3);

        mockMvc.perform(get("/api/v1/books").param("q", title).header("Authorization", member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value(title))
                .andExpect(jsonPath("$.content[0].totalCopies").value(3))
                .andExpect(jsonPath("$.content[0].availableCopies").value(3));
    }

    @Test
    void memberCannotAddBooks() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson(uniqueTitle(), randomIsbn13(), 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void catalogRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/v1/books")).andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateIsbnIsRejectedEvenWithHyphens() throws Exception {
        String isbn = randomIsbn13();
        createBook(uniqueTitle(), isbn, 0);

        String hyphenated = isbn.substring(0, 3) + "-" + isbn.substring(3);
        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson(uniqueTitle(), hyphenated, 0)))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidIsbnFailsValidation() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson(uniqueTitle(), "9780132350885", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.isbn").exists());
    }

    @Test
    void addingCopiesContinuesBarcodeNumbering() throws Exception {
        long bookId = createBook(uniqueTitle(), randomIsbn13(), 1);

        mockMvc.perform(post("/api/v1/books/{id}/copies", bookId)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\": 2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].barcode", endsWith("-002")))
                .andExpect(jsonPath("$[1].barcode", endsWith("-003")));

        mockMvc.perform(get("/api/v1/books/{id}", bookId).header("Authorization", member))
                .andExpect(jsonPath("$.totalCopies").value(3));
    }

    @Test
    void cannotDeleteBookWhileCopyIsBorrowed() throws Exception {
        long bookId = createBook(uniqueTitle(), randomIsbn13(), 1);
        BookCopy copy = copyRepository.findByBookIdOrderByIdAsc(bookId).get(0);
        copy.setStatus(CopyStatus.BORROWED);
        copyRepository.save(copy);

        mockMvc.perform(delete("/api/v1/books/{id}", bookId).header("Authorization", admin))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void softDeletedBookDisappearsFromCatalog() throws Exception {
        String title = uniqueTitle();
        long bookId = createBook(title, randomIsbn13(), 1);

        mockMvc.perform(delete("/api/v1/books/{id}", bookId).header("Authorization", admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/books/{id}", bookId).header("Authorization", member))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/books").param("q", title).header("Authorization", member))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void damagedCopiesAreNotCountedAsAvailable() throws Exception {
        String title = uniqueTitle();
        long bookId = createBook(title, randomIsbn13(), 1);
        long copyId = copyRepository.findByBookIdOrderByIdAsc(bookId).get(0).getId();

        mockMvc.perform(patch("/api/v1/copies/{id}/status", copyId)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DAMAGED\", \"note\": \"water damage\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DAMAGED"));

        mockMvc.perform(get("/api/v1/books").param("q", title).param("availableOnly", "true")
                        .header("Authorization", member))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/books").param("q", title).header("Authorization", member))
                .andExpect(jsonPath("$.content[0].availableCopies").value(0));
    }

    @Test
    void adminCannotSetBorrowedByHand() throws Exception {
        long bookId = createBook(uniqueTitle(), randomIsbn13(), 1);
        long copyId = copyRepository.findByBookIdOrderByIdAsc(bookId).get(0).getId();

        mockMvc.perform(patch("/api/v1/copies/{id}/status", copyId)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"BORROWED\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unknownSortFieldIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("sort", "passwordHash").header("Authorization", member))
                .andExpect(status().isBadRequest());
    }

    // ---------- helpers ----------

    private long createBook(String title, String isbn, int copies) throws Exception {
        String body = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson(title, isbn, copies)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        assertThat(node.get("totalCopies").asInt()).isEqualTo(copies);
        return node.get("id").asLong();
    }

    private String bookJson(String title, String isbn, int copies) throws Exception {
        Map<String, Object> book = new LinkedHashMap<>();
        book.put("title", title);
        book.put("author", "Robert C. Martin");
        book.put("isbn", isbn);
        book.put("category", "Programming");
        book.put("publicationYear", 2008);
        book.put("replacementCost", 599.00);
        book.put("initialCopies", copies);
        return json.writeValueAsString(book);
    }

    private static String uniqueTitle() {
        return "Test Book " + UUID.randomUUID().toString().substring(0, 8);
    }
}
