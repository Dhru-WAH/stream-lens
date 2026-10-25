package com.dhruva.streamlens.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class BackupFile(
    val app: String = "StreamLens",
    val version: Int = 1,
    val exportedAt: String,
    val marks: List<Mark>,
    val filters: Filters? = null,
    val alerts: AlertPrefs? = null,
)

/** Watched / watchlist / ratings / hidden, plus settings, as one JSON file the user picks a place for. */
class Backup(private val context: Context, private val dao: MarkDao, private val prefs: Prefs) {

    suspend fun write(uri: Uri): Int = withContext(Dispatchers.IO) {
        val marks = dao.allOnce().filter { it.watched || it.watchlist || it.rating > 0 || it.hidden }
        val file = BackupFile(exportedAt = Instant.now().toString(), marks = marks, filters = prefs.filters, alerts = prefs.alerts)
        context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use {
            it.write(FeedJson.encodeToString(BackupFile.serializer(), file))
        }
        marks.size
    }

    /** Restores by overwriting the marks it contains; anything not in the file is left alone. */
    suspend fun read(uri: Uri): Int = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
        val file = FeedJson.decodeFromString<BackupFile>(text)
        require(file.app == "StreamLens") { "Not a StreamLens backup" }
        dao.upsertAll(file.marks)
        file.alerts?.let { prefs.alerts = it }
        file.marks.size
    }
}
