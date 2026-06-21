package com.xplex.privacy.profile

/**
 * A small curated set of real (manufacturer, brand, model) device tuples
 * to randomize between. Generating these from raw random strings would be
 * trivially detectable as fake - apps that check fingerprints generally
 * just compare against a denylist of known-emulator markers, but a
 * manufacturer/brand/model combination that doesn't correspond to any real
 * device is its own kind of red flag.
 */
data class DeviceTuple(
    val manufacturer: String,
    val brand: String,
    val model: String
)

object DeviceFingerprints {
    val DEVICES: List<DeviceTuple> = listOf(
        DeviceTuple("Google", "google", "Pixel 8"),
        DeviceTuple("Google", "google", "Pixel 8 Pro"),
        DeviceTuple("Google", "google", "Pixel 7a"),
        DeviceTuple("samsung", "samsung", "SM-S921B"),
        DeviceTuple("samsung", "samsung", "SM-A546B"),
        DeviceTuple("OnePlus", "OnePlus", "CPH2581"),
        DeviceTuple("Xiaomi", "Redmi", "23090RA98I"),
        DeviceTuple("motorola", "motorola", "moto g84 5G"),
        DeviceTuple("Sony", "Sony", "XQ-DQ72")
    )

    fun random(): DeviceTuple = DEVICES.random()
}
