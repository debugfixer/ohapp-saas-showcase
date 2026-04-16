package ru.soat.ohapp.saas.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.soat.ohapp.saas.dto.admin.LicenseStatusRequest;
import ru.soat.ohapp.saas.dto.admin.LicenseUpsertRequest;
import ru.soat.ohapp.saas.dto.admin.LicenseView;
import ru.soat.ohapp.saas.service.admin.LicenseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/licenses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLicenseController {

    private final LicenseService service;

    @PostMapping
    public ResponseEntity<LicenseView> upsert(@Valid @RequestBody LicenseUpsertRequest req) {
        return ResponseEntity.ok(service.upsert(req));
    }

    @PostMapping("{id}/status")
    public ResponseEntity<Void> changeStatus(@PathVariable UUID id, @Valid @RequestBody LicenseStatusRequest req) {
        service.changeStatus(id, req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<LicenseView>> listByTenant(@RequestParam UUID tenantId) {
        return ResponseEntity.ok(service.listByTenant(tenantId));
    }

    @GetMapping("active")
    public ResponseEntity<LicenseView> active(@RequestParam UUID tenantId) {
        return ResponseEntity.ok(service.getActive(tenantId));
    }
}
