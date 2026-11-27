package com.example.data.remote

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

fun handleCloudError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is SupabaseApiException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val isSignedIn = runCatching { Firebase.auth.currentUser != null }.getOrDefault(false)
    val sanitizedResource = path?.substringBefore('?')?.take(80)
    val statusCode = (rootError as? SupabaseApiException)?.statusCode

    val errorInfoJson = JSONObject().apply {
        put("errorType", rootError::class.java.simpleName)
        if (statusCode != null) put("statusCode", statusCode)
        put("operationType", operationType.value)
        put("resource", sanitizedResource)
        put("authenticated", isSignedIn)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("SupabaseError", "Cloud operation failed: $jsonString")
    return jsonString
}
