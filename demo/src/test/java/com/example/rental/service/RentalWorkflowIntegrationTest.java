package com.example.rental.service;

import com.example.rental.dto.CarAvailabilityResponse;
import com.example.rental.dto.CarResponse;
import com.example.rental.dto.CreateCarRequest;
import com.example.rental.dto.CreateRentalRequest;
import com.example.rental.dto.RentalResponse;
import com.example.rental.entity.Rental;
import com.example.rental.entity.RentalStatus;
import com.example.rental.entity.CarStatus;
import com.example.rental.exception.BusinessException;
import com.example.rental.exception.InvalidRequestException;
import com.example.rental.repository.CarRepository;
import com.example.rental.repository.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RentalWorkflowIntegrationTest {
    @Autowired CarService carService;
    @Autowired RentalService rentalService;
    @Autowired CarRepository carRepository;
    @Autowired RentalRepository rentalRepository;

    @BeforeEach
    void cleanDatabase() {
        rentalRepository.deleteAllInBatch();
        carRepository.deleteAllInBatch();
    }

    @Test
    void bookingConflictsAndCancellationReleasesDateRange() {
        CarResponse car = createCar("B 1234 XYZ");
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = start.plusDays(2);

        RentalResponse original = rentalService.create(request(car.id(), start, end, "a@example.com"), "integration-booking-1");
        assertEquals(new BigDecimal("1050000.00"), original.totalPrice());
        assertThrows(BusinessException.class, () -> rentalService.create(request(car.id(), start, end, "b@example.com"), "integration-booking-2"));

        RentalResponse cancelled = rentalService.cancel(original.id());
        assertEquals(RentalStatus.CANCELLED, cancelled.status());
        RentalResponse replacement = rentalService.create(request(car.id(), start, end, "b@example.com"), "integration-booking-3");
        assertEquals(RentalStatus.BOOKED, replacement.status());
    }

    @Test
    void availabilityIsCalculatedFromRequestedDateRange() {
        CarResponse car = createCar("B 2222 ABC");
        LocalDate bookedStart = LocalDate.now().plusDays(4);
        LocalDate bookedEnd = bookedStart.plusDays(1);
        rentalService.create(request(car.id(), bookedStart, bookedEnd, "a@example.com"), "availability-booking");

        List<CarAvailabilityResponse> samePeriod = carService.availability(bookedStart, bookedEnd);
        assertFalse(samePeriod.stream().filter(c -> c.id().equals(car.id())).findFirst().orElseThrow().availableForPeriod());

        List<CarAvailabilityResponse> anotherPeriod = carService.availability(bookedEnd.plusDays(1), bookedEnd.plusDays(2));
        assertTrue(anotherPeriod.stream().filter(c -> c.id().equals(car.id())).findFirst().orElseThrow().availableForPeriod());
    }

    @Test
    void databaseFailureDoesNotLeaveARentalRecord() {
        CarResponse car = createCar("B 3333 QWE");
        LocalDate start = LocalDate.now().plusDays(2);
        String tooLongEmail = "a".repeat(181);
        CreateRentalRequest invalidForDatabase = new CreateRentalRequest(car.id(), "Customer", tooLongEmail, start, start.plusDays(1));

        assertThrows(RuntimeException.class, () -> rentalService.create(invalidForDatabase, "rollback-test-key"));
        assertEquals(0, rentalRepository.count());
    }

    @Test
    void completedRentalCannotBeCancelledAgain() {
        CarResponse car = createCar("B 4444 DEF");
        LocalDate today = LocalDate.now();
        RentalResponse booking = rentalService.create(request(car.id(), today, today, "complete@example.com"), "completion-test");
        RentalResponse completed = rentalService.complete(booking.id());
        assertEquals(RentalStatus.COMPLETED, completed.status());
        assertThrows(BusinessException.class, () -> rentalService.cancel(booking.id()));
    }


    @Test
    void maintenanceCarCannotBeRented() {
        CarResponse car = createCar("B 8888 PQR");
        carService.updateStatus(car.id(), CarStatus.MAINTENANCE);
        LocalDate start = LocalDate.now().plusDays(3);
        assertThrows(BusinessException.class, () -> rentalService.create(
            request(car.id(), start, start.plusDays(1), "maintenance@example.com"), "maintenance-key"));
        assertEquals(0, rentalRepository.count());
    }

    @Test
    void invalidStartDateIsRejected() {
        CarResponse car = createCar("B 5555 GHI");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        assertThrows(InvalidRequestException.class, () -> rentalService.create(request(car.id(), yesterday, yesterday, "past@example.com"), null));
        assertEquals(0, rentalRepository.count());
    }


    @Test
    void repeatedIdempotencyKeyReturnsSameRentalForSamePayload() {
        CarResponse car = createCar("B 6666 JKL");
        LocalDate start = LocalDate.now().plusDays(5);
        CreateRentalRequest request = request(car.id(), start, start.plusDays(1), "same@example.com");

        RentalResponse first = rentalService.create(request, "retry-safe-key");
        RentalResponse second = rentalService.create(request, "retry-safe-key");

        assertEquals(first.id(), second.id());
        assertEquals(1, rentalRepository.count());
    }

    @Test
    void concurrentRequestsCannotBookSameCarForOverlappingDates() throws Exception {
        CarResponse car = createCar("B 7777 MNO");
        LocalDate startDate = LocalDate.now().plusDays(7);
        CreateRentalRequest requestA = request(car.id(), startDate, startDate.plusDays(2), "one@example.com");
        CreateRentalRequest requestB = request(car.id(), startDate, startDate.plusDays(2), "two@example.com");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = pool.submit(() -> tryConcurrentBooking(ready, start, requestA, "concurrent-key-a"));
            Future<String> second = pool.submit(() -> tryConcurrentBooking(ready, start, requestB, "concurrent-key-b"));
            assertTrue(ready.await(5, TimeUnit.SECONDS), "Workers should be ready");
            start.countDown();
            List<String> outcomes = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(1, outcomes.stream().filter("BOOKED"::equals).count());
            assertEquals(1, outcomes.stream().filter("CONFLICT"::equals).count());
            assertEquals(1, rentalRepository.count());
        } finally {
            pool.shutdownNow();
        }
    }

    private String tryConcurrentBooking(CountDownLatch ready, CountDownLatch start,
                                        CreateRentalRequest request, String idempotencyKey) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Start signal timed out");
        try {
            rentalService.create(request, idempotencyKey);
            return "BOOKED";
        } catch (BusinessException ex) {
            return "CONFLICT";
        }
    }

    private CarResponse createCar(String plate) {
        return carService.create(new CreateCarRequest("Toyota", "Avanza", plate, new BigDecimal("350000.00")));
    }

    private CreateRentalRequest request(Long carId, LocalDate start, LocalDate end, String email) {
        return new CreateRentalRequest(carId, "Test Customer", email, start, end);
    }
}
