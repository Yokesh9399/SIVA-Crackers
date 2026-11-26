package com.sivacrackers.inventory;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

    long countBySupplierId(Long supplierId);

    /** Deliveries received from one supplier (stock in), with units and recorded purchase value. */
    @Query("""
            select count(m) as deliveries, coalesce(sum(m.quantityChange), 0) as units,
                   coalesce(sum(m.quantityChange * m.unitCost), 0) as purchaseValue,
                   max(m.createdAt) as lastDeliveryAt
            from StockMovement m
            where m.supplier.id = :supplierId and m.type = com.sivacrackers.inventory.MovementType.STOCK_IN""")
    SupplierTotals totalsForSupplier(@Param("supplierId") Long supplierId);

    interface SupplierTotals {
        long getDeliveries();
        long getUnits();
        BigDecimal getPurchaseValue();
        java.time.Instant getLastDeliveryAt();
    }
}
