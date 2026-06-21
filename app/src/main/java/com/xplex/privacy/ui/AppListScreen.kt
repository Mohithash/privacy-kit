package com.xplex.privacy.ui

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xplex.privacy.profile.ProfileRepository

private enum class AppFilter { ALL, CONFIGURED }

@Composable
fun AppListScreen(onAppSelected: (String, String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { ProfileRepository(context) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(AppFilter.ALL) }

    val configuredPackages = remember { repository.configuredPackages().toSet() }

    val apps = remember {
        context.packageManager.getInstalledApplications(0)
            .filter { it.packageName != context.packageName }
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 }
            .map { it.packageName to it.loadLabel(context.packageManager).toString() }
            .sortedBy { it.second.lowercase() }
    }

    val filtered = remember(query, filter, apps, configuredPackages) {
        apps
            .filter { query.isBlank() || it.second.contains(query, ignoreCase = true) }
            .filter { filter == AppFilter.ALL || it.first in configuredPackages }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Apps", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 12.dp))

        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search apps") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true
        )

        Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filter == AppFilter.ALL,
                onClick = { filter = AppFilter.ALL },
                label = { Text("All (${apps.size})") }
            )
            FilterChip(
                selected = filter == AppFilter.CONFIGURED,
                onClick = { filter = AppFilter.CONFIGURED },
                label = { Text("Configured (${configuredPackages.size})") }
            )
        }

        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Apps,
                title = "No apps found",
                subtitle = if (filter == AppFilter.CONFIGURED) "Nothing configured yet." else "Try a different search."
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filtered, key = { it.first }) { (packageName, label) ->
                    AppRow(
                        packageName = packageName,
                        label = label,
                        isConfigured = packageName in configuredPackages,
                        onClick = { onAppSelected(packageName, label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(packageName: String, label: String, isConfigured: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName)
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isConfigured) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("Protected") },
                    icon = { Icon(Icons.Filled.Shield, contentDescription = null) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}
