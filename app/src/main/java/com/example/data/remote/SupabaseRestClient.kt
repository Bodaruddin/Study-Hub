package com.example.data.remote

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import java.io.IOException
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
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
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun isRobolectricRuntime(): Boolean =
        Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

    private fun getOrCreateAesKey(): SecretKey? {
        return try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (ks.getKey(KEYSTORE_ALIAS, null) as? SecretKey) ?: run {
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                keyGen.init(
                    KeyGenParameterSpec.Builder(
                        KEYSTORE_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
                keyGen.generateKey()
            }
        } catch (e: Exception) {
            if (isRobolectricRuntime()) null else throw e
        }
    }

    private fun encryptSecret(plaintext: String): String {
        val key = getOrCreateAesKey()
            ?: return if (isRobolectricRuntime()) {
                ROBOLECTRIC_PREFIX + Base64.encodeToString(plaintext.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            } else {
                throw SecurityException("Hardware keystore unavailable for credential encryption.")
            }
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ivB64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encB64 = Base64.encodeToString(cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        return "$ivB64:$encB64"
    }

    private fun decryptSecret(stored: String?): String {
        if (stored.isNullOrBlank()) return ""
        if (stored.startsWith(ROBOLECTRIC_PREFIX)) {
            if (!isRobolectricRuntime()) return ""
            return runCatching {
                String(Base64.decode(stored.removePrefix(ROBOLECTRIC_PREFIX), Base64.NO_WRAP), Charsets.UTF_8)
            }.getOrDefault("")
        }
        val parts = stored.split(':')
        if (parts.size != 2) return ""
        return runCatching {
            val key = getOrCreateAesKey() ?: return ""
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun loadManualCredentials(): Pair<String, String> {
        val sp = prefs ?: return "" to ""
        val encUrl = decryptSecret(sp.getString(KEY_ENC_MANUAL_URL, null))
        val encKey = decryptSecret(sp.getString(KEY_ENC_MANUAL_KEY, null))
        if (encUrl.isNotBlank() && encKey.isNotBlank()) {
            return encUrl to encKey
        }
        // Migrate any legacy plaintext preferences to Keystore-encrypted entries and wipe plaintext
        val legacyUrl = sp.getString(KEY_MANUAL_URL, null).orEmpty()
        val legacyKey = sp.getString(KEY_MANUAL_KEY, null).orEmpty()
        if (legacyUrl.isNotBlank() && legacyKey.isNotBlank()) {
            val cleanUrl = normalizeUrl(legacyUrl)
            val cleanKey = legacyKey.trim()
            if (isValidEndpointUrl(cleanUrl) && isValidAnonKey(cleanKey)) {
                sp.edit()
                    .putString(KEY_ENC_MANUAL_URL, encryptSecret(cleanUrl))
                    .putString(KEY_ENC_MANUAL_KEY, encryptSecret(cleanKey))
                    .remove(KEY_MANUAL_URL)
                    .remove(KEY_MANUAL_KEY)
                    .apply()
                return cleanUrl to cleanKey
            } else {
                sp.edit().remove(KEY_MANUAL_URL).remove(KEY_MANUAL_KEY).apply()
            }
        }
        return "" to ""
    }

    private fun resolveCredentials(): Pair<String, String> {
        val (manualUrl, manualKey) = loadManualCredentials()
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
        val (manualUrl, manualKey) = loadManualCredentials()
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
        require(!cleanUrl.contains("YOUR_PROJECT_REF", ignoreCase = true)) {
            "Replace placeholder with your real Supabase Project URL."
        }
        require(isValidEndpointUrl(cleanUrl)) {
            "Enter a valid public HTTPS Supabase Project URL (e.g. https://xxxx.supabase.co)."
        }
        require(isValidAnonKey(cleanKey)) {
            "Enter a valid Supabase Anon / Public Key."
        }
        prefs?.edit()
            ?.putString(KEY_ENC_MANUAL_URL, encryptSecret(cleanUrl))
            ?.putString(KEY_ENC_MANUAL_KEY, encryptSecret(cleanKey))
            ?.remove(KEY_MANUAL_URL)
            ?.remove(KEY_MANUAL_KEY)
            ?.apply()
    }

    fun clearManualConfig() {
        prefs?.edit()
            ?.remove(KEY_ENC_MANUAL_URL)
            ?.remove(KEY_ENC_MANUAL_KEY)
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
                message = "Enter a valid public https:// Supabase URL and Anon Key first."
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
                } else if (resp.code in 300..399) {
                    return@withContext SupabaseTestResult(
                        connected = false,
                        schemaReady = false,
                        message = "Unexpected redirect (HTTP ${resp.code}). Verify you entered the exact Supabase Project URL."
                    )
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
                        message = "Supabase returned HTTP ${resp.code}. Verify your Project URL, Anon Key, and SQL setup."
                    )
                }
            }
        } catch (e: Exception) {
            return@withContext SupabaseTestResult(
                connected = false,
                schemaReady = false,
                message = "Connection failed (${e::class.java.simpleName}). Check Project URL and internet connection."
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
        validateIdentifier(table, "table")
        require(SELECT_COLS_REGEX.matches(selectCols.trim())) { "Invalid select columns." }
        if (!order.isNullOrBlank()) {
            require(ORDER_REGEX.matches(order.trim())) { "Invalid order clause." }
        }
        filters.keys.forEach { validateIdentifier(it, "filter column") }

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
            require(limit in 1..10_000) { "Limit out of valid range." }
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
        validateIdentifier(table, "table")
        require(ON_CONFLICT_REGEX.matches(onConflict.trim())) { "Invalid on_conflict column." }
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
        validateIdentifier(table, "table")
        require(filters.isNotEmpty()) { "Refusing unfiltered UPDATE on $table" }
        filters.keys.forEach { validateIdentifier(it, "filter column") }

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
        validateIdentifier(table, "table")
        require(filters.isNotEmpty()) { "Refusing unfiltered DELETE on $table" }
        filters.keys.forEach { validateIdentifier(it, "filter column") }

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

    private fun buildHttpError(code: Int, @Suppress("UNUSED_PARAMETER") rawBody: String, action: String): SupabaseApiException {
        val hint = when {
            code in 300..399 -> "Supabase rejected redirect ($code): check your Project URL."
            code == 401 || code == 403 -> "Supabase rejected request ($code): check Anon Key or run SQL grants."
            code == 404 -> "Supabase table missing ($code): run the SQL schema in Supabase SQL Editor."
            else -> "Supabase $action failed ($code)."
        }
        Log.e(TAG, "$action failed with HTTP $code")
        return SupabaseApiException(code, hint)
    }

    companion object {
        private const val TAG = "SupabaseRest"
        private const val PREFS_NAME = "studyhub_supabase_config"
        private const val KEY_MANUAL_URL = "manual_supabase_url"
        private const val KEY_MANUAL_KEY = "manual_supabase_anon_key"
        private const val KEY_ENC_MANUAL_URL = "enc_manual_supabase_url"
        private const val KEY_ENC_MANUAL_KEY = "enc_manual_supabase_anon_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "studyhub_supabase_creds_key"
        private const val AES_GCM_TRANSFORM = "AES/GCM/NoPadding"
        private const val ROBOLECTRIC_PREFIX = "robo:"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val IDENTIFIER_REGEX = Regex("^[a-z][a-z0-9_]{0,63}$")
        private val SELECT_COLS_REGEX = Regex("^(\\*|[a-z][a-z0-9_]{0,63}(\\s*,\\s*[a-z][a-z0-9_]{0,63})*)$")
        private val ORDER_REGEX = Regex("^[a-z][a-z0-9_]{0,63}(\\.(asc|desc))?(\\s*,\\s*[a-z][a-z0-9_]{0,63}(\\.(asc|desc))?)*$")
        private val ON_CONFLICT_REGEX = Regex("^[a-z][a-z0-9_]{0,63}(\\s*,\\s*[a-z][a-z0-9_]{0,63})*$")
        private val IPV4_REGEX = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")

        fun normalizeUrl(raw: String): String {
            val trimmed = raw.trim().trimEnd('/')
            return trimmed.removeSuffix("/rest/v1").trimEnd('/')
        }

        fun isValidEndpointUrl(rawUrl: String): Boolean {
            val url = normalizeUrl(rawUrl)
            if (!url.startsWith("https://", ignoreCase = true)) return false
            if (url.contains("YOUR_PROJECT_REF", ignoreCase = true)) return false
            val parsed = url.toHttpUrlOrNull() ?: return false
            if (!parsed.scheme.equals("https", ignoreCase = true)) return false
            if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) return false
            if (parsed.query != null || parsed.fragment != null) return false
            if (parsed.encodedPath.isNotEmpty() && parsed.encodedPath != "/") return false

            val host = parsed.host.lowercase()
            if (host.isBlank() || !host.contains('.')) return false
            if (IPV4_REGEX.matches(host)) return false
            if (host.contains(':') || host.startsWith("[")) return false
            if (host == "localhost" ||
                host.endsWith(".localhost") ||
                host.endsWith(".local") ||
                host.endsWith(".internal") ||
                host.endsWith(".arpa") ||
                host.endsWith(".localdomain")
            ) {
                return false
            }
            return true
        }

        fun isValidAnonKey(rawKey: String): Boolean {
            val key = rawKey.trim()
            return key.length >= 20 &&
                !key.contains("YOUR_SUPABASE_ANON_KEY", ignoreCase = true) &&
                !key.any { it.isWhitespace() }
        }

        fun isValidConfig(url: String, key: String): Boolean =
            isValidEndpointUrl(url) && isValidAnonKey(key)

        private fun validateIdentifier(name: String, label: String) {
            require(IDENTIFIER_REGEX.matches(name)) { "Invalid $label identifier." }
        }
    }
}
