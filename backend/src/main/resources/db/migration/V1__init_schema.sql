-- ShelfSpace initial schema
-- Books are titles; book_copies are the physical items that get borrowed.

CREATE TABLE borrowing_policies (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    membership_tier   VARCHAR(20)    NOT NULL UNIQUE,
    max_books         INT            NOT NULL,
    loan_days         INT            NOT NULL,
    max_renewals      INT            NOT NULL,
    fine_per_day      DECIMAL(10, 2) NOT NULL,
    fine_cap          DECIMAL(10, 2) NOT NULL,
    max_unpaid_fines  DECIMAL(10, 2) NOT NULL,
    created_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6)    NOT NULL
);

CREATE TABLE users (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    email            VARCHAR(150) NOT NULL UNIQUE,
    phone            VARCHAR(20),
    password_hash    VARCHAR(255) NOT NULL,
    role             VARCHAR(20)  NOT NULL,
    membership_tier  VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL
);

CREATE TABLE books (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    author            VARCHAR(255) NOT NULL,
    isbn              VARCHAR(20)  NOT NULL UNIQUE,
    category          VARCHAR(100) NOT NULL,
    publication_year  INT,
    replacement_cost  DECIMAL(10, 2),
    deleted_at        DATETIME(6),
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    FULLTEXT INDEX ft_books_title_author (title, author),
    INDEX idx_books_category (category)
);

CREATE TABLE book_copies (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id     BIGINT      NOT NULL,
    barcode     VARCHAR(50) NOT NULL UNIQUE,
    status      VARCHAR(20) NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,   -- optimistic locking
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    CONSTRAINT fk_copies_book FOREIGN KEY (book_id) REFERENCES books (id),
    INDEX idx_copies_book_status (book_id, status)
);

CREATE TABLE loans (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id    BIGINT      NOT NULL,
    copy_id      BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    borrowed_at  DATETIME(6),
    due_date     DATE,
    returned_at  DATETIME(6),
    renew_count  INT         NOT NULL DEFAULT 0,
    approved_by  BIGINT,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    CONSTRAINT fk_loans_member   FOREIGN KEY (member_id)   REFERENCES users (id),
    CONSTRAINT fk_loans_copy     FOREIGN KEY (copy_id)     REFERENCES book_copies (id),
    CONSTRAINT fk_loans_approver FOREIGN KEY (approved_by) REFERENCES users (id),
    INDEX idx_loans_member_status (member_id, status),
    INDEX idx_loans_status_due (status, due_date)
);

CREATE TABLE reservations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT      NOT NULL,
    book_id     BIGINT      NOT NULL,
    status      VARCHAR(20) NOT NULL,
    held_copy_id BIGINT,
    expires_at  DATETIME(6),
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    CONSTRAINT fk_res_member FOREIGN KEY (member_id)    REFERENCES users (id),
    CONSTRAINT fk_res_book   FOREIGN KEY (book_id)      REFERENCES books (id),
    CONSTRAINT fk_res_copy   FOREIGN KEY (held_copy_id) REFERENCES book_copies (id),
    INDEX idx_res_book_status_created (book_id, status, created_at)
);

CREATE TABLE fines (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id     BIGINT         NOT NULL,
    amount      DECIMAL(10, 2) NOT NULL,
    reason      VARCHAR(20)    NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    paid_at     DATETIME(6),
    waived_by   BIGINT,
    note        VARCHAR(255),
    created_at  DATETIME(6)    NOT NULL,
    updated_at  DATETIME(6)    NOT NULL,
    CONSTRAINT fk_fines_loan   FOREIGN KEY (loan_id)   REFERENCES loans (id),
    CONSTRAINT fk_fines_waiver FOREIGN KEY (waived_by) REFERENCES users (id),
    INDEX idx_fines_status (status)
);

CREATE TABLE audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id    BIGINT,
    action      VARCHAR(50)  NOT NULL,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   BIGINT,
    details     VARCHAR(500),
    created_at  DATETIME(6)  NOT NULL,
    INDEX idx_audit_entity (entity_type, entity_id)
);
