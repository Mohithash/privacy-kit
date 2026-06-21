package com.xplex.privacy.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.util.Log

private const val TAG = "PrivacyKit.Provider"

const val METHOD_GET_SETTING = "getSetting"
const val METHOD_PUT_SETTING = "putSetting"
const val METHOD_GET_ALL_SETTINGS = "getAllSettings"
const val METHOD_IS_HOOK_ENABLED = "isHookEnabled"
const val METHOD_GET_ENABLED_HOOK_IDS = "getEnabledHookIds"
const val METHOD_SET_HOOK_ENABLED = "setHookEnabled"
const val METHOD_RESET_PACKAGE = "resetPackage"
const val METHOD_RECORD_DIAGNOSTIC = "recordDiagnostic"
const val METHOD_GET_DIAGNOSTICS = "getDiagnostics"
const val METHOD_SAVE_PRESET = "savePreset"
const val METHOD_GET_PRESET = "getPreset"
const val METHOD_LIST_PRESETS = "listPresets"
const val METHOD_DELETE_PRESET = "deletePreset"
const val METHOD_LIST_CONFIGURED_PACKAGES = "listConfiguredPackages"

const val ARG_PACKAGE = "package"
const val ARG_NAME = "name"
const val ARG_VALUE = "value"
const val ARG_HOOK_ID = "hookId"
const val ARG_ENABLED = "enabled"
const val ARG_STATUS = "status"
const val ARG_MESSAGE = "message"
const val ARG_PRESET_NAME = "presetName"

/**
 * Exposed as eu.faircode-style cross-process IPC: hooked apps call in to
 * read their own settings/assignment, and the management UI calls in to
 * write them. Every call is required to declare which package it's acting
 * on behalf of via [ARG_PACKAGE]; [verifyCaller] checks that the caller's
 * real UID actually owns that package name (or is this app itself, for the
 * management UI), closing the gap the old project's equivalent provider had
 * - exported with zero caller validation.
 */
class PrivacyContentProvider : ContentProvider() {

    private lateinit var db: SettingsDatabase

    override fun onCreate(): Boolean {
        db = SettingsDatabase(context!!.applicationContext)
        return true
    }

    private fun verifyCaller(claimedPackage: String): Boolean {
        val ctx = context ?: return false
        val callingUid = Binder.getCallingUid()
        val pm = ctx.packageManager
        val packagesForUid = pm.getPackagesForUid(callingUid) ?: return false
        if (claimedPackage in packagesForUid) return true
        // The management app itself is allowed to act on behalf of any
        // package (it's writing settings/assignments for apps it's
        // configuring, not pretending to be them).
        return ctx.packageName in packagesForUid
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val packageName = extras?.getString(ARG_PACKAGE)
        if (packageName == null) {
            Log.w(TAG, "call($method) missing $ARG_PACKAGE")
            return null
        }
        if (!verifyCaller(packageName)) {
            Log.w(TAG, "call($method) rejected: caller uid=${Binder.getCallingUid()} claimed package=$packageName")
            return null
        }

        return when (method) {
            METHOD_GET_SETTING -> Bundle().apply {
                val name = extras.getString(ARG_NAME) ?: return null
                putString(ARG_VALUE, db.getSetting(packageName, name))
            }

            METHOD_PUT_SETTING -> {
                val name = extras.getString(ARG_NAME) ?: return null
                db.putSetting(packageName, name, extras.getString(ARG_VALUE))
                Bundle()
            }

            METHOD_GET_ALL_SETTINGS -> Bundle().apply {
                val map = db.getAllSettingsForPackage(packageName)
                putStringArray("keys", map.keys.toTypedArray())
                putStringArray("values", map.values.toTypedArray())
            }

            METHOD_IS_HOOK_ENABLED -> Bundle().apply {
                val hookId = extras.getString(ARG_HOOK_ID) ?: return null
                putBoolean(ARG_ENABLED, db.isHookEnabled(packageName, hookId))
            }

            METHOD_GET_ENABLED_HOOK_IDS -> Bundle().apply {
                putStringArray("hookIds", db.getEnabledHookIds(packageName).toTypedArray())
            }

            METHOD_SET_HOOK_ENABLED -> {
                val hookId = extras.getString(ARG_HOOK_ID) ?: return null
                db.setHookEnabled(packageName, hookId, extras.getBoolean(ARG_ENABLED, true))
                Bundle()
            }

            METHOD_RESET_PACKAGE -> {
                db.resetPackage(packageName)
                Bundle()
            }

            METHOD_RECORD_DIAGNOSTIC -> {
                val hookId = extras.getString(ARG_HOOK_ID) ?: return null
                val status = extras.getString(ARG_STATUS) ?: return null
                db.recordDiagnostic(packageName, hookId, status, extras.getString(ARG_MESSAGE))
                Bundle()
            }

            METHOD_GET_DIAGNOSTICS -> Bundle().apply {
                val entries = db.getDiagnostics(packageName)
                putStringArray("hookIds", entries.map { it.hookId }.toTypedArray())
                putStringArray("statuses", entries.map { it.status }.toTypedArray())
                putStringArray("messages", entries.map { it.message ?: "" }.toTypedArray())
                putLongArray("timestamps", entries.map { it.timestamp }.toLongArray())
            }

            METHOD_SAVE_PRESET -> {
                val presetName = extras.getString(ARG_PRESET_NAME) ?: return null
                val hookIds = extras.getStringArray("hookIds") ?: return null
                val values = extras.getStringArray("values") ?: return null
                val map = hookIds.indices.associate { hookIds[it] to values[it] }
                db.savePreset(presetName, map)
                Bundle()
            }

            METHOD_GET_PRESET -> Bundle().apply {
                val presetName = extras.getString(ARG_PRESET_NAME) ?: return null
                val map = db.getPreset(presetName)
                putStringArray("hookIds", map.keys.toTypedArray())
                putStringArray("values", map.values.toTypedArray())
            }

            METHOD_LIST_PRESETS -> Bundle().apply {
                putStringArray("presetNames", db.listPresetNames().toTypedArray())
            }

            METHOD_DELETE_PRESET -> {
                val presetName = extras.getString(ARG_PRESET_NAME) ?: return null
                db.deletePreset(presetName)
                Bundle()
            }

            METHOD_LIST_CONFIGURED_PACKAGES -> Bundle().apply {
                putStringArray("packages", db.listConfiguredPackages().toTypedArray())
            }

            else -> {
                Log.w(TAG, "Unknown method: $method")
                null
            }
        }
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
