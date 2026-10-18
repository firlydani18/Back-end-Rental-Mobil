package com.example.rental.dto;

import com.example.rental.entity.Rental;
import com.example.rental.entity.RentalStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record RentalResponse(Long id, Long carId, String carName, String plateNumber,
                             String customerName, String customerEmail, LocalDate startDate,
                             LocalDate endDate, long rentalDays, BigDecimal totalPrice,
                             RentalStatus status, Instant createdAt, Instant updatedAt) {
    public static RentalResponse from(Rental rental) {
        long days = ChronoUnit.DAYS.between(rental.getStartDate(), rental.getEndDate()) + 1;
        return new RentalResponse(rental.getId(), rental.getCar().getId(),
            rental.getCar().getBrand() + " " + rental.getCar().getModel(), rental.getCar().getPlateNumber(),
            rental.getCustomerName(), rental.getCustomerEmail(), rental.getStartDate(), rental.getEndDate(),
            days, rental.getTotalPrice(), rental.getStatus(), rental.getCreatedAt(), rental.getUpdatedAt());
    }
}
