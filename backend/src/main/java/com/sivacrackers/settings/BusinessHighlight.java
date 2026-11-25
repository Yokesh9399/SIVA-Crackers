package com.sivacrackers.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/** One "What makes us different" card on the homepage and About page. */
@Embeddable
public class BusinessHighlight {

    @Column(name = "title", nullable = false, length = 80)
    private String title;

    @Column(name = "text", nullable = false, length = 300)
    private String text;

    protected BusinessHighlight() {
        // for JPA
    }

    public BusinessHighlight(String title, String text) {
        this.title = title;
        this.text = text;
    }

    public String getTitle() { return title; }
    public String getText() { return text; }

    @Override
    public boolean equals(Object o) {
        return o instanceof BusinessHighlight other && Objects.equals(title, other.title) && Objects.equals(text, other.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, text);
    }
}
