package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CachedChapterEntity
import com.example.data.local.CachedClassEntity
import com.example.data.local.CachedExamPaperEntity
import com.example.data.local.CachedMaterialEntity
import com.example.data.local.CachedPaperAttachmentEntity
import com.example.data.local.CachedQuestionEntity
import com.example.data.local.CachedQuizEntity
import com.example.data.local.CachedSubjectEntity
import com.example.data.local.StudyHubDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CurriculumCascadeSyncTest {

    private fun createInMemoryDb(): StudyHubDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, StudyHubDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun `deleteClassCascade removes class and all descendants without affecting other classes`() = runTest {
        val db = createInMemoryDb()
        val dao = db.studyHubDao()
        try {
            dao.upsertClasses(
                listOf(
                    CachedClassEntity("class_7", 7L, "Class 7", "Middle School", 0L, true),
                    CachedClassEntity("class_8", 8L, "Class 8", "Middle School", 0L, true)
                )
            )
            dao.upsertSubjects(
                listOf(
                    CachedSubjectEntity("sub_7_sci", 7L, "Science", "Physics & Chem", "science", "blue", 1L, true),
                    CachedSubjectEntity("sub_8_math", 8L, "Math", "Algebra", "math", "purple", 1L, true)
                )
            )
            dao.upsertChapters(
                listOf(
                    CachedChapterEntity("ch_7_1", 7L, "sub_7_sci", "Science", 1L, "Acids", "Summary", true),
                    CachedChapterEntity("ch_8_1", 8L, "sub_8_math", "Math", 1L, "Linear Eq", "Summary", true)
                )
            )
            dao.upsertMaterials(
                listOf(
                    CachedMaterialEntity("mat_7_1", 7L, "sub_7_sci", "Science", "ch_7_1", "Acids", "Acids Notes", "Desc", "short_notes", "# Notes", "100 KB", "", false, false, true),
                    CachedMaterialEntity("mat_8_1", 8L, "sub_8_math", "Math", "ch_8_1", "Linear Eq", "Math Notes", "Desc", "short_notes", "# Math", "100 KB", "", false, false, true)
                )
            )
            dao.upsertQuizzes(
                listOf(
                    CachedQuizEntity("qz_7_1", 7L, "sub_7_sci", "Science", "ch_7_1", "Acids", "Acids Quiz", "Desc", 10L, 1L, false, true),
                    CachedQuizEntity("qz_8_1", 8L, "sub_8_math", "Math", "ch_8_1", "Linear Eq", "Math Quiz", "Desc", 10L, 1L, false, true)
                )
            )
            dao.upsertQuestions(
                listOf(
                    CachedQuestionEntity("q_7_1", "qz_7_1", "mcq", "Prompt 7", "A||B", "0", "", "Exp", 5L, true),
                    CachedQuestionEntity("q_8_1", "qz_8_1", "mcq", "Prompt 8", "A||B", "1", "", "Exp", 5L, true)
                )
            )
            dao.upsertExamPapers(
                listOf(
                    CachedExamPaperEntity("ep_7_1", 7L, "sub_7_sci", "Science", "Paper 7", "model", 2026L, "1 MB", "Body", true),
                    CachedExamPaperEntity("ep_8_1", 8L, "sub_8_math", "Math", "Paper 8", "model", 2026L, "1 MB", "Body", true)
                )
            )
            dao.upsertPaperAttachments(
                listOf(
                    CachedPaperAttachmentEntity("att_ch_7", "ch_7_1", "pdf", "ch7.pdf", "application/pdf", 10L, 0L, "/tmp/ch7.pdf", true),
                    CachedPaperAttachmentEntity("att_mat_7", "mat_7_1", "image", "m7.png", "image/png", 10L, 0L, "/tmp/m7.png", true),
                    CachedPaperAttachmentEntity("att_ep_7", "ep_7_1", "pdf", "ep7.pdf", "application/pdf", 10L, 0L, "/tmp/ep7.pdf", true),
                    CachedPaperAttachmentEntity("att_ep_8", "ep_8_1", "pdf", "ep8.pdf", "application/pdf", 10L, 0L, "/tmp/ep8.pdf", true)
                )
            )

            val removedParents = dao.deleteClassCascade("class_7", 7L)

            assertTrue(removedParents.containsAll(listOf("ch_7_1", "mat_7_1", "ep_7_1")))
            assertEquals(listOf("class_8"), dao.observeClasses().first().map { it.id })
            assertEquals(listOf("sub_8_math"), dao.observeAllSubjects().first().map { it.id })
            assertEquals(listOf("ch_8_1"), dao.observeAllChapters().first().map { it.id })
            assertEquals(listOf("mat_8_1"), dao.observeAllMaterials().first().map { it.id })
            assertEquals(listOf("qz_8_1"), dao.observeAllQuizzes().first().map { it.id })
            assertEquals(listOf("q_8_1"), dao.observeAllQuestions().first().map { it.id })
            assertEquals(listOf("ep_8_1"), dao.observeAllExamPapers().first().map { it.id })
            assertTrue(dao.getAttachmentsForParentOnce("ch_7_1").isEmpty())
            assertTrue(dao.getAttachmentsForParentOnce("mat_7_1").isEmpty())
            assertTrue(dao.getAttachmentsForParentOnce("ep_7_1").isEmpty())
            assertEquals(1, dao.getAttachmentsForParentOnce("ep_8_1").size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `deleteSubjectCascade and deleteChapterCascade leave sibling entities intact`() = runTest {
        val db = createInMemoryDb()
        val dao = db.studyHubDao()
        try {
            dao.upsertSubjects(
                listOf(
                    CachedSubjectEntity("sub_1", 7L, "Science", "", "science", "blue", 2L, true),
                    CachedSubjectEntity("sub_2", 7L, "History", "", "history", "orange", 1L, true)
                )
            )
            dao.upsertChapters(
                listOf(
                    CachedChapterEntity("ch_1", 7L, "sub_1", "Science", 1L, "Ch 1", "", true),
                    CachedChapterEntity("ch_2", 7L, "sub_1", "Science", 2L, "Ch 2", "", true),
                    CachedChapterEntity("ch_3", 7L, "sub_2", "History", 1L, "Ch H", "", true)
                )
            )
            dao.upsertMaterials(
                listOf(
                    CachedMaterialEntity("m_1", 7L, "sub_1", "Science", "ch_1", "Ch 1", "M1", "", "short_notes", "", "1 KB", "", false, false, true),
                    CachedMaterialEntity("m_2", 7L, "sub_1", "Science", "ch_2", "Ch 2", "M2", "", "short_notes", "", "1 KB", "", false, false, true),
                    CachedMaterialEntity("m_3", 7L, "sub_2", "History", "ch_3", "Ch H", "M3", "", "short_notes", "", "1 KB", "", false, false, true)
                )
            )
            dao.upsertQuizzes(
                listOf(
                    CachedQuizEntity("qz_1", 7L, "sub_1", "Science", "ch_1", "Ch 1", "Q1", "", 10L, 1L, false, true),
                    CachedQuizEntity("qz_2", 7L, "sub_1", "Science", "ch_2", "Ch 2", "Q2", "", 10L, 1L, false, true)
                )
            )
            dao.upsertQuestions(
                listOf(
                    CachedQuestionEntity("qu_1", "qz_1", "mcq", "P1", "A||B", "0", "", "", 5L, true),
                    CachedQuestionEntity("qu_2", "qz_2", "mcq", "P2", "A||B", "0", "", "", 5L, true)
                )
            )
            dao.upsertExamPapers(
                listOf(
                    CachedExamPaperEntity("ep_1", 7L, "sub_1", "Science", "Sci Paper", "model", 2026L, "1 MB", "", true),
                    CachedExamPaperEntity("ep_2", 7L, "sub_2", "History", "Hist Paper", "model", 2026L, "1 MB", "", true)
                )
            )

            // 1. Delete single chapter ch_1 -> ch_3 (num 1) & ch_2 (num 2) remain
            dao.deleteChapterCascade("ch_1")
            assertEquals(listOf("ch_3", "ch_2"), dao.observeAllChapters().first().map { it.id })
            assertEquals(listOf("m_2", "m_3"), dao.observeAllMaterials().first().map { it.id })
            assertEquals(listOf("qz_2"), dao.observeAllQuizzes().first().map { it.id })
            assertEquals(listOf("qu_2"), dao.observeAllQuestions().first().map { it.id })

            // 2. Delete subject sub_1 -> ch_2, m_2, qz_2, qu_2, ep_1 removed; sub_2/ch_3/m_3/ep_2 remain
            dao.deleteSubjectCascade("sub_1")
            assertEquals(listOf("sub_2"), dao.observeAllSubjects().first().map { it.id })
            assertEquals(listOf("ch_3"), dao.observeAllChapters().first().map { it.id })
            assertEquals(listOf("m_3"), dao.observeAllMaterials().first().map { it.id })
            assertTrue(dao.observeAllQuizzes().first().isEmpty())
            assertTrue(dao.observeAllQuestions().first().isEmpty())
            assertEquals(listOf("ep_2"), dao.observeAllExamPapers().first().map { it.id })
        } finally {
            db.close()
        }
    }

    @Test
    fun `replaceClasses clears cached classes when cloud returns empty list`() = runTest {
        val db = createInMemoryDb()
        val dao = db.studyHubDao()
        try {
            dao.upsertClasses(
                listOf(CachedClassEntity("class_7", 7L, "Class 7", "Middle", 0L, true))
            )
            assertEquals(1, dao.observeClasses().first().size)

            dao.replaceClasses(emptyList())
            assertTrue(dao.observeClasses().first().isEmpty())
        } finally {
            db.close()
        }
    }
}
