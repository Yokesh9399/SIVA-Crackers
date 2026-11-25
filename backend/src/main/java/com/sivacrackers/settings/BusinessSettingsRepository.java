package com.sivacrackers.settings;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessSettingsRepository extends JpaRepository<BusinessSettings, Long> {

    /** The single settings row (created by migration V2) with its highlights. */
    @EntityGraph(attributePaths = "highlights")
    Optional<BusinessSettings> findWithHighlightsById(Long id);

    default Optional<BusinessSettings> findSingleton() {
        return findWithHighlightsById(BusinessSettings.SINGLETON_ID);
    }
}
