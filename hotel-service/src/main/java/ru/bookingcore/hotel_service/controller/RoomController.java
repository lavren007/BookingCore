package ru.bookingcore.hotel_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.bookingcore.hotel_service.dto.CreateRoomRequest;
import ru.bookingcore.hotel_service.dto.RoomDto;
import ru.bookingcore.hotel_service.service.RoomService;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<RoomDto> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        log.info("Creating room: {} for hotel ID: {}", request.getNumber(), request.getHotelId());
        RoomDto room = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @GetMapping
    public ResponseEntity<List<RoomDto>> getAllAvailableRooms() {
        log.info("Retrieving all available rooms");
        List<RoomDto> rooms = roomService.getAllAvailableRooms();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/recommend")
    public ResponseEntity<List<RoomDto>> getRecommendedRooms() {
        log.info("Retrieving recommended rooms");
        List<RoomDto> rooms = roomService.getRecommendedRooms();
        return ResponseEntity.ok(rooms);
    }
}
