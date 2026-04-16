package ru.soat.ohapp.saas.domain;

import java.time.Instant;

public record UserDto(long id, String email, String name, Instant createdAt) {
    // Совместимость с контроллерами, которые ожидают "get*"
    public long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
}
