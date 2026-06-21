package com.xplex.privacy.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xplex.privacy.data.SettingsClient
import com.xplex.privacy.data.SettingsDatabase
import com.xplex.privacy.hooks.HookDefinition
import com.xplex.privacy.hooks.HookRegistry
import com.xplex.privacy.profile.ProfileRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(packageName: String, appLabel: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { ProfileRepository(context) }
    val allHooks = remember { HookRegistry.loadAll(context.assets) }

    var currentValues by remember(packageName) { mutableStateOf(repository.currentValues(packageName)) }
    var enabledIds by remember(packageName) { mutableStateOf(repository.enabledHookIds(packageName)) }
    var diagnostics by remember(packageName) { mutableStateOf(SettingsClient.getDiagnostics(context, packageName)) }
    var presetNames by remember { mutableStateOf(repository.listPresetNames()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var newPresetName by rememberSaveable { mutableStateOf("") }
    var showPresetField by rememberSaveable { mutableStateOf(false) }

    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

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

    fun refresh() {
        currentValues = repository.currentValues(packageName)
        enabledIds = repository.enabledHookIds(packageName)
        diagnostics = SettingsClient.getDiagnostics(context, packageName)
        presetNames = repository.listPresetNames()
    }

    val groupedHooks = remember(allHooks) { allHooks.groupBy { it.categoryKey to it.categoryLabel } }
    val failed = diagnostics.filter { it.status == SettingsDatabase.STATUS_FAILED }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(appLabel.ifBlank { packageName }, maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AppHeaderCard(packageName, enabledIds.size, allHooks.size)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = {
                            repository.applyRandomProfile(packageName, allHooks)
                            refresh()
                            statusMessage = "Randomized fingerprint applied"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Casino, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Randomize")
                    }
                    OutlinedButton(
                        onClick = {
                            repository.reset(packageName)
                            refresh()
                            statusMessage = "Profile reset"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Reset")
                    }
                }
            }

            statusMessage?.let { message ->
                item { Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }

            item {
                PresetsCard(
                    presetNames = presetNames,
                    showField = showPresetField,
                    fieldValue = newPresetName,
                    onFieldValueChange = { newPresetName = it },
                    onToggleField = { showPresetField = !showPresetField },
                    onSave = {
                        repository.saveAsPreset(packageName, newPresetName)
                        statusMessage = "Saved preset \"$newPresetName\""
                        newPresetName = ""
                        showPresetField = false
                        refresh()
                    },
                    onApply = { name ->
                        repository.applyPreset(packageName, name, allHooks)
                        statusMessage = "Applied preset \"$name\""
                        refresh()
                    },
                    onDelete = { name ->
                        repository.deletePreset(name)
                        refresh()
                    },
                    onExport = { exportLauncher.launch("privacy-kit-presets.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json")) }
                )
            }

            if (failed.isNotEmpty()) {
                item {
                    FailedHooksCard(failed)
                }
            }

            item {
                Text("Hooks", style = MaterialTheme.typography.titleMedium)
            }

            groupedHooks.forEach { (categoryPair, hooksInCategory) ->
                val (categoryKey, categoryLabel) = categoryPair
                val isExpanded = expandedCategories[categoryKey] ?: true
                val enabledInCategory = hooksInCategory.count { it.id in enabledIds }

                item(key = "header:$categoryKey") {
                    CategoryHeader(
                        icon = categoryIcon(categoryKey),
                        label = categoryLabel,
                        enabledCount = enabledInCategory,
                        totalCount = hooksInCategory.size,
                        expanded = isExpanded,
                        onToggle = { expandedCategories[categoryKey] = !isExpanded }
                    )
                }

                if (isExpanded) {
                    items(hooksInCategory, key = { it.id }) { hook ->
                        HookValueCard(
                            hook = hook,
                            value = currentValues[hook.id],
                            enabled = hook.id in enabledIds,
                            onSave = { newValue ->
                                repository.setValue(packageName, hook.id, newValue)
                                repository.setHookEnabled(packageName, hook.id, true)
                                refresh()
                                statusMessage = "Saved ${hook.id}"
                            },
                            onEnabledChange = { isEnabled ->
                                repository.setHookEnabled(packageName, hook.id, isEnabled)
                                refresh()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppHeaderCard(packageName: String, enabledCount: Int, totalCount: Int) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName, size = 56.dp)
            Column {
                Text(packageName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "$enabledCount of $totalCount hooks active",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabledCount: Int,
    totalCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Text("$enabledCount/$totalCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
    }
}

@Composable
private fun PresetsCard(
    presetNames: List<String>,
    showField: Boolean,
    fieldValue: String,
    onFieldValueChange: (String) -> Unit,
    onToggleField: () -> Unit,
    onSave: () -> Unit,
    onApply: (String) -> Unit,
    onDelete: (String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Presets", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onToggleField) { Text(if (showField) "Cancel" else "Save current as preset") }
            }

            AnimatedVisibility(showField) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = fieldValue,
                        onValueChange = onFieldValueChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Preset name") },
                        singleLine = true
                    )
                    FilledTonalButton(onClick = onSave, enabled = fieldValue.isNotBlank()) { Text("Save") }
                }
            }

            if (presetNames.isEmpty()) {
                Text("No saved presets yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                presetNames.forEach { name ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row {
                            TextButton(onClick = { onApply(name) }) { Text("Apply") }
                            TextButton(onClick = { onDelete(name) }) { Text("Delete") }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f), enabled = presetNames.isNotEmpty()) { Text("Export") }
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Import") }
            }
        }
    }
}

@Composable
private fun FailedHooksCard(failed: List<SettingsClient.DiagnosticEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    "${failed.size} hook(s) failed on this app",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            failed.forEach { entry ->
                Column {
                    Text(entry.hookId, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(
                        entry.message ?: "unknown error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun HookValueCard(
    hook: HookDefinition,
    value: String?,
    enabled: Boolean,
    onSave: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    var editedValue by rememberSaveable(hook.id, value) { mutableStateOf(value.orEmpty()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    hook.description.ifBlank { hook.id },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            TextField(
                value = editedValue,
                onValueChange = { editedValue = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("real value passes through if unset") },
                singleLine = true
            )

            TextButton(
                onClick = { onSave(editedValue) },
                modifier = Modifier.fillMaxWidth(),
                enabled = editedValue.isNotBlank() && editedValue != value
            ) {
                Text("Save")
            }
        }
    }
}
