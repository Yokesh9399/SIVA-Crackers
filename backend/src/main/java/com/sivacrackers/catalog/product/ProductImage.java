package com.sivacrackers.catalog.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/** One product photo. Position in the product's list = display order (first = primary image). */
@Embeddable
public class ProductImage {

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "alt_text", length = 200)
    private String altText;

    protected ProductImage() {
        // for JPA
    }

    public ProductImage(String url, String altText) {
        this.url = url;
        this.altText = altText;
    }

    public String getUrl() { return url; }
    public String getAltText() { return altText; }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProductImage other && Objects.equals(url, other.url) && Objects.equals(altText, other.altText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, altText);
    }
}
