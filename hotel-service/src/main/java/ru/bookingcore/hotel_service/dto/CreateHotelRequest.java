package ru.bookingcore.hotel_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateHotelRequest {
    @NotBlank(message = "Hotel name is required")
    private String name;

    @NotBlank(message = "Hotel address is required")
    private String address;
}
