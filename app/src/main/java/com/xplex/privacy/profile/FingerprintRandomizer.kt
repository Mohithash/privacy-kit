package com.xplex.privacy.profile

import kotlin.random.Random

/**
 * Generates a full random privacy profile: one value per hook id, keyed
 * exactly the way [com.xplex.privacy.hooks.HookEngine] looks settings up
 * (the hook's own id). Pure functions, no Android dependencies, so this is
 * fully unit-testable on the plain JVM.
 */
object FingerprintRandomizer {

    fun randomProfile(random: Random = Random): Map<String, String> {
        val device = DeviceFingerprints.random()
        val imei = randomImei(random)
        return mapOf(
            "device.build.model" to device.model,
            "device.build.manufacturer" to device.manufacturer,
            "device.build.brand" to device.brand,
            "device.build.getSerial" to randomSerial(random),
            "telephony.deviceId" to imei,
            "telephony.imei" to imei,
            "telephony.subscriberId" to randomImsi(random),
            "telephony.simSerialNumber" to randomIccid(random),
            "telephony.line1Number" to randomPhoneNumber(random),
            "settings.androidId" to randomAndroidId(random),
            "wifi.macAddress" to randomMacAddress(random),
            "bluetooth.address" to randomMacAddress(random)
        )
    }

    /** 15 digits, last digit a valid Luhn check digit, matching real IMEI format. */
    fun randomImei(random: Random = Random): String {
        val digits = StringBuilder()
        repeat(14) { digits.append(random.nextInt(10)) }
        val withCheck = digits.toString() + luhnCheckDigit(digits.toString())
        return withCheck
    }

    private fun luhnCheckDigit(number: String): Int {
        var sum = 0
        for ((index, char) in number.reversed().withIndex()) {
            var digit = char - '0'
            if (index % 2 == 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return (10 - (sum % 10)) % 10
    }

    /** 15 digits, mirroring IMSI's MCC+MNC+MSIN structure loosely (not tied to a real carrier). */
    fun randomImsi(random: Random = Random): String =
        (1..15).joinToString("") { random.nextInt(10).toString() }

    /** 19-20 digit ICCID-shaped string, starting with the telecom industry prefix 89. */
    fun randomIccid(random: Random = Random): String =
        "89" + (1..18).joinToString("") { random.nextInt(10).toString() }

    fun randomPhoneNumber(random: Random = Random): String =
        "+1555" + (1..7).joinToString("") { random.nextInt(10).toString() }

    /** 16 lowercase hex chars, matching ANDROID_ID's real shape. */
    fun randomAndroidId(random: Random = Random): String {
        val hexChars = "0123456789abcdef"
        return (1..16).map { hexChars[random.nextInt(hexChars.length)] }.joinToString("")
    }

    /**
     * Random MAC with the locally-administered bit set on the first octet
     * (matches what a real randomized-MAC Android device produces, rather
     * than colliding with a real OUI-assigned vendor prefix).
     */
    fun randomMacAddress(random: Random = Random): String {
        val firstOctet = (random.nextInt(256) and 0xFC) or 0x02
        val octets = mutableListOf(firstOctet)
        repeat(5) { octets.add(random.nextInt(256)) }
        return octets.joinToString(":") { "%02X".format(it) }
    }

    /** 8 uppercase alphanumeric chars, matching typical device serial format. */
    fun randomSerial(random: Random = Random): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}
