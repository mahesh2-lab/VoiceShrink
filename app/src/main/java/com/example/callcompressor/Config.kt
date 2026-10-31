package com.example.callcompressor

import android.content.Context
import android.net.Uri

object Config {
    const val OUTPUT_DIR = "compressed"          // created inside the watched folder
    const val SETTLE_MS = 2_000L                 // ignore files modified in the last 2 s (actively being written)

    val AUDIO_EXTS = setOf(
        "m4a", "mp3", "wav", "aac", "amr", "3gp", "ogg", "opus", "flac", "mp4", "wma",
    )
}

object Prefs {
    private const val FILE = "prefs"
    private const val KEY_TREE = "tree_uri"
    private const val KEY_BITRATE = "bitrate"
    private const val KEY_DELETE_ORIGINAL = "delete_original"
    private const val KEY_REQUIRE_CHARGING = "require_charging"
    private const val KEY_HIDE_PHONE_NUMBERS = "hide_phone_numbers"

    fun treeUri(ctx: Context): Uri? =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_TREE, null)?.let(Uri::parse)

    fun setTreeUri(ctx: Context, uri: Uri?) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY_TREE, uri?.toString())
            .apply()
    }

    fun bitrate(ctx: Context): Int =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_BITRATE, 24_000)

    fun setBitrate(ctx: Context, bitrate: Int) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt(KEY_BITRATE, bitrate)
            .apply()
    }

    fun deleteOriginal(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_DELETE_ORIGINAL, false)

    fun setDeleteOriginal(ctx: Context, delete: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_DELETE_ORIGINAL, delete)
            .apply()
    }

    fun requireCharging(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_REQUIRE_CHARGING, false)

    fun setRequireCharging(ctx: Context, require: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_REQUIRE_CHARGING, require)
            .apply()
    }

    fun hidePhoneNumbers(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_HIDE_PHONE_NUMBERS, false)

    fun setHidePhoneNumbers(ctx: Context, hide: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_HIDE_PHONE_NUMBERS, hide)
            .apply()
    }
}
