package com.example.rental.service;

import com.example.rental.dto.CreateRentalRequest;
import com.example.rental.entity.Car;
import com.example.rental.entity.CarStatus;
import com.example.rental.entity.Rental;
import com.example.rental.entity.RentalStatus;
import com.example.rental.exception.BusinessException;
import com.example.rental.exception.InvalidRequestException;
import com.example.rental.repository.CarRepository;
import com.example.rental.repository.RentalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {
    @Mock CarRepository cars;
    @Mock RentalRepository rentals;
    @InjectMocks RentalService service;

    @Test
    void rejectsEndDateBeforeStartDateBeforeDatabaseAccess() {
        LocalDate start = LocalDate.now().plusDays(2);
        CreateRentalRequest request = request(1L, start, start.minusDays(1));
        assertThrows(InvalidRequestException.class, () -> service.create(request, null));
        verifyNoInteractions(cars, rentals);
    }

    @Test
    void rejectsOverlappingBooking() {
        LocalDate start = LocalDate.now().plusDays(2);
        Car car = mock(Car.class);
        when(car.getId()).thenReturn(1L);
        when(car.getStatus()).thenReturn(CarStatus.AVAILABLE);
        when(cars.findByIdForUpdate(1L)).thenReturn(Optional.of(car));
        when(rentals.hasOverlap(1L, RentalStatus.BOOKED, start, start.plusDays(2))).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(request(1L, start, start.plusDays(2)), null));
        verify(rentals, never()).saveAndFlush(any());
    }

    @Test
    void calculatesInclusiveRentalDaysOnServer() {
        LocalDate start = LocalDate.now().plusDays(2);
        Car car = mock(Car.class);
        when(car.getId()).thenReturn(1L);
        when(car.getBrand()).thenReturn("Toyota");
        when(car.getModel()).thenReturn("Avanza");
        when(car.getPlateNumber()).thenReturn("B 1234 XYZ");
        when(car.getDailyRate()).thenReturn(new BigDecimal("350000.00"));
        when(car.getStatus()).thenReturn(CarStatus.AVAILABLE);
        when(cars.findByIdForUpdate(1L)).thenReturn(Optional.of(car));
        when(rentals.hasOverlap(1L, RentalStatus.BOOKED, start, start.plusDays(2))).thenReturn(false);
        when(rentals.saveAndFlush(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request(1L, start, start.plusDays(2)), "booking-test-001");

        ArgumentCaptor<Rental> captor = ArgumentCaptor.forClass(Rental.class);
        verify(rentals).saveAndFlush(captor.capture());
        assertEquals(new BigDecimal("1050000.00"), captor.getValue().getTotalPrice());
        assertEquals("booking-test-001", captor.getValue().getIdempotencyKey());
    }

    private CreateRentalRequest request(Long carId, LocalDate start, LocalDate end) {
        return new CreateRentalRequest(carId, "Budi Santoso", "budi@example.com", start, end);
    }
}
