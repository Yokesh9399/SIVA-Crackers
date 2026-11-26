package com.sivacrackers.audit;

import com.sivacrackers.common.dto.PageResponse;
import com.sivacrackers.common.exception.InvalidRequestException;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Activity log – OWNER only, read-only. */
@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('OWNER')")
public class AdminAuditController {

    public record AuditDto(Long id, Instant occurredAt, String actorName, String action, String entityType, Long entityId,
                           String details) {
    }

    private final AuditRepository entries;

    public AdminAuditController(AuditRepository entries) {
        this.entries = entries;
    }

    /** {@code ?entityType=STAFF|PRODUCT|...&page=0&size=25|50|100} */
    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<AuditDto> list(@RequestParam(required = false) String entityType,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "50") int size) {
        if (page < 0) {
            throw new InvalidRequestException("page", "must not be negative");
        }
        if (size != 25 && size != 50 && size != 100) {
            throw new InvalidRequestException("size", "must be one of [25, 50, 100]");
        }
        var pageable = PageRequest.of(page, size);
        var result = entityType == null || entityType.isBlank()
                ? entries.findAllByOrderByOccurredAtDescIdDesc(pageable)
                : entries.findAllByEntityTypeOrderByOccurredAtDescIdDesc(entityType.trim().toUpperCase(), pageable);
        return PageResponse.of(result, e -> new AuditDto(e.getId(), e.getOccurredAt(), e.getActorName(), e.getAction(),
                e.getEntityType(), e.getEntityId(), e.getDetails()));
    }
}
