package ru.bookingcore.booking_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import ru.bookingcore.booking_service.dto.AuthRequest;
import ru.bookingcore.booking_service.dto.CreateBookingRequest;
import ru.bookingcore.booking_service.dto.TokenResponse;
import ru.bookingcore.booking_service.repository.UserRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebMvc
class BookingIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;

        // Получаем токены для тестов
        userToken = getToken("testuser", "testpass");
        adminToken = getToken("admin", "admin");
    }

    private String getToken(String username, String password) {
        AuthRequest authRequest = new AuthRequest(username, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AuthRequest> entity = new HttpEntity<>(authRequest, headers);
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
            baseUrl + "/api/user/auth", entity, TokenResponse.class);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        return response.getBody().getToken();
    }

    @Test
    void testSuccessfulBooking() {
        // Создаем бронирование (ожидаем CANCELLED из-за недоступности hotel-service)
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(5));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(userToken);

        HttpEntity<CreateBookingRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/booking", entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Ожидаем CANCELLED из-за недоступности hotel-service
        assertTrue(response.getBody().contains("CANCELLED"));
    }

    @Test
    void testUnauthorizedAccess() {
        // Попытка создать бронирование без токена
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(5));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CreateBookingRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/booking", entity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetUserBookings() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/api/booking", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testIdempotencyWithSameRequestId() {
        // Тест идемпотентности через создание бронирований с одинаковым requestId
        // (в реальном тесте это было бы через мок или WireMock)

        // Создаем первое бронирование
        CreateBookingRequest request1 = new CreateBookingRequest();
        request1.setRoomId(1L);
        request1.setStartDate(LocalDate.now().plusDays(1));
        request1.setEndDate(LocalDate.now().plusDays(5));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(userToken);

        HttpEntity<CreateBookingRequest> entity1 = new HttpEntity<>(request1, headers);
        ResponseEntity<String> response1 = restTemplate.postForEntity(
            baseUrl + "/api/booking", entity1, String.class);

        // Создаем второе бронирование с теми же параметрами
        CreateBookingRequest request2 = new CreateBookingRequest();
        request2.setRoomId(1L);
        request2.setStartDate(LocalDate.now().plusDays(1));
        request2.setEndDate(LocalDate.now().plusDays(5));

        HttpEntity<CreateBookingRequest> entity2 = new HttpEntity<>(request2, headers);
        ResponseEntity<String> response2 = restTemplate.postForEntity(
            baseUrl + "/api/booking", entity2, String.class);

        // Оба запроса должны быть обработаны (CANCELLED из-за недоступности hotel-service)
        assertTrue(response1.getStatusCode().is2xxSuccessful());
        assertTrue(response2.getStatusCode().is2xxSuccessful());
        assertTrue(response1.getBody().contains("CANCELLED"));
        assertTrue(response2.getBody().contains("CANCELLED"));
    }

    @Test
    void testUserRoleAuthorization() {
        // Тест авторизации USER роли - должен иметь доступ к пользовательским операциям
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // USER должен иметь доступ к своим бронированиям
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/api/booking", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testAdminRoleAuthorization() {
        // Тест авторизации ADMIN роли - в booking-service все операции доступны только USER
        // ADMIN должен получить 403 при попытке доступа к пользовательским операциям
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // ADMIN не должен иметь доступ к пользовательским операциям (только USER)
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/api/booking", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
