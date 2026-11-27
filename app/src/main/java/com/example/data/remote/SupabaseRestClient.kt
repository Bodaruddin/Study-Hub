package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class SupabaseConnectionStatus(
    val configured: Boolean = false,
    val hostLabel: String? = null,
    val sourceLabel: String = "Not Configured",
    val savedUrl: String = "",
    val savedKeyPreview: String = "",
    val hasManualOverride: Boolean = false
)

data class SupabaseTestResult(
    val connected: Boolean,
    val schemaReady: Boolean,
    val message: String
)

class SupabaseApiException(
    val statusCode: Int,
    message: String
) : IOException(message)

class SupabaseRestClient(
    context: Context? = null,
    private val defaultBaseUrl: String = BuildConfig.SUPABASE_URL,
    private val defaultAnonKey: String = BuildConfig.SUPABASE_ANON_KEY
) {
    private val prefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        return trimmed.removeSuffix("/rest/v1").trimEnd('/')
    }

    private fun isValidConfig(url: String, key: String): Boolean {
        val parsed = url.toHttpUrlOrNull()
        return url.startsWith("https://", ignoreCase = true) &&
            parsed != null &&
            !url.contains("YOUR_PROJECT_REF", ignoreCase = true) &&
            key.isNotBlank() &&
            !key.contains("YOUR_SUPABASE_ANON_KEY", ignoreCase = true)
    }

    private fun resolveCredentials(): Pair<String, String> {
        val manualUrl = prefs?.getString(KEY_MANUAL_URL, null).orEmpty()
        val manualKey = prefs?.getString(KEY_MANUAL_KEY, null).orEmpty()
        if (manualUrl.isNotBlank() && manualKey.isNotBlank()) {
            return normalizeUrl(manualUrl) to manualKey.trim()
        }
        return normalizeUrl(defaultBaseUrl) to defaultAnonKey.trim()
    }

    fun isConfigured(): Boolean {
        val (url, key) = resolveCredentials()
        return isValidConfig(url, key)
    }

    fun activeHostLabel(): String? {
        val (url, key) = resolveCredentials()
        if (!isValidConfig(url, key)) return null
        return url.toHttpUrlOrNull()?.host
    }

    fun connectionStatus(): SupabaseConnectionStatus {
        val manualUrl = prefs?.getString(KEY_MANUAL_URL, null).orEmpty()
        val manualKey = prefs?.getString(KEY_MANUAL_KEY, null).orEmpty()
        val hasManual = manualUrl.isNotBlank() && manualKey.isNotBlank()
        val (url, key) = resolveCredentials()
        val configured = isValidConfig(url, key)
        val preview = if (configured && key.length > 10) {
            "${key.take(6)}••••${key.takeLast(4)}"
        } else if (configured) {
            "Configured"
        } else {
            ""
        }
        return SupabaseConnectionStatus(
            configured = configured,
            hostLabel = if (configured) url.toHttpUrlOrNull()?.host else null,
            sourceLabel = when {
                !configured -> "Not Configured"
                hasManual -> "Manual Admin Setup"
                else -> "Build Secrets"
            },
            savedUrl = if (hasManual) manualUrl else if (configured) url else "",
            savedKeyPreview = preview,
            hasManualOverride = hasManual
        )
    }

    fun saveManualConfig(rawUrl: String, rawAnonKey: String) {
        val cleanUrl = normalizeUrl(rawUrl)
        val cleanKey = rawAnonKey.trim()
        require(cleanUrl.startsWith("https://", ignoreCase = true) && cleanUrl.toHttpUrlOrNull() != null) {
            "Enter a valid HTTPS Supabase Project URL (e.g. https://xxxx.supabase.co)."
        }
        require(!cleanUrl.contains("YOUR_PROJECT_REF", ignoreCase = true)) {
            "Replace placeholder with your real Supabase Project URL."
        }
        require(cleanKey.length >= 20 && !cleanKey.contains("YOUR_SUPABASE_ANON_KEY", ignoreCase = true)) {
            "Enter a valid Supabase Anon / Public Key."
        }
        prefs?.edit()
            ?.putString(KEY_MANUAL_URL, cleanUrl)
            ?.putString(KEY_MANUAL_KEY, cleanKey)
            ?.apply()
    }

    fun clearManualConfig() {
        prefs?.edit()
            ?.remove(KEY_MANUAL_URL)
            ?.remove(KEY_MANUAL_KEY)
            ?.apply()
    }

    suspend fun testConnection(
        urlOverride: String? = null,
        anonKeyOverride: String? = null
    ): SupabaseTestResult = withContext(Dispatchers.IO) {
        val (resolvedUrl, resolvedKey) = resolveCredentials()
        val testUrl = normalizeUrl(urlOverride?.takeIf { it.isNotBlank() } ?: resolvedUrl)
        val testKey = (anonKeyOverride?.takeIf { it.isNotBlank() } ?: resolvedKey).trim()

        if (!isValidConfig(testUrl, testKey)) {
            return@withContext SupabaseTestResult(
                connected = false,
                schemaReady = false,
                message = "Enter a valid https:// Supabase URL and Anon Key first."
            )
        }

        val restRoot = "$testUrl/rest/v1".toHttpUrlOrNull()
            ?: return@withContext SupabaseTestResult(
                connected = false,
                schemaReady = false,
                message = "Invalid Supabase URL format."
            )

        try {
            val classesUrl = restRoot.newBuilder()
                .addPathSegment("classes")
                .addQueryParameter("select", "id")
                .addQueryParameter("limit", "1")
                .build()

            val classesReq = Request.Builder()
                .url(classesUrl)
                .header("apikey", testKey)
                .header("Authorization", "Bearer $testKey")
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(classesReq).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val profilesUrl = restRoot.newBuilder()
                        .addPathSegment("user_profiles")
                        .addQueryParameter("select", "id")
                        .addQueryParameter("limit", "1")
                        .build()
                    val profilesReq = Request.Builder()
                        .url(profilesUrl)
                        .header("apikey", testKey)
                        .header("Authorization", "Bearer $testKey")
                        .header("Accept", "application/json")
                        .get()
                        .build()
                    val profilesOk = runCatching {
                        client.newCall(profilesReq).execute().use { it.isSuccessful }
                    }.getOrDefault(false)

                    return@withContext if (profilesOk) {
                        SupabaseTestResult(
                            connected = true,
                            schemaReady = true,
                            message = "Connected! Supabase project and all StudyHub tables are ready."
                        )
                    } else {
                        SupabaseTestResult(
                            connected = true,
                            schemaReady = false,
                            message = "Connected to Supabase, but some tables are missing. Copy & run the SQL schema in Supabase SQL Editor."
                        )
                    }
                } else if (resp.code == 404 || raw.contains("PGRST205", ignoreCase = true) || raw.contains("relation", ignoreCase = true)) {
                    return@withContext SupabaseTestResult(
                        connected = true,
                        schemaReady = false,
                        message = "Connected to Supabase, but tables do not exist yet. Copy & run the SQL schema in Supabase SQL Editor."
                    )
                } else if (resp.code == 401 || resp.code == 403) {
                    return@withContext SupabaseTestResult(
                        connected = false,
                        schemaReady = false,
                        message = "Authentication failed (${resp.code}). Verify your Supabase Anon Key and run the SQL grants."
                    )
                } else {
                    return@withContext SupabaseTestResult(
                        connected = false,
                        schemaReady = false,
                        message = "Supabase returned HTTP ${resp.code}: ${raw.take(180)}"
                    )
                }
            }
        } catch (e: Exception) {
            return@withContext SupabaseTestResult(
                connected = false,
                schemaReady = false,
                message = "Connection error: ${e.localizedMessage ?: "Check URL & internet connection"}"
            )
        }
    }

    private fun requireConfigured(): Triple<HttpUrl, String, String> {
        val (url, key) = resolveCredentials()
        if (!isValidConfig(url, key)) {
            throw IllegalStateException(
                "Supabase is not configured. Open Admin → Manage Database → Setup Supabase Database."
            )
        }
        val httpUrl = "$url/rest/v1".toHttpUrlOrNull()
            ?: throw IllegalStateException("Invalid Supabase URL format.")
        return Triple(httpUrl, url, key)
    }

    suspend fun select(
        table: String,
        filters: Map<String, String> = emptyMap(),
        selectCols: String = "*",
        order: String? = null,
        limit: Int? = null
    ): JSONArray = withContext(Dispatchers.IO) {
        val (restRoot, _, key) = requireConfigured()
        val urlBuilder = restRoot.newBuilder()
            .addPathSegment(table)
            .addQueryParameter("select", selectCols)
        filters.forEach { (col, expr) ->
            urlBuilder.addQueryParameter(col, expr)
        }
        if (!order.isNullOrBlank()) {
            urlBuilder.addQueryParameter("order", order)
        }
        if (limit != null) {
            urlBuilder.addQueryParameter("limit", limit.toString())
        }

        val request = baseRequestBuilder(urlBuilder.build(), key)
            .get()
            .build()

        executeForJsonArray(request, "SELECT $table")
    }

    suspend fun upsert(
        table: String,
        rows: JSONArray,
        onConflict: String = "id"
    ): JSONArray = withContext(Dispatchers.IO) {
        if (rows.length() == 0) return@withContext JSONArray()
        val (restRoot, _, key) = requireConfigured()
        val url = restRoot.newBuilder()
            .addPathSegment(table)
            .addQueryParameter("on_conflict", onConflict)
            .build()

        val body = rows.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = baseRequestBuilder(url, key)
            .header("Prefer", "resolution=merge-duplicates,return=representation")
            .post(body)
            .build()

        executeForJsonArray(request, "UPSERT $table")
    }

    suspend fun upsertOne(
        table: String,
        row: JSONObject,
        onConflict: String = "id"
    ): JSONObject? {
        val result = upsert(table, JSONArray().put(row), onConflict)
        return if (result.length() > 0) result.optJSONObject(0) else null
    }

    suspend fun updateWhere(
        table: String,
        filters: Map<String, String>,
        bodyJson: JSONObject
    ): JSONArray = withContext(Dispatchers.IO) {
        val (restRoot, _, key) = requireConfigured()
        val urlBuilder = restRoot.newBuilder()
            .addPathSegment(table)
        filters.forEach { (col, expr) ->
            urlBuilder.addQueryParameter(col, expr)
        }
        val body = bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = baseRequestBuilder(urlBuilder.build(), key)
            .header("Prefer", "return=representation")
            .patch(body)
            .build()

        executeForJsonArray(request, "UPDATE $table")
    }

    suspend fun deleteWhere(
        table: String,
        filters: Map<String, String>
    ): Unit = withContext(Dispatchers.IO) {
        require(filters.isNotEmpty()) { "Refusing unfiltered DELETE on $table" }
        val (restRoot, _, key) = requireConfigured()
        val urlBuilder = restRoot.newBuilder()
            .addPathSegment(table)
        filters.forEach { (col, expr) ->
            urlBuilder.addQueryParameter(col, expr)
        }
        val request = baseRequestBuilder(urlBuilder.build(), key)
            .header("Prefer", "return=minimal")
            .delete()
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw buildHttpError(response.code, raw, "DELETE $table")
            }
        }
    }

    private fun baseRequestBuilder(url: HttpUrl, key: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("apikey", key)
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")

    private fun executeForJsonArray(request: Request, action: String): JSONArray {
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw buildHttpError(response.code, raw, action)
            }
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return JSONArray()
            return if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                JSONArray().put(JSONObject(trimmed))
            }
        }
    }

    private fun buildHttpError(code: Int, rawBody: String, action: String): SupabaseApiException {
        val parsedMsg = runCatching {
            val obj = JSONObject(rawBody)
            obj.optString("message").ifBlank { obj.optString("error") }
        }.getOrNull().orEmpty().ifBlank { rawBody.take(240) }
        val hint = when (code) {
            401, 403 -> "Supabase rejected request ($code): check Anon Key or run SQL grants."
            404 -> "Supabase table missing ($code): run the SQL schema in Supabase SQL Editor."
            else -> "Supabase $action failed ($code): $parsedMsg"
        }
        Log.e(TAG, "$action HTTP $code: $parsedMsg")
        return SupabaseApiException(code, hint)
    }

    companion object {
        private const val TAG = "SupabaseRest"
        private const val PREFS_NAME = "studyhub_supabase_config"
        private const val KEY_MANUAL_URL = "manual_supabase_url"
        private const val KEY_MANUAL_KEY = "manual_supabase_anon_key"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
