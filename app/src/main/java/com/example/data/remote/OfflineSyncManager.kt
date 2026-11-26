package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.local.PendingSyncEntity
import com.example.data.local.StudyHubDao
import com.example.data.model.SyncStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class OfflineSyncManager(
    context: Context,
    private val dao: StudyHubDao,
    private val db: FirebaseFirestore
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
                handleFirestoreError(e, OperationType.WRITE, "sync/${item.actionType}/${item.targetId}")
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
        val userRef = db.collection("users").document(userId)

        when (item.actionType) {
            ACTION_UPSERT_BOOKMARK -> {
                val docRef = userRef.collection("bookmarks").document(item.targetId)
                val exists = docRef.get().await().exists()
                val updatePayload = mapOf(
                    "materialId" to json.getString("materialId"),
                    "title" to json.getString("title"),
                    "subjectName" to json.getString("subjectName"),
                    "classGrade" to json.getLong("classGrade"),
                    "materialType" to json.getString("materialType"),
                    "subtitle" to json.getString("subtitle"),
                    "accentKey" to json.getString("accentKey"),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (exists) {
                    docRef.update(updatePayload).await()
                } else {
                    docRef.set(updatePayload + ("createdAt" to FieldValue.serverTimestamp())).await()
                }
            }

            ACTION_DELETE_BOOKMARK -> {
                userRef.collection("bookmarks").document(item.targetId).delete().await()
            }

            ACTION_ADD_QUIZ_ATTEMPT -> {
                val docRef = userRef.collection("quiz_attempts").document(item.targetId)
                val payload = mapOf(
                    "quizId" to json.getString("quizId"),
                    "quizTitle" to json.getString("quizTitle"),
                    "subjectName" to json.getString("subjectName"),
                    "classGrade" to json.getLong("classGrade"),
                    "mode" to json.getString("mode"),
                    "totalQuestions" to json.getLong("totalQuestions"),
                    "correctAnswers" to json.getLong("correctAnswers"),
                    "scorePercent" to json.getLong("scorePercent"),
                    "durationSeconds" to json.getLong("durationSeconds"),
                    "pointsEarned" to json.getLong("pointsEarned"),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }

            ACTION_UPSERT_PROGRESS -> {
                val docRef = userRef.collection("reading_progress").document(item.targetId)
                val exists = docRef.get().await().exists()
                if (exists) {
                    val updatePayload = mapOf(
                        "title" to json.getString("title"),
                        "subjectName" to json.getString("subjectName"),
                        "chapterTitle" to json.getString("chapterTitle"),
                        "classGrade" to json.getLong("classGrade"),
                        "progressPercent" to json.getLong("progressPercent"),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updatePayload).await()
                } else {
                    val createPayload = mapOf(
                        "materialId" to json.getString("materialId"),
                        "title" to json.getString("title"),
                        "subjectName" to json.getString("subjectName"),
                        "chapterTitle" to json.getString("chapterTitle"),
                        "classGrade" to json.getLong("classGrade"),
                        "progressPercent" to json.getLong("progressPercent"),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(createPayload).await()
                }
            }

            ACTION_ADD_DOWNLOAD -> {
                val docRef = userRef.collection("downloads").document(item.targetId)
                val exists = docRef.get().await().exists()
                if (exists) {
                    val updatePayload = mapOf(
                        "title" to json.getString("title"),
                        "subjectName" to json.getString("subjectName"),
                        "classGrade" to json.getLong("classGrade"),
                        "fileSizeLabel" to json.getString("fileSizeLabel"),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updatePayload).await()
                } else {
                    val createPayload = mapOf(
                        "materialId" to json.getString("materialId"),
                        "title" to json.getString("title"),
                        "subjectName" to json.getString("subjectName"),
                        "classGrade" to json.getLong("classGrade"),
                        "fileSizeLabel" to json.getString("fileSizeLabel"),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(createPayload).await()
                }
            }

            ACTION_DELETE_DOWNLOAD -> {
                userRef.collection("downloads").document(item.targetId).delete().await()
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
