package com.xplex.privacy.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xplex.privacy.profile.ProfileRepository

@Composable
fun PresetsScreen() {
    val context = LocalContext.current
    val repository = remember { ProfileRepository(context) }
    var presetNames by remember { mutableStateOf(repository.listPresetNames()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(repository.exportAllPresetsAsJson().toByteArray()) }
            statusMessage = "Presets exported"
        } catch (t: Throwable) {
            statusMessage = "Export failed: ${t.message}"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            if (text != null) {
                val count = repository.importPresetsFromJson(text)
                presetNames = repository.listPresetNames()
                statusMessage = "Imported $count preset(s)"
            }
        } catch (t: Throwable) {
            statusMessage = "Import failed: ${t.message}"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Presets", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Saved fingerprint configurations you can reapply to any app",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { exportLauncher.launch("privacy-kit-presets.json") },
                modifier = Modifier.weight(1f),
                enabled = presetNames.isNotEmpty()
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Export all")
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Import")
            }
        }

        statusMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }

        if (presetNames.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Bookmarks,
                title = "No presets yet",
                subtitle = "Save one from any app's profile editor, or import a JSON file."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(presetNames, key = { it }) { name ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Filled.Bookmarks, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(name, style = MaterialTheme.typography.titleMedium)
                            }
                            IconButton(onClick = {
                                repository.deletePreset(name)
                                presetNames = repository.listPresetNames()
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete preset")
                            }
                        }
                    }
                }
            }
        }
    }
}
