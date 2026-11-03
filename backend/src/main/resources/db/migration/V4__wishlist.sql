-- Members save books for later; each book at most once per member.
CREATE TABLE wishlist_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id  BIGINT      NOT NULL,
    book_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_wishlist_member FOREIGN KEY (member_id) REFERENCES users (id),
    CONSTRAINT fk_wishlist_book   FOREIGN KEY (book_id)   REFERENCES books (id),
    CONSTRAINT uq_wishlist_member_book UNIQUE (member_id, book_id),
    INDEX idx_wishlist_book (book_id)
);
