package com.gymsaas.plan.domain;

/**
 * Freeze limits of a plan, snapshotted on each membership. A null limit means
 * "no limit"; maxFreezes = 0 disables freezing.
 *
 * @param maxFreezeDays maximum total frozen days per contract
 * @param maxFreezes    maximum number of freezes per contract
 * @param minFreezeDays minimum length of one freeze
 */
public record FreezePolicy(Integer maxFreezeDays, Integer maxFreezes, Integer minFreezeDays) {

    public static FreezePolicy unlimited() {
        return new FreezePolicy(null, null, null);
    }

    /** Returns a violation code, or null when the configuration is consistent. */
    public String configurationViolation() {
        if (maxFreezeDays != null && maxFreezeDays <= 0) {
            return "INVALID_FREEZE_POLICY";
        }
        if (maxFreezes != null && maxFreezes < 0) {
            return "INVALID_FREEZE_POLICY";
        }
        if (minFreezeDays != null && minFreezeDays <= 0) {
            return "INVALID_FREEZE_POLICY";
        }
        if (minFreezeDays != null && maxFreezeDays != null && minFreezeDays > maxFreezeDays) {
            return "INVALID_FREEZE_POLICY";
        }
        return null;
    }

    /**
     * Checks a new freeze request against this policy.
     *
     * @param freezesSoFar     non-cancelled freezes already registered
     * @param frozenDaysSoFar  days already consumed (effective or planned)
     * @param requestedDays    planned days of the new freeze
     * @return violation code, or null when allowed
     */
    public String freezeViolation(int freezesSoFar, int frozenDaysSoFar, int requestedDays) {
        if (requestedDays <= 0) {
            return "INVALID_FREEZE_PERIOD";
        }
        if (maxFreezes != null && freezesSoFar >= maxFreezes) {
            return maxFreezes == 0 ? "FREEZE_NOT_ALLOWED" : "FREEZE_LIMIT_REACHED";
        }
        if (minFreezeDays != null && requestedDays < minFreezeDays) {
            return "FREEZE_TOO_SHORT";
        }
        if (maxFreezeDays != null && frozenDaysSoFar + requestedDays > maxFreezeDays) {
            return "FREEZE_DAYS_EXCEEDED";
        }
        return null;
    }
}
