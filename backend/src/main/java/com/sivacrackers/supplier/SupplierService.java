package com.sivacrackers.supplier;

import com.sivacrackers.audit.AuditService;
import com.sivacrackers.common.exception.ConflictException;
import com.sivacrackers.common.exception.NotFoundException;
import com.sivacrackers.inventory.StockMovementRepository;
import com.sivacrackers.supplier.SupplierDtos.SupplierDetail;
import com.sivacrackers.supplier.SupplierDtos.SupplierDto;
import com.sivacrackers.supplier.SupplierDtos.SupplierRequest;
import com.sivacrackers.user.Identifiers;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Suppliers are never deleted (stock history refers to them); they are deactivated instead. */
@Service
public class SupplierService {

    private final SupplierRepository suppliers;
    private final StockMovementRepository movements;
    private final AuditService audit;

    public SupplierService(SupplierRepository suppliers, StockMovementRepository movements, AuditService audit) {
        this.suppliers = suppliers;
        this.movements = movements;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<SupplierDto> list(String search, Boolean active) {
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return suppliers.findAllByOrderByActiveDescNameAsc().stream()
                .filter(s -> active == null || s.isActive() == active)
                .filter(s -> q.isEmpty() || contains(s.getName(), q) || contains(s.getContactPerson(), q)
                        || contains(s.getPhone(), q) || contains(s.getEmail(), q))
                .map(SupplierDto::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplierDetail get(Long id) {
        Supplier s = find(id);
        var t = movements.totalsForSupplier(id);
        return new SupplierDetail(SupplierDto.of(s), t.getDeliveries(), t.getUnits(), t.getPurchaseValue(), t.getLastDeliveryAt());
    }

    @Transactional
    public SupplierDto create(SupplierRequest req) {
        String name = req.name().trim();
        if (suppliers.existsByNameIgnoreCase(name)) {
            throw new ConflictException("name", "is already used by another supplier");
        }
        Supplier s = new Supplier(name);
        apply(s, req);
        Supplier saved = save(s);
        audit.record("SUPPLIER_CREATED", "SUPPLIER", saved.getId(), saved.getName());
        return SupplierDto.of(saved);
    }

    @Transactional
    public SupplierDto update(Long id, SupplierRequest req) {
        Supplier s = find(id);
        String name = req.name().trim();
        if (suppliers.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("name", "is already used by another supplier");
        }
        s.setName(name);
        apply(s, req);
        Supplier saved = save(s);
        audit.record("SUPPLIER_UPDATED", "SUPPLIER", id, saved.getName());
        return SupplierDto.of(saved);
    }

    @Transactional
    public SupplierDto setActive(Long id, boolean active) {
        Supplier s = find(id);
        if (s.isActive() != active) {
            s.setActive(active);
            save(s);
            audit.record(active ? "SUPPLIER_ACTIVATED" : "SUPPLIER_DEACTIVATED", "SUPPLIER", id, s.getName());
        }
        return SupplierDto.of(s);
    }

    private void apply(Supplier s, SupplierRequest req) {
        s.setContactPerson(blankToNull(req.contactPerson()));
        s.setPhone(Identifiers.phone(req.phone()));
        s.setEmail(req.email() == null || req.email().isBlank() ? null : Identifiers.email(req.email()));
        s.setAddress(blankToNull(req.address()));
        s.setNotes(blankToNull(req.notes()));
    }

    private Supplier save(Supplier s) {
        try {
            return suppliers.saveAndFlush(s);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This supplier name is already used. Please choose another.");
        }
    }

    private Supplier find(Long id) {
        return suppliers.findById(id).orElseThrow(() -> new NotFoundException("Supplier not found."));
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
