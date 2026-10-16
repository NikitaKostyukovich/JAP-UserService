package com.example.userservice.mapper;

import com.example.userservice.dto.cardDTO.CardRequestDTO;
import com.example.userservice.dto.cardDTO.CardResponseDTO;
import com.example.userservice.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CardMapper {

    Card toEntity(CardRequestDTO dto);

    @Mapping(source = "user.id", target = "userId")
    CardResponseDTO toDTO(Card entity);

    void updateEntityFromDTO(CardRequestDTO dto, @MappingTarget Card card);
}
