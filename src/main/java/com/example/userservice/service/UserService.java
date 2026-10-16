package com.example.userservice.service;

import com.example.userservice.dto.userDTO.UserRequestDTO;
import com.example.userservice.dto.userDTO.UserResponseDTO;
import com.example.userservice.entity.User;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
@Service
public class UserService{
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO dto){
        User user = userMapper.toEntity(dto);
        User savedUser = userRepository.save(user);
        return userMapper.toDTO(savedUser);
    }
    @Cacheable(value = "users", key = "#id")
    public UserResponseDTO getUserById(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("There is no user with such id"));
        return userMapper.toDTO(user);
    }

    public Page<UserResponseDTO> getFilteredUsers(Specification<User> spec, Pageable pageable){
        Page<User> foundedUsers = userRepository.findAll(spec, pageable);
        return foundedUsers.map(userMapper::toDTO);
    }
    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO dto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Cannot update. User not found with id: " + id));
        userMapper.updateUserFromDto(dto, existingUser);
        User updatedUser = userRepository.save(existingUser);
        return userMapper.toDTO(updatedUser);
    }
    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("Cannot delete. User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public void updateUserStatus(Long id, boolean active) {
        if(userRepository.existsById(id)){
        userRepository.updateActivityAndTime(active, id);
        }else{
            throw new UserNotFoundException("User not found with id:" + id);
        }
    }


}
