package ru.bookingcore.booking_service.config;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignConfig {

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5000, // connect timeout
                10000, // read timeout
                true // follow redirects
        );
    }
}
