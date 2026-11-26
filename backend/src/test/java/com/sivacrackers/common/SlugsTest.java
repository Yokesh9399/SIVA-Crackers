package com.sivacrackers.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlugsTest {

    @Test
    @DisplayName("Slugs: lower-case words joined by single hyphens, accents and symbols removed")
    void slugs() {
        assertThat(Slugs.from("Flower Pots – Deluxe", 100)).isEqualTo("flower-pots-deluxe");
        assertThat(Slugs.from("  10 cm Électric  Sparklers!! ", 100)).isEqualTo("10-cm-electric-sparklers");
        assertThat(Slugs.from("Sky & Ground", 100)).isEqualTo("sky-and-ground");
        assertThat(Slugs.from("<script>alert(1)</script>", 100)).isEqualTo("script-alert-1-script");
        assertThat(Slugs.from("!!!", 100)).isEmpty();
        assertThat(Slugs.from("one two three four", 9)).isEqualTo("one-two");
        assertThat(Slugs.from("abcdefghijkl", 5)).isEqualTo("abcde");
        assertThat(Slugs.from("abc def", 7)).matches(Slugs.PATTERN);
    }
}
