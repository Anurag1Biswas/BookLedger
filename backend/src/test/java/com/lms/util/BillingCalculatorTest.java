package com.lms.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BillingCalculatorTest {

    @ParameterizedTest(name = "{0} days kept -> {1}")
    @CsvSource({"0,30", "1,30", "10,30", "11,50", "15,130", "30,430"})
    void flatFeeForFirstTenDaysThenTwentyPerExtraDay(long daysKept, double expected) {
        assertEquals(expected, BillingCalculator.calculate(daysKept, false, 999), 0.0001);
    }

    @Test
    void damagedBookAddsFullBookPrice() {
        assertEquals(30 + 400, BillingCalculator.calculate(5, true, 400), 0.0001);
        assertEquals(130 + 250, BillingCalculator.calculate(15, true, 250), 0.0001);
    }
}
