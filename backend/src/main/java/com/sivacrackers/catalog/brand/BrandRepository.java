package com.sivacrackers.catalog.brand;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findBySlug(String slug);

    List<Brand> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();

    boolean existsBySlug(String slug);

    boolean existsByDemoFalse();
}
