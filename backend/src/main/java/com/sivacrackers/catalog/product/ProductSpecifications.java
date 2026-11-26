package com.sivacrackers.catalog.product;

import com.sivacrackers.catalog.brand.Brand;
import com.sivacrackers.catalog.category.Category;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Database filters for the public catalogue.
 *
 * <p>"Publicly visible" = product active AND visible on website AND its category and brand active.
 * Text search: every word must appear in the product, brand or category name (case- and
 * accent-insensitive thanks to the utf8mb4_0900_ai_ci collation); LIKE wildcards in user input are escaped.
 */
public final class ProductSpecifications {

    private static final char ESCAPE = '\\';

    private ProductSpecifications() {
    }

    /** Full catalogue query (search, slugs, price, stock). */
    public static Specification<Product> catalogue(ProductQuery q) {
        return (root, query, cb) -> {
            Joins j = joins(root, query.getResultType());
            List<Predicate> where = visible(root, j, cb);
            for (String word : q.searchWords()) {
                String pattern = "%" + escapeLike(word) + "%";
                where.add(cb.or(
                        cb.like(root.get("name"), pattern, ESCAPE),
                        cb.like(j.brand.get("name"), pattern, ESCAPE),
                        cb.like(j.category.get("name"), pattern, ESCAPE)));
            }
            if (!q.categories().isEmpty()) {
                where.add(j.category.get("slug").in(q.categories()));
            }
            if (!q.brands().isEmpty()) {
                where.add(j.brand.get("slug").in(q.brands()));
            }
            if (q.minPrice() != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("price"), q.minPrice()));
            }
            if (q.maxPrice() != null) {
                where.add(cb.lessThanOrEqualTo(root.get("price"), q.maxPrice()));
            }
            if (q.inStock()) {
                where.add(cb.greaterThan(root.get("stockQuantity"), 0));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }

    /** Visible products only, optionally limited to featured ones. */
    public static Specification<Product> visibleProducts(boolean featuredOnly) {
        return (root, query, cb) -> {
            Joins j = joins(root, query.getResultType());
            List<Predicate> where = visible(root, j, cb);
            if (featuredOnly) {
                where.add(cb.isTrue(root.get("featured")));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }

    /** Visible products in one category / of one brand, excluding a product. */
    public static Specification<Product> related(Long categoryId, Long brandId, Long excludeProductId,
                                                 boolean sameCategory) {
        return (root, query, cb) -> {
            Joins j = joins(root, query.getResultType());
            List<Predicate> where = visible(root, j, cb);
            where.add(cb.notEqual(root.get("id"), excludeProductId));
            if (sameCategory) {
                where.add(cb.equal(j.category.get("id"), categoryId));
            } else {
                where.add(cb.equal(j.brand.get("id"), brandId));
                where.add(cb.notEqual(j.category.get("id"), categoryId));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }

    private record Joins(Join<Product, Category> category, Join<Product, Brand> brand) {
    }

    /**
     * Fetch-join category and brand for data queries (one SQL statement per page, no N+1);
     * plain joins for the count query (fetching is not allowed there).
     */
    @SuppressWarnings("unchecked")
    private static Joins joins(Root<Product> root, Class<?> resultType) {
        boolean countQuery = Long.class.equals(resultType) || long.class.equals(resultType);
        if (countQuery) {
            return new Joins(root.join("category", JoinType.INNER), root.join("brand", JoinType.INNER));
        }
        // Hibernate's fetch objects are also joins, so filters and sorting can use them directly.
        return new Joins(
                (Join<Product, Category>) (Object) root.fetch("category", JoinType.INNER),
                (Join<Product, Brand>) (Object) root.fetch("brand", JoinType.INNER));
    }

    private static List<Predicate> visible(Root<Product> root, Joins j, CriteriaBuilder cb) {
        List<Predicate> where = new ArrayList<>();
        where.add(cb.isTrue(root.get("active")));
        where.add(cb.isTrue(root.get("visibleOnWebsite")));
        where.add(cb.isTrue(j.category.get("active")));
        where.add(cb.isTrue(j.brand.get("active")));
        return where;
    }

    /** Escapes LIKE wildcards so a search for "50%" or "a_b" matches literally. */
    public static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
