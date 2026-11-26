package com.sivacrackers.supplier;

import com.sivacrackers.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A company or person the shop buys stock from. Never shown on the public website. */
@Entity
@Table(name = "suppliers")
public class Supplier extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(length = 15)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(length = 300)
    private String address;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "is_demo", nullable = false)
    private boolean demo;

    protected Supplier() {
        // for JPA
    }

    public Supplier(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isDemo() { return demo; }
}
