package com.dhruva.streamlens.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dhruva.streamlens.R
import com.dhruva.streamlens.data.Feed
import com.dhruva.streamlens.data.Mark
import com.dhruva.streamlens.data.Prefs
import com.dhruva.streamlens.data.Release
import com.dhruva.streamlens.ui.MainActivity
import java.time.LocalDate

/** Decides what's worth a notification after a sync, and posts it. */
object Alerts {
    const val CHANNEL = "releases"
    const val EXTRA_OPEN_ID = "openId"
    private const val ID_WATCHLIST = 1
    private const val ID_NEW = 2

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Release alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Watchlist titles that start streaming, and new releases in your languages"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Run after the background sync. [Prefs.knownIds] is empty only before the first sync ever,
     * so the whole starting list never arrives as one giant "new releases" alert.
     */
    fun afterSync(context: Context, feed: Feed, marks: List<Mark>, prefs: Prefs, today: LocalDate = LocalDate.now()) {
        val titles = feed.titles.filter { it.isVisible }
        val byId = marks.associateBy { it.id }
        val alerts = prefs.alerts

        if (alerts.watchlistStreaming) {
            val notified = prefs.notifiedIds
            val due = titles.filter { r ->
                val m = byId[r.id]
                m != null && m.watchlist && !m.watched && !m.hidden && r.isOut(today) && r.id !in notified
            }
            if (due.isNotEmpty()) {
                val title = if (due.size == 1) "${due[0].title} is streaming now" else "${due.size} watchlist titles are streaming now"
                val text = due.joinToString("\n") { "${it.title} · ${it.platform ?: "OTT"}" }
                post(context, ID_WATCHLIST, title, text, due.singleOrNull()?.id)
                prefs.notifiedIds = notified + due.map { it.id }
            }
        }

        val known = prefs.knownIds
        if (known.isNotEmpty()) {
            val fresh = titles.filter { it.id !in known && alerts.wantsNew(it.lang) && byId[it.id]?.hidden != true }
            if (fresh.isNotEmpty()) {
                val langs = fresh.map { it.langName }.distinct().joinToString(" & ")
                val title = if (fresh.size == 1) "New $langs release: ${fresh[0].title}" else "${fresh.size} new $langs releases"
                val text = fresh.joinToString("\n") { r -> "${r.title} · ${r.platform ?: "OTT"}${whenText(r, today)}" }
                post(context, ID_NEW, title, text, fresh.singleOrNull()?.id)
            }
        }
        markSeen(prefs, feed)
    }

    /** The app shows new titles itself, so an in-app refresh just records them as seen. */
    fun markSeen(prefs: Prefs, feed: Feed) {
        prefs.knownIds = feed.titles.map { it.id }.toSet()
    }

    private fun whenText(r: Release, today: LocalDate): String {
        val d = r.ott ?: return " · date TBA"
        return if (d.isAfter(today)) " · from ${d.dayOfMonth} ${d.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}" else ""
    }

    private fun post(context: Context, id: Int, title: String, text: String, openId: String?) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            openId?.let { putExtra(EXTRA_OPEN_ID, it) }
        }
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(0xFFFFB547.toInt())
            .setContentTitle(title)
            .setContentText(text.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
