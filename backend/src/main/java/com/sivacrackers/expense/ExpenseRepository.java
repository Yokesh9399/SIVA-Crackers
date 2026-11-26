package com.sivacrackers.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    @EntityGraph(attributePaths = "category")
    Optional<Expense> findWithCategoryById(Long id);

    /** Total of ACTIVE expenses dated within [from, to] (both inclusive). */
    @Query("""
            select coalesce(sum(e.amount), 0) from Expense e
            where e.status = com.sivacrackers.expense.Expense.Status.ACTIVE
              and e.expenseDate >= :from and e.expenseDate <= :to""")
    BigDecimal activeTotalBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
