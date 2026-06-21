package com.xplex.privacy.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Backing store for per-app hook settings and hook assignment, reached via
 * [com.xplex.privacy.data.PrivacyContentProvider] from other apps' processes.
 * Deliberately simple for v1 - two tables, no ORM. Room can replace this
 * later without changing the ContentProvider's external call contract.
 */
class SettingsDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "privacykit.db"
        const val DB_VERSION = 1

        const val TABLE_SETTINGS = "settings"
        const val TABLE_ASSIGNMENTS = "assignments"
        const val TABLE_DIAGNOSTICS = "diagnostics"
        const val TABLE_PRESETS = "presets"

        const val STATUS_INSTALLED = "installed"
        const val STATUS_FAILED = "failed"
        const val STATUS_NOT_APPLICABLE = "not_applicable"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_SETTINGS (" +
                "package TEXT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "value TEXT, " +
                "PRIMARY KEY (package, name))"
        )
        db.execSQL(
            "CREATE TABLE $TABLE_ASSIGNMENTS (" +
                "package TEXT NOT NULL, " +
                "hookId TEXT NOT NULL, " +
                "enabled INTEGER NOT NULL DEFAULT 1, " +
                "PRIMARY KEY (package, hookId))"
        )
        db.execSQL(
            "CREATE TABLE $TABLE_DIAGNOSTICS (" +
                "package TEXT NOT NULL, " +
                "hookId TEXT NOT NULL, " +
                "status TEXT NOT NULL, " +
                "message TEXT, " +
                "timestamp INTEGER NOT NULL, " +
                "PRIMARY KEY (package, hookId))"
        )
        db.execSQL(
            "CREATE TABLE $TABLE_PRESETS (" +
                "presetName TEXT NOT NULL, " +
                "hookId TEXT NOT NULL, " +
                "value TEXT, " +
                "PRIMARY KEY (presetName, hookId))"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No upgrades yet - schema version 1 is the first shipped version.
    }

    fun getSetting(packageName: String, name: String): String? {
        readableDatabase.query(
            TABLE_SETTINGS, arrayOf("value"),
            "package = ? AND name = ?", arrayOf(packageName, name),
            null, null, null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    fun putSetting(packageName: String, name: String, value: String?) {
        val values = ContentValues().apply {
            put("package", packageName)
            put("name", name)
            put("value", value)
        }
        writableDatabase.insertWithOnConflict(
            TABLE_SETTINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getAllSettingsForPackage(packageName: String): Map<String, String?> {
        val result = LinkedHashMap<String, String?>()
        readableDatabase.query(
            TABLE_SETTINGS, arrayOf("name", "value"),
            "package = ?", arrayOf(packageName),
            null, null, null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result[cursor.getString(0)] = cursor.getString(1)
            }
        }
        return result
    }

    fun isHookEnabled(packageName: String, hookId: String): Boolean {
        readableDatabase.query(
            TABLE_ASSIGNMENTS, arrayOf("enabled"),
            "package = ? AND hookId = ?", arrayOf(packageName, hookId),
            null, null, null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) != 0 else false
        }
    }

    fun getEnabledHookIds(packageName: String): Set<String> {
        val result = LinkedHashSet<String>()
        readableDatabase.query(
            TABLE_ASSIGNMENTS, arrayOf("hookId"),
            "package = ? AND enabled = 1", arrayOf(packageName),
            null, null, null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0))
            }
        }
        return result
    }

    fun setHookEnabled(packageName: String, hookId: String, enabled: Boolean) {
        val values = ContentValues().apply {
            put("package", packageName)
            put("hookId", hookId)
            put("enabled", if (enabled) 1 else 0)
        }
        writableDatabase.insertWithOnConflict(
            TABLE_ASSIGNMENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun resetPackage(packageName: String) {
        writableDatabase.delete(TABLE_SETTINGS, "package = ?", arrayOf(packageName))
        writableDatabase.delete(TABLE_ASSIGNMENTS, "package = ?", arrayOf(packageName))
        writableDatabase.delete(TABLE_DIAGNOSTICS, "package = ?", arrayOf(packageName))
    }

    fun recordDiagnostic(packageName: String, hookId: String, status: String, message: String?) {
        val values = ContentValues().apply {
            put("package", packageName)
            put("hookId", hookId)
            put("status", status)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            TABLE_DIAGNOSTICS, null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    data class DiagnosticEntry(
        val hookId: String,
        val status: String,
        val message: String?,
        val timestamp: Long
    )

    fun getDiagnostics(packageName: String): List<DiagnosticEntry> {
        val result = mutableListOf<DiagnosticEntry>()
        readableDatabase.query(
            TABLE_DIAGNOSTICS, arrayOf("hookId", "status", "message", "timestamp"),
            "package = ?", arrayOf(packageName),
            null, null, "timestamp DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    DiagnosticEntry(
                        hookId = cursor.getString(0),
                        status = cursor.getString(1),
                        message = cursor.getString(2),
                        timestamp = cursor.getLong(3)
                    )
                )
            }
        }
        return result
    }

    /** Presets are global (not per-app) - reusable named fingerprint configs. */
    fun savePreset(presetName: String, values: Map<String, String>) {
        writableDatabase.delete(TABLE_PRESETS, "presetName = ?", arrayOf(presetName))
        writableDatabase.beginTransaction()
        try {
            for ((hookId, value) in values) {
                val row = ContentValues().apply {
                    put("presetName", presetName)
                    put("hookId", hookId)
                    put("value", value)
                }
                writableDatabase.insertWithOnConflict(TABLE_PRESETS, null, row, SQLiteDatabase.CONFLICT_REPLACE)
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun getPreset(presetName: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        readableDatabase.query(
            TABLE_PRESETS, arrayOf("hookId", "value"),
            "presetName = ?", arrayOf(presetName),
            null, null, null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result[cursor.getString(0)] = cursor.getString(1)
            }
        }
        return result
    }

    fun listPresetNames(): List<String> {
        val result = mutableListOf<String>()
        readableDatabase.query(
            true, TABLE_PRESETS, arrayOf("presetName"),
            null, null, null, null, "presetName ASC", null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0))
            }
        }
        return result
    }

    fun deletePreset(presetName: String) {
        writableDatabase.delete(TABLE_PRESETS, "presetName = ?", arrayOf(presetName))
    }
}
