package com.example.rental.service;

import com.example.rental.dto.CreateRentalRequest;
import com.example.rental.dto.RentalResponse;
import com.example.rental.entity.Car;
import com.example.rental.entity.CarStatus;
import com.example.rental.entity.Rental;
import com.example.rental.entity.RentalStatus;
import com.example.rental.exception.BusinessException;
import com.example.rental.exception.NotFoundException;
import com.example.rental.exception.InvalidRequestException;
import com.example.rental.repository.CarRepository;
import com.example.rental.repository.RentalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class RentalService {
    private final CarRepository cars;
    private final RentalRepository rentals;

    public RentalService(CarRepository cars, RentalRepository rentals) {
        this.cars = cars;
        this.rentals = rentals;
    }

    @Transactional
    public RentalResponse create(CreateRentalRequest request, String rawIdempotencyKey) {
        String key = normalizeIdempotencyKey(rawIdempotencyKey);
        validateDateOrder(request.startDate(), request.endDate());

        // First lookup makes retries return the original result even if its rental dates are now in the past.
        if (key != null) {
            var existing = rentals.findByIdempotencyKey(key);
            if (existing.isPresent()) return replayOrReject(existing.get(), request);
        }
        validateStartDate(request.startDate());

        // Lock one car row so booking requests for this car are serialized while overlap is checked and saved.
        Car car = cars.findByIdForUpdate(request.carId())
            .orElseThrow(() -> new NotFoundException("Mobil tidak ditemukan: " + request.carId()));

        // Recheck after acquiring the lock to cover concurrent retries with the same key for the same car.
        if (key != null) {
            var existing = rentals.findByIdempotencyKey(key);
            if (existing.isPresent()) return replayOrReject(existing.get(), request);
        }

        if (car.getStatus() != CarStatus.AVAILABLE)
            throw new BusinessException("Mobil tidak tersedia: status " + car.getStatus());
        if (rentals.hasOverlap(car.getId(), RentalStatus.BOOKED, request.startDate(), request.endDate()))
            throw new BusinessException("Mobil sudah dipesan pada periode tersebut");

        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        BigDecimal total = car.getDailyRate().multiply(BigDecimal.valueOf(days));
        Rental rental = new Rental(car, request.customerName().trim(), request.customerEmail().trim().toLowerCase(),
            request.startDate(), request.endDate(), total, key);
        return RentalResponse.from(rentals.saveAndFlush(rental));
    }

    @Transactional(readOnly = true)
    public RentalResponse get(Long id) {
        Rental rental = rentals.findById(id)
            .orElseThrow(() -> new NotFoundException("Transaksi rental tidak ditemukan: " + id));
        return RentalResponse.from(rental);
    }

    @Transactional(readOnly = true)
    public Page<RentalResponse> list(Pageable pageable) {
        return rentals.findAllByOrderByCreatedAtDesc(pageable).map(RentalResponse::from);
    }

    @Transactional
    public RentalResponse cancel(Long id) {
        Rental rental = rentals.findByIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("Transaksi rental tidak ditemukan: " + id));
        if (rental.getStatus() != RentalStatus.BOOKED)
            throw new BusinessException("Hanya transaksi BOOKED yang dapat dibatalkan");
        if (!LocalDate.now().isBefore(rental.getStartDate()))
            throw new BusinessException("Rental tidak dapat dibatalkan pada atau setelah tanggal mulai");
        rental.setStatus(RentalStatus.CANCELLED);
        return RentalResponse.from(rental);
    }

    @Transactional
    public RentalResponse complete(Long id) {
        Rental rental = rentals.findByIdForUpdate(id)
            .orElseThrow(() -> new NotFoundException("Transaksi rental tidak ditemukan: " + id));
        if (rental.getStatus() != RentalStatus.BOOKED)
            throw new BusinessException("Hanya transaksi BOOKED yang dapat diselesaikan");
        if (LocalDate.now().isBefore(rental.getEndDate()))
            throw new BusinessException("Rental belum dapat diselesaikan sebelum tanggal akhir sewa");
        rental.setStatus(RentalStatus.COMPLETED);
        return RentalResponse.from(rental);
    }

    private void validateDateOrder(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) throw new InvalidRequestException("endDate tidak boleh sebelum startDate");
    }

    private void validateStartDate(LocalDate startDate) {
        if (startDate.isBefore(LocalDate.now())) throw new InvalidRequestException("Tanggal mulai sewa tidak boleh di masa lalu");
    }

    private String normalizeIdempotencyKey(String key) {
        if (key == null || key.isBlank()) return null;
        String normalized = key.trim();
        if (normalized.length() > 100) throw new InvalidRequestException("Idempotency-Key maksimal 100 karakter");
        return normalized;
    }

    private RentalResponse replayOrReject(Rental existing, CreateRentalRequest request) {
        boolean samePayload = existing.getCar().getId().equals(request.carId())
            && existing.getCustomerName().equals(request.customerName().trim())
            && existing.getCustomerEmail().equals(request.customerEmail().trim().toLowerCase())
            && existing.getStartDate().equals(request.startDate())
            && existing.getEndDate().equals(request.endDate());
        if (!samePayload) throw new BusinessException("Idempotency-Key sudah digunakan untuk request yang berbeda");
        return RentalResponse.from(existing);
    }
}
