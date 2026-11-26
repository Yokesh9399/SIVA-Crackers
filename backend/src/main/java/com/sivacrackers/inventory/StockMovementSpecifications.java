package com.sivacrackers.inventory;

import com.sivacrackers.catalog.product.ProductSpecifications;
import com.sivacrackers.common.ShopTime;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Filters for the stock movement history and report (all values are bound parameters). */
public final class StockMovementSpecifications {

    private StockMovementSpecifications() {
    }

    public record Filter(ShopTime.Range range, MovementType type, MovementReason reason, Long productId, Long supplierId,
                         String search) {
    }

    public static Specification<StockMovement> matching(Filter f) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (f.range() != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.range().startInstant()));
                where.add(cb.lessThan(root.get("createdAt"), f.range().endInstantExclusive()));
            }
            if (f.type() != null) {
                where.add(cb.equal(root.get("type"), f.type()));
            }
            if (f.reason() != null) {
                where.add(cb.equal(root.get("reason"), f.reason()));
            }
            if (f.productId() != null) {
                where.add(cb.equal(root.get("productId"), f.productId()));
            }
            if (f.supplierId() != null) {
                where.add(cb.equal(root.get("supplier").get("id"), f.supplierId()));
            }
            if (f.search() != null && !f.search().isBlank()) {
                String pattern = "%" + ProductSpecifications.escapeLike(f.search().trim()) + "%";
                where.add(cb.or(cb.like(root.get("productName"), pattern, '\\'), cb.like(root.get("productSku"), pattern, '\\')));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }
}
