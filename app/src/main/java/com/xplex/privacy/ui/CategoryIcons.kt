package com.xplex.privacy.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/** One icon per [com.xplex.privacy.hooks.HookDefinition.categoryKey]. */
fun categoryIcon(categoryKey: String): ImageVector = when (categoryKey) {
    "device" -> Icons.Filled.PhoneAndroid
    "telephony" -> Icons.Filled.SimCard
    "settings" -> Icons.Filled.Fingerprint
    "wifi" -> Icons.Filled.Wifi
    "bluetooth" -> Icons.Filled.Bluetooth
    "pm" -> Icons.Filled.VisibilityOff
    "location" -> Icons.Filled.LocationOn
    "ads" -> Icons.Filled.Campaign
    else -> Icons.Outlined.Tune
}
