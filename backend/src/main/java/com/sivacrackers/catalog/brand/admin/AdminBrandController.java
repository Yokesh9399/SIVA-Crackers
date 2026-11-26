package com.sivacrackers.catalog.brand.admin;

import com.sivacrackers.catalog.brand.admin.AdminBrandDtos.AdminBrand;
import com.sivacrackers.catalog.brand.admin.AdminBrandDtos.BrandRequest;
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

/** Brand management – OWNER only. */
@RestController
@RequestMapping("/api/admin/brands")
@PreAuthorize("hasRole('OWNER')")
public class AdminBrandController {

    private final AdminBrandService service;

    public AdminBrandController(AdminBrandService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminBrand> list(@RequestParam(required = false) String search) {
        return service.list(search);
    }

    @GetMapping("/{id}")
    public AdminBrand get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<AdminBrand> create(@Valid @RequestBody BrandRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }

    @PutMapping("/{id}")
    public AdminBrand update(@PathVariable Long id, @Valid @RequestBody BrandRequest body) {
        return service.update(id, body);
    }

    /** 409 with an explanation while products still use the brand. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
