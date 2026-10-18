-- Dummy data for Rental Mobil Spring Boot project
-- IMPORTANT:
-- 1) Run the Spring Boot application once first so Flyway creates the `cars` and `rentals` tables.
-- 2) Then execute this script in MySQL Workbench or your MySQL client.
-- 3) Safe to re-run: seed cars are upserted and seed rental records are replaced by seed email prefix.

CREATE DATABASE IF NOT EXISTS rental_mobil;
USE rental_mobil;

START TRANSACTION;

-- 12 sample cars across operational statuses. Plate numbers are unique.
INSERT INTO cars (brand, model, plate_number, daily_rate, status, created_at, updated_at)
VALUES
('Toyota', 'Avanza',          'B 1234 XYZ', 350000.00, 'AVAILABLE',  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('Honda', 'Brio',             'B 2345 ABC', 300000.00, 'AVAILABLE',  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('Mitsubishi', 'Xpander',     'B 3456 DEF', 500000.00, 'AVAILABLE',  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('Toyota', 'Innova Zenix',    'B 4567 GHI', 650000.00, 'AVAILABLE',  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('Suzuki', 'Ertiga',          'B 5678 JKL', 400000.00, 'AVAILABLE',  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('Honda', 'HR-V',             'B 6789 MNO', 700000.00, 'AVAILABLE',  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('Daihatsu', 'Terios',        'B 7890 PQR', 450000.00, 'AVAILABLE',  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('Hyundai', 'Stargazer',      'B 8901 STU', 500000.00, 'AVAILABLE',  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('Toyota', 'Calya',           'B 9012 VWX', 275000.00, 'AVAILABLE',  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('BYD', 'Atto 3',             'B 1122 YZA', 850000.00, 'AVAILABLE',  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('Nissan', 'Livina',          'B 2233 BCD', 400000.00, 'MAINTENANCE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('Toyota', 'Fortuner',        'B 3344 CDE', 1100000.00, 'INACTIVE',   CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    brand = VALUES(brand),
    model = VALUES(model),
    daily_rate = VALUES(daily_rate),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(6);

-- Remove previously inserted sample rentals, so this script can be run repeatedly.
DELETE FROM rentals WHERE customer_email LIKE 'dummy.%@example.com';

-- 12 rentals: completed/cancelled history plus future bookings for Postman tests.
-- Rental date ranges for each car do not overlap with other BOOKED sample rentals.
INSERT INTO rentals
    (car_id, customer_name, customer_email, start_date, end_date, total_price, status, created_at, updated_at)
SELECT c.id, d.customer_name, d.customer_email, d.start_date, d.end_date,
       d.total_price, d.status, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM cars c
JOIN (
    SELECT 'B 1234 XYZ' AS plate_number, 'Andi Pratama' AS customer_name,
           'dummy.completed.avanza@example.com' AS customer_email,
           DATE('2026-09-01') AS start_date, DATE('2026-09-03') AS end_date,
           1050000.00 AS total_price, 'COMPLETED' AS status
    UNION ALL SELECT 'B 2345 ABC', 'Siti Rahma', 'dummy.completed.brio@example.com',
           DATE('2026-09-05'), DATE('2026-09-06'), 600000.00, 'COMPLETED'
    UNION ALL SELECT 'B 3456 DEF', 'Bambang Saputra', 'dummy.completed.xpander@example.com',
           DATE('2026-09-10'), DATE('2026-09-12'), 1500000.00, 'COMPLETED'
    UNION ALL SELECT 'B 4567 GHI', 'Dewi Anggraini', 'dummy.cancelled.innova@example.com',
           DATE('2026-10-01'), DATE('2026-10-02'), 1300000.00, 'CANCELLED'
    UNION ALL SELECT 'B 1234 XYZ', 'Rizky Maulana', 'dummy.booked.avanza@example.com',
           DATE('2026-11-01'), DATE('2026-11-03'), 1050000.00, 'BOOKED'
    UNION ALL SELECT 'B 2345 ABC', 'Putri Lestari', 'dummy.booked.brio@example.com',
           DATE('2026-11-05'), DATE('2026-11-06'), 600000.00, 'BOOKED'
    UNION ALL SELECT 'B 3456 DEF', 'Agus Firmansyah', 'dummy.booked.xpander@example.com',
           DATE('2026-11-10'), DATE('2026-11-12'), 1500000.00, 'BOOKED'
    UNION ALL SELECT 'B 6789 MNO', 'Nadia Permata', 'dummy.booked.hrv@example.com',
           DATE('2026-10-25'), DATE('2026-10-26'), 1400000.00, 'BOOKED'
    UNION ALL SELECT 'B 8901 STU', 'Fajar Hidayat', 'dummy.booked.stargazer@example.com',
           DATE('2026-12-01'), DATE('2026-12-04'), 2000000.00, 'BOOKED'
    UNION ALL SELECT 'B 9012 VWX', 'Rina Kurnia', 'dummy.booked.calya@example.com',
           DATE('2026-11-20'), DATE('2026-11-21'), 550000.00, 'BOOKED'
    UNION ALL SELECT 'B 5678 JKL', 'Yusuf Akbar', 'dummy.completed.ertiga@example.com',
           DATE('2026-10-05'), DATE('2026-10-06'), 800000.00, 'COMPLETED'
    UNION ALL SELECT 'B 7890 PQR', 'Maya Safitri', 'dummy.cancelled.terios@example.com',
           DATE('2026-09-15'), DATE('2026-09-17'), 1350000.00, 'CANCELLED'
) d ON d.plate_number = c.plate_number;

COMMIT;

-- Quick verification queries:
-- SELECT status, COUNT(*) AS total FROM cars GROUP BY status;
-- SELECT status, COUNT(*) AS total FROM rentals GROUP BY status;
-- SELECT r.id, c.plate_number, r.customer_name, r.start_date, r.end_date, r.total_price, r.status
-- FROM rentals r JOIN cars c ON c.id = r.car_id ORDER BY r.start_date;
