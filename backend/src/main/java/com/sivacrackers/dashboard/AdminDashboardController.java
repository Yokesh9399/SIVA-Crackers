package com.sivacrackers.dashboard;

import com.sivacrackers.catalog.brand.BrandRepository;
import com.sivacrackers.catalog.category.Category;
import com.sivacrackers.catalog.category.CategoryRepository;
import com.sivacrackers.catalog.product.ProductRepository;
import com.sivacrackers.catalog.product.ProductRepository.CountByParent;
import com.sivacrackers.catalog.product.ProductSort;
import com.sivacrackers.catalog.product.admin.AdminProductDtos.AdminProductRow;
import com.sivacrackers.catalog.product.admin.AdminProductService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard figures, all counted live from the database (catalogue, staff, suppliers, expenses). There are
 * deliberately no sales, order or revenue figures: nothing is sold online and in-store billing is not approved.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class AdminDashboardController {

    public record CategoryCount(Long categoryId, String name, String slug, boolean active, long productCount) {
    }

    public record Dashboard(
            long totalProducts, long activeProducts, long inactiveProducts, long visibleProducts, long hiddenProducts,
            long featuredProducts, long lowStockProducts, long outOfStockProducts, long demoProducts,
            long totalCategories, long activeCategories, long totalBrands, long activeBrands,
            long totalStaff, long activeStaff, long activeSuppliers, java.math.BigDecimal expensesThisMonth,
            List<CategoryCount> productsByCategory, List<AdminProductRow> recentProducts) {
    }

    private static final java.util.Set<com.sivacrackers.user.Role> STAFF = java.util.EnumSet.of(
            com.sivacrackers.user.Role.OWNER, com.sivacrackers.user.Role.MANAGER, com.sivacrackers.user.Role.CASHIER);

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final com.sivacrackers.user.UserRepository users;
    private final com.sivacrackers.supplier.SupplierRepository suppliers;
    private final com.sivacrackers.expense.ExpenseService expenses;

    public AdminDashboardController(ProductRepository products, CategoryRepository categories, BrandRepository brands,
                                    com.sivacrackers.user.UserRepository users, com.sivacrackers.supplier.SupplierRepository suppliers,
                                    com.sivacrackers.expense.ExpenseService expenses) {
        this.products = products;
        this.categories = categories;
        this.brands = brands;
        this.users = users;
        this.suppliers = suppliers;
        this.expenses = expenses;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Dashboard dashboard() {
        Map<Long, Long> perCategory = products.countAllByCategory().stream()
                .collect(Collectors.toMap(CountByParent::getParentId, CountByParent::getProductCount));
        List<CategoryCount> byCategory = categories.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map((Category c) -> new CategoryCount(c.getId(), c.getName(), c.getSlug(), c.isActive(),
                        perCategory.getOrDefault(c.getId(), 0L)))
                .sorted(Comparator.comparingLong(CategoryCount::productCount).reversed())
                .toList();
        List<AdminProductRow> recent = products.findAll(PageRequest.of(0, 5, ProductSort.NEWEST.sort()))
                .map(AdminProductService::toRow).getContent();
        return new Dashboard(
                products.count(), products.countByActiveTrue(), products.countByActiveFalse(),
                products.countByActiveTrueAndVisibleOnWebsiteTrue(), products.countByActiveTrueAndVisibleOnWebsiteFalse(),
                products.countByActiveTrueAndFeaturedTrue(), products.countActiveLowStock(), products.countActiveOutOfStock(),
                products.countByDemoTrue(), categories.count(), categories.countByActiveTrue(), brands.count(),
                brands.countByActiveTrue(), users.countByRoleIn(STAFF), users.countByRoleInAndActiveTrue(STAFF),
                suppliers.countByActiveTrue(), expenses.thisMonthTotal(), byCategory, recent);
    }
}
