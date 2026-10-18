package com.example.rental.dto;

import com.example.rental.entity.CarStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCarStatusRequest(@NotNull CarStatus status) {}
