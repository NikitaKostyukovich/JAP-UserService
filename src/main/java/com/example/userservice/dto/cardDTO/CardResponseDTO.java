package com.example.userservice.dto.cardDTO;

import java.io.Serializable;
import java.time.LocalDate;

public record CardResponseDTO(
        Long id,
        Long userId,
        String number,
        String holder,
        LocalDate expirationDate,
        boolean active
) implements Serializable {
}
