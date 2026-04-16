package ru.soat.ohapp.saas.service.admin;

import ru.soat.ohapp.saas.dto.admin.AdminUserView;
import ru.soat.ohapp.saas.dto.admin.CreateUserRequest;
import ru.soat.ohapp.saas.dto.admin.UpdateUserRequest;

import java.util.List;
import java.util.UUID;

public interface UserAdminService {
    UUID createUser(CreateUserRequest req);
    void updateUser(UUID userId, UpdateUserRequest req);
    void deleteUser(UUID userId);
    List<AdminUserView> listUsers(UUID tenantId, int page, int size);
}
