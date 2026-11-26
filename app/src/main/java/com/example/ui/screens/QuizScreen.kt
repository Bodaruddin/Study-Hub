package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.QuestionType
import com.example.data.model.QuizAttemptRecord
import com.example.data.model.QuizItem
import com.example.data.model.QuizQuestion
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.Tactile3DCard
import com.example.ui.theme.BlueStart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GreenStart
import com.example.ui.theme.IndigoHeaderEnd
import com.example.ui.theme.IndigoHeaderStart
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LavenderBorder
import com.example.ui.theme.LavenderSurface
import com.example.ui.theme.OrangeStart
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.viewmodel.ActiveQuizState
import com.example.ui.viewmodel.QuizMode

@Composable
fun QuizScreen(
    selectedClass: Long,
    quizzes: List<QuizItem>,
    quizAttempts: List<QuizAttemptRecord>,
    activeQuiz: ActiveQuizState?,
    onStartQuiz: (QuizItem, QuizMode, Boolean) -> Unit,
    onSelectSingleOption: (String, Int) -> Unit,
    onToggleMultiOption: (String, Int) -> Unit,
    onUpdateFillBlank: (String, String) -> Unit,
    onConfirmPractice: (String) -> Unit,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit,
    onSubmitQuiz: () -> Unit,
    onCloseQuiz: () -> Unit,
    isQuestionCorrect: (ActiveQuizState, QuizQuestion) -> Boolean,
    modifier: Modifier = Modifier
) {
    if (activeQuiz != null) {
        BackHandler { onCloseQuiz() }
        if (activeQuiz.isFinished) {
            QuizResultsPage(
                state = activeQuiz,
                isQuestionCorrect = isQuestionCorrect,
                onDone = onCloseQuiz
            )
        } else {
            ActiveQuizRunner(
                state = activeQuiz,
                onSelectSingleOption = onSelectSingleOption,
                onToggleMultiOption = onToggleMultiOption,
                onUpdateFillBlank = onUpdateFillBlank,
                onConfirmPractice = onConfirmPractice,
                onNextQuestion = onNextQuestion,
                onPreviousQuestion = onPreviousQuestion,
                onSubmitQuiz = onSubmitQuiz,
                onBack = onCloseQuiz
            )
        }
        return
    }

    var selectedMode by remember { mutableStateOf(QuizMode.PRACTICE) }
    var randomizeQuestions by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    val classQuizzes = quizzes.filter { it.published && (it.classGrade == selectedClass || selectedClass == 0L) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Practice & Quizzes",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    text = "Class $selectedClass • Chapter & Subject Tests",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { showHistory = !showHistory },
                shape = RoundedCornerShape(50),
                color = if (showHistory) ElectricPurple else MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = "Quiz history",
                        tint = if (showHistory) Color.White else ElectricPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (showHistory) "Quizzes" else "History (${quizAttempts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (showHistory) Color.White else ElectricPurple
                    )
                }
            }
        }

        if (showHistory) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (quizAttempts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "Complete your first quiz to see your score history and streak progress!",
                                modifier = Modifier.padding(24.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(quizAttempts, key = { it.id }) { att ->
                        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        Tactile3DCard(
                            faceBrush = if (isDark) {
                                Brush.linearGradient(listOf(Color(0xFF202A63).copy(alpha = 0.88f), Color(0xFF151C45).copy(alpha = 0.86f)))
                            } else {
                                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.90f), Color(0xFFEEF3FF).copy(alpha = 0.86f)))
                            },
                            depthColor = if (isDark) Color(0xFF0C102B) else Color(0xFFCCD7F9).copy(alpha = 0.85f),
                            cornerRadius = 18.dp,
                            depthHeight = 4.5.dp,
                            shadowElevation = 7.dp,
                            shadowColor = Color(0xFF4758C9),
                            highlightBorderColor = Color.White.copy(alpha = if (isDark) 0.38f else 0.92f),
                            borderEndColor = Color.White.copy(alpha = if (isDark) 0.14f else 0.38f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (att.scorePercent >= 70) GreenStart.copy(alpha = 0.14f)
                                            else OrangeStart.copy(alpha = 0.14f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${att.scorePercent}%",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (att.scorePercent >= 70) GreenStart else OrangeStart
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(att.quizTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${att.subjectName} • ${att.correctAnswers}/${att.totalQuestions} correct • ${att.mode.uppercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "+${att.pointsEarned} pts",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode & Randomize Controls
                item {
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    Tactile3DCard(
                        faceBrush = if (isDark) {
                            Brush.linearGradient(listOf(Color(0xFF202A63).copy(alpha = 0.88f), Color(0xFF151C45).copy(alpha = 0.86f)))
                        } else {
                            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.90f), Color(0xFFEEF3FF).copy(alpha = 0.86f)))
                        },
                        depthColor = if (isDark) Color(0xFF0C102B) else Color(0xFFCCD7F9).copy(alpha = 0.85f),
                        cornerRadius = 20.dp,
                        depthHeight = 4.5.dp,
                        shadowElevation = 7.dp,
                        shadowColor = Color(0xFF4758C9),
                        highlightBorderColor = Color.White.copy(alpha = if (isDark) 0.38f else 0.92f),
                        borderEndColor = Color.White.copy(alpha = if (isDark) 0.14f else 0.38f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuizMode.entries.forEach { mode ->
                                    val selected = selectedMode == mode
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedMode = mode },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = mode.label,
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Randomize Question Order", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = if (selectedMode == QuizMode.PRACTICE) {
                                            "Instant feedback & explanations after answering"
                                        } else {
                                            "Timed exam • Answer key hidden until submitted"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = randomizeQuestions,
                                    onCheckedChange = { randomizeQuestions = it }
                                )
                            }
                        }
                    }
                }

                items(classQuizzes, key = { it.id }) { quiz ->
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    Tactile3DCard(
                        faceBrush = if (isDark) {
                            Brush.linearGradient(listOf(Color(0xFF202A63).copy(alpha = 0.88f), Color(0xFF151C45).copy(alpha = 0.86f)))
                        } else {
                            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.90f), Color(0xFFEEF3FF).copy(alpha = 0.86f)))
                        },
                        depthColor = if (isDark) Color(0xFF0C102B) else Color(0xFFCCD7F9).copy(alpha = 0.85f),
                        cornerRadius = 20.dp,
                        depthHeight = 4.5.dp,
                        shadowElevation = 7.dp,
                        shadowColor = Color(0xFF4758C9),
                        highlightBorderColor = Color.White.copy(alpha = if (isDark) 0.38f else 0.92f),
                        borderEndColor = Color.White.copy(alpha = if (isDark) 0.14f else 0.38f),
                        onClick = { onStartQuiz(quiz, selectedMode, randomizeQuestions) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_card_${quiz.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(listOf(BlueStart, ElectricPurple))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Psychology,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quiz.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${quiz.subjectName} • ${quiz.questionCount} Qs • ${quiz.durationMinutes} min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = ElectricPurple
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Start quiz",
                                    tint = Color.White,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveQuizRunner(
    state: ActiveQuizState,
    onSelectSingleOption: (String, Int) -> Unit,
    onToggleMultiOption: (String, Int) -> Unit,
    onUpdateFillBlank: (String, String) -> Unit,
    onConfirmPractice: (String) -> Unit,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit,
    onSubmitQuiz: () -> Unit,
    onBack: () -> Unit
) {
    val question = state.questions[state.currentIndex]
    val qType = QuestionType.fromKey(question.questionType)
    val isLast = state.currentIndex == state.questions.lastIndex
    val progress = (state.currentIndex + 1).toFloat() / state.questions.size.coerceAtLeast(1)
    val showExplanation = state.mode == QuizMode.PRACTICE && question.id in state.confirmedPracticeQuestionIds
    val mins = state.remainingSeconds / 60
    val secs = state.remainingSeconds % 60
    val timerText = "%02d:%02d".format(mins, secs)

    // Deep Navy Header + White Rounded Sheet matching Screenshot 2 (Top-Right)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(IndigoHeaderStart, IndigoHeaderEnd)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Exit quiz",
                    tint = Color.White
                )
            }
            Text(
                text = "Quiz",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = "Timer",
                    tint = Color(0xFFFFC542),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = timerText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFC542),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Question Counter & Progress Bar
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ques. ${state.currentIndex + 1} / ${state.questions.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${state.mode.label} • ${qType.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD0D8FF)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = Color(0xFF7B4BFF),
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rounded White Sheet
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = question.prompt,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 27.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    when (qType) {
                        QuestionType.MCQ, QuestionType.TRUE_FALSE -> {
                            val selectedIdx = state.selectedSingleIndices[question.id]
                            question.options.forEachIndexed { idx, option ->
                                val isSelected = selectedIdx == idx
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            width = if (isSelected) 1.8.dp else 1.dp,
                                            color = if (isSelected) ElectricPurple else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { onSelectSingleOption(question.id, idx) }
                                        .testTag("quiz_option_$idx"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) LavenderSurface else MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onSelectSingleOption(question.id, idx) },
                                            colors = RadioButtonDefaults.colors(selectedColor = ElectricPurple)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = option,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF1F1652) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        QuestionType.MULTI_SELECT -> {
                            val selectedSet = state.selectedMultiIndices[question.id] ?: emptySet()
                            question.options.forEachIndexed { idx, option ->
                                val isChecked = idx in selectedSet
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            width = if (isChecked) 1.8.dp else 1.dp,
                                            color = if (isChecked) ElectricPurple else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { onToggleMultiOption(question.id, idx) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isChecked) LavenderSurface else MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { onToggleMultiOption(question.id, idx) }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = option,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isChecked) Color(0xFF1F1652) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            if (state.mode == QuizMode.PRACTICE && !showExplanation && selectedSet.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { onConfirmPractice(question.id) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Check Answer")
                                }
                            }
                        }

                        QuestionType.FILL_BLANK -> {
                            val currentText = state.fillBlankAnswers[question.id] ?: ""
                            OutlinedTextField(
                                value = currentText,
                                onValueChange = { onUpdateFillBlank(question.id, it) },
                                label = { Text("Type your answer") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (state.mode == QuizMode.PRACTICE && !showExplanation && currentText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { onConfirmPractice(question.id) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Check Answer")
                                }
                            }
                        }
                    }

                    // Explanation Box (Shown ONLY after answering in Practice mode — matching Screenshot 2!)
                    if (showExplanation) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, LavenderBorder, RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            color = LavenderSurface
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_quiz_tip),
                                    contentDescription = "Explanation",
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = question.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF1F1A45),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                // Bottom Previous / Next-Submit Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onPreviousQuestion,
                        enabled = state.currentIndex > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Previous", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { if (isLast) onSubmitQuiz() else onNextQuestion() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("quiz_next_submit_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                    ) {
                        Text(
                            text = if (isLast) "Submit" else "Next",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isLast) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizResultsPage(
    state: ActiveQuizState,
    isQuestionCorrect: (ActiveQuizState, QuizQuestion) -> Boolean,
    onDone: () -> Unit
) {
    val pct = if (state.questions.isNotEmpty()) (state.correctCount * 100) / state.questions.size else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(IndigoHeaderStart, IndigoHeaderEnd)),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Quiz Complete!",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))
                CircularProgressRing(
                    progress = pct / 100f,
                    label = "$pct%",
                    size = 92.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${state.correctCount} of ${state.questions.size} Correct • +${state.pointsEarned} pts",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFDCE2FF)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.questions, key = { it.id }) { q ->
                val ok = isQuestionCorrect(state, q)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (ok) "✓ Correct" else "✗ Review",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (ok) GreenStart else DangerRed,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(q.prompt, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = q.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
        ) {
            Text("Back to Quizzes", fontWeight = FontWeight.Bold)
        }
    }
}
