package ru.bookingcore.hotel_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bookingcore.hotel_service.dto.CreateRoomRequest;
import ru.bookingcore.hotel_service.dto.RoomDto;
import ru.bookingcore.hotel_service.entity.Hotel;
import ru.bookingcore.hotel_service.entity.Room;
import ru.bookingcore.hotel_service.repository.HotelRepository;
import ru.bookingcore.hotel_service.repository.RoomRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final IdempotencyService idempotencyService;

    @Transactional
    public RoomDto createRoom(CreateRoomRequest request) {
        log.info("Creating room: {} for hotel ID: {}", request.getNumber(), request.getHotelId());

        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new RuntimeException("Hotel not found with ID: " + request.getHotelId()));

        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(request.getNumber());
        room.setAvailable(true);
        room.setTimesBooked(0);

        Room savedRoom = roomRepository.save(room);
        log.info("Room created with ID: {}", savedRoom.getId());

        return convertToDto(savedRoom);
    }

    public List<RoomDto> getAllAvailableRooms() {
        log.info("Retrieving all available rooms");
        return roomRepository.findByAvailableTrue().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<RoomDto> getRecommendedRooms() {
        log.info("Retrieving recommended rooms (sorted by times_booked)");
        return roomRepository.findAvailableRoomsOrderedByTimesBooked().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean confirmAvailability(Long roomId, String requestId) {
        log.info("Confirming availability for room ID: {} with requestId: {}", roomId, requestId);

        // Проверка идемпотентности
        if (idempotencyService.isProcessed(requestId)) {
            log.info("Request {} already processed - returning cached result (true)", requestId);
            return true; // Возвращаем успешный результат для уже обработанного запроса
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found with ID: " + roomId));

        if (!room.getAvailable()) {
            log.warn("Room {} is not available", roomId);
            return false;
        }

        // Временная блокировка слота (в реальной системе здесь была бы логика блокировки)
        idempotencyService.markAsProcessed(requestId);
        log.info("Room {} availability confirmed for requestId: {}", roomId, requestId);
        return true;
    }

    @Transactional
    public void releaseSlot(Long roomId, String requestId) {
        log.info("Releasing slot for room ID: {} with requestId: {}", roomId, requestId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found with ID: " + roomId));

        // Снятие временной блокировки (в реальной системе здесь была бы логика разблокировки)
        log.info("Slot released for room {} with requestId: {}", roomId, requestId);
    }

    @Transactional
    public void incrementTimesBooked(Long roomId, String requestId) {
        log.info("Incrementing times booked for room ID: {} with requestId: {}", roomId, requestId);

        // Проверка идемпотентности
        String incrementKey = requestId + "-increment";
        if (idempotencyService.isProcessed(incrementKey)) {
            log.info("Request {} already processed - skipping increment", incrementKey);
            return;
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found with ID: " + roomId));

        room.setTimesBooked(room.getTimesBooked() + 1);
        roomRepository.save(room);

        idempotencyService.markAsProcessed(incrementKey);
        log.info("Times booked incremented for room {}: {}", roomId, room.getTimesBooked());
    }

    private RoomDto convertToDto(Room room) {
        return new RoomDto(
                room.getId(),
                room.getHotel().getId(),
                room.getNumber(),
                room.getAvailable(),
                room.getTimesBooked()
        );
    }
}
