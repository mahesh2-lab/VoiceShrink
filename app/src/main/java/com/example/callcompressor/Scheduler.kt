package com.example.callcompressor

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object Scheduler {

    /** Schedule recurring background scan (~15 min) without forcing an immediate scan */
    fun schedulePeriodic(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        wm.enqueueUniquePeriodicWork(
            "scan",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ScanWorker>(15, TimeUnit.MINUTES).build(),
        )
    }

    /** Helper called when a new folder is picked: sets up periodic scan and kicks off initial scan */
    fun onFolderConfigured(ctx: Context) {
        schedulePeriodic(ctx)
        scanNow(ctx)
    }

    /** Run an immediate one-time folder scan and trigger background processing */
    fun scanNow(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "scan-now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ScanWorker>().build(),
        )
        processNow(ctx)
    }

    /** Heavy work (decode + Opus encode): runs with user-configured constraints */
    fun enqueueProcess(ctx: Context) {
        val requireCharging = Prefs.requireCharging(ctx)
        val constraintsBuilder = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
        if (requireCharging) {
            constraintsBuilder.setRequiresCharging(true)
        }
        val constraints = constraintsBuilder.build()

        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "process",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ProcessWorker>().setConstraints(constraints).build(),
        )
    }

    /** Force process immediately without waiting for charger */
    fun processNow(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "process",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ProcessWorker>().build(),
        )
    }
}
