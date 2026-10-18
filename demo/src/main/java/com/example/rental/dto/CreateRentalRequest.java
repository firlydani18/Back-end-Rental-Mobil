package com.example.rental.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateRentalRequest(
    @NotNull @Positive Long carId,
    @NotBlank @Size(max = 120) String customerName,
    @NotBlank @Email @Size(max = 180) String customerEmail,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate
) {}
