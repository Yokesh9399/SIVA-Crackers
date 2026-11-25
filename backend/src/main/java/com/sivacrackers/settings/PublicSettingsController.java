package com.sivacrackers.settings;

import com.sivacrackers.settings.SettingsDtos.Contact;
import com.sivacrackers.settings.SettingsDtos.PublicBanner;
import com.sivacrackers.settings.SettingsDtos.PublicSettings;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public business information (no login). Editing is an OWNER-only admin feature. */
@RestController
@RequestMapping("/api/public")
public class PublicSettingsController {

    private final SettingsService service;

    public PublicSettingsController(SettingsService service) {
        this.service = service;
    }

    /** Everything the website needs: business details, notices, highlights, safety tips, demoMode. */
    @GetMapping("/settings")
    public PublicSettings settings() {
        return service.publicSettings();
    }

    @GetMapping("/settings/contact")
    public Contact contact() {
        return service.contact();
    }

    @GetMapping("/settings/safety-tips")
    public List<String> safetyTips() {
        return service.safetyTips();
    }

    @GetMapping("/banners")
    public List<PublicBanner> banners() {
        return service.banners();
    }
}
