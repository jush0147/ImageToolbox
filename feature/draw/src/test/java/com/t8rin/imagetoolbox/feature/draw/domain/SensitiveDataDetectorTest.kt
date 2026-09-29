/*
 * Modifications Copyright (c) 2026 jush0147
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.t8rin.imagetoolbox.feature.draw.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SensitiveDataDetectorTest {

    @Test
    fun detectsTaiwanMobileWithCommonFormatting() {
        assertEquals(
            SensitiveDataDetector.Kind.TaiwanMobile,
            SensitiveDataDetector.detect("0912-345-678")
        )
        assertEquals(
            SensitiveDataDetector.Kind.TaiwanMobile,
            SensitiveDataDetector.detect("+886 912 345 678")
        )
    }

    @Test
    fun detectsEmail() {
        assertEquals(
            SensitiveDataDetector.Kind.Email,
            SensitiveDataDetector.detect("hello@example.com")
        )
    }

    @Test
    fun validatesTaiwanIdChecksum() {
        assertEquals(
            SensitiveDataDetector.Kind.TaiwanId,
            SensitiveDataDetector.detect("A123456789")
        )
        assertNull(SensitiveDataDetector.detect("A123456788"))
    }

    @Test
    fun usesLuhnForCreditCards() {
        assertEquals(
            SensitiveDataDetector.Kind.CreditCard,
            SensitiveDataDetector.detect("4111 1111 1111 1111")
        )
        assertNull(SensitiveDataDetector.detect("4111 1111 1111 1112"))
    }

    @Test
    fun avoidsTreatingUnformattedLongNumbersAsLandlines() {
        assertNull(SensitiveDataDetector.detect("0212345678"))
        assertEquals(
            SensitiveDataDetector.Kind.TaiwanLandline,
            SensitiveDataDetector.detect("02-1234-5678")
        )
    }
}
