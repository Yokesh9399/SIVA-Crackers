package com.sivacrackers.catalog.category.admin;

import com.sivacrackers.catalog.category.admin.AdminCategoryDtos.AdminCategory;
import com.sivacrackers.catalog.category.admin.AdminCategoryDtos.CategoryRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Category management – OWNER and MANAGER. */
@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class AdminCategoryController {

    private final AdminCategoryService service;

    public AdminCategoryController(AdminCategoryService service) {
        this.service = service;
    }

    /** All categories (active and inactive) with product counts; optional {@code ?search=}. */
    @GetMapping
    public List<AdminCategory> list(@RequestParam(required = false) String search) {
        return service.list(search);
    }

    @GetMapping("/{id}")
    public AdminCategory get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<AdminCategory> create(@Valid @RequestBody CategoryRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }

    @PutMapping("/{id}")
    public AdminCategory update(@PathVariable Long id, @Valid @RequestBody CategoryRequest body) {
        return service.update(id, body);
    }

    /** 409 with an explanation while products still use the category. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
