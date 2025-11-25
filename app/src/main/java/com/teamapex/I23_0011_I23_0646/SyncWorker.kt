package com.teamapex.I23_0011_I23_0646

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = OfflineRepository(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "SyncWorker"
        private const val BASE_URL = "http://192.168.18.35/socially_app/"
        private const val WORK_NAME = "offline_sync_work"
        private const val MAX_RETRY_COUNT = 3

        /**
         * Schedule periodic sync every 15 minutes
         */
        fun scheduleSyncWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            )
        }

        /**
         * Trigger immediate sync
         */
        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncWorkRequest)
        }

        /**
         * Cancel all sync work
         */
        fun cancelSyncWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting sync worker...")

        // Check network availability
        if (!NetworkUtil.isNetworkAvailable(applicationContext)) {
            Log.d(TAG, "No network available, rescheduling...")
            return@withContext Result.retry()
        }

        try {
            // Get all pending actions
            val pendingActions = repository.getPendingActions()
            Log.d(TAG, "Found ${pendingActions.size} pending actions to sync")

            if (pendingActions.isEmpty()) {
                // Clean up completed actions
                repository.deleteCompletedActions()
                return@withContext Result.success()
            }

            var successCount = 0
            var failCount = 0

            // Process each pending action
            for (action in pendingActions) {
                try {
                    // Update status to processing
                    repository.updateActionStatus(action.id, "processing", null)

                    // Execute the action based on type
                    val success = when (action.actionType) {
                        "send_message" -> syncSendMessage(action)
                        "upload_post" -> syncUploadPost(action)
                        "upload_story" -> syncUploadStory(action)
                        "like_post" -> syncLikePost(action)
                        else -> {
                            Log.w(TAG, "Unknown action type: ${action.actionType}")
                            false
                        }
                    }

                    if (success) {
                        // Mark as completed and delete
                        repository.updateActionStatus(action.id, "completed", null)
                        repository.deleteAction(action.id)
                        successCount++
                        Log.d(TAG, "Successfully synced action ${action.id}")
                    } else {
                        // Check retry count
                        if (action.retryCount >= MAX_RETRY_COUNT) {
                            repository.updateActionStatus(
                                action.id,
                                "failed",
                                "Max retries exceeded"
                            )
                            failCount++
                        } else {
                            repository.updateActionStatus(
                                action.id,
                                "pending",
                                "Retry ${action.retryCount + 1}"
                            )
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing action ${action.id}: ${e.message}", e)
                    repository.updateActionStatus(action.id, "pending", e.message)
                    failCount++
                }
            }

            Log.d(TAG, "Sync completed: $successCount success, $failCount failed")

            // Return success if at least some actions succeeded
            return@withContext if (successCount > 0) {
                Result.success()
            } else if (failCount > 0) {
                Result.retry()
            } else {
                Result.failure()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Sync worker error: ${e.message}", e)
            return@withContext Result.retry()
        }
    }

    private suspend fun syncSendMessage(action: QueuedAction): Boolean {
        return try {
            val payload = JSONObject(action.payload)

            val formBody = FormBody.Builder()
                .add("chat_id", payload.getString("chat_id"))
                .add("sender_id", payload.getString("sender_id"))
                .add("message_type", payload.getString("message_type"))
                .add("content", payload.optString("content", ""))
                .add("vanish_mode", payload.optString("vanish_mode", "0"))

            if (payload.has("media_data")) {
                formBody.add("media_data", payload.getString("media_data"))
            }

            if (payload.has("shared_post_id")) {
                formBody.add("shared_post_id", payload.getString("shared_post_id"))
            }

            val request = Request.Builder()
                .url("${BASE_URL}send_message.php")
                .post(formBody.build())
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                if (json.getInt("statuscode") == 200) {
                    Log.d(TAG, "Message sent successfully")
                    return true
                }
            }

            Log.w(TAG, "Failed to send message: $responseBody")
            false

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing message: ${e.message}", e)
            false
        }
    }

    private suspend fun syncUploadPost(action: QueuedAction): Boolean {
        return try {
            val payload = JSONObject(action.payload)

            val formBody = FormBody.Builder()
                .add("user_id", payload.getString("user_id"))
                .add("image_data", payload.getString("image_data"))
                .add("caption", payload.getString("caption"))
                .build()

            val request = Request.Builder()
                .url("${BASE_URL}upload_post.php")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                if (json.getInt("statuscode") == 200) {
                    Log.d(TAG, "Post uploaded successfully")
                    return true
                }
            }

            Log.w(TAG, "Failed to upload post: $responseBody")
            false

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing post: ${e.message}", e)
            false
        }
    }

    private suspend fun syncUploadStory(action: QueuedAction): Boolean {
        return try {
            val payload = JSONObject(action.payload)

            val formBody = FormBody.Builder()
                .add("user_id", payload.getString("user_id"))
                .add("media_data", payload.getString("media_data"))
                .add("media_type", payload.getString("media_type"))
                .build()

            val request = Request.Builder()
                .url("${BASE_URL}upload_story.php")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                if (json.getInt("statuscode") == 200) {
                    Log.d(TAG, "Story uploaded successfully")
                    return true
                }
            }

            Log.w(TAG, "Failed to upload story: $responseBody")
            false

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing story: ${e.message}", e)
            false
        }
    }

    private suspend fun syncLikePost(action: QueuedAction): Boolean {
        return try {
            val payload = JSONObject(action.payload)

            val formBody = FormBody.Builder()
                .add("user_id", payload.getString("user_id"))
                .add("post_id", payload.getString("post_id"))
                .build()

            val request = Request.Builder()
                .url("${BASE_URL}like_post.php")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                if (json.getInt("statuscode") == 200) {
                    Log.d(TAG, "Post liked successfully")
                    return true
                }
            }

            Log.w(TAG, "Failed to like post: $responseBody")
            false

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing like: ${e.message}", e)
            false
        }
    }
}