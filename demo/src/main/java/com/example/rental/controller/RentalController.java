package com.example.rental.controller;

import com.example.rental.dto.ApiResponse;
import com.example.rental.dto.CreateRentalRequest;
import com.example.rental.dto.RentalResponse;
import com.example.rental.service.RentalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rentals")
public class RentalController {
    private final RentalService service;
    public RentalController(RentalService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<RentalResponse>> create(@Valid @RequestBody CreateRentalRequest request,
                                                               @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Rental berhasil dibuat", service.create(request, idempotencyKey)));
    }

    @GetMapping
    public ApiResponse<Page<RentalResponse>> list(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success("Daftar transaksi rental", service.list(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<RentalResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Detail transaksi rental", service.get(id));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<RentalResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success("Rental berhasil dibatalkan", service.cancel(id));
    }

    @PatchMapping("/{id}/complete")
    public ApiResponse<RentalResponse> complete(@PathVariable Long id) {
        return ApiResponse.success("Rental berhasil diselesaikan", service.complete(id));
    }
}
