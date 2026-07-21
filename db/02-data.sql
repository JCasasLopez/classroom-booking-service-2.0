-- Clean up tables to ensure predictable IDs starting from 1
TRUNCATE TABLE watch_alerts;
TRUNCATE TABLE bookings;

-- Insert bookings with dynamic dates relative to execution time
-- Booking 1: ACTIVE (for 7 days from now)
INSERT INTO bookings (idUser, idClassroom, start, finish, status) 
VALUES (1, 1, DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY), 'ACTIVE');

-- Booking 2: COMPLETED (Always in the past)
INSERT INTO bookings (idUser, idClassroom, start, finish, status) 
VALUES (1, 1, '2026-01-01 10:00:00', '2026-01-01 12:00:00', 'COMPLETED');

-- Booking 3: CANCELLED (Always in the past)
INSERT INTO bookings (idUser, idClassroom, start, finish, status) 
VALUES (1, 1, '2026-02-01 10:00:00', '2026-02-01 12:00:00', 'CANCELLED');

-- Insert watch alerts using the same user email for all
INSERT INTO watch_alerts (idBooking, userEmail) VALUES (1, 'user@example.com');
INSERT INTO watch_alerts (idBooking, userEmail) VALUES (2, 'user@example.com');
INSERT INTO watch_alerts (idBooking, userEmail) VALUES (3, 'user@example.com');