package com.sivacrackers.supplier;

import com.sivacrackers.supplier.SupplierDtos.StatusRequest;
import com.sivacrackers.supplier.SupplierDtos.SupplierDetail;
import com.sivacrackers.supplier.SupplierDtos.SupplierDto;
import com.sivacrackers.supplier.SupplierDtos.SupplierRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Suppliers – OWNER and MANAGER. Purchase history: {@code GET /api/admin/inventory/movements?supplierId=}. */
@RestController
@RequestMapping("/api/admin/suppliers")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class AdminSupplierController {

    private final SupplierService service;

    public AdminSupplierController(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    public List<SupplierDto> list(@RequestParam(required = false) String search, @RequestParam(required = false) Boolean active) {
        return service.list(search, active);
    }

    @GetMapping("/{id}")
    public SupplierDetail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<SupplierDto> create(@Valid @RequestBody SupplierRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }

    @PutMapping("/{id}")
    public SupplierDto update(@PathVariable Long id, @Valid @RequestBody SupplierRequest body) {
        return service.update(id, body);
    }

    @PatchMapping("/{id}/status")
    public SupplierDto status(@PathVariable Long id, @Valid @RequestBody StatusRequest body) {
        return service.setActive(id, body.active());
    }
}
