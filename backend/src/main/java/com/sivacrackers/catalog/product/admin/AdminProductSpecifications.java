package com.sivacrackers.catalog.product.admin;

import com.sivacrackers.catalog.brand.Brand;
import com.sivacrackers.catalog.category.Category;
import com.sivacrackers.catalog.product.Product;
import com.sivacrackers.catalog.product.ProductSpecifications;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Admin product list filters (all products, any status). Search input is bound as parameters with LIKE wildcards escaped. */
public final class AdminProductSpecifications {

    private static final char ESCAPE = '\\';

    private AdminProductSpecifications() {
    }

    @SuppressWarnings("unchecked")
    public static Specification<Product> matching(AdminProductQuery q) {
        return (root, query, cb) -> {
            boolean count = Long.class.equals(query.getResultType()) || long.class.equals(query.getResultType());
            Join<Product, Category> category = count ? root.join("category", JoinType.INNER)
                    : (Join<Product, Category>) (Object) root.fetch("category", JoinType.INNER);
            Join<Product, Brand> brand = count ? root.join("brand", JoinType.INNER)
                    : (Join<Product, Brand>) (Object) root.fetch("brand", JoinType.INNER);

            List<Predicate> where = new ArrayList<>();
            for (String word : q.searchWords()) {
                String pattern = "%" + ProductSpecifications.escapeLike(word) + "%";
                where.add(cb.or(cb.like(root.get("name"), pattern, ESCAPE), cb.like(root.get("sku"), pattern, ESCAPE)));
            }
            if (q.categoryId() != null) {
                where.add(cb.equal(category.get("id"), q.categoryId()));
            }
            if (q.brandId() != null) {
                where.add(cb.equal(brand.get("id"), q.brandId()));
            }
            if (q.availability() != null) {
                Expression<Integer> stock = root.get("stockQuantity");
                Expression<Integer> reorder = root.get("reorderLevel");
                where.add(switch (q.availability()) {
                    case OUT_OF_STOCK -> cb.lessThanOrEqualTo(stock, 0);
                    case LOW_STOCK -> cb.and(cb.greaterThan(stock, 0), cb.lessThanOrEqualTo(stock, reorder));
                    case IN_STOCK -> cb.greaterThan(stock, reorder);
                });
            }
            if (q.visibility() != null) {
                where.add(switch (q.visibility()) {
                    case VISIBLE -> cb.and(cb.isTrue(root.get("active")), cb.isTrue(root.get("visibleOnWebsite")));
                    case HIDDEN -> cb.and(cb.isTrue(root.get("active")), cb.isFalse(root.get("visibleOnWebsite")));
                    case INACTIVE -> cb.isFalse(root.get("active"));
                });
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }
}
