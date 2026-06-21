package com.xplex.privacy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class DashboardStats(
    val configuredAppCount: Int,
    val totalHookCount: Int,
    val presetCount: Int
)

/**
 * The app's front door: a sense of "what's protected right now" rather than
 * dropping straight into a flat app list. Three stat cards plus a primary
 * call to action (pick an app to configure).
 */
@Composable
fun HomeScreen(stats: DashboardStats, onBrowseApps: () -> Unit, onBrowsePresets: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column {
            Text("Privacy Kit", style = MaterialTheme.typography.headlineMedium)
            Text(
                if (stats.configuredAppCount == 0) {
                    "No apps configured yet - pick one to get started."
                } else {
                    "${stats.configuredAppCount} app${if (stats.configuredAppCount == 1) "" else "s"} currently protected."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                icon = Icons.Filled.Shield,
                value = stats.configuredAppCount.toString(),
                label = "Apps protected",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Filled.Apps,
                value = stats.totalHookCount.toString(),
                label = "Hooks available",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Filled.Bookmarks,
                value = stats.presetCount.toString(),
                label = "Saved presets",
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            onClick = onBrowseApps,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Casino, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Column {
                    Text(
                        "Configure an app",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Randomize a fingerprint, hide identifiers, or apply a saved preset",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Card(onClick = onBrowsePresets, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Bookmarks, contentDescription = null)
                Column {
                    Text("Manage presets", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "View, apply, export, or import saved fingerprint configurations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.aspectRatio(1f)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(value, style = MaterialTheme.typography.headlineSmall)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
