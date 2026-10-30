-- Default borrowing rules per membership tier. Change rules here, not in code.
INSERT INTO borrowing_policies
    (membership_tier, max_books, loan_days, max_renewals, fine_per_day, fine_cap, max_unpaid_fines, created_at, updated_at)
VALUES
    ('STUDENT', 5,  14, 2, 10.00, 500.00,  100.00, NOW(6), NOW(6)),
    ('FACULTY', 10, 30, 3, 5.00,  500.00,  200.00, NOW(6), NOW(6)),
    ('GUEST',   2,  7,  0, 20.00, 500.00,  50.00,  NOW(6), NOW(6));
