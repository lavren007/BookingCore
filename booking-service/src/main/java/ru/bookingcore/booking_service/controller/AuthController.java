package ru.bookingcore.booking_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bookingcore.booking_service.entity.User;
import ru.bookingcore.booking_service.dto.AuthRequest;
import ru.bookingcore.booking_service.dto.RegisterRequest;
import ru.bookingcore.booking_service.dto.TokenResponse;
import ru.bookingcore.booking_service.dto.UserDto;
import ru.bookingcore.booking_service.service.JwtService;
import ru.bookingcore.booking_service.service.UserService;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("User registration request for: {}", request.getUsername());
        UserDto user = userService.registerUser(request);
        String token = jwtService.generateToken(convertToEntity(user));
        return ResponseEntity.ok(new TokenResponse(token, "Bearer"));
    }

    @PostMapping("/auth")
    public ResponseEntity<TokenResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        log.info("User authentication request for: {}", request.getUsername());
        UserDto user = userService.authenticateUser(request.getUsername(), request.getPassword());
        String token = jwtService.generateToken(convertToEntity(user));
        return ResponseEntity.ok(new TokenResponse(token, "Bearer"));
    }

    private User convertToEntity(UserDto userDto) {
        User user = new User();
        user.setId(userDto.getId());
        user.setUsername(userDto.getUsername());
        user.setRole(userDto.getRole());
        return user;
    }
}
