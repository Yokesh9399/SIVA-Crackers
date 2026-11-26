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
            long totalStaff, long activeStaff, long totalSuppliers, long activeSuppliers, java.math.BigDecimal expensesThisMonth,
            long billsToday, java.math.BigDecimal revenueToday, long billsThisMonth, java.math.BigDecimal revenueThisMonth,
            List<CategoryCount> productsByCategory, List<AdminProductRow> recentProducts,
            List<com.sivacrackers.inventory.InventoryDtos.MovementDto> recentMovements,
            List<com.sivacrackers.expense.ExpenseDtos.ExpenseDto> recentExpenses,
            List<com.sivacrackers.billing.BillingDtos.BillRow> recentBills) {
    }

    private static final java.util.Set<com.sivacrackers.user.Role> STAFF = java.util.EnumSet.of(
            com.sivacrackers.user.Role.OWNER, com.sivacrackers.user.Role.MANAGER, com.sivacrackers.user.Role.CASHIER);

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final com.sivacrackers.user.UserRepository users;
    private final com.sivacrackers.supplier.SupplierRepository suppliers;
    private final com.sivacrackers.expense.ExpenseService expenses;
    private final com.sivacrackers.report.ReportService reports;
    private final com.sivacrackers.inventory.StockMovementRepository movements;
    private final com.sivacrackers.expense.ExpenseRepository expenseRepo;
    private final com.sivacrackers.billing.BillRepository bills;
    private final java.time.Clock clock;

    public AdminDashboardController(ProductRepository products, CategoryRepository categories, BrandRepository brands,
                                    com.sivacrackers.user.UserRepository users, com.sivacrackers.supplier.SupplierRepository suppliers,
                                    com.sivacrackers.expense.ExpenseService expenses, com.sivacrackers.report.ReportService reports,
                                    com.sivacrackers.inventory.StockMovementRepository movements,
                                    com.sivacrackers.expense.ExpenseRepository expenseRepo,
                                    com.sivacrackers.billing.BillRepository bills, java.time.Clock clock) {
        this.products = products;
        this.categories = categories;
        this.brands = brands;
        this.users = users;
        this.suppliers = suppliers;
        this.expenses = expenses;
        this.reports = reports;
        this.movements = movements;
        this.expenseRepo = expenseRepo;
        this.bills = bills;
        this.clock = clock;
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
        java.time.LocalDate today = com.sivacrackers.common.ShopTime.today(clock);
        var todaySales = reports.billing(new com.sivacrackers.common.ShopTime.Range(today, today));
        var monthSales = reports.billing(new com.sivacrackers.common.ShopTime.Range(today.withDayOfMonth(1), today));
        return new Dashboard(
                products.count(), products.countByActiveTrue(), products.countByActiveFalse(),
                products.countByActiveTrueAndVisibleOnWebsiteTrue(), products.countByActiveTrueAndVisibleOnWebsiteFalse(),
                products.countByActiveTrueAndFeaturedTrue(), products.countActiveLowStock(), products.countActiveOutOfStock(),
                products.countByDemoTrue(), categories.count(), categories.countByActiveTrue(), brands.count(),
                brands.countByActiveTrue(), users.countByRoleIn(STAFF), users.countByRoleInAndActiveTrue(STAFF),
                suppliers.count(), suppliers.countByActiveTrue(), expenses.thisMonthTotal(),
                todaySales.finalizedBills(), todaySales.revenue(), monthSales.finalizedBills(), monthSales.revenue(),
                byCategory, recent,
                movements.findTop5ByOrderByCreatedAtDescIdDesc().stream().map(com.sivacrackers.inventory.InventoryDtos.MovementDto::of).toList(),
                expenseRepo.findTop5ByOrderByCreatedAtDescIdDesc().stream().map(com.sivacrackers.expense.ExpenseDtos.ExpenseDto::of).toList(),
                bills.findTop5ByOrderByCreatedAtDescIdDesc().stream().map(com.sivacrackers.billing.BillingService::row).toList());
    }
}
