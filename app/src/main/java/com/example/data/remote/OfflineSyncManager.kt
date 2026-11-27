package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.local.PendingSyncEntity
import com.example.data.local.StudyHubDao
import com.example.data.model.SupabaseMappers
import com.example.data.model.SyncStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class OfflineSyncManager(
    context: Context,
    private val dao: StudyHubDao,
    private val supabase: SupabaseRestClient
) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _syncStatus = MutableStateFlow(if (_isOnline.value) SyncStatus.ONLINE else SyncStatus.OFFLINE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var activeUserId: String? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            _syncStatus.value = SyncStatus.ONLINE
            activeUserId?.let { uid -> scope.launch { flushPendingSync(uid) } }
        }

        override fun onLost(network: Network) {
            val online = checkCurrentConnectivity()
            _isOnline.value = online
            if (!online) {
                _syncStatus.value = SyncStatus.OFFLINE
            }
        }
    }

    init {
        runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }
    }

    fun attachUser(userId: String) {
        activeUserId = userId
        if (_isOnline.value) {
            scope.launch { flushPendingSync(userId) }
        } else {
            _syncStatus.value = SyncStatus.OFFLINE
        }
    }

    fun detachUser() {
        activeUserId = null
    }

    private fun checkCurrentConnectivity(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun flushPendingSync(userId: String): Result<Int> {
        if (userId.isBlank()) return Result.success(0)
        if (!checkCurrentConnectivity()) {
            _isOnline.value = false
            _syncStatus.value = SyncStatus.OFFLINE
            return Result.success(0)
        }
        if (!supabase.isConfigured()) {
            _syncStatus.value = SyncStatus.ONLINE
            return Result.success(0)
        }
        val pending = dao.getPendingSyncOnce(userId)
        if (pending.isEmpty()) {
            _syncStatus.value = SyncStatus.SYNCED
            return Result.success(0)
        }

        _syncStatus.value = SyncStatus.SYNCING
        var syncedCount = 0
        var hadFailure = false

        for (item in pending) {
            try {
                executeSyncAction(userId, item)
                dao.removeSync(item.syncKey)
                syncedCount++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                hadFailure = true
                handleCloudError(e, OperationType.WRITE, "sync/${item.actionType}/${item.targetId}")
                dao.markSyncFailed(item.syncKey, e.message ?: "Sync failed")
            }
        }

        _syncStatus.value = when {
            !checkCurrentConnectivity() -> SyncStatus.OFFLINE
            hadFailure -> SyncStatus.SYNC_FAILED
            else -> SyncStatus.SYNCED
        }
        return Result.success(syncedCount)
    }

    private suspend fun executeSyncAction(userId: String, item: PendingSyncEntity) {
        val json = JSONObject(item.payloadJson)
        val rowId = "${userId}__${item.targetId}"
        val nowIso = SupabaseMappers.nowIso()

        when (item.actionType) {
            ACTION_UPSERT_BOOKMARK -> {
                val payload = JSONObject().apply {
                    put("id", rowId)
                    put("user_id", userId)
                    put("material_id", json.getString("materialId"))
                    put("title", json.getString("title"))
                    put("subject_name", json.getString("subjectName"))
                    put("class_grade", json.getLong("classGrade"))
                    put("material_type", json.getString("materialType"))
                    put("subtitle", json.getString("subtitle"))
                    put("accent_key", json.getString("accentKey"))
                    put("updated_at", nowIso)
                }
                supabase.upsertOne("bookmarks", payload)
            }

            ACTION_DELETE_BOOKMARK -> {
                supabase.deleteWhere(
                    "bookmarks",
                    mapOf("id" to "eq.$rowId", "user_id" to "eq.$userId")
                )
            }

            ACTION_ADD_QUIZ_ATTEMPT -> {
                val payload = JSONObject().apply {
                    put("id", rowId)
                    put("user_id", userId)
                    put("quiz_id", json.getString("quizId"))
                    put("quiz_title", json.getString("quizTitle"))
                    put("subject_name", json.getString("subjectName"))
                    put("class_grade", json.getLong("classGrade"))
                    put("mode", json.getString("mode"))
                    put("total_questions", json.getLong("totalQuestions"))
                    put("correct_answers", json.getLong("correctAnswers"))
                    put("score_percent", json.getLong("scorePercent"))
                    put("duration_seconds", json.getLong("durationSeconds"))
                    put("points_earned", json.getLong("pointsEarned"))
                    put("created_at", nowIso)
                    put("updated_at", nowIso)
                }
                supabase.upsertOne("quiz_attempts", payload)
            }

            ACTION_UPSERT_PROGRESS -> {
                val payload = JSONObject().apply {
                    put("id", rowId)
                    put("user_id", userId)
                    put("material_id", json.getString("materialId"))
                    put("title", json.getString("title"))
                    put("subject_name", json.getString("subjectName"))
                    put("chapter_title", json.getString("chapterTitle"))
                    put("class_grade", json.getLong("classGrade"))
                    put("progress_percent", json.getLong("progressPercent"))
                    put("updated_at", nowIso)
                }
                supabase.upsertOne("reading_progress", payload)
            }

            ACTION_ADD_DOWNLOAD -> {
                val payload = JSONObject().apply {
                    put("id", rowId)
                    put("user_id", userId)
                    put("material_id", json.getString("materialId"))
                    put("title", json.getString("title"))
                    put("subject_name", json.getString("subjectName"))
                    put("class_grade", json.getLong("classGrade"))
                    put("file_size_label", json.getString("fileSizeLabel"))
                    put("updated_at", nowIso)
                }
                supabase.upsertOne("downloads", payload)
            }

            ACTION_DELETE_DOWNLOAD -> {
                supabase.deleteWhere(
                    "downloads",
                    mapOf("id" to "eq.$rowId", "user_id" to "eq.$userId")
                )
            }
        }
    }

    companion object {
        const val ACTION_UPSERT_BOOKMARK = "UPSERT_BOOKMARK"
        const val ACTION_DELETE_BOOKMARK = "DELETE_BOOKMARK"
        const val ACTION_ADD_QUIZ_ATTEMPT = "ADD_QUIZ_ATTEMPT"
        const val ACTION_UPSERT_PROGRESS = "UPSERT_PROGRESS"
        const val ACTION_ADD_DOWNLOAD = "ADD_DOWNLOAD"
        const val ACTION_DELETE_DOWNLOAD = "DELETE_DOWNLOAD"
    }
}
