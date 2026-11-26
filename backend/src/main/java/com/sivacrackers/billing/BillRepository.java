package com.sivacrackers.billing;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillRepository extends JpaRepository<Bill, Long>, JpaSpecificationExecutor<Bill> {

    @EntityGraph(attributePaths = {"items", "paymentMethod"})
    Optional<Bill> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"items", "paymentMethod"})
    java.util.List<Bill> findTop5ByOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = {"items", "paymentMethod"})
    Optional<Bill> findByRequestKey(String requestKey);

    /** Locks the bill row (voiding, payment updates) so two staff cannot change it at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bill b where b.id = :id")
    Optional<Bill> findForUpdateById(@Param("id") Long id);
}
