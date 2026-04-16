package ru.soat.ohapp.saas.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.soat.ohapp.saas.dto.admin.AdminUserView;
import ru.soat.ohapp.saas.dto.admin.CreateUserRequest;
import ru.soat.ohapp.saas.dto.admin.UpdateUserRequest;
import ru.soat.ohapp.saas.service.admin.UserAdminService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserAdminService service;

    @PostMapping
    public ResponseEntity<UUID> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.ok(service.createUser(req));
    }

    @PutMapping("{id}")
    public ResponseEntity<Void> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req) {
        service.updateUser(id, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<AdminUserView>> list(@RequestParam UUID tenantId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listUsers(tenantId, page, size));
    }
}
