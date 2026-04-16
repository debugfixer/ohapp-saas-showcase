package ru.soat.ohapp.saas.service.admin;

import ru.soat.ohapp.saas.dto.admin.LicenseStatusRequest;
import ru.soat.ohapp.saas.dto.admin.LicenseUpsertRequest;
import ru.soat.ohapp.saas.dto.admin.LicenseView;

import java.util.List;
import java.util.UUID;

public interface LicenseService {
    LicenseView upsert(LicenseUpsertRequest req);
    void changeStatus(UUID licenseId, LicenseStatusRequest req);
    List<LicenseView> listByTenant(UUID tenantId);
    LicenseView getActive(UUID tenantId);
    void autoExpireAndActivate();
}
