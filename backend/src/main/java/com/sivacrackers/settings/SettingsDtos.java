package com.sivacrackers.settings;

import java.util.List;

/**
 * Public business information JSON – the exact shape of the Phase 1–3 settings object, plus
 * {@code demoMode}. Unknown details are null (the website shows "To be updated"); nothing is invented.
 */
public final class SettingsDtos {

    private SettingsDtos() {
    }

    public record Address(String line1, String area, String city, String state, String pincode) {
    }

    public record Announcement(boolean active, String text) {
    }

    public record Highlight(String title, String text) {
    }

    public record PublicSettings(
            String shopName,
            String tagline,
            Address address,
            String phone,
            String alternatePhone,
            String whatsappNumber,
            boolean whatsappEnquiryEnabled,
            String email,
            String businessHours,
            String mapEmbedUrl,
            boolean mapIsApproximate,
            String licenceDetails,
            Announcement announcement,
            String aboutText,
            List<Highlight> highlights,
            String priceDisclaimer,
            String legalNotice,
            String whatsappNotice,
            List<String> safetyTips,
            /** true while any visible product is demonstration data → website shows the demo notice. */
            boolean demoMode) {
    }

    /** Contact details only (GET /api/public/settings/contact). */
    public record Contact(String shopName, Address address, String phone, String alternatePhone,
                          String whatsappNumber, boolean whatsappEnquiryEnabled, String whatsappNotice,
                          String email, String businessHours, String mapEmbedUrl, boolean mapIsApproximate) {
    }

    public record PublicBanner(Long id, String eyebrow, String title, String subtitle, String ctaLabel,
                               String ctaLink, String theme, boolean isDemo) {
    }
}
