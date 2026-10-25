package com.dhruva.streamlens.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dhruva.streamlens.BuildConfig
import com.dhruva.streamlens.R
import com.dhruva.streamlens.data.AlertPrefs
import com.dhruva.streamlens.ui.theme.Display
import com.dhruva.streamlens.ui.theme.Palette
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val home by vm.home.collectAsState()
    val alerts by vm.alerts.collectAsState()
    val showHidden by vm.showHidden.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val context = LocalContext.current

    var notifAllowed by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifAllowed = it }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::backup) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::restore) }

    Column(Modifier.fillMaxSize().background(Palette.Bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Palette.Ink) }
            Text("SETTINGS", fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 26.sp)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card("Release list") {
                val updated = home.updatedAt?.let { runCatching { OffsetDateTime.parse(it).format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)) }.getOrNull() }
                Text("Updated ${updated ?: "—"}", style = MaterialTheme.typography.bodyMedium)
                if (vm.lastSync > 0) Text("Last checked ${SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH).format(Date(vm.lastSync))}", style = MaterialTheme.typography.bodySmall)
                Text("Researched daily by a scheduled Claude task on Dhruva's Mac from public sources, kept at github.com/Dhru-WAH/stream-lens.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                ActionButton(if (refreshing) "Checking…" else "Check for updates now", Icons.Rounded.Refresh) { vm.refresh() }
            }

            Card("Alerts") {
                if (!notifAllowed) {
                    Text("Notifications are off for StreamLens, so alerts can't reach you.", style = MaterialTheme.typography.bodySmall, color = Palette.Rose)
                    ActionButton("Allow notifications", null) { if (Build.VERSION.SDK_INT >= 33) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS) }
                }
                SwitchRow("Watchlist title starts streaming", alerts.watchlistStreaming) { v -> vm.updateAlerts { it.copy(watchlistStreaming = v) } }
                SwitchRow("New Telugu releases", alerts.newTelugu) { v -> vm.updateAlerts { it.copy(newTelugu = v) } }
                SwitchRow("New Hindi releases", alerts.newHindi) { v -> vm.updateAlerts { it.copy(newHindi = v) } }
                SwitchRow("New English releases", alerts.newEnglish) { v -> vm.updateAlerts { it.copy(newEnglish = v) } }
                Text("Checked once a day around 7 pm.", style = MaterialTheme.typography.bodySmall)
            }

            Card("Your data") {
                Text("Watched ticks, watchlist, star ratings and hidden titles live only on this phone. Back them up before you change phones or reinstall.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton("Back up", Icons.Rounded.Upload, Modifier.weight(1f)) { backup.launch("streamlens-backup-${LocalDate.now()}.json") }
                    ActionButton("Restore", Icons.Rounded.Download, Modifier.weight(1f)) { restore.launch(arrayOf("application/json", "text/plain", "*/*")) }
                }
                SwitchRow("Show hidden titles (${home.hiddenCount})", showHidden) { vm.showHidden.value = it }
            }

            AboutCard()
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.Bg2).border(1.dp, Palette.Line, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = Palette.Bulb)
        content()
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Palette.BulbInk, checkedTrackColor = Palette.Bulb,
                uncheckedThumbColor = Palette.Muted, uncheckedTrackColor = Palette.Surface2, uncheckedBorderColor = Palette.Line,
            ),
        )
    }
}

@Composable
private fun ActionButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = Palette.Surface2, border = BorderStroke(1.dp, Palette.Line), modifier = modifier) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = Palette.Ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)) }
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

/** House About card: ivory, gold seal, the app name and the signature. */
@Composable
private fun AboutCard() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = Palette.Ivory,
        border = BorderStroke(1.dp, Palette.Gold.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(vertical = 26.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.ic_seal), contentDescription = null, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            Text("StreamLens", style = MaterialTheme.typography.titleLarge, color = Palette.IvoryInk)
            Signature(size = 24.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Made for my own movie nights — what's landed on OTT, what's coming, and whether it was worth the hype.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.IvoryInk.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Box(Modifier.size(width = 40.dp, height = 1.dp).background(Palette.Gold.copy(alpha = 0.5f)))
            Spacer(Modifier.height(12.dp))
            val built = runCatching { LocalDate.parse(BuildConfig.BUILD_DATE).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)) }.getOrDefault(BuildConfig.BUILD_DATE)
            Text("Version ${BuildConfig.VERSION_NAME} · built $built", style = MaterialTheme.typography.bodySmall, color = Palette.IvoryInk.copy(alpha = 0.6f))
            Text("Ratings: IMDb, BookMyShow · box office: trade reports", style = MaterialTheme.typography.bodySmall, color = Palette.IvoryInk.copy(alpha = 0.6f), textAlign = TextAlign.Center)
        }
    }
}
