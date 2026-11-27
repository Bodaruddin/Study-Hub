package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PaperAttachmentChunk
import com.example.data.model.SupabaseMappers
import com.example.data.remote.AttachmentMediaHelper
import com.example.data.remote.SupabaseRestClient
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecuritySafeguardsTest {

    @Test
    fun `supabase url validator blocks ssrf loopback metadata private ips and accepts valid https host`() {
        assertTrue(SupabaseRestClient.isValidEndpointUrl("https://abcdefghijkl.supabase.co"))
        assertTrue(SupabaseRestClient.isValidEndpointUrl("https://abcdefghijkl.supabase.co/rest/v1/"))

        assertFalse(SupabaseRestClient.isValidEndpointUrl("http://abcdefghijkl.supabase.co"))
        assertFalse(SupabaseRestClient.isValidEndpointUrl("https://127.0.0.1"))
        assertFalse(SupabaseRestClient.isValidEndpointUrl("https://169.254.169.254"))
        assertFalse(SupabaseRestClient.isValidEndpointUrl("https://10.0.0.1"))
        assertFalse(SupabaseRestClient.isValidEndpointUrl("https://192.168.1.10"))
        assertFalse(SupabaseRestClient.isValidEndpointUrl("https://[::1]"))
        assertFalse(SupabaseRestClient.isValidEndpointUrl("https://localhost"))
        assertFalse(SupabaseRestClient.isValidEndpointUrl("https://metadata.google.internal"))
        assertFalse(SupabaseRestClient.isValidEndpointUrl("https://user:pass@abcdefghijkl.supabase.co"))
        assertFalse(SupabaseRestClient.isValidEndpointUrl("https://abcdefghijkl.supabase.co?leak=1"))
        assertFalse(SupabaseRestClient.isValidEndpointUrl("https://YOUR_PROJECT_REF.supabase.co"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `supabase saveManualConfig rejects metadata endpoint`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = SupabaseRestClient(context, "", "")
        client.saveManualConfig("https://169.254.169.254", "anon_key_with_sufficient_length_123456")
    }

    @Test
    fun `supabase manual config does not persist plaintext secret in shared preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = SupabaseRestClient(context, "", "")
        val testUrl = "https://studyhubtestref.supabase.co"
        val testKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.samplekeypayload12345"

        client.saveManualConfig(testUrl, testKey)
        val status = client.connectionStatus()
        assertTrue(status.configured)
        assertTrue(status.hasManualOverride)
        assertEquals("studyhubtestref.supabase.co", status.hostLabel)

        val rawPrefs = context.getSharedPreferences("studyhub_supabase_config", Context.MODE_PRIVATE)
        assertFalse(rawPrefs.contains("manual_supabase_url"))
        assertFalse(rawPrefs.contains("manual_supabase_anon_key"))

        client.clearManualConfig()
        assertFalse(client.connectionStatus().hasManualOverride)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `supabase select rejects malformed table identifier`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = SupabaseRestClient(
            context,
            "https://studyhubtestref.supabase.co",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.samplekeypayload12345"
        )
        client.select("classes/../user_profiles")
    }

    @Test
    fun `attachment media helper blocks path traversal and validates pdf header`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sentinel = File(context.filesDir, "keep_me.txt").apply { writeText("important") }

        assertEquals("file", AttachmentMediaHelper.sanitizeSegment(".."))
        assertFalse(AttachmentMediaHelper.sanitizeSegment("../etc/passwd").contains(".."))

        val parentDir = AttachmentMediaHelper.mediaDirForParent(context, "../../escape_attempt")
        val mediaRoot = File(context.filesDir, "paper_media").canonicalFile
        assertTrue(parentDir.canonicalPath.startsWith(mediaRoot.path + File.separator))

        // Attempting to delete ".." must NOT touch filesDir or sentinel outside paper_media
        AttachmentMediaHelper.deleteParentMediaCache(context, "..")
        assertTrue(sentinel.exists())

        assertTrue(AttachmentMediaHelper.hasPdfHeader("%PDF-1.7\n...".toByteArray()))
        assertFalse(AttachmentMediaHelper.hasPdfHeader("<html><script>alert(1)</script></html>".toByteArray()))

        // Valid decode lands inside paper_media/<parentId>
        val decoded = AttachmentMediaHelper.decodeChunksToFile(
            context = context,
            parentId = "paper_test",
            groupId = "att_1",
            fileName = "../../evil.pdf",
            chunks = listOf(
                PaperAttachmentChunk(
                    id = "c0",
                    parentId = "paper_test",
                    groupId = "att_1",
                    kind = "pdf",
                    fileName = "../../evil.pdf",
                    mimeType = "application/pdf",
                    sizeBytes = 9,
                    chunkIndex = 0,
                    totalChunks = 1,
                    sortOrder = 0,
                    published = true,
                    dataBase64 = "JVBERi0xLjQK"
                )
            )
        )
        assertTrue(decoded.canonicalPath.startsWith(mediaRoot.path + File.separator))
        assertTrue(decoded.exists())
        AttachmentMediaHelper.deleteParentMediaCache(context, "paper_test")
        sentinel.delete()
    }

    @Test
    fun `profile patch mapper blocks privilege escalation and mass assignment fields`() {
        val patch = SupabaseMappers.userProfilePatchToSnakeCase(
            mapOf(
                "role" to "admin",
                "subscriptionTier" to "premium",
                "subscription_tier" to "premium",
                "id" to "victim_uid",
                "userId" to "victim_uid",
                "user_id" to "victim_uid",
                "email" to "attacker@evil.com",
                "arbitrary_col" to "x",
                "displayName" to "  Alice Student  ",
                "selectedClass" to 15L,
                "darkThemeEnabled" to true
            )
        )

        assertFalse(patch.has("role"))
        assertFalse(patch.has("subscriptionTier"))
        assertFalse(patch.has("subscription_tier"))
        assertFalse(patch.has("id"))
        assertFalse(patch.has("userId"))
        assertFalse(patch.has("user_id"))
        assertFalse(patch.has("email"))
        assertFalse(patch.has("arbitrary_col"))

        assertEquals("Alice Student", patch.getString("display_name"))
        assertEquals(12L, patch.getLong("selected_class"))
        assertTrue(patch.getBoolean("dark_theme_enabled"))
        assertTrue(patch.has("updated_at"))
    }
}
