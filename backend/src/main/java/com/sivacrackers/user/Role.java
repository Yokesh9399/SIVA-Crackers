package com.sivacrackers.user;

/**
 * Account roles (Phase 0 role matrix). Must match the CHECK constraint ck_users_role.
 * Public registration may only ever create CUSTOMER.
 */
public enum Role {
    OWNER,
    MANAGER,
    CASHIER,
    CUSTOMER;

    public boolean isStaff() {
        return this != CUSTOMER;
    }
}
