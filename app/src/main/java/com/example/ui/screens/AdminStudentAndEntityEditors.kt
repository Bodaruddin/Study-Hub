package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnouncementItem
import com.example.data.model.ChapterItem
import com.example.data.model.QuestionType
import com.example.data.model.QuizItem
import com.example.data.model.QuizQuestion
import com.example.data.model.SchoolClass
import com.example.data.model.SubjectItem
import com.example.data.model.UserProfile
import com.example.data.remote.SupabaseConnectionStatus
import com.example.ui.components.Tactile3DCard
import com.example.ui.theme.BlueEnd
import com.example.ui.theme.BlueStart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GreenStart
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.OrangeStart
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PurpleEnd
import com.example.ui.theme.PurpleStart
import com.example.ui.theme.SuccessGreen

@Composable
fun AdminConfirmDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(confirmLabel, color = DangerRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminClassEditorDialog(
    initialClass: SchoolClass? = null,
    onDismiss: () -> Unit,
    onSave: (SchoolClass) -> Unit
) {
    var selectedGrade by remember(initialClass?.id) { mutableLongStateOf(initialClass?.grade ?: 7L) }
    var name by remember(initialClass?.id) { mutableStateOf(initialClass?.name ?: "Class 7") }
    var desc by remember(initialClass?.id) { mutableStateOf(initialClass?.description ?: "") }
    var studentCountText by remember(initialClass?.id) {
        mutableStateOf((initialClass?.studentCount ?: 0L).toString())
    }
    var published by remember(initialClass?.id) { mutableStateOf(initialClass?.published ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialClass == null) "Add Class" else "Edit Class",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (initialClass == null) {
                    Text("Select Grade (6–12)", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (6L..12L).forEach { g ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedGrade = g
                                        if (name.startsWith("Class ")) {
                                            name = "Class $g"
                                        }
                                    },
                                color = if (selectedGrade == g) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Class $g",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    color = if (selectedGrade == g) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Class Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = studentCountText,
                    onValueChange = { studentCountText = it.filter { ch -> ch.isDigit() }.take(7) },
                    label = { Text("Enrolled Students Count") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Published")
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val grade = selectedGrade.coerceIn(6L, 12L)
                    val parsedCount = (studentCountText.toLongOrNull() ?: 0L).coerceAtLeast(0L)
                    onSave(
                        (initialClass ?: SchoolClass(id = "class_$grade")).copy(
                            grade = grade,
                            name = name.trim().ifBlank { "Class $grade" },
                            description = desc.trim(),
                            studentCount = parsedCount,
                            published = published
                        )
                    )
                },
                modifier = Modifier.testTag("save_class_button")
            ) {
                Text(if (initialClass == null) "Save" else "Update")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminSubjectEditorDialog(
    initialSubject: SubjectItem? = null,
    availableGrades: List<Long> = (6L..12L).toList(),
    onDismiss: () -> Unit,
    onSave: (SubjectItem) -> Unit
) {
    val gradeChoices = remember(availableGrades, initialSubject?.classGrade) {
        (availableGrades + listOfNotNull(initialSubject?.classGrade))
            .distinct()
            .sorted()
            .ifEmpty { (6L..12L).toList() }
    }
    var selectedGrade by remember(initialSubject?.id) {
        mutableLongStateOf(initialSubject?.classGrade ?: gradeChoices.firstOrNull() ?: 7L)
    }
    var name by remember(initialSubject?.id) { mutableStateOf(initialSubject?.name ?: "") }
    var subtitle by remember(initialSubject?.id) {
        mutableStateOf(initialSubject?.subtitle?.ifBlank { "Notes • Summary • MCQs" } ?: "Notes • Summary • MCQs")
    }
    var chapterCountText by remember(initialSubject?.id) {
        mutableStateOf((initialSubject?.chapterCount ?: 4L).toString())
    }
    var published by remember(initialSubject?.id) { mutableStateOf(initialSubject?.published ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSubject == null) "Add Subject" else "Edit Subject",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Class Grade", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gradeChoices.forEach { g ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedGrade = g },
                            color = if (selectedGrade == g) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Class $g",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                color = if (selectedGrade == g) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_name_input")
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = chapterCountText,
                    onValueChange = { chapterCountText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Chapter Count") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Published")
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    if (name.isNotBlank()) {
                        val parsedChapters = (chapterCountText.toLongOrNull() ?: 4L).coerceIn(0L, 200L)
                        onSave(
                            (initialSubject ?: SubjectItem(iconKey = "book", colorKey = "blue")).copy(
                                classGrade = selectedGrade.coerceIn(6L, 12L),
                                name = name.trim(),
                                subtitle = subtitle.trim().ifBlank { "Notes • Summary • MCQs" },
                                chapterCount = parsedChapters,
                                published = published
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_subject_button")
            ) {
                Text(if (initialSubject == null) "Save" else "Update")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminQuizEditorDialog(
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    initialQuiz: QuizItem? = null,
    availableGrades: List<Long> = (6L..12L).toList(),
    onDismiss: () -> Unit,
    onSave: (QuizItem, QuizQuestion?) -> Unit
) {
    val gradeChoices = remember(availableGrades, initialQuiz?.classGrade) {
        (availableGrades + listOfNotNull(initialQuiz?.classGrade))
            .distinct()
            .sorted()
            .ifEmpty { (6L..12L).toList() }
    }
    var selectedGrade by remember(initialQuiz?.id) {
        mutableLongStateOf(initialQuiz?.classGrade ?: gradeChoices.firstOrNull() ?: 7L)
    }
    var subjectName by remember(initialQuiz?.id) {
        mutableStateOf(initialQuiz?.subjectName?.ifBlank { "English" } ?: "English")
    }
    var title by remember(initialQuiz?.id) { mutableStateOf(initialQuiz?.title ?: "") }
    var description by remember(initialQuiz?.id) {
        mutableStateOf(initialQuiz?.description ?: "Admin-created assessment")
    }
    var duration by remember(initialQuiz?.id) { mutableStateOf((initialQuiz?.durationMinutes ?: 10L).toString()) }
    var premiumOnly by remember(initialQuiz?.id) { mutableStateOf(initialQuiz?.premiumOnly ?: false) }
    var published by remember(initialQuiz?.id) { mutableStateOf(initialQuiz?.published ?: true) }
    var firstPrompt by remember(initialQuiz?.id) { mutableStateOf("") }
    var opt1 by remember(initialQuiz?.id) { mutableStateOf("Option A") }
    var opt2 by remember(initialQuiz?.id) { mutableStateOf("Option B") }

    val gradeSubjects = remember(subjects, selectedGrade) {
        subjects.filter { it.classGrade == selectedGrade }.map { it.name }.distinct()
            .ifEmpty { listOf("English", "Hindi", "Mathematics", "Science", "Social Science") }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialQuiz == null) "Create Quiz / Practice Test" else "Edit Quiz",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Class", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gradeChoices.forEach { g ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedGrade = g },
                            color = if (selectedGrade == g) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Class $g",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                color = if (selectedGrade == g) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Text("Subject", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gradeSubjects.forEach { subj ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { subjectName = subj },
                            color = if (subjectName.equals(subj, ignoreCase = true)) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = subj,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                color = if (subjectName.equals(subj, ignoreCase = true)) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quiz Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Duration (minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (initialQuiz == null) {
                    OutlinedTextField(
                        value = firstPrompt,
                        onValueChange = { firstPrompt = it },
                        label = { Text("First Question (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opt1,
                        onValueChange = { opt1 = it },
                        label = { Text("Choice 1 (Correct)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opt2,
                        onValueChange = { opt2 = it },
                        label = { Text("Choice 2") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Premium Only")
                    Switch(checked = premiumOnly, onCheckedChange = { premiumOnly = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Live / Published")
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val cleanTitle = title.trim()
                    if (cleanTitle.isNotEmpty()) {
                        val cleanSubject = subjectName.trim().ifBlank { "English" }
                        val subj = subjects.firstOrNull {
                            it.classGrade == selectedGrade && it.name.equals(cleanSubject, ignoreCase = true)
                        }
                        val chap = chapters.firstOrNull {
                            it.classGrade == selectedGrade && it.subjectName.equals(cleanSubject, ignoreCase = true)
                        } ?: chapters.firstOrNull { it.classGrade == selectedGrade }
                        val quiz = (initialQuiz ?: QuizItem()).copy(
                            classGrade = selectedGrade,
                            subjectId = subj?.id ?: initialQuiz?.subjectId?.takeIf { it.isNotBlank() }
                                ?: "sub_${cleanSubject.lowercase().replace(" ", "_")}_$selectedGrade",
                            subjectName = cleanSubject,
                            chapterId = chap?.id ?: initialQuiz?.chapterId?.takeIf { it.isNotBlank() }
                                ?: "chap_eng_${selectedGrade}_1",
                            chapterTitle = chap?.title ?: initialQuiz?.chapterTitle?.takeIf { it.isNotBlank() }
                                ?: "Chapter 1",
                            title = cleanTitle,
                            description = description.trim().ifBlank { "Admin-created assessment" },
                            durationMinutes = (duration.toLongOrNull() ?: 10L).coerceIn(1L, 180L),
                            questionCount = initialQuiz?.questionCount ?: if (firstPrompt.isNotBlank()) 1L else 0L,
                            premiumOnly = premiumOnly,
                            published = published
                        )
                        val q = if (initialQuiz == null && firstPrompt.isNotBlank()) {
                            QuizQuestion(
                                quizId = "",
                                questionType = QuestionType.MCQ.key,
                                prompt = firstPrompt.trim(),
                                options = listOf(opt1.ifBlank { "Option A" }, opt2.ifBlank { "Option B" }),
                                correctIndices = listOf(0L),
                                correctText = "",
                                explanation = "Choice 1 is the correct answer.",
                                points = 20L,
                                published = true
                            )
                        } else null
                        onSave(quiz, q)
                    }
                },
                modifier = Modifier.testTag("save_quiz_button")
            ) {
                Text(if (initialQuiz == null) "Create" else "Update")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminMcqEditorDialog(
    quizzes: List<QuizItem>,
    initialQuestion: QuizQuestion? = null,
    preselectedQuizId: String? = null,
    onDismiss: () -> Unit,
    onSave: (QuizQuestion) -> Unit
) {
    var selectedQuizId by remember(initialQuestion?.id, preselectedQuizId) {
        mutableStateOf(
            initialQuestion?.quizId?.takeIf { it.isNotBlank() }
                ?: preselectedQuizId?.takeIf { it.isNotBlank() }
                ?: quizzes.firstOrNull()?.id
                ?: "quiz_eng_7_tenses"
        )
    }
    var prompt by remember(initialQuestion?.id) { mutableStateOf(initialQuestion?.prompt ?: "") }
    var optA by remember(initialQuestion?.id) { mutableStateOf(initialQuestion?.options?.getOrNull(0) ?: "") }
    var optB by remember(initialQuestion?.id) { mutableStateOf(initialQuestion?.options?.getOrNull(1) ?: "") }
    var optC by remember(initialQuestion?.id) { mutableStateOf(initialQuestion?.options?.getOrNull(2) ?: "") }
    var optD by remember(initialQuestion?.id) { mutableStateOf(initialQuestion?.options?.getOrNull(3) ?: "") }
    var correctIdx by remember(initialQuestion?.id) {
        mutableIntStateOf((initialQuestion?.correctIndices?.firstOrNull()?.toInt() ?: 0).coerceIn(0, 3))
    }
    var pointsText by remember(initialQuestion?.id) {
        mutableStateOf((initialQuestion?.points ?: 20L).toString())
    }
    var explanation by remember(initialQuestion?.id) { mutableStateOf(initialQuestion?.explanation ?: "") }
    var published by remember(initialQuestion?.id) { mutableStateOf(initialQuestion?.published ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialQuestion == null) "Add MCQ Question" else "Edit MCQ Question",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (quizzes.isNotEmpty()) {
                    Text("Target Quiz", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quizzes.forEach { q ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedQuizId = q.id },
                                color = if (selectedQuizId == q.id) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = q.title,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    color = if (selectedQuizId == q.id) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Question Prompt") }, minLines = 2)
                OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option 1") })
                OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option 2") })
                OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option 3 (Optional)") })
                OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option 4 (Optional)") })

                Text("Correct Option", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 1, 2, 3).forEach { idx ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { correctIdx = idx },
                            color = if (correctIdx == idx) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Opt ${idx + 1}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = if (correctIdx == idx) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = pointsText,
                    onValueChange = { pointsText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Points") },
                    singleLine = true
                )
                OutlinedTextField(value = explanation, onValueChange = { explanation = it }, label = { Text("Explanation") })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Published")
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            val opts = listOf(optA, optB, optC, optD).map { it.trim() }.filter { it.isNotBlank() }
            TextButton(
                enabled = prompt.isNotBlank() && opts.size >= 2,
                onClick = {
                    val safeIdx = correctIdx.toLong().coerceIn(0L, (opts.size - 1).toLong())
                    val pts = (pointsText.toLongOrNull() ?: 20L).coerceIn(1L, 100L)
                    onSave(
                        (initialQuestion ?: QuizQuestion()).copy(
                            quizId = selectedQuizId,
                            questionType = QuestionType.MCQ.key,
                            prompt = prompt.trim(),
                            options = opts,
                            correctIndices = listOf(safeIdx),
                            correctText = "",
                            explanation = explanation.trim().ifBlank { "Correct option is #${safeIdx + 1}." },
                            points = pts,
                            published = published
                        )
                    )
                },
                modifier = Modifier.testTag("save_mcq_button")
            ) {
                Text(if (initialQuestion == null) "Add MCQ" else "Update MCQ")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminAnnouncementEditorDialog(
    initialAnnouncement: AnnouncementItem? = null,
    onDismiss: () -> Unit,
    onSave: (AnnouncementItem) -> Unit
) {
    var title by remember(initialAnnouncement?.id) { mutableStateOf(initialAnnouncement?.title ?: "") }
    var message by remember(initialAnnouncement?.id) { mutableStateOf(initialAnnouncement?.message ?: "") }
    var targetClass by remember(initialAnnouncement?.id) { mutableLongStateOf(initialAnnouncement?.targetClass ?: 0L) }
    var highPriority by remember(initialAnnouncement?.id) { mutableStateOf(initialAnnouncement?.priority == "high") }
    var published by remember(initialAnnouncement?.id) { mutableStateOf(initialAnnouncement?.published ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialAnnouncement == null) "Send Announcement" else "Edit Announcement",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Target Audience", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0L, 6L, 7L, 8L, 9L, 10L, 11L, 12L).forEach { g ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { targetClass = g },
                            color = if (targetClass == g) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (g == 0L) "All Classes" else "Class $g",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                color = if (targetClass == g) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("High Priority Alert")
                    Switch(checked = highPriority, onCheckedChange = { highPriority = it })
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Published")
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && message.isNotBlank(),
                onClick = {
                    if (title.isNotBlank() && message.isNotBlank()) {
                        onSave(
                            (initialAnnouncement ?: AnnouncementItem()).copy(
                                title = title.trim(),
                                message = message.trim(),
                                targetClass = targetClass,
                                priority = if (highPriority) "high" else "normal",
                                published = published
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_announcement_button")
            ) {
                Text(if (initialAnnouncement == null) "Broadcast" else "Update")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminEditStudentDialog(
    student: UserProfile,
    canBlockOrDelete: Boolean,
    availableGrades: List<Long> = (6L..12L).toList(),
    onDismiss: () -> Unit,
    onSave: (displayName: String, selectedClass: Long, subscriptionTier: String, blocked: Boolean) -> Unit
) {
    val gradeChoices = remember(availableGrades, student.selectedClass) {
        (availableGrades + student.selectedClass.coerceIn(6L, 12L))
            .distinct()
            .sorted()
            .ifEmpty { (6L..12L).toList() }
    }
    var displayName by remember(student.userId) { mutableStateOf(student.displayName) }
    var selectedClass by remember(student.userId) { mutableLongStateOf(student.selectedClass.coerceIn(6L, 12L)) }
    var subscriptionTier by remember(student.userId) {
        mutableStateOf(if (student.subscriptionTier == "premium") "premium" else "free")
    }
    var blocked by remember(student.userId) { mutableStateOf(student.blocked) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Student", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = student.email.ifBlank { student.userId },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Student Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Class Grade", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gradeChoices.forEach { g ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedClass = g },
                            color = if (selectedClass == g) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Class $g",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                color = if (selectedClass == g) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Text("Subscription Plan", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("free" to "Free", "premium" to "Premium").forEach { (key, label) ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { subscriptionTier = key },
                            color = if (subscriptionTier == key) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (subscriptionTier == key) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                if (canBlockOrDelete) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Block Student Access", color = if (blocked) DangerRed else MaterialTheme.colorScheme.onSurface)
                        Switch(checked = blocked, onCheckedChange = { blocked = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = displayName.isNotBlank(),
                onClick = {
                    onSave(
                        displayName.trim(),
                        selectedClass,
                        subscriptionTier,
                        if (canBlockOrDelete) blocked else false
                    )
                },
                modifier = Modifier.testTag("save_student_button")
            ) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun LazyListScope.adminManageStudentsItems(
    adminUsers: List<UserProfile>,
    currentAdminUid: String,
    isLoadingUsers: Boolean,
    usersError: String?,
    supabaseStatus: SupabaseConnectionStatus,
    searchQuery: String,
    filterMode: String,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onRefreshStudents: () -> Unit,
    onOpenSupabaseSetup: () -> Unit,
    onEditStudent: (UserProfile) -> Unit,
    onRequestBlockToggle: (UserProfile) -> Unit,
    onRequestDeleteStudent: (UserProfile) -> Unit
) {
    val totalStudents = adminUsers.count { it.role != "admin" }
    val blockedCount = adminUsers.count { it.blocked }
    val activeStudents = (totalStudents - adminUsers.count { it.role != "admin" && it.blocked }).coerceAtLeast(0)

    val filtered = adminUsers.filter { u ->
        val matchesFilter = when (filterMode) {
            "Active" -> u.role != "admin" && !u.blocked
            "Blocked" -> u.blocked
            "Admins" -> u.role == "admin"
            else -> true
        }
        val q = searchQuery.trim()
        val matchesQuery = q.isEmpty() ||
            u.displayName.contains(q, ignoreCase = true) ||
            u.email.contains(q, ignoreCase = true) ||
            "Class ${u.selectedClass}".contains(q, ignoreCase = true)
        matchesFilter && matchesQuery
    }

    item {
        Tactile3DCard(
            faceBrush = Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF312E81), Color(0xFF4C1D95))),
            depthColor = Color(0xFF17134D),
            shadowColor = BlueEnd,
            cornerRadius = 20.dp,
            depthHeight = 4.5.dp,
            floatingLift = 4.5.dp,
            glassGlossAlpha = 1.0f,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Group, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Student Management",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Edit, block, unblock or delete registered accounts",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFDCE4FF)
                            )
                        }
                    }
                    IconButton(
                        onClick = onRefreshStudents,
                        enabled = !isLoadingUsers,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("refresh_students_btn")
                    ) {
                        if (isLoadingUsers) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh students", tint = Color.White)
                        }
                    }
                }
                if (isLoadingUsers) {
                    LinearProgressIndicator(
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudentSummaryPill("Students", "$totalStudents", Modifier.weight(1f))
                    StudentSummaryPill("Active", "$activeStudents", Modifier.weight(1f))
                    StudentSummaryPill("Blocked", "$blockedCount", Modifier.weight(1f))
                }
            }
        }
    }

    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search name, email or class...", color = Color(0xFF9AA5D1)) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = "Search students", tint = Color(0xFFC5A6FF))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = Color(0xFF9AA5D1))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = ElectricPurple,
                    unfocusedBorderColor = NavyCardBorder,
                    cursorColor = ElectricPurple
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_search_input")
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Active", "Blocked", "Admins").forEach { mode ->
                    val selected = filterMode == mode
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onFilterChange(mode) }
                            .testTag("student_filter_${mode.lowercase()}"),
                        color = if (selected) ElectricPurple else NavyCard,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = mode,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (!usersError.isNullOrBlank()) {
        item {
            Tactile3DCard(
                faceBrush = Brush.linearGradient(
                    listOf(Color(0xFF4C1D24), Color(0xFF2B1220))
                ),
                depthColor = Color(0xFF1A0A10),
                shadowColor = DangerRed,
                highlightBorderColor = DangerRed.copy(alpha = 0.55f),
                borderEndColor = NavyCardBorder,
                cornerRadius = 18.dp,
                depthHeight = 4.dp,
                floatingLift = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("students_sync_error_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFFB4B4))
                        Text(
                            text = "Unable to sync students from Supabase",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        text = usersError,
                        color = Color(0xFFFFD6D6),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onRefreshStudents,
                            enabled = !isLoadingUsers,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", color = Color.White, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onOpenSupabaseSetup,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Storage, contentDescription = null, tint = Color(0xFFC5A6FF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Setup SQL", color = Color(0xFFC5A6FF), maxLines = 1)
                        }
                    }
                }
            }
        }
    } else if (filtered.isEmpty() && !isLoadingUsers) {
        item {
            Tactile3DCard(
                faceBrush = Brush.linearGradient(
                    listOf(Color(0xFF1D2761).copy(alpha = 0.94f), Color(0xFF11173D).copy(alpha = 0.96f))
                ),
                depthColor = Color(0xFF070B21),
                shadowColor = Color(0xFF05081A),
                highlightBorderColor = Color.White.copy(alpha = 0.28f),
                borderEndColor = NavyCardBorder,
                cornerRadius = 18.dp,
                depthHeight = 4.dp,
                floatingLift = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (adminUsers.isEmpty()) "No registered users synced yet" else "No matching students",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            adminUsers.isNotEmpty() -> "Try clearing your search or switching the filter chip above."
                            supabaseStatus.hasManualOverride ->
                                "Manual Admin Setup only saves credentials on this device. To sync students signing in on other devices, add SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets, or sign in as a student on this device and tap Refresh."
                            else ->
                                "Tap Refresh after students sign in, or check Manage Database → Setup Supabase if RLS is blocking user_profiles."
                        },
                        color = Color(0xFF9AA5D1),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    itemsIndexed(filtered, key = { idx, item -> item.id.ifBlank { item.userId }.ifBlank { "user_$idx" } }) { _, u ->
        val uid = u.id.ifBlank { u.userId }
        val isAdminUser = u.role == "admin" || uid == currentAdminUid
        val canBlockOrDelete = !isAdminUser

        Tactile3DCard(
            faceBrush = Brush.linearGradient(
                if (u.blocked) {
                    listOf(Color(0xFF3E1929).copy(alpha = 0.94f), Color(0xFF211124).copy(alpha = 0.96f))
                } else {
                    listOf(Color(0xFF1D2761).copy(alpha = 0.94f), Color(0xFF11173D).copy(alpha = 0.96f))
                }
            ),
            depthColor = Color(0xFF070B21),
            shadowColor = Color(0xFF05081A),
            highlightBorderColor = if (u.blocked) DangerRed.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.28f),
            borderEndColor = NavyCardBorder,
            cornerRadius = 18.dp,
            depthHeight = 4.dp,
            floatingLift = 4.dp,
            glassGlossAlpha = 0.65f,
            onClick = { onEditStudent(u) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("student_card_$uid")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    u.blocked -> DangerRed.copy(alpha = 0.28f)
                                    isAdminUser -> ElectricPurple.copy(alpha = 0.35f)
                                    else -> BlueStart.copy(alpha = 0.35f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (u.blocked) DangerRed.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = u.displayName.firstOrNull()?.uppercase() ?: "S",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = u.displayName.ifBlank { "Student" },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = u.email.ifBlank { uid },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBCC6EC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Class ${u.selectedClass} • ${u.points} pts • ${u.avgScore}% avg • ${u.subscriptionTier.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9AA5D1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                u.blocked -> DangerRed.copy(alpha = 0.28f)
                                isAdminUser -> ElectricPurple.copy(alpha = 0.32f)
                                else -> GreenStart.copy(alpha = 0.22f)
                            }
                        ) {
                            Text(
                                text = when {
                                    u.blocked -> "BLOCKED"
                                    isAdminUser -> "ADMIN"
                                    else -> "ACTIVE"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    u.blocked -> Color(0xFFFFB4B4)
                                    isAdminUser -> Color(0xFFE0C8FF)
                                    else -> Color(0xFF9BF6C6)
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onEditStudent(u) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_student_$uid")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color(0xFFC5A6FF), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", color = Color.White, maxLines = 1)
                    }
                    if (canBlockOrDelete) {
                        OutlinedButton(
                            onClick = { onRequestBlockToggle(u) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("block_student_$uid")
                        ) {
                            Icon(
                                imageVector = if (u.blocked) Icons.Rounded.LockOpen else Icons.Rounded.Block,
                                contentDescription = null,
                                tint = if (u.blocked) SuccessGreen else OrangeStart,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (u.blocked) "Unblock" else "Block",
                                color = if (u.blocked) Color(0xFF9BF6C6) else Color(0xFFFFD8A8),
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = { onRequestDeleteStudent(u) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("delete_student_$uid")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete student ${u.displayName}",
                                tint = DangerRed
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NavyCard,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Shield, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Protected", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC5CEF0))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentSummaryPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = value,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFDCE4FF)
        )
    }
}

@Composable
fun BlockedAccountScreen(
    profile: UserProfile,
    supportEmail: String,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Tactile3DCard(
            faceBrush = Brush.linearGradient(
                listOf(Color(0xFF2B1328), Color(0xFF15193D))
            ),
            depthColor = Color(0xFF070B21),
            shadowColor = DangerRed,
            highlightBorderColor = DangerRed.copy(alpha = 0.55f),
            borderEndColor = NavyCardBorder,
            cornerRadius = 24.dp,
            depthHeight = 5.dp,
            floatingLift = 5.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DangerRed.copy(alpha = 0.2f))
                        .border(1.5.dp, DangerRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = "Account blocked",
                        tint = DangerRed,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Text(
                    text = "Account Blocked",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )
                Text(
                    text = "Hi ${profile.displayName.ifBlank { "Student" }}, your StudyHub account has been blocked by an administrator.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD5DCFA),
                    textAlign = TextAlign.Center
                )
                if (supportEmail.isNotBlank()) {
                    Text(
                        text = "Need help? Contact $supportEmail",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9AA5D1),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("blocked_sign_out_button")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
