package app.subtle.player.data.storage

import android.content.Intent

/**
 * Grants read access to the whole shared storage.
 *
 * API 30+ uses "All files access" (`MANAGE_EXTERNAL_STORAGE`) via a Settings screen.
 * API 26-29 uses the `READ_EXTERNAL_STORAGE` runtime permission. Exactly one of
 * [allFilesIntent] / [requiredRuntimePermission] is non-null for a given device.
 */
interface StorageAccess {
    fun isGranted(): Boolean
    fun allFilesIntent(): Intent?
    fun requiredRuntimePermission(): String?
}
