package com.voiceshrink.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Heavy work. Scheduled with WorkManager constraints (or runs on demand),
 * and stops cleanly if interrupted. Unfinished files stay PENDING and resume later.
 */
class ProcessWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ctx = applicationContext
        val tree = Prefs.treeUri(ctx) ?: return@withContext Result.success()
        val root = DocumentFile.fromTreeUri(ctx, tree) ?: return@withContext Result.failure()
        val outDir = root.findFile(Config.OUTPUT_DIR)?.takeIf { it.isDirectory }
            ?: root.createDirectory(Config.OUTPUT_DIR)
            ?: return@withContext Result.failure()
        val dao = AppDb.get(ctx).dao()
        val compressor = OpusCompressor(ctx)
        val bitrate = Prefs.bitrate(ctx)
        val deleteOriginal = Prefs.deleteOriginal(ctx)

        val initialPending = dao.pendingCount()
        if (initialPending > 0) {
            runCatching { setForeground(createForegroundInfo(ctx, initialPending)) }
        }

        while (true) {
            val batch = dao.nextPending()
            if (batch.isEmpty()) break

            for (rec in batch) {
                if (isStopped) return@withContext Result.retry()

                val tmp = File(ctx.cacheDir, "job-${rec.id}.ogg")
                var out: DocumentFile? = null
                try {
                    val finished = compressor.compress(Uri.parse(rec.uri), tmp, bitrate) { !isStopped }
                    if (!finished) return@withContext Result.retry()   // charger unplugged or cancelled: retry later

                    val outName = rec.name.substringBeforeLast('.') + ".ogg"
                    // Delete existing file with the same name if present so SAF doesn't create "call (1).ogg"
                    outDir.findFile(outName)?.delete()
                    out = outDir.createFile("audio/ogg", outName) ?: error("Cannot create $outName")

                    ctx.contentResolver.openOutputStream(out.uri)!!.use { os ->
                        tmp.inputStream().use { it.copyTo(os) }
                    }

                    val outSize = tmp.length()
                    val outUri = out.uri.toString()
                    withContext(NonCancellable) { dao.markDone(rec.id, outSize, outUri) }

                    if (deleteOriginal && outSize > 0) {
                        val origUri = Uri.parse(rec.uri)
                        val deleted = DocumentFile.fromSingleUri(ctx, origUri)?.delete() == true
                        if (!deleted) {
                            runCatching {
                                DocumentsContract.deleteDocument(ctx.contentResolver, origUri)
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    out?.delete()
                    throw e
                } catch (e: Exception) {
                    out?.delete()
                    withContext(NonCancellable) { dao.markFailed(rec.id, e.message ?: e.javaClass.simpleName) }
                } finally {
                    tmp.delete()
                }
            }
        }
        Result.success()
    }

    private fun createForegroundInfo(ctx: Context, count: Int): ForegroundInfo {
        val channelId = "voiceshrink_bg"
        val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "VoiceShrink Compression Progress", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(ctx, channelId)
            .setContentTitle("VoiceShrink")
            .setContentText("Compressing $count audio recording(s) in background...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        return ForegroundInfo(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }
}
