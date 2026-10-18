package com.example.rental.dto;

import com.example.rental.entity.Car;
import com.example.rental.entity.CarStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record CarResponse(Long id, String brand, String model, String plateNumber,
                          BigDecimal dailyRate, CarStatus status, Instant createdAt, Instant updatedAt) {
    public static CarResponse from(Car car) {
        return new CarResponse(car.getId(), car.getBrand(), car.getModel(), car.getPlateNumber(),
            car.getDailyRate(), car.getStatus(), car.getCreatedAt(), car.getUpdatedAt());
    }
}
