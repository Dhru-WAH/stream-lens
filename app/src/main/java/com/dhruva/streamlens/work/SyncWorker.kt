package com.dhruva.streamlens.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dhruva.streamlens.StreamLensApp
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Once a day, after the Claude refresh job has pushed the morning's list: download it and post alerts
 * for watchlist titles that started streaming and new titles in my languages.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as StreamLensApp
        val feed = app.feed.refresh().getOrElse {
            return if (runAttemptCount < 3) Result.retry() else Result.success()
        }
        app.prefs.lastSync = System.currentTimeMillis()
        Alerts.afterSync(applicationContext, feed, app.db.marks().allOnce(), app.prefs)
        return Result.success()
    }

    companion object {
        private const val NAME = "daily-sync"
        /** The refresh job runs early morning IST; checking at 10:00 leaves it plenty of time. */
        private val AT = LocalTime.of(10, 0)

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(AT)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
