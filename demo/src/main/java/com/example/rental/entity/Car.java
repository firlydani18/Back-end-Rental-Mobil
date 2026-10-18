package com.example.rental.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cars", uniqueConstraints = @UniqueConstraint(name = "uk_car_plate_number", columnNames = "plate_number"))
public class Car {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80) private String brand;
    @Column(nullable = false, length = 100) private String model;
    @Column(name = "plate_number", nullable = false, length = 20) private String plateNumber;
    @Column(name = "daily_rate", nullable = false, precision = 12, scale = 2) private BigDecimal dailyRate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CarStatus status = CarStatus.AVAILABLE;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected Car() {}

    public Car(String brand, String model, String plateNumber, BigDecimal dailyRate) {
        this.brand = brand;
        this.model = model;
        this.plateNumber = plateNumber;
        this.dailyRate = dailyRate;
        this.status = CarStatus.AVAILABLE;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public void updateDetails(String brand, String model, String plateNumber, BigDecimal dailyRate) {
        this.brand = brand;
        this.model = model;
        this.plateNumber = plateNumber;
        this.dailyRate = dailyRate;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getPlateNumber() { return plateNumber; }
    public BigDecimal getDailyRate() { return dailyRate; }
    public CarStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setStatus(CarStatus status) { this.status = status; this.updatedAt = Instant.now(); }
}
