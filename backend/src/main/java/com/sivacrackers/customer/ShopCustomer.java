package com.sivacrackers.customer;

import com.sivacrackers.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A contact record kept by the shop (not a website account). Staff-only personal data:
 * never exposed by a public API, no purchase data (in-store billing is not approved).
 */
@Entity
@Table(name = "shop_customers")
public class ShopCustomer extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 15)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(length = 300)
    private String address;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    protected ShopCustomer() {
        // for JPA
    }

    public ShopCustomer(String fullName, Long createdBy) {
        this.fullName = fullName;
        this.createdBy = createdBy;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
