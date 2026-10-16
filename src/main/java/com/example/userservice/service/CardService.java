package com.example.userservice.service;

import com.example.userservice.dto.cardDTO.CardRequestDTO;
import com.example.userservice.dto.cardDTO.CardResponseDTO;
import com.example.userservice.entity.Card;
import com.example.userservice.entity.User;
import com.example.userservice.exception.CardLimitExceededException;
import com.example.userservice.exception.CardNotFoundException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.mapper.CardMapper;
import com.example.userservice.repository.CardRepository;
import com.example.userservice.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    public CardService(CardRepository cardRepository, UserRepository userRepository, CardMapper cardMapper) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.cardMapper = cardMapper;
    }

    @CacheEvict(value = {"cards", "users", "userCards"}, allEntries = true)
    @Transactional
    public CardResponseDTO addCardToUser(Long userId, CardRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        long activeCardsCount = cardRepository.countByUserIdAndActiveTrue(userId);
        if (activeCardsCount >= 5) {
            throw new CardLimitExceededException("User has reached the maximum limit of 5 active cards.");
        }

        Card card = cardMapper.toEntity(dto);
        card.setUser(user);

        Card savedCard = cardRepository.save(card);
        return cardMapper.toDTO(savedCard);
    }
    @Cacheable(value = "userCards", key = "#userId")
    public List<CardResponseDTO> getCardsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }

        List<Card> cards = cardRepository.findByUserId(userId);
        return cards.stream()
                .map(cardMapper::toDTO)
                .collect(Collectors.toList());
    }
    @Cacheable(value = "card", key = "#cardId")
    public CardResponseDTO getCardById(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + cardId));
        return cardMapper.toDTO(card);
    }
    @CacheEvict(value = {"card", "userCards", "users"}, allEntries = true)
    @Transactional
    public CardResponseDTO updateCard(Long cardId, CardRequestDTO dto) {
        Card existingCard = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + cardId));

        cardMapper.updateEntityFromDTO(dto, existingCard);

        Card updatedCard = cardRepository.save(existingCard);
        return cardMapper.toDTO(updatedCard);
    }
    @CacheEvict(value = {"card", "userCards", "users"}, allEntries = true)
    @Transactional
    public void deleteCard(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException("Card not found with id: " + cardId);
        }
        cardRepository.activateOrDeactivateCard(false, cardId);
    }
}