package com.sivacrackers.settings;

import com.sivacrackers.common.exception.NotFoundException;
import com.sivacrackers.settings.AdminSettingsDtos.AdminSettings;
import com.sivacrackers.settings.AdminSettingsDtos.HighlightInput;
import com.sivacrackers.settings.AdminSettingsDtos.UpdateSettingsRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read (OWNER, MANAGER) and update (OWNER) the business information. Real details are entered only by the owner. */
@Service
public class AdminSettingsService {

    private final BusinessSettingsRepository settings;

    public AdminSettingsService(BusinessSettingsRepository settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public AdminSettings get() {
        return toDto(load());
    }

    @Transactional
    public AdminSettings update(UpdateSettingsRequest r) {
        BusinessSettings s = load();
        s.setShopName(r.shopName().trim());
        s.setTagline(blankToNull(r.tagline()));
        s.setAddressLine1(blankToNull(r.addressLine1()));
        s.setArea(blankToNull(r.area()));
        s.setCity(blankToNull(r.city()));
        s.setState(blankToNull(r.state()));
        s.setPincode(blankToNull(r.pincode()));
        s.setPhone(blankToNull(r.phone()));
        s.setAlternatePhone(blankToNull(r.alternatePhone()));
        s.setWhatsappNumber(blankToNull(r.whatsappNumber()));
        s.setWhatsappEnquiryEnabled(r.whatsappEnquiryEnabled());
        s.setEmail(blankToNull(r.email()));
        s.setBusinessHours(blankToNull(r.businessHours()));
        s.setMapEmbedUrl(blankToNull(r.mapEmbedUrl()));
        s.setMapIsApproximate(r.mapIsApproximate());
        s.setLicenceDetails(blankToNull(r.licenceDetails()));
        s.setAnnouncementText(blankToNull(r.announcementText()));
        s.setAnnouncementActive(r.announcementActive());
        s.setAboutText(blankToNull(r.aboutText()));
        s.setPriceDisclaimer(blankToNull(r.priceDisclaimer()));
        s.setLegalNotice(r.legalNotice().trim());
        s.setWhatsappNotice(blankToNull(r.whatsappNotice()));
        s.getHighlights().clear();
        r.highlights().forEach(h -> s.getHighlights().add(new BusinessHighlight(h.title().trim(), h.text().trim())));
        s.getSafetyTips().clear();
        r.safetyTips().forEach(t -> s.getSafetyTips().add(t.trim()));
        settings.flush();
        return toDto(s);
    }

    private BusinessSettings load() {
        return settings.findSingleton().orElseThrow(() -> new NotFoundException("Business settings not found"));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static AdminSettings toDto(BusinessSettings s) {
        return new AdminSettings(s.getShopName(), s.getTagline(), s.getAddressLine1(), s.getArea(), s.getCity(),
                s.getState(), s.getPincode(), s.getPhone(), s.getAlternatePhone(), s.getWhatsappNumber(),
                s.isWhatsappEnquiryEnabled(), s.getEmail(), s.getBusinessHours(), s.getMapEmbedUrl(),
                s.isMapIsApproximate(), s.getLicenceDetails(), s.getAnnouncementText(), s.isAnnouncementActive(),
                s.getAboutText(), s.getPriceDisclaimer(), s.getLegalNotice(), s.getWhatsappNotice(),
                s.getHighlights().stream().map(h -> new HighlightInput(h.getTitle(), h.getText())).toList(),
                java.util.List.copyOf(s.getSafetyTips()), s.getUpdatedAt());
    }
}
