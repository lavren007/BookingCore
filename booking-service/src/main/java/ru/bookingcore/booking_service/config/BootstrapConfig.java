package ru.bookingcore.booking_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.bookingcore.booking_service.entity.User;
import ru.bookingcore.booking_service.repository.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapConfig implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Bootstrap ADMIN
        String adminUsername = System.getenv().getOrDefault("BOOTSTRAP_ADMIN_USER", "admin");
        String adminPassword = System.getenv().getOrDefault("BOOTSTRAP_ADMIN_PASS", "admin");

        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(User.Role.ADMIN);

            userRepository.save(admin);
            log.info("Bootstrap ADMIN user created: {}", adminUsername);
        } else {
            log.info("Bootstrap ADMIN user already exists: {}", adminUsername);
        }

        // Bootstrap test USER
        String testUsername = "testuser";
        String testPassword = "testpass";
        if (userRepository.findByUsername(testUsername).isEmpty()) {
            User testUser = new User();
            testUser.setUsername(testUsername);
            testUser.setPassword(passwordEncoder.encode(testPassword));
            testUser.setRole(User.Role.USER);

            userRepository.save(testUser);
            log.info("Bootstrap TEST user created: {} / {}", testUsername, testPassword);
        } else {
            log.info("Bootstrap TEST user already exists: {}", testUsername);
        }
    }
}
