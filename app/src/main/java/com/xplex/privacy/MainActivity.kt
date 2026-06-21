package com.xplex.privacy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.xplex.privacy.hooks.HookRegistry
import com.xplex.privacy.profile.ProfileRepository
import com.xplex.privacy.ui.AppListScreen
import com.xplex.privacy.ui.DashboardStats
import com.xplex.privacy.ui.HomeScreen
import com.xplex.privacy.ui.PresetsScreen
import com.xplex.privacy.ui.ProfileEditorScreen
import com.xplex.privacy.ui.theme.XplexTheme

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

private enum class Tab(val label: String) { HOME("Home"), APPS("Apps"), PRESETS("Presets") }

private sealed interface Screen {
    data object Main : Screen
    data class ProfileEditor(val packageName: String, val appLabel: String) : Screen
}

@Composable
fun PrivacyKitApp() {
    val context = LocalContext.current
    val repository = remember { ProfileRepository(context) }

    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }

    when (val current = screen) {
        is Screen.ProfileEditor -> ProfileEditorScreen(
            packageName = current.packageName,
            appLabel = current.appLabel,
            onBack = { screen = Screen.Main }
        )

        is Screen.Main -> Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == Tab.HOME,
                        onClick = { tab = Tab.HOME },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text(Tab.HOME.label) }
                    )
                    NavigationBarItem(
                        selected = tab == Tab.APPS,
                        onClick = { tab = Tab.APPS },
                        icon = { Icon(Icons.Filled.Apps, contentDescription = null) },
                        label = { Text(Tab.APPS.label) }
                    )
                    NavigationBarItem(
                        selected = tab == Tab.PRESETS,
                        onClick = { tab = Tab.PRESETS },
                        icon = { Icon(Icons.Filled.Bookmarks, contentDescription = null) },
                        label = { Text(Tab.PRESETS.label) }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    Tab.HOME -> {
                        val stats = remember {
                            DashboardStats(
                                configuredAppCount = repository.configuredPackages().size,
                                totalHookCount = HookRegistry.loadAll(context.assets).size,
                                presetCount = repository.listPresetNames().size
                            )
                        }
                        HomeScreen(
                            stats = stats,
                            onBrowseApps = { tab = Tab.APPS },
                            onBrowsePresets = { tab = Tab.PRESETS }
                        )
                    }

                    Tab.APPS -> AppListScreen(onAppSelected = { packageName, label ->
                        screen = Screen.ProfileEditor(packageName, label)
                    })

                    Tab.PRESETS -> PresetsScreen()
                }
            }
        }
    }
}
