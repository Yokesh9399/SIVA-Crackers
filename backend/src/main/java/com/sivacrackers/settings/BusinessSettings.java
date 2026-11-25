package com.sivacrackers.settings;

import com.sivacrackers.common.entity.AuditedEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * The shop's business information – exactly one row (id = 1), created by migration V2.
 * Every unknown detail is null and shown on the website as "To be updated"; nothing is invented.
 */
@Entity
@Table(name = "business_settings")
public class BusinessSettings extends AuditedEntity {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "shop_name", nullable = false, length = 100)
    private String shopName;

    @Column(length = 200)
    private String tagline;

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(length = 100)
    private String area;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 10)
    private String pincode;

    @Column(length = 20)
    private String phone;

    @Column(name = "alternate_phone", length = 20)
    private String alternatePhone;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Column(name = "whatsapp_enquiry_enabled", nullable = false)
    private boolean whatsappEnquiryEnabled = true;

    @Column(length = 254)
    private String email;

    @Column(name = "business_hours", length = 200)
    private String businessHours;

    @Column(name = "map_embed_url", length = 500)
    private String mapEmbedUrl;

    @Column(name = "map_is_approximate", nullable = false)
    private boolean mapIsApproximate = true;

    @Column(name = "licence_details", length = 300)
    private String licenceDetails;

    @Column(name = "announcement_text", length = 300)
    private String announcementText;

    @Column(name = "announcement_active", nullable = false)
    private boolean announcementActive;

    @Column(name = "about_text", length = 2000)
    private String aboutText;

    @Column(name = "price_disclaimer", length = 500)
    private String priceDisclaimer;

    @Column(name = "legal_notice", length = 1000)
    private String legalNotice;

    @Column(name = "whatsapp_notice", length = 500)
    private String whatsappNotice;

    @ElementCollection
    @CollectionTable(name = "business_highlights", joinColumns = @JoinColumn(name = "settings_id"))
    @OrderColumn(name = "position")
    private List<BusinessHighlight> highlights = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "safety_tips", joinColumns = @JoinColumn(name = "settings_id"))
    @OrderColumn(name = "position")
    @Column(name = "text", nullable = false, length = 300)
    private List<String> safetyTips = new ArrayList<>();

    protected BusinessSettings() {
        // for JPA
    }

    public Long getId() { return id; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAlternatePhone() { return alternatePhone; }
    public void setAlternatePhone(String alternatePhone) { this.alternatePhone = alternatePhone; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String whatsappNumber) { this.whatsappNumber = whatsappNumber; }
    public boolean isWhatsappEnquiryEnabled() { return whatsappEnquiryEnabled; }
    public void setWhatsappEnquiryEnabled(boolean v) { this.whatsappEnquiryEnabled = v; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getBusinessHours() { return businessHours; }
    public void setBusinessHours(String businessHours) { this.businessHours = businessHours; }
    public String getMapEmbedUrl() { return mapEmbedUrl; }
    public void setMapEmbedUrl(String mapEmbedUrl) { this.mapEmbedUrl = mapEmbedUrl; }
    public boolean isMapIsApproximate() { return mapIsApproximate; }
    public void setMapIsApproximate(boolean mapIsApproximate) { this.mapIsApproximate = mapIsApproximate; }
    public String getLicenceDetails() { return licenceDetails; }
    public void setLicenceDetails(String licenceDetails) { this.licenceDetails = licenceDetails; }
    public String getAnnouncementText() { return announcementText; }
    public void setAnnouncementText(String announcementText) { this.announcementText = announcementText; }
    public boolean isAnnouncementActive() { return announcementActive; }
    public void setAnnouncementActive(boolean announcementActive) { this.announcementActive = announcementActive; }
    public String getAboutText() { return aboutText; }
    public void setAboutText(String aboutText) { this.aboutText = aboutText; }
    public String getPriceDisclaimer() { return priceDisclaimer; }
    public void setPriceDisclaimer(String priceDisclaimer) { this.priceDisclaimer = priceDisclaimer; }
    public String getLegalNotice() { return legalNotice; }
    public void setLegalNotice(String legalNotice) { this.legalNotice = legalNotice; }
    public String getWhatsappNotice() { return whatsappNotice; }
    public void setWhatsappNotice(String whatsappNotice) { this.whatsappNotice = whatsappNotice; }
    public List<BusinessHighlight> getHighlights() { return highlights; }
    public List<String> getSafetyTips() { return safetyTips; }
}
