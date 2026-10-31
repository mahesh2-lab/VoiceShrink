package com.example.callcompressor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Lists the watched folder, indexes recordings, and queues uncompressed files.
 * Uses strict content hash matching with URI exclusion to prevent false duplicates.
 */
class ScanWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    companion object {
        // Prevent concurrent scan passes from running simultaneously
        private val scanMutex = Mutex()
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!scanMutex.tryLock()) {
            // A scan is already in progress, avoid duplicate concurrent scanning
            return@withContext Result.success()
        }

        try {
            val ctx = applicationContext
            val tree = Prefs.treeUri(ctx) ?: return@withContext Result.success()
            val root = DocumentFile.fromTreeUri(ctx, tree) ?: return@withContext Result.failure()
            val dao = AppDb.get(ctx).dao()
            val now = System.currentTimeMillis()

            val files = runCatching { root.listFiles() }.getOrNull() ?: return@withContext Result.failure()
            val outDir = root.findFile(Config.OUTPUT_DIR)?.takeIf { it.isDirectory }

            // Pre-clean any corrupt false duplicates in DB before scanning
            dao.cleanCorruptedDuplicates()

            for (f in files) {
                if (!f.isFile) continue                       // skips directories (including "compressed")
                val name = f.name ?: continue
                if (name.equals(Config.OUTPUT_DIR, ignoreCase = true)) continue
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext !in Config.AUDIO_EXTS) continue

                val size = f.length()
                val modified = f.lastModified()
                val age = now - modified
                // Settle window: ignore files modified in the last 2 seconds (actively being written)
                if (size <= 0 || (age in 0 until Config.SETTLE_MS)) continue

                val uri = f.uri.toString()

                // Check if this exact file URI is already tracked in our database
                val existing = dao.findByUri(uri)
                if (existing != null) {
                    val outName = name.substringBeforeLast('.') + ".ogg"
                    val existingOut = outDir?.findFile(outName)?.takeIf { it.exists() && it.length() > 0 }
                    if (existingOut != null) {
                        dao.markDone(existing.id, existingOut.length(), existingOut.uri.toString())
                    } else if (existing.status == Status.PENDING) {
                        val sha = sha256(f.uri) ?: existing.sha256
                        dao.updateFile(existing.id, sha, size, modified, Status.PENDING)
                    }
                    if (existing.durationMs <= 0L) {
                        val durMs = extractAudioDuration(ctx, f.uri)
                        if (durMs > 0L) {
                            dao.updateDuration(existing.id, durMs)
                        }
                    }
                    continue
                }

                // Brand new file
                val sha = sha256(f.uri) ?: continue
                val durMs = extractAudioDuration(ctx, f.uri)

                // Check if already compressed in the output folder
                val outName = name.substringBeforeLast('.') + ".ogg"
                val existingOut = outDir?.findFile(outName)?.takeIf { it.exists() && it.length() > 0 }
                val status: String
                val outSize: Long
                val outUri: String?

                if (existingOut != null) {
                    status = Status.DONE
                    outSize = existingOut.length()
                    outUri = existingOut.uri.toString()
                } else {
                    status = Status.PENDING
                    outSize = 0
                    outUri = null
                }

                dao.insert(
                    Recording(
                        sha256 = sha,
                        uri = uri,
                        name = name,
                        sizeBytes = size,
                        lastModified = modified,
                        status = status,
                        outSizeBytes = outSize,
                        outUri = outUri,
                        durationMs = durMs,
                    )
                )
            }

            if (dao.pendingCount() > 0) {
                Scheduler.processNow(ctx)
            }
            Result.success()
        } finally {
            scanMutex.unlock()
        }
    }

    private fun extractAudioDuration(context: Context, uri: Uri): Long {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, uri)
            val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            mmr.release()
            durStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun sha256(uri: Uri): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val stream = applicationContext.contentResolver.openInputStream(uri) ?: return null
            stream.use { ins ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    md.update(buf, 0, n)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
