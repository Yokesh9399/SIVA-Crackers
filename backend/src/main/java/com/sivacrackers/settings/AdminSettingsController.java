package com.sivacrackers.settings;

import com.sivacrackers.settings.AdminSettingsDtos.AdminSettings;
import com.sivacrackers.settings.AdminSettingsDtos.UpdateSettingsRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Business information management. Read: OWNER, MANAGER. Change: OWNER only. */
@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    private final AdminSettingsService service;

    public AdminSettingsController(AdminSettingsService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public AdminSettings get() {
        return service.get();
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public AdminSettings update(@Valid @RequestBody UpdateSettingsRequest body) {
        return service.update(body);
    }
}
