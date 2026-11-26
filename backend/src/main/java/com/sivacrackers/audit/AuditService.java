package com.sivacrackers.audit;

import com.sivacrackers.security.CurrentUser;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records sensitive actions (staff accounts, stock, suppliers, expenses, customers, deletions) together with
 * the staff member who did them. Runs inside the caller's transaction, so the log line is saved only if the
 * action itself is saved. Callers must never pass passwords, tokens or other secrets in {@code details}.
 */
@Service
public class AuditService {

    private final AuditRepository entries;
    private final Clock clock;

    public AuditService(AuditRepository entries, Clock clock) {
        this.entries = entries;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String entityType, Long entityId, String details) {
        String safe = details == null ? null : details.length() > 1000 ? details.substring(0, 997) + "..." : details;
        entries.save(new AuditEntry(clock.instant(), CurrentUser.idOrNull(), CurrentUser.nameOrNull(), action, entityType,
                entityId, safe));
    }
}
