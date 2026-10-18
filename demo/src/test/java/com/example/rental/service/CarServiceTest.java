package com.example.rental.service;

import com.example.rental.dto.CreateCarRequest;
import com.example.rental.entity.Car;
import com.example.rental.exception.BusinessException;
import com.example.rental.repository.CarRepository;
import com.example.rental.repository.RentalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {
    @Mock CarRepository cars;
    @Mock RentalRepository rentals;
    @InjectMocks CarService service;

    @Test
    void rejectsDuplicatePlate() {
        when(cars.existsByPlateNumberIgnoreCase("B 1234 XYZ")).thenReturn(true);
        CreateCarRequest request = new CreateCarRequest("Toyota", "Avanza", "b 1234 xyz", new BigDecimal("350000"));
        assertThrows(BusinessException.class, () -> service.create(request));
        verify(cars, never()).save(any(Car.class));
    }
}
