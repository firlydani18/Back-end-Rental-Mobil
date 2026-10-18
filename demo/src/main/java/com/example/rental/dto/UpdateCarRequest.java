package com.example.rental.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateCarRequest(
    @NotBlank @Size(max = 80) String brand,
    @NotBlank @Size(max = 100) String model,
    @NotBlank @Size(max = 20) String plateNumber,
    @NotNull @DecimalMin(value = "0.01", message = "dailyRate harus lebih besar dari 0") BigDecimal dailyRate
) {}
