/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 * Modifications Copyright (c) 2026 jush0147
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.t8rin.imagetoolbox.feature.draw.domain

internal object SensitiveDataDetector {

    enum class Kind {
        Email,
        TaiwanMobile,
        TaiwanLandline,
        TaiwanId,
        CreditCard
    }

    fun detect(text: String): Kind? {
        val raw = text.trim()
        if (raw.isEmpty()) return null

        if (EMAIL.matches(raw.replace(" ", ""))) {
            return Kind.Email
        }

        val compact = raw
            .replace("（", "(")
            .replace("）", ")")
            .replace(SEPARATORS, "")

        val upper = compact.uppercase()

        if (TAIWAN_ID.matches(upper) && isValidTaiwanId(upper)) {
            return Kind.TaiwanId
        }

        val phone = normalizeTaiwanPhone(compact)
        if (TAIWAN_MOBILE.matches(phone)) {
            return Kind.TaiwanMobile
        }

        val hasPhoneFormatting = raw.any {
            it == '-' || it == '(' || it == ')' || it == '（' || it == '）'
        }
        if (hasPhoneFormatting && TAIWAN_LANDLINE.matches(phone)) {
            return Kind.TaiwanLandline
        }

        val cardDigits = compact.removePrefix("+")
        if (
            cardDigits.length in 13..19 &&
            cardDigits.all(Char::isDigit) &&
            isValidLuhn(cardDigits)
        ) {
            return Kind.CreditCard
        }

        return null
    }

    private fun normalizeTaiwanPhone(value: String): String = when {
        value.startsWith("+886") -> "0" + value.drop(4)
        value.startsWith("886") -> "0" + value.drop(3)
        else -> value
    }

    private fun isValidTaiwanId(value: String): Boolean {
        if (!TAIWAN_ID.matches(value)) return false

        val letterValue = TAIWAN_LETTER_CODES[value.first()] ?: return false
        val digits = value.drop(1).map(Char::digitToInt)

        var sum = letterValue / 10 + (letterValue % 10) * 9
        val weights = intArrayOf(8, 7, 6, 5, 4, 3, 2, 1)
        for (index in 0 until 8) {
            sum += digits[index] * weights[index]
        }
        sum += digits[8]

        return sum % 10 == 0
    }

    private fun isValidLuhn(value: String): Boolean {
        var sum = 0
        var doubleDigit = false

        for (index in value.indices.reversed()) {
            var digit = value[index].digitToInt()
            if (doubleDigit) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
            doubleDigit = !doubleDigit
        }

        return sum % 10 == 0
    }

    private val SEPARATORS = Regex("[\\s\\-().]")
    private val EMAIL = Regex(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        RegexOption.IGNORE_CASE
    )
    private val TAIWAN_MOBILE = Regex("^09\\d{8}$")
    private val TAIWAN_LANDLINE = Regex("^0\\d{8,9}$")
    private val TAIWAN_ID = Regex("^[A-Z][12]\\d{8}$")

    private val TAIWAN_LETTER_CODES = mapOf(
        'A' to 10, 'B' to 11, 'C' to 12, 'D' to 13, 'E' to 14, 'F' to 15,
        'G' to 16, 'H' to 17, 'I' to 34, 'J' to 18, 'K' to 19, 'L' to 20,
        'M' to 21, 'N' to 22, 'O' to 35, 'P' to 23, 'Q' to 24, 'R' to 25,
        'S' to 26, 'T' to 27, 'U' to 28, 'V' to 29, 'W' to 32, 'X' to 30,
        'Y' to 31, 'Z' to 33
    )
}
