package ru.bookingcore.hotel_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bookingcore.hotel_service.service.RoomService;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
public class InternalRoomController {

    private final RoomService roomService;

    @PostMapping("/{id}/confirm-availability")
    public ResponseEntity<Boolean> confirmAvailability(
            @PathVariable Long id,
            @RequestHeader("X-Request-Id") String requestId) {

        log.info("Internal endpoint: confirming availability for room ID: {} with requestId: {}", id, requestId);

        boolean confirmed = roomService.confirmAvailability(id, requestId);

        if (confirmed) {
            log.info("Room {} availability confirmed for requestId: {}", id, requestId);
            return ResponseEntity.ok(true);
        } else {
            log.warn("Room {} availability denied for requestId: {}", id, requestId);
            return ResponseEntity.ok(false);
        }
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Void> releaseSlot(
            @PathVariable Long id,
            @RequestHeader("X-Request-Id") String requestId) {

        log.info("Internal endpoint: releasing slot for room ID: {} with requestId: {}", id, requestId);

        roomService.releaseSlot(id, requestId);

        log.info("Slot released for room {} with requestId: {}", id, requestId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/increment-bookings")
    public ResponseEntity<Void> incrementTimesBooked(
            @PathVariable Long id,
            @RequestHeader("X-Request-Id") String requestId) {

        log.info("Internal endpoint: incrementing times booked for room ID: {} with requestId: {}", id, requestId);

        roomService.incrementTimesBooked(id, requestId);

        log.info("Times booked incremented for room {} with requestId: {}", id, requestId);
        return ResponseEntity.ok().build();
    }
}
