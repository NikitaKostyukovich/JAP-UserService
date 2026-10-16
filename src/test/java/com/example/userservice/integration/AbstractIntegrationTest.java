package com.example.userservice.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import com.example.userservice.repository.CardRepository;
import com.example.userservice.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Общая база для интеграционных тестов: настоящие Postgres и Redis в Docker.
 * Контейнеры стартуют один раз на весь прогон тестов.
 * Для запуска нужен работающий Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    // Эти значения перекрывают ${DB_URL} и т.д. из application.yaml, поэтому .env в тестах не нужен
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected CardRepository cardRepository;
    @Autowired
    private ObjectProvider<CacheManager> cacheManager;

    @BeforeEach
    void cleanUp() {
        cardRepository.deleteAll();
        userRepository.deleteAll();
        cacheManager.ifAvailable(cm -> cm.getCacheNames().forEach(name -> {
            Cache cache = cm.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }));
    }

    // ---------- JSON-хелперы ----------
    // Если у ваших DTO другие имена полей, поправьте их только здесь.

    protected String userJson(String name, String surname, String email) {
        return """
                {"name":"%s","surname":"%s","birthDate":"1999-05-20","email":"%s"}
                """.formatted(name, surname, email);
    }

    protected String cardJson(String number, String holder) {
        return """
                {"number":"%s","holder":"%s","expirationDate":"%s"}
                """.formatted(number, holder, LocalDate.now().plusYears(3));
    }

    /** Уникальный 16-значный номер карты по индексу. */
    protected String cardNumber(int i) {
        return String.format("4000000000000%03d", i);
    }

    // ---------- хелперы создания данных через API ----------

    protected long createUser(String name, String surname, String email) throws Exception {
        String body = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(name, surname, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected long createCard(long userId, String number) throws Exception {
        String body = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(number, "IVAN PETROV")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}