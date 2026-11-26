package com.sivacrackers.settings;

import com.sivacrackers.common.exception.InvalidRequestException;
import com.sivacrackers.common.exception.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Homepage slides ("banners") – OWNER only. Links must be pages of this website
 * (the database also enforces this), so a slide can never point to an outside site.
 */
@RestController
@RequestMapping("/api/admin/banners")
@PreAuthorize("hasRole('OWNER')")
public class AdminBannerController {

    public record BannerRequest(
            @Size(max = 80, message = "must be at most 80 characters") String eyebrow,
            @NotBlank(message = "is required") @Size(max = 120, message = "must be at most 120 characters") String title,
            @Size(max = 300, message = "must be at most 300 characters") String subtitle,
            @Size(max = 40, message = "must be at most 40 characters") String ctaLabel,
            @Size(max = 200, message = "must be at most 200 characters")
            @Pattern(regexp = "^$|^/(?!/)[A-Za-z0-9\\-._~/?=&%#]*$", message = "must be a page of this website, starting with /")
            String ctaLink,
            @NotNull(message = "is required") @Pattern(regexp = "^(gold|orange|blue)$", message = "must be gold, orange or blue") String theme,
            @NotNull(message = "is required") @Min(value = 0, message = "must not be negative") @Max(value = 9999, message = "is too large") Integer displayOrder,
            @NotNull(message = "is required") Boolean active) {
    }

    public record AdminBanner(Long id, String eyebrow, String title, String subtitle, String ctaLabel, String ctaLink,
                              String theme, int displayOrder, boolean active, boolean isDemo, Instant updatedAt) {
    }

    private final BannerRepository banners;

    public AdminBannerController(BannerRepository banners) {
        this.banners = banners;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AdminBanner> list() {
        return banners.findAllByOrderByDisplayOrderAscIdAsc().stream().map(AdminBannerController::toAdmin).toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<AdminBanner> create(@Valid @RequestBody BannerRequest body) {
        Banner b = new Banner(body.title().trim(), body.theme());
        apply(b, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(toAdmin(banners.saveAndFlush(b)));
    }

    @PutMapping("/{id}")
    @Transactional
    public AdminBanner update(@PathVariable Long id, @Valid @RequestBody BannerRequest body) {
        Banner b = banners.findById(id).orElseThrow(() -> new NotFoundException("Slide not found."));
        b.setTitle(body.title().trim());
        b.setTheme(body.theme());
        apply(b, body);
        return toAdmin(banners.saveAndFlush(b));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Banner b = banners.findById(id).orElseThrow(() -> new NotFoundException("Slide not found."));
        banners.delete(b);
        return ResponseEntity.noContent().build();
    }

    private static void apply(Banner b, BannerRequest body) {
        String label = blankToNull(body.ctaLabel());
        String link = blankToNull(body.ctaLink());
        if ((label == null) != (link == null)) {
            throw new InvalidRequestException(label == null ? "ctaLabel" : "ctaLink", "is required when a button is shown");
        }
        b.setEyebrow(blankToNull(body.eyebrow()));
        b.setSubtitle(blankToNull(body.subtitle()));
        b.setCtaLabel(label);
        b.setCtaLink(link);
        b.setDisplayOrder(body.displayOrder());
        b.setActive(body.active());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static AdminBanner toAdmin(Banner b) {
        return new AdminBanner(b.getId(), b.getEyebrow(), b.getTitle(), b.getSubtitle(), b.getCtaLabel(), b.getCtaLink(),
                b.getTheme(), b.getDisplayOrder(), b.isActive(), b.isDemo(), b.getUpdatedAt());
    }
}
