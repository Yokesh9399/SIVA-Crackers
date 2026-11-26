package com.sivacrackers.billing;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findAllByOrderByDisplayOrderAscIdAsc();

    Optional<PaymentMethod> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByLabelIgnoreCase(String label);

    boolean existsByLabelIgnoreCaseAndIdNot(String label, Long id);
}
