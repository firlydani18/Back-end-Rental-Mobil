package com.example.rental.controller;

import com.example.rental.dto.*;
import com.example.rental.service.CarService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cars")
public class CarController {
    private final CarService service;
    public CarController(CarService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<CarResponse>> create(@Valid @RequestBody CreateCarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Mobil berhasil dibuat", service.create(request)));
    }

    @GetMapping
    public ApiResponse<Page<CarResponse>> list(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.success("Daftar mobil", service.list(pageable));
    }

    @GetMapping("/availability")
    public ApiResponse<List<CarAvailabilityResponse>> availability(@RequestParam LocalDate startDate,
                                                                   @RequestParam LocalDate endDate) {
        return ApiResponse.success("Ketersediaan mobil pada periode yang diminta", service.availability(startDate, endDate));
    }

    @GetMapping("/{id}")
    public ApiResponse<CarResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Detail mobil", service.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<CarResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateCarRequest request) {
        return ApiResponse.success("Data mobil berhasil diperbarui", service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<CarResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateCarStatusRequest request) {
        return ApiResponse.success("Status mobil berhasil diperbarui", service.updateStatus(id, request.status()));
    }
}
