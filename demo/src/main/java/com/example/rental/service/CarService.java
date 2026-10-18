package com.example.rental.service;

import com.example.rental.dto.*;
import com.example.rental.entity.Car;
import com.example.rental.entity.CarStatus;
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
import java.time.LocalDate;
import java.util.List;

@Service
public class CarService {
    private final CarRepository cars;
    private final RentalRepository rentals;

    public CarService(CarRepository cars, RentalRepository rentals) {
        this.cars = cars;
        this.rentals = rentals;
    }

    @Transactional
    public CarResponse create(CreateCarRequest request) {
        String plate = normalizePlate(request.plateNumber());
        if (cars.existsByPlateNumberIgnoreCase(plate)) throw new BusinessException("Nomor plat sudah terdaftar");
        Car car = new Car(request.brand().trim(), request.model().trim(), plate, request.dailyRate());
        return CarResponse.from(cars.save(car));
    }

    @Transactional(readOnly = true)
    public Page<CarResponse> list(Pageable pageable) {
        return cars.findAll(pageable).map(CarResponse::from);
    }

    @Transactional(readOnly = true)
    public CarResponse get(Long id) {
        return CarResponse.from(findCar(id));
    }

    @Transactional(readOnly = true)
    public List<CarAvailabilityResponse> availability(LocalDate startDate, LocalDate endDate) {
        validatePeriod(startDate, endDate);
        return cars.findAll().stream().map(car -> {
            if (car.getStatus() != CarStatus.AVAILABLE)
                return CarAvailabilityResponse.from(car, startDate, endDate, false, "CAR_" + car.getStatus());
            boolean overlap = rentals.hasOverlap(car.getId(), RentalStatus.BOOKED, startDate, endDate);
            return CarAvailabilityResponse.from(car, startDate, endDate, !overlap,
                overlap ? "PERIOD_ALREADY_BOOKED" : "AVAILABLE");
        }).toList();
    }

    @Transactional
    public CarResponse update(Long id, UpdateCarRequest request) {
        Car car = cars.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Mobil tidak ditemukan: " + id));
        String plate = normalizePlate(request.plateNumber());
        cars.findByPlateNumberIgnoreCase(plate).filter(other -> !other.getId().equals(id))
            .ifPresent(other -> { throw new BusinessException("Nomor plat sudah digunakan mobil lain"); });
        car.updateDetails(request.brand().trim(), request.model().trim(), plate, request.dailyRate());
        return CarResponse.from(car);
    }

    @Transactional
    public CarResponse updateStatus(Long id, CarStatus newStatus) {
        Car car = cars.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Mobil tidak ditemukan: " + id));
        if (newStatus != CarStatus.AVAILABLE &&
            rentals.existsByCar_IdAndStatusAndEndDateGreaterThanEqual(id, RentalStatus.BOOKED, LocalDate.now())) {
            throw new BusinessException("Status tidak dapat diubah: masih ada rental BOOKED yang aktif atau akan datang");
        }
        car.setStatus(newStatus);
        return CarResponse.from(car);
    }

    private Car findCar(Long id) {
        return cars.findById(id).orElseThrow(() -> new NotFoundException("Mobil tidak ditemukan: " + id));
    }

    private String normalizePlate(String plate) {
        return plate.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) throw new InvalidRequestException("startDate dan endDate wajib diisi");
        if (startDate.isBefore(LocalDate.now())) throw new InvalidRequestException("startDate tidak boleh di masa lalu");
        if (endDate.isBefore(startDate)) throw new InvalidRequestException("endDate tidak boleh sebelum startDate");
    }
}
