package ru.bookingcore.hotel_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.bookingcore.hotel_service.entity.Hotel;
import ru.bookingcore.hotel_service.entity.Room;
import ru.bookingcore.hotel_service.repository.HotelRepository;
import ru.bookingcore.hotel_service.repository.RoomRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapConfig implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) throws Exception {
        if (hotelRepository.count() == 0) {
            log.info("Bootstrapping test hotels and rooms...");

            // Создаём тестовые отели
            Hotel hotel1 = new Hotel();
            hotel1.setName("Grand Hotel");
            hotel1.setAddress("123 Main Street, Moscow");
            hotelRepository.save(hotel1);

            Hotel hotel2 = new Hotel();
            hotel2.setName("Cozy Inn");
            hotel2.setAddress("456 Park Avenue, St. Petersburg");
            hotelRepository.save(hotel2);

            // Создаём тестовые номера для первого отеля
            for (int i = 101; i <= 105; i++) {
                Room room = new Room();
                room.setHotel(hotel1);
                room.setNumber(String.valueOf(i));
                room.setAvailable(true);
                room.setTimesBooked(0);
                roomRepository.save(room);
            }

            // Создаём тестовые номера для второго отеля
            for (int i = 201; i <= 203; i++) {
                Room room = new Room();
                room.setHotel(hotel2);
                room.setNumber(String.valueOf(i));
                room.setAvailable(true);
                room.setTimesBooked(0);
                roomRepository.save(room);
            }

            log.info("Bootstrap complete: 2 hotels, 8 rooms created");
        } else {
            log.info("Bootstrap skipped: hotels already exist");
        }
    }
}

