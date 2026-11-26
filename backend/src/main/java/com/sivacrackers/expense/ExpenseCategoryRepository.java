package com.sivacrackers.expense;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    List<ExpenseCategory> findAllByOrderByDisplayOrderAscNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
