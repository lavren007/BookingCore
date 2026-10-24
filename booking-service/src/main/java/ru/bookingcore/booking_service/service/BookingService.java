package ru.bookingcore.booking_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bookingcore.booking_service.client.HotelServiceClient;
import ru.bookingcore.booking_service.dto.BookingDto;
import ru.bookingcore.booking_service.dto.CreateBookingRequest;
import ru.bookingcore.booking_service.entity.Booking;
import ru.bookingcore.booking_service.entity.User;
import ru.bookingcore.booking_service.exception.RetryExhaustedException;
import ru.bookingcore.booking_service.repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final HotelServiceClient hotelServiceClient;

    @Transactional
    public BookingDto createBooking(CreateBookingRequest request, User user) {
        String requestId = UUID.randomUUID().toString();
        log.info("Creating booking for user {} with requestId: {}", user.getUsername(), requestId);

        // Создаём бронирование в статусе PENDING
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoomId(request.getRoomId());
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setStatus(Booking.Status.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setRequestId(requestId);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created with ID: {} in PENDING status", savedBooking.getId());

        try {
            // Подтверждаем доступность в hotel-service
            boolean confirmed = confirmAvailabilityWithRetry(request.getRoomId(), requestId);

            if (confirmed) {
                // Переводим в CONFIRMED
                savedBooking.setStatus(Booking.Status.CONFIRMED);
                bookingRepository.save(savedBooking);

                // Инкрементируем счётчик бронирований
                incrementTimesBookedWithRetry(request.getRoomId(), requestId);

                log.info("Booking {} confirmed successfully", savedBooking.getId());
            } else {
                // Переводим в CANCELLED
                savedBooking.setStatus(Booking.Status.CANCELLED);
                bookingRepository.save(savedBooking);
                log.warn("Booking {} cancelled - room not available", savedBooking.getId());
            }
        } catch (RetryExhaustedException e) {
            // При исчерпании повторов переводим в CANCELLED и выполняем компенсацию
            savedBooking.setStatus(Booking.Status.CANCELLED);
            bookingRepository.save(savedBooking);

            // Компенсация: освобождаем слот
            try {
                releaseSlotWithRetry(request.getRoomId(), requestId);
            } catch (Exception compensationException) {
                log.error("Compensation failed for booking {}: {}", savedBooking.getId(), compensationException.getMessage());
            }

            log.error("Booking {} failed after retry exhaustion: {}", savedBooking.getId(), e.getMessage());
        } catch (Exception e) {
            // При других ошибках переводим в CANCELLED и выполняем компенсацию
            savedBooking.setStatus(Booking.Status.CANCELLED);
            bookingRepository.save(savedBooking);

            // Компенсация: освобождаем слот
            try {
                releaseSlotWithRetry(request.getRoomId(), requestId);
            } catch (Exception compensationException) {
                log.error("Compensation failed for booking {}: {}", savedBooking.getId(), compensationException.getMessage());
            }

            log.error("Booking {} failed: {}", savedBooking.getId(), e.getMessage());
        }

        return convertToDto(savedBooking);
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private boolean confirmAvailabilityWithRetry(Long roomId, String requestId) {
        log.info("Confirming availability for room {} with requestId: {}", roomId, requestId);
        return hotelServiceClient.confirmAvailability(roomId, requestId);
    }

    @Recover
    private boolean confirmAvailabilityRecover(Exception ex, Long roomId, String requestId) {
        log.error("All retry attempts exhausted for confirmAvailability room {} with requestId: {}", roomId, requestId, ex);
        throw new RetryExhaustedException("Failed to confirm availability after all retry attempts", ex);
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private void incrementTimesBookedWithRetry(Long roomId, String requestId) {
        log.info("Incrementing times booked for room {} with requestId: {}", roomId, requestId);
        hotelServiceClient.incrementTimesBooked(roomId, requestId);
    }

    @Recover
    private void incrementTimesBookedRecover(Exception ex, Long roomId, String requestId) {
        log.error("All retry attempts exhausted for incrementTimesBooked room {} with requestId: {}", roomId, requestId, ex);
        throw new RetryExhaustedException("Failed to increment times booked after all retry attempts", ex);
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private void releaseSlotWithRetry(Long roomId, String requestId) {
        log.info("Releasing slot for room {} with requestId: {}", roomId, requestId);
        hotelServiceClient.releaseSlot(roomId, requestId);
    }

    @Recover
    private void releaseSlotRecover(Exception ex, Long roomId, String requestId) {
        log.error("All retry attempts exhausted for releaseSlot room {} with requestId: {}", roomId, requestId, ex);
        // Для компенсации не бросаем исключение, просто логируем
        log.warn("Compensation failed for room {} with requestId: {} - manual intervention may be required", roomId, requestId);
    }

    public List<BookingDto> getUserBookings(Long userId) {
        log.info("Retrieving bookings for user ID: {}", userId);
        return bookingRepository.findByUserId(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public BookingDto getBookingById(Long id, Long userId) {
        log.info("Retrieving booking ID: {} for user ID: {}", id, userId);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));

        if (!booking.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied to booking: " + id);
        }

        return convertToDto(booking);
    }

    @Transactional
    public void cancelBooking(Long id, Long userId) {
        log.info("Cancelling booking ID: {} for user ID: {}", id, userId);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));

        if (!booking.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied to booking: " + id);
        }

        if (booking.getStatus() == Booking.Status.CONFIRMED) {
            booking.setStatus(Booking.Status.CANCELLED);
            bookingRepository.save(booking);
            log.info("Booking {} cancelled", id);
        } else {
            log.warn("Booking {} cannot be cancelled - status: {}", id, booking.getStatus());
        }
    }

    private BookingDto convertToDto(Booking booking) {
        return new BookingDto(
                booking.getId(),
                booking.getUser().getId(),
                booking.getRoomId(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
