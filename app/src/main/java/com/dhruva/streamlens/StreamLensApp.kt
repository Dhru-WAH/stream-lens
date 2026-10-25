package com.dhruva.streamlens

import android.app.Application
import com.dhruva.streamlens.data.AppDb
import com.dhruva.streamlens.data.Backup
import com.dhruva.streamlens.data.FeedRepository
import com.dhruva.streamlens.data.Prefs
import com.dhruva.streamlens.work.Alerts
import com.dhruva.streamlens.work.SyncWorker

class StreamLensApp : Application() {

    val db: AppDb by lazy { AppDb.get(this) }
    val prefs: Prefs by lazy { Prefs(this) }
    val feed: FeedRepository by lazy { FeedRepository(this) }
    val backup: Backup by lazy { Backup(this, db.marks(), prefs) }

    override fun onCreate() {
        super.onCreate()
        Alerts.createChannel(this)
        SyncWorker.schedule(this)
    }
}
