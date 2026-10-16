package com.example.userservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CardApiIntegrationTest extends AbstractIntegrationTest {

    // ---------- POST /api/users/{userId}/cards ----------

    @Test
    void addCard_validBody_returns201() throws Exception {
        long userId = createUser("Ivan", "Petrov", "ivan@test.com");

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(cardNumber(1), "IVAN PETROV")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.number").value(cardNumber(1)))
                .andExpect(jsonPath("$.holder").value("IVAN PETROV"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void addCard_unknownUser_returns404() throws Exception {
        mockMvc.perform(post("/api/users/{userId}/cards", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(cardNumber(1), "IVAN PETROV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void addCard_blankHolder_returns400() throws Exception {
        long userId = createUser("Ivan", "Petrov", "ivan@test.com");

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(cardNumber(1), "")))
                .andExpect(status().isBadRequest());
    }

    // ---------- лимит 5 активных карт ----------

    @Test
    void addCard_sixthActiveCard_returns400() throws Exception {
        long userId = createUser("Ivan", "Petrov", "ivan@test.com");
        for (int i = 0; i < 5; i++) {
            createCard(userId, cardNumber(i));
        }

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(cardNumber(5), "IVAN PETROV")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void addCard_afterDeactivatingOne_limitIsFreedUp() throws Exception {
        long userId = createUser("Ivan", "Petrov", "ivan@test.com");
        long firstCardId = createCard(userId, cardNumber(0));
        for (int i = 1; i < 5; i++) {
            createCard(userId, cardNumber(i));
        }

        // в вашей реализации DELETE деактивирует карту, значит активных становится 4
        mockMvc.perform(delete("/api/cards/{id}", firstCardId))
                .andExpect(status().isNoContent());

        createCard(userId, cardNumber(5)); // ожидаем 201, внутри хелпера
    }

    @Test
    void addCard_limitIsPerUser() throws Exception {
        long first = createUser("Ivan", "Petrov", "ivan@test.com");
        long second = createUser("Anna", "Sidorova", "anna@test.com");
        for (int i = 0; i < 5; i++) {
            createCard(first, cardNumber(i));
        }

        createCard(second, cardNumber(10)); // у второго пользователя своя квота
    }

    // ---------- GET /api/cards/{id} ----------

    @Test
    void getCardById_existing_returns200() throws Exception {
        long userId = createUser("Ivan", "Petrov", "ivan@test.com");
        long cardId = createCard(userId, cardNumber(1));

        mockMvc.perform(get("/api/cards/{id}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.number").value(cardNumber(1)));
    }

    @Test
    void getCardById_unknown_returns404() throws Exception {
        mockMvc.perform(get("/api/cards/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------- GET /api/users/{userId}/cards ----------

    @Test
    void getCardsByUser_returnsOnlyThatUsersCards() throws Exception {
        long first = createUser("Ivan", "Petrov", "ivan@test.com");
        long second = createUser("Anna", "Sidorova", "anna@test.com");
        createCard(first, cardNumber(1));
        createCard(first, cardNumber(2));
        createCard(second, cardNumber(3));

        mockMvc.perform(get("/api/users/{userId}/cards", first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getCardsByUser_unknownUser_returns404() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/cards", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCardsByUser_listIsNotStaleAfterAddingCard() throws Exception {
        long userId = createUser("Ivan", "Petrov", "ivan@test.com");

        // прогреваем кэш пустым списком
        mockMvc.perform(get("/api/users/{userId}/cards", userId))
                .andExpect(jsonPath("$.length()").value(0));

        createCard(userId, cardNumber(1));

        mockMvc.perform(get("/api/users/{userId}/cards", userId))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---------- PUT /api/cards/{id} ----------

    @Test
    void updateCard_existing_changesHolder() throws Exception {
        long userId = createUser("Ivan", "Petrov", "ivan@test.com");
        long cardId = createCard(userId, cardNumber(1));

        mockMvc.perform(put("/api/cards/{id}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(cardNumber(1), "PETR IVANOV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("PETR IVANOV"));

        // после обновления GET не должен отдавать старое значение из кэша
        mockMvc.perform(get("/api/cards/{id}", cardId))
                .andExpect(jsonPath("$.holder").value("PETR IVANOV"));
    }

    @Test
    void updateCard_unknown_returns404() throws Exception {
        mockMvc.perform(put("/api/cards/{id}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(cardNumber(1), "PETR IVANOV")))
                .andExpect(status().isNotFound());
    }

    // ---------- DELETE /api/cards/{id} ----------

    @Test
    void deleteCard_returns204_andCardBecomesInactive() throws Exception {
        long userId = createUser("Ivan", "Petrov", "ivan@test.com");
        long cardId = createCard(userId, cardNumber(1));

        // прогреваем кэш
        mockMvc.perform(get("/api/cards/{id}", cardId))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(delete("/api/cards/{id}", cardId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cards/{id}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deleteCard_unknown_returns404() throws Exception {
        mockMvc.perform(delete("/api/cards/{id}", 999999))
                .andExpect(status().isNotFound());
    }
}