package com.sivacrackers.catalog.product.admin;

import com.sivacrackers.catalog.product.admin.AdminProductDtos.AdminProduct;
import com.sivacrackers.catalog.product.admin.AdminProductDtos.AdminProductRow;
import com.sivacrackers.catalog.product.admin.AdminProductDtos.ProductRequest;
import com.sivacrackers.catalog.product.admin.AdminProductDtos.QuickUpdateRequest;
import com.sivacrackers.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Product management – OWNER only (checked here on the server for every request). */
@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('OWNER')")
@Validated
public class AdminProductController {

    private final AdminProductService service;

    public AdminProductController(AdminProductService service) {
        this.service = service;
    }

    /**
     * {@code ?search=&categoryId=&brandId=&availability=IN_STOCK|LOW_STOCK|OUT_OF_STOCK
     * &visibility=VISIBLE|HIDDEN|INACTIVE&sort=default|name,asc|name,desc|price,asc|price,desc|newest&page=0&size=10|25|50}
     */
    @GetMapping
    public PageResponse<AdminProductRow> list(@RequestParam(required = false) String search,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) Long brandId,
                                              @RequestParam(required = false) String availability,
                                              @RequestParam(required = false) String visibility,
                                              @RequestParam(required = false) String sort,
                                              @RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size) {
        if (search != null && search.length() > 100) {
            search = search.substring(0, 100);
        }
        return service.list(AdminProductQuery.of(search, categoryId, brandId, availability, visibility, sort, page, size));
    }

    @GetMapping("/{id}")
    public AdminProduct get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<AdminProduct> create(@Valid @RequestBody ProductRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }

    @PutMapping("/{id}")
    public AdminProduct update(@PathVariable Long id, @Valid @RequestBody ProductRequest body) {
        return service.update(id, body);
    }

    @PatchMapping("/{id}")
    public AdminProduct quickUpdate(@PathVariable Long id, @Valid @RequestBody QuickUpdateRequest body) {
        return service.quickUpdate(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
