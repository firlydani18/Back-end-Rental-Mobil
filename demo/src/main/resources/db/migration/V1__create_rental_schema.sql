CREATE TABLE cars (
  id BIGINT NOT NULL AUTO_INCREMENT,
  brand VARCHAR(80) NOT NULL,
  model VARCHAR(100) NOT NULL,
  plate_number VARCHAR(20) NOT NULL,
  daily_rate DECIMAL(12,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_car_plate_number UNIQUE (plate_number),
  CONSTRAINT chk_car_rate CHECK (daily_rate > 0),
  CONSTRAINT chk_car_status CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'INACTIVE'))
) ENGINE=InnoDB;

CREATE TABLE rentals (
  id BIGINT NOT NULL AUTO_INCREMENT,
  car_id BIGINT NOT NULL,
  customer_name VARCHAR(120) NOT NULL,
  customer_email VARCHAR(180) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  total_price DECIMAL(14,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  idempotency_key VARCHAR(100) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_rental_car FOREIGN KEY (car_id) REFERENCES cars(id),
  CONSTRAINT uk_rental_idempotency_key UNIQUE (idempotency_key),
  CONSTRAINT chk_rental_dates CHECK (end_date >= start_date),
  CONSTRAINT chk_rental_total CHECK (total_price > 0),
  CONSTRAINT chk_rental_status CHECK (status IN ('BOOKED', 'CANCELLED', 'COMPLETED')),
  INDEX idx_rental_car_dates_status (car_id, status, start_date, end_date),
  INDEX idx_rental_status_created (status, created_at)
) ENGINE=InnoDB;
