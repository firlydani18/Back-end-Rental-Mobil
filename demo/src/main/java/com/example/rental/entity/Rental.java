package com.example.rental.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "rentals", indexes = {
    @Index(name = "idx_rental_car_dates_status", columnList = "car_id,status,start_date,end_date"),
    @Index(name = "idx_rental_status_created", columnList = "status,created_at")
}, uniqueConstraints = @UniqueConstraint(name = "uk_rental_idempotency_key", columnNames = "idempotency_key"))
public class Rental {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false) private Car car;
    @Column(name = "customer_name", nullable = false, length = 120) private String customerName;
    @Column(name = "customer_email", nullable = false, length = 180) private String customerEmail;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Column(name = "total_price", nullable = false, precision = 14, scale = 2) private BigDecimal totalPrice;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RentalStatus status = RentalStatus.BOOKED;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected Rental() {}

    public Rental(Car car, String customerName, String customerEmail, LocalDate startDate,
                  LocalDate endDate, BigDecimal totalPrice, String idempotencyKey) {
        this.car = car;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalPrice = totalPrice;
        this.idempotencyKey = idempotencyKey;
        this.status = RentalStatus.BOOKED;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Car getCar() { return car; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public RentalStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setStatus(RentalStatus status) { this.status = status; this.updatedAt = Instant.now(); }
}
