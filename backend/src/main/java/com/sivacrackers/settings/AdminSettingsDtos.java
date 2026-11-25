package com.sivacrackers.settings;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Business settings as edited by the OWNER (read: OWNER and MANAGER). */
public final class AdminSettingsDtos {

    private AdminSettingsDtos() {
    }

    static final String PHONE = "^[6-9][0-9]{9}$";
    static final String PHONE_MESSAGE = "must be a 10-digit Indian mobile number (digits only)";

    public record HighlightInput(
            @NotBlank(message = "is required") @Size(max = 80) String title,
            @NotBlank(message = "is required") @Size(max = 300) String text) {
    }

    /**
     * Full replacement of the editable business information. Blank optional fields are stored as
     * empty (null) and shown on the website as "To be updated".
     */
    public record UpdateSettingsRequest(
            @NotBlank(message = "is required") @Size(max = 100) String shopName,
            @Size(max = 200) String tagline,
            @Size(max = 200) String addressLine1,
            @Size(max = 100) String area,
            @Size(max = 100) String city,
            @Size(max = 100) String state,
            @Pattern(regexp = "^$|^[0-9]{6}$", message = "must be a 6-digit pincode") String pincode,
            @Pattern(regexp = "^$|" + PHONE, message = PHONE_MESSAGE) String phone,
            @Pattern(regexp = "^$|" + PHONE, message = PHONE_MESSAGE) String alternatePhone,
            @Pattern(regexp = "^$|" + PHONE, message = PHONE_MESSAGE) String whatsappNumber,
            @NotNull(message = "is required") Boolean whatsappEnquiryEnabled,
            @Email(message = "must be a valid email address") @Size(max = 254) String email,
            @Size(max = 200) String businessHours,
            @Pattern(regexp = "^$|^https://www\\.google\\.com/maps.*$", message = "must be a Google Maps https link")
            @Size(max = 500) String mapEmbedUrl,
            @NotNull(message = "is required") Boolean mapIsApproximate,
            @Size(max = 300) String licenceDetails,
            @Size(max = 300) String announcementText,
            @NotNull(message = "is required") Boolean announcementActive,
            @Size(max = 2000) String aboutText,
            @Size(max = 500) String priceDisclaimer,
            @NotBlank(message = "is required") @Size(max = 1000) String legalNotice,
            @Size(max = 500) String whatsappNotice,
            @NotNull(message = "is required") @Size(max = 8, message = "at most 8 highlights") List<@Valid HighlightInput> highlights,
            @NotNull(message = "is required") @Size(max = 20, message = "at most 20 safety tips")
            List<@NotBlank @Size(max = 300) String> safetyTips) {
    }

    public record AdminSettings(
            String shopName, String tagline, String addressLine1, String area, String city, String state,
            String pincode, String phone, String alternatePhone, String whatsappNumber, boolean whatsappEnquiryEnabled,
            String email, String businessHours, String mapEmbedUrl, boolean mapIsApproximate, String licenceDetails,
            String announcementText, boolean announcementActive, String aboutText, String priceDisclaimer,
            String legalNotice, String whatsappNotice, List<HighlightInput> highlights, List<String> safetyTips,
            Instant updatedAt) {
    }
}
