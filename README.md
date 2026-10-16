# User Service

Микросервис для управления профилями пользователей и их платёжными картами. Реализует полный вертикальный срез микросервиса: БД, REST API, кэш, тесты, контейнеризацию и CI/CD. Часть архитектуры E-commerce: Internet → API Gateway → User Service → PostgreSQL / Redis.

## Стек

- Java 21+
- Spring Boot 3.4.0+
- Spring Data JPA (Specifications, Derived Queries, JPQL)
- MapStruct (маппинг DTO ↔ Entity)
- Liquibase (версионирование схемы БД)
- PostgreSQL (основная БД)
- Redis (кэш профилей пользователей)
- JUnit 5 + Mockito (unit-тесты)
- Testcontainers (интеграционные тесты)
- Docker (контейнеризация)
- GitHub Actions (CI/CD)

## Архитектура

Сервис слушает порт 8081 (порт 8080 зарезервирован за API Gateway). Все публичные запросы идут через API Gateway, который проверяет JWT.

## REST API

### User endpoints

| Метод | Путь | Описание |
|---|---|---|
| POST | /api/users | Создать пользователя |
| GET | /api/users | Список пользователей (пагинация + фильтр по имени/фамилии) |
| GET | /api/users/{id} | Получить пользователя по ID (кэшируется) |
| PUT | /api/users/{id} | Обновить пользователя |
| DELETE | /api/users/{id} | Удалить пользователя |
| PATCH | /api/users/{id}/status | Активировать / деактивировать пользователя |

### Card endpoints

| Метод | Путь | Описание |
|---|---|---|
| POST | /api/users/{userId}/cards | Добавить карту пользователю |
| GET | /api/users/{userId}/cards | Получить все карты пользователя |
| GET | /api/cards/{cardId} | Получить карту по ID |
| PUT | /api/cards/{cardId} | Обновить карту |
| DELETE | /api/cards/{cardId} | Удалить карту |

### Бизнес-правила

- У одного пользователя может быть не более 5 активных карт. При попытке добавить 6-ю выбрасывается CardLimitExceededException.
- Удаление пользователя каскадно удаляет его карты.
- Активация / деактивация карт и пользователей выполняется через @Modifying JPQL / Native SQL-запросы.
- Все UPDATE / DELETE / CREATE-операции в Service-слое помечены @Transactional.

### Обработка ошибок

- Глобальный @ControllerAdvice (GlobalExceptionHandler).
- Кастомные исключения: UserNotFoundException, CardNotFoundException, CardLimitExceededException.
- Ответы обёрнуты в ResponseEntity с корректными HTTP-статусами: 201 Created, 200 OK, 204 No Content, 400 Bad Request, 404 Not Found, 409 Conflict.

## Структура БД

### users

| Колонка | Тип | Примечание |
|---|---|---|
| id | bigint | PK |
| name | varchar | |
| surname | varchar | |
| birth_date | date | |
| email | varchar | unique |
| active | boolean | |
| created_at | timestamp | аудит |
| updated_at | timestamp | аудит |

### payment_cards

| Колонка | Тип | Примечание |
|---|---|---|
| id | bigint | PK |
| user_id | bigint | FK → users.id |
| number | varchar | хранится в открытом виде (учебный проект) |
| holder | varchar | |
| expiration_date | date | |
| active | boolean | |
| created_at | timestamp | аудит |
| updated_at | timestamp | аудит |

> ⚠️ Дисклеймер. В учебных целях номера карт хранятся в открытом виде. В проде это нарушает PCI-DSS — там хранят только токен платёжного шлюза.

Схема создаётся автоматически через Liquibase (src/main/resources/db/changelog). Аудит (created_at, updated_at) — через JPA Auditing (@CreatedDate, @LastModifiedDate). Индексы добавлены на часто используемые поля (email, user_id, active).

## Кэширование (Redis)

- Кэшируется метод getUserById, getCardById, getCardsByUserId.
- Кэш инвалидируется при: обновлении / удалении пользователя, добавлении / удалении / изменении карты.
- Конфигурация — @Cacheable, @CacheEvict в Service-слое.

## Конфигурация

Все параметры БД и Redis задаются через переменные окружения:
