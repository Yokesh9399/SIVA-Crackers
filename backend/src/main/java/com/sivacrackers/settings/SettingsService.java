package com.sivacrackers.settings;

import com.sivacrackers.catalog.product.ProductRepository;
import com.sivacrackers.common.exception.NotFoundException;
import com.sivacrackers.settings.SettingsDtos.Address;
import com.sivacrackers.settings.SettingsDtos.Announcement;
import com.sivacrackers.settings.SettingsDtos.Contact;
import com.sivacrackers.settings.SettingsDtos.Highlight;
import com.sivacrackers.settings.SettingsDtos.PublicBanner;
import com.sivacrackers.settings.SettingsDtos.PublicSettings;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public business information, contact details, safety tips and homepage banners. */
@Service
@Transactional(readOnly = true)
public class SettingsService {

    private final BusinessSettingsRepository settings;
    private final BannerRepository banners;
    private final ProductRepository products;

    public SettingsService(BusinessSettingsRepository settings, BannerRepository banners, ProductRepository products) {
        this.settings = settings;
        this.banners = banners;
        this.products = products;
    }

    public PublicSettings publicSettings() {
        BusinessSettings s = load();
        return new PublicSettings(
                s.getShopName(), s.getTagline(), address(s),
                s.getPhone(), s.getAlternatePhone(), s.getWhatsappNumber(), s.isWhatsappEnquiryEnabled(),
                s.getEmail(), s.getBusinessHours(), s.getMapEmbedUrl(), s.isMapIsApproximate(), s.getLicenceDetails(),
                new Announcement(s.isAnnouncementActive(), s.getAnnouncementText()),
                s.getAboutText(),
                s.getHighlights().stream().map(h -> new Highlight(h.getTitle(), h.getText())).toList(),
                s.getPriceDisclaimer(), s.getLegalNotice(), s.getWhatsappNotice(),
                List.copyOf(s.getSafetyTips()),
                products.anyVisibleDemoProduct());
    }

    public Contact contact() {
        BusinessSettings s = load();
        return new Contact(s.getShopName(), address(s), s.getPhone(), s.getAlternatePhone(), s.getWhatsappNumber(),
                s.isWhatsappEnquiryEnabled(), s.getWhatsappNotice(), s.getEmail(), s.getBusinessHours(),
                s.getMapEmbedUrl(), s.isMapIsApproximate());
    }

    public List<String> safetyTips() {
        return List.copyOf(load().getSafetyTips());
    }

    public List<PublicBanner> banners() {
        return banners.findAllByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(b -> new PublicBanner(b.getId(), b.getEyebrow(), b.getTitle(), b.getSubtitle(), b.getCtaLabel(),
                        b.getCtaLink(), b.getTheme(), b.isDemo()))
                .toList();
    }

    private BusinessSettings load() {
        // The row is created by migration V2; missing means the database was not migrated.
        return settings.findSingleton().orElseThrow(() -> new NotFoundException("Business settings not found"));
    }

    private static Address address(BusinessSettings s) {
        return new Address(s.getAddressLine1(), s.getArea(), s.getCity(), s.getState(), s.getPincode());
    }
}
