-- Week 3: reservation limits and hold length per tier, plus in-app notifications.

ALTER TABLE borrowing_policies
    ADD COLUMN max_reservations INT NOT NULL DEFAULT 3,
    ADD COLUMN hold_hours       INT NOT NULL DEFAULT 48;

CREATE TABLE notifications (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    type           VARCHAR(30)  NOT NULL,
    title          VARCHAR(150) NOT NULL,
    message        VARCHAR(500) NOT NULL,
    reference_type VARCHAR(30),
    reference_id   BIGINT,
    read_at        DATETIME(6),
    created_at     DATETIME(6)  NOT NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_notifications_user_read (user_id, read_at),
    INDEX idx_notifications_dedupe (user_id, type, reference_id, created_at)
);
