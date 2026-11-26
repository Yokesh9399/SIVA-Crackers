package com.sivacrackers.catalog.brand;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findBySlug(String slug);

    List<Brand> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();

    boolean existsBySlug(String slug);

    boolean existsByDemoFalse();

    // ---------- Admin (Phase 5) ----------

    List<Brand> findAllByOrderByDisplayOrderAscIdAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsBySlugAndIdNot(String slug, Long id);

    long countByActiveTrue();
}
