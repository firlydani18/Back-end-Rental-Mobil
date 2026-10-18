package com.example.rental.repository;

import com.example.rental.entity.Rental;
import com.example.rental.entity.RentalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {
    @Query("select case when count(r) > 0 then true else false end from Rental r where r.car.id = :carId and r.status = :status " +
           "and r.startDate <= :endDate and r.endDate >= :startDate")
    boolean hasOverlap(@Param("carId") Long carId, @Param("status") RentalStatus status,
                       @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    boolean existsByCar_IdAndStatusAndEndDateGreaterThanEqual(Long carId, RentalStatus status, LocalDate date);
    Optional<Rental> findByIdempotencyKey(String idempotencyKey);
    Page<Rental> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rental r where r.id = :id")
    Optional<Rental> findByIdForUpdate(@Param("id") Long id);
}
