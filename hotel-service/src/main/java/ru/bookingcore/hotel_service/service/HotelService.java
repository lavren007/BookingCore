package ru.bookingcore.hotel_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bookingcore.hotel_service.dto.CreateHotelRequest;
import ru.bookingcore.hotel_service.dto.HotelDto;
import ru.bookingcore.hotel_service.entity.Hotel;
import ru.bookingcore.hotel_service.repository.HotelRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelService {

    private final HotelRepository hotelRepository;

    @Transactional
    public HotelDto createHotel(CreateHotelRequest request) {
        log.info("Creating hotel: {}", request.getName());

        Hotel hotel = new Hotel();
        hotel.setName(request.getName());
        hotel.setAddress(request.getAddress());

        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("Hotel created with ID: {}", savedHotel.getId());

        return convertToDto(savedHotel);
    }

    public List<HotelDto> getAllHotels() {
        log.info("Retrieving all hotels");
        return hotelRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public HotelDto getHotelById(Long id) {
        log.info("Retrieving hotel by ID: {}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hotel not found with ID: " + id));
        return convertToDto(hotel);
    }

    private HotelDto convertToDto(Hotel hotel) {
        return new HotelDto(
                hotel.getId(),
                hotel.getName(),
                hotel.getAddress()
        );
    }
}
