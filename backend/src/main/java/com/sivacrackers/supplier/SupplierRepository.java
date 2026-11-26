package com.sivacrackers.supplier;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findAllByOrderByActiveDescNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long countByActiveTrue();
}
