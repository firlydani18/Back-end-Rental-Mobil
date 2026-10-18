package com.example.rental.repository;

import com.example.rental.entity.Car;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CarRepository extends JpaRepository<Car, Long> {
    boolean existsByPlateNumberIgnoreCase(String plateNumber);
    Optional<Car> findByPlateNumberIgnoreCase(String plateNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Car c where c.id = :id")
    Optional<Car> findByIdForUpdate(@Param("id") Long id);
}
