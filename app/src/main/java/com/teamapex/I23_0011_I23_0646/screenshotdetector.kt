package com.teamapex.I23_0011_I23_0646

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import java.util.*

class ScreenshotDetector(
    private val context: Context,
    private val onScreenshotDetected: () -> Unit
) {

    private var contentObserver: ContentObserver? = null
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "ScreenshotDetector"
        private val SCREENSHOT_PATHS = arrayOf(
            "screenshot",
            "screen_shot",
            "screencapture",
            "screen-capture",
            "screen capture",
            "screencap"
        )
    }

    fun startDetection() {
        if (contentObserver != null) {
            Log.w(TAG, "Screenshot detection already active")
            return
        }

        contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)

                uri?.let {
                    if (isScreenshotUri(it)) {
                        Log.d(TAG, "📸 Screenshot detected!")
                        onScreenshotDetected()
                    }
                }
            }
        }

        // Register observer for external storage images
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!
        )

        Log.d(TAG, "✓ Screenshot detection started")
    }

    fun stopDetection() {
        contentObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
            contentObserver = null
            Log.d(TAG, "✓ Screenshot detection stopped")
        }
    }

    private fun isScreenshotUri(uri: Uri): Boolean {
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED
            )

            cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )

            if (cursor != null && cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                val dateAddedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)

                if (displayNameIndex >= 0 && dataIndex >= 0 && dateAddedIndex >= 0) {
                    val displayName = cursor.getString(displayNameIndex)?.lowercase(Locale.ROOT) ?: ""
                    val path = cursor.getString(dataIndex)?.lowercase(Locale.ROOT) ?: ""
                    val dateAdded = cursor.getLong(dateAddedIndex)

                    // Check if image was added in the last 2 seconds
                    val currentTime = System.currentTimeMillis() / 1000
                    if (currentTime - dateAdded > 2) {
                        return false
                    }

                    // Check if path or name contains screenshot keywords
                    val isScreenshot = SCREENSHOT_PATHS.any { keyword ->
                        displayName.contains(keyword) || path.contains(keyword)
                    }

                    if (isScreenshot) {
                        Log.d(TAG, "Screenshot match: $displayName | $path")
                    }

                    return isScreenshot
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking screenshot: ${e.message}")
        } finally {
            cursor?.close()
        }

        return false
    }
}