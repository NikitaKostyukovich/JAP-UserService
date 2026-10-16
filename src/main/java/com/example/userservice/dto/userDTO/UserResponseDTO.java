package com.example.userservice.dto.userDTO;

import com.example.userservice.dto.cardDTO.CardResponseDTO;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public record UserResponseDTO(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        boolean active,
        List<CardResponseDTO> cards
) implements Serializable {
}
