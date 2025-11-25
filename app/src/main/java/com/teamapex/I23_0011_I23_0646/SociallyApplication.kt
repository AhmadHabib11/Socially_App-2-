package com.teamapex.I23_0011_I23_0646

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SociallyApplication : Application() {

    lateinit var offlineRepository: OfflineRepository
        private set

    override fun onCreate() {
        super.onCreate()

        Log.d("SociallyApp", "Initializing application...")

        // Initialize offline repository
        offlineRepository = OfflineRepository(this)

        // Schedule background sync
        SyncWorker.scheduleSyncWork(this)

        // Monitor network connectivity
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            NetworkUtil.observeNetworkConnectivity(this@SociallyApplication)
                .collect { isConnected ->
                    Log.d("SociallyApp", "Network status: ${if (isConnected) "Connected" else "Disconnected"}")

                    if (isConnected) {
                        // Trigger immediate sync when network becomes available
                        val pendingCount = offlineRepository.getPendingActionsCount()
                        if (pendingCount > 0) {
                            Log.d("SociallyApp", "Network restored. Syncing $pendingCount pending actions...")
                            SyncWorker.triggerImmediateSync(this@SociallyApplication)
                        }
                    }
                }
        }

        Log.d("SociallyApp", "Application initialized successfully")
    }

    companion object {
        private const val TAG = "SociallyApplication"
    }
}