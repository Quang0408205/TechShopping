package com.example.Tech.entity.contact;

/**
 * NEW → IN_PROGRESS → RESOLVED (NEW may go straight to RESOLVED); RESOLVED can be reopened to IN_PROGRESS.
 * Nothing goes back to NEW. RESOLVED needs a staff note (DB CHECK chk_contact_requests_resolved_note).
 */
public enum ContactStatus {
    NEW,
    IN_PROGRESS,
    RESOLVED;

    public boolean canMoveTo(ContactStatus target) {
        return target == this || target != NEW;
    }
}
