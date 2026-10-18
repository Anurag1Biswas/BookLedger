package com.lms.util;

/**
 * The library's fee policy (unchanged from the original IssueService):
 * first 10 days flat 30, then 20 per extra day, plus the full book price if damaged.
 */
public final class BillingCalculator {

    public static final long INCLUDED_DAYS = 10;
    public static final double BASE_FEE = 30;
    public static final double EXTRA_PER_DAY = 20;

    private BillingCalculator() {
    }

    public static double calculate(long daysKept, boolean damaged, double bookPrice) {
        double bill = daysKept <= INCLUDED_DAYS
                ? BASE_FEE
                : BASE_FEE + EXTRA_PER_DAY * (daysKept - INCLUDED_DAYS);
        if (damaged) {
            bill += bookPrice;
        }
        return bill;
    }
}
