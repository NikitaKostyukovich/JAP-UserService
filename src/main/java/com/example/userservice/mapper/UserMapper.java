package com.example.userservice.mapper;

import com.example.userservice.dto.userDTO.UserRequestDTO;
import com.example.userservice.dto.userDTO.UserResponseDTO;
import com.example.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponseDTO toDTO(User entity);
    User toEntity(UserRequestDTO dto);
    void updateUserFromDto(UserRequestDTO dto, @MappingTarget  User user);
}
