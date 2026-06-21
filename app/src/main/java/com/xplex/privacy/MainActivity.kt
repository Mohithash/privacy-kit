package com.xplex.privacy

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xplex.privacy.hooks.HookDefinition
import com.xplex.privacy.hooks.HookRegistry
import com.xplex.privacy.profile.ProfileRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XplexTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PrivacyKitApp()
                }
            }
        }
    }
}

private sealed interface Screen {
    data object AppList : Screen
    data class ProfileEditor(val packageName: String, val appLabel: String) : Screen
}

@Composable
fun PrivacyKitApp() {
    var screen by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.mapSaver(
        save = { mapOf("packageName" to ((it as? Screen.ProfileEditor)?.packageName ?: "")) },
        restore = { if ((it["packageName"] as? String).isNullOrEmpty()) Screen.AppList else Screen.ProfileEditor(it["packageName"] as String, "") }
    )) { mutableStateOf<Screen>(Screen.AppList) }

    when (val current = screen) {
        is Screen.AppList -> AppListScreen(onAppSelected = { packageName, label ->
            screen = Screen.ProfileEditor(packageName, label)
        })
        is Screen.ProfileEditor -> ProfileEditorScreen(
            packageName = current.packageName,
            appLabel = current.appLabel,
            onBack = { screen = Screen.AppList }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(onAppSelected: (String, String) -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }

    val apps = remember {
        context.packageManager.getInstalledApplications(0)
            .filter { it.packageName != context.packageName }
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 }
            .map { it.packageName to it.loadLabel(context.packageManager).toString() }
            .sortedBy { it.second.lowercase() }
    }

    val filtered = remember(query, apps) {
        if (query.isBlank()) apps else apps.filter { it.second.contains(query, ignoreCase = true) }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Privacy Kit") })
    }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search apps") }
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(filtered, key = { it.first }) { (packageName, label) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        supportingContent = { Text(packageName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppSelected(packageName, label) }
                    )
                    androidx.compose.material3.HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(packageName: String, appLabel: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { ProfileRepository(context) }
    val allHooks = remember { HookRegistry.loadAll(context.assets) }

    var currentValues by remember(packageName) {
        mutableStateOf(repository.currentValues(packageName))
    }
    var enabledIds by remember(packageName) {
        mutableStateOf(repository.enabledHookIds(packageName))
    }
    var diagnostics by remember(packageName) {
        mutableStateOf(com.xplex.privacy.data.SettingsClient.getDiagnostics(context, packageName))
    }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        currentValues = repository.currentValues(packageName)
        enabledIds = repository.enabledHookIds(packageName)
        diagnostics = com.xplex.privacy.data.SettingsClient.getDiagnostics(context, packageName)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(appLabel.ifBlank { packageName }) },
            navigationIcon = {
                TextButton(onClick = onBack) { Text("Back") }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(packageName, style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = {
                    repository.applyRandomProfile(packageName, allHooks)
                    refresh()
                    statusMessage = "Randomized fingerprint applied"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Randomize all values")
            }

            OutlinedButton(
                onClick = {
                    repository.reset(packageName)
                    refresh()
                    statusMessage = "Profile reset"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset profile")
            }

            statusMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

            val failed = diagnostics.filter { it.status == com.xplex.privacy.data.SettingsDatabase.STATUS_FAILED }
            if (failed.isNotEmpty()) {
                Text(
                    "${failed.size} hook(s) failed to install on this app - they had no effect",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    failed.forEach { entry ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(entry.hookId, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    entry.message ?: "unknown error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            Text("Current values", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allHooks, key = { it.id }) { hook ->
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
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(hook.description.ifBlank { hook.id }, style = MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.Switch(checked = enabled, onCheckedChange = onEnabledChange)
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

@Composable
fun XplexTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
