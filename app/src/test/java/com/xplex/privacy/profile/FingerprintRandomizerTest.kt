package com.xplex.privacy.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FingerprintRandomizerTest {

    @Test
    fun `random imei is 15 digits with a valid luhn check digit`() {
        repeat(50) { seed ->
            val imei = FingerprintRandomizer.randomImei(Random(seed))
            assertEquals(15, imei.length)
            assertTrue(imei.all { it.isDigit() })
            assertTrue("imei $imei failed luhn check", isLuhnValid(imei))
        }
    }

    @Test
    fun `random android id is 16 lowercase hex chars`() {
        val id = FingerprintRandomizer.randomAndroidId(Random(1))
        assertEquals(16, id.length)
        assertTrue(id.all { it in "0123456789abcdef" })
    }

    @Test
    fun `random mac address has locally administered bit set`() {
        repeat(20) { seed ->
            val mac = FingerprintRandomizer.randomMacAddress(Random(seed))
            val octets = mac.split(":")
            assertEquals(6, octets.size)
            val firstOctet = octets[0].toInt(16)
            // Locally-administered bit (0x02) must be set, multicast bit (0x01) must be clear.
            assertEquals(0x02, firstOctet and 0x03)
        }
    }

    @Test
    fun `random serial is 8 uppercase alphanumeric chars`() {
        val serial = FingerprintRandomizer.randomSerial(Random(7))
        assertEquals(8, serial.length)
        assertTrue(serial.all { it.isUpperCase() || it.isDigit() })
    }

    @Test
    fun `random profile contains every expected hook id with non-blank values`() {
        val profile = FingerprintRandomizer.randomProfile(Random(42))
        val expectedIds = setOf(
            "device.build.model", "device.build.manufacturer", "device.build.brand",
            "device.build.getSerial", "telephony.deviceId", "telephony.imei",
            "telephony.subscriberId", "telephony.simSerialNumber", "telephony.line1Number",
            "settings.androidId", "wifi.macAddress", "bluetooth.address",
            "ads.advertisingId", "ads.isLimitAdTrackingEnabled"
        )
        assertEquals(expectedIds, profile.keys)
        profile.values.forEach { assertTrue(it.isNotBlank()) }
    }

    @Test
    fun `random advertising id matches the real GAID UUID shape`() {
        val gaid = FingerprintRandomizer.randomAdvertisingId(Random(11))
        val uuidPattern = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        assertTrue("expected $gaid to match UUID shape", uuidPattern.matches(gaid))
    }

    @Test
    fun `random profile picks a real device tuple, not independently random fields`() {
        val profile = FingerprintRandomizer.randomProfile(Random(3))
        val matching = DeviceFingerprints.DEVICES.any { tuple ->
            tuple.model == profile["device.build.model"] &&
                tuple.manufacturer == profile["device.build.manufacturer"] &&
                tuple.brand == profile["device.build.brand"]
        }
        assertTrue("expected model/manufacturer/brand to come from one known device tuple", matching)
    }

    private fun isLuhnValid(number: String): Boolean {
        var sum = 0
        for ((index, char) in number.reversed().withIndex()) {
            var digit = char - '0'
            if (index % 2 == 1) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return sum % 10 == 0
    }
}
