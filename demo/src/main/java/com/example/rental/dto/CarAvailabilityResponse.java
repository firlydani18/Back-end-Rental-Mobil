package com.example.rental.dto;

import com.example.rental.entity.Car;
import com.example.rental.entity.CarStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CarAvailabilityResponse(Long id, String brand, String model, String plateNumber,
                                      BigDecimal dailyRate, CarStatus status, LocalDate startDate,
                                      LocalDate endDate, boolean availableForPeriod, String availabilityReason) {
    public static CarAvailabilityResponse from(Car car, LocalDate startDate, LocalDate endDate,
                                               boolean available, String reason) {
        return new CarAvailabilityResponse(car.getId(), car.getBrand(), car.getModel(), car.getPlateNumber(),
            car.getDailyRate(), car.getStatus(), startDate, endDate, available, reason);
    }
}
