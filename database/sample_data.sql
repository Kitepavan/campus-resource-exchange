-- Development sample data.
-- Passwords (documented in README, development only):
--   admin@campus.test  -> Admin@12345
--   all students       -> Student@123
-- Run after schema.sql on a fresh database:
--   mysql -u campus_app -p campus_exchange < database/sample_data.sql

INSERT INTO users (user_id, full_name, email, password_hash, student_id, department,
                   year_of_study, role, verification_status, is_active)
VALUES
    (1, 'Campus Administrator', 'admin@campus.test',
     '$2a$12$8BDBE/Rqkqu/jxEO9eUZtO1.52cSkXrFLHCw1lotPbwHsqvHmdu0S',
     NULL, NULL, NULL, 'ADMIN', 'VERIFIED', TRUE),
    (2, 'Alice Fernando', 'alice@campus.test',
     '$2a$12$zYQ/Uvb.V/vUFvyHJvgDWen3Cdiyi8E/CA6w/1RjYXam88vxmz0Fm',
     'STU-2024-001', 'Computer Science', 3, 'STUDENT', 'VERIFIED', TRUE),
    (3, 'Bob Ramirez', 'bob@campus.test',
     '$2a$12$DobIeLBJAv47deGfAG4BEOEYlNz/CO1fW9PRN1T6P614ZTjQrjcA6',
     'STU-2024-002', 'Engineering', 2, 'STUDENT', 'VERIFIED', TRUE),
    (4, 'Carol Mendes', 'carol@campus.test',
     '$2a$12$2eFMDVerTg2JSfc0GZeDJ.WkdFnPTarrfP5jNlYQ.5dRRPfYVwFEC',
     'STU-2024-003', 'Mathematics', 1, 'STUDENT', 'PENDING', TRUE);

INSERT INTO resources (resource_id, owner_id, title, description, category,
                       resource_type, `condition`, exchange_type, price,
                       lending_terms, course_code, pickup_location, image_path, status)
VALUES
    (1, 2, 'Introduction to Algorithms',
     'Third edition. Clean pages, a few pencil notes in the first three chapters.',
     'Computer Science', 'BOOK', 'GOOD', 'SELL', 45.00, NULL, 'CS301',
     'Central Library', NULL, 'AVAILABLE'),
    (2, 3, 'Clean Code',
     'Like-new hardcover. Available for the semester; please return it in the same condition.',
     'Computer Science', 'BOOK', 'LIKE_NEW', 'LEND', NULL,
     '30 days. Return to the Engineering Hall front desk.', 'CS201',
     'Engineering Hall', NULL, 'AVAILABLE'),
    (3, 2, 'Arduino Uno Starter Kit',
     'Uno board, breadboard, jumper wires, LEDs, resistors, and a USB cable. All parts included.',
     'Electronics', 'LAB_EQUIPMENT', 'GOOD', 'LEND', NULL,
     '7 days. Return all cables and components in the box.', 'EC201',
     'Electronics Lab', NULL, 'AVAILABLE'),
    (4, 3, 'Calculus II Complete Notes',
     'Full semester notes with worked examples and past quiz solutions.',
     'Mathematics', 'STUDY_MATERIAL', 'GOOD', 'DONATE', NULL, NULL, 'MA201',
     'Science Block', NULL, 'AVAILABLE'),
    (5, 2, 'Database System Concepts',
     'Seventh edition, new condition. No highlighting or writing.',
     'Computer Science', 'BOOK', 'NEW', 'SELL', 60.00, NULL, 'CS305',
     'Central Library', NULL, 'AVAILABLE'),
    (6, 3, 'Physics Lab Manual',
     'Lab manual with the first two experiments completed in pencil. Everything else is blank.',
     'Physics', 'STUDY_MATERIAL', 'FAIR', 'LEND', NULL,
     '14 days. Return to the Physics Lab.', 'PH101',
     'Physics Lab', NULL, 'AVAILABLE');

INSERT INTO resource_requests (request_id, resource_id, requester_id, message, status)
VALUES (1, 1, 3,
        'Hi Alice, I am taking CS301 this semester. Is the book still available?',
        'PENDING');
