package com.sivacrackers.settings;

import com.sivacrackers.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A homepage hero slide. The database only allows internal links (paths starting with a single "/"). */
@Entity
@Table(name = "banners")
public class Banner extends BaseEntity {

    @Column(length = 80)
    private String eyebrow;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 300)
    private String subtitle;

    @Column(name = "cta_label", length = 40)
    private String ctaLabel;

    @Column(name = "cta_link", length = 200)
    private String ctaLink;

    /** gold | orange | blue (CHECK constraint ck_banners_theme). */
    @Column(nullable = false, length = 10)
    private String theme = "gold";

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "is_demo", nullable = false)
    private boolean demo;

    protected Banner() {
        // for JPA
    }

    public Banner(String title, String theme) {
        this.title = title;
        this.theme = theme;
    }

    public String getEyebrow() { return eyebrow; }
    public void setEyebrow(String eyebrow) { this.eyebrow = eyebrow; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getCtaLabel() { return ctaLabel; }
    public void setCtaLabel(String ctaLabel) { this.ctaLabel = ctaLabel; }
    public String getCtaLink() { return ctaLink; }
    public void setCtaLink(String ctaLink) { this.ctaLink = ctaLink; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
}
