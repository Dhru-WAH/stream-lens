package com.dhruva.streamlens.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.graphics.Color as AndroidColor
import com.dhruva.streamlens.StreamLensApp
import com.dhruva.streamlens.ui.theme.Palette
import com.dhruva.streamlens.ui.theme.StreamLensTheme
import com.dhruva.streamlens.work.Alerts

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()
    private var pendingOpen by mutableStateOf<String?>(null)

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        pendingOpen = intent.getStringExtra(Alerts.EXTRA_OPEN_ID)
        askForNotificationsOnce()

        setContent {
            StreamLensTheme {
                val snackbar = remember { SnackbarHostState() }
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var openId by rememberSaveable { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
                LaunchedEffect(pendingOpen) {
                    pendingOpen?.let { openId = it; showSettings = false; pendingOpen = null }
                }

                Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = Palette.Bg) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        if (showSettings) SettingsScreen(vm, onBack = { showSettings = false })
                        else HomeScreen(vm, onOpen = { openId = it }, onSettings = { showSettings = true })
                    }
                    openId?.let { id -> DetailSheet(id, vm, onDismiss = { openId = null }) }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Alerts.EXTRA_OPEN_ID)?.let { pendingOpen = it }
    }

    private fun askForNotificationsOnce() {
        val prefs = (application as StreamLensApp).prefs
        if (Build.VERSION.SDK_INT >= 33 && !prefs.askedNotificationPermission) {
            prefs.askedNotificationPermission = true
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
