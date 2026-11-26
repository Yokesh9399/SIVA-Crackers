package com.sivacrackers.catalog.category;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Includes safety notes (shown on product pages). */
    @EntityGraph(attributePaths = "safetyNotes")
    Optional<Category> findBySlug(String slug);

    /** Public listing order: display order, then id for a stable order. */
    List<Category> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();

    boolean existsBySlug(String slug);

    boolean existsByDemoFalse();

    // ---------- Admin (Phase 5) ----------

    /** Every category (active or not) in display order, with safety notes. */
    @EntityGraph(attributePaths = "safetyNotes")
    List<Category> findAllByOrderByDisplayOrderAscIdAsc();

    @EntityGraph(attributePaths = "safetyNotes")
    Optional<Category> findWithNotesById(Long id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsBySlugAndIdNot(String slug, Long id);

    long countByActiveTrue();
}
