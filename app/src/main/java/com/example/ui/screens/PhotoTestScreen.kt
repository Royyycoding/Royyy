package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TestRecord
import com.example.data.local.TypingTestQuestion
import com.example.data.local.VocabularyWord
import com.example.data.repository.TopiaRepository
import com.example.ui.theme.TopiaGreenSuccess
import com.example.ui.theme.TopiaIndigoPrimary
import com.example.util.TtsManager
import kotlinx.coroutines.launch

@Composable
fun PhotoTestScreen(
    repository: TopiaRepository,
    ttsManager: TtsManager,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val questions = remember { mutableStateListOf<TypingTestQuestion>() }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var isTestCompleted by remember { mutableStateOf(false) }
    var savedWordsCount by remember { mutableIntStateOf(0) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            isGenerating = true
            errorMessage = null
            coroutineScope.launch {
                val res = repository.generateTestFromImage(bitmap)
                isGenerating = false
                res.onSuccess { list ->
                    questions.clear()
                    questions.addAll(list)
                    currentQuestionIndex = 0
                    isTestCompleted = false
                }.onFailure { err ->
                    errorMessage = err.localizedMessage ?: "Failed to generate test from photo"
                }
            }
        }
    }

    // Photo gallery picker launcher (zero-permission)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    selectedBitmap = bitmap
                    isGenerating = true
                    errorMessage = null
                    coroutineScope.launch {
                        val res = repository.generateTestFromImage(bitmap)
                        isGenerating = false
                        res.onSuccess { list ->
                            questions.clear()
                            questions.addAll(list)
                            currentQuestionIndex = 0
                            isTestCompleted = false
                        }.onFailure { err ->
                            errorMessage = err.localizedMessage ?: "Failed to generate test from photo"
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Could not load selected photo: ${e.message}"
            }
        }
    }

    fun startSampleTest(passageText: String) {
        isGenerating = true
        errorMessage = null
        coroutineScope.launch {
            val res = repository.generateTestFromText(passageText)
            isGenerating = false
            res.onSuccess { list ->
                questions.clear()
                questions.addAll(list)
                currentQuestionIndex = 0
                isTestCompleted = false
            }.onFailure { err ->
                errorMessage = err.localizedMessage
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Photo English Test",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Snap workbook -> Strict Typing Test (No Hints)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (questions.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        questions.clear()
                        isTestCompleted = false
                        selectedBitmap = null
                    },
                    modifier = Modifier.testTag("reset_test_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Test", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Gemini is analyzing the textbook page...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Extracting key sentences and hiding all hints for active typing recall",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (questions.isEmpty()) {
            // Zero-state / Photo capture prompt
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Take a Picture of English Material",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Point your camera at any textbook, test paper, or worksheet. Topia AI extracts key vocabulary and challenges you to type the answer with zero hints.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { cameraLauncher.launch(null) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("take_camera_photo_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Take Photo")
                                }

                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pick_gallery_photo_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gallery")
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Or try with ready-made textbook samples:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                val samplePassages = listOf(
                    "High School CSAT English: Reading comprehension on cognitive resilience and scientific methodology" to
                            "Scientific experiments require meticulous methodology to remain valid. When conditions become unpredictable, researchers must remain resilient and adapt their pragmatic hypotheses to uncover ubiquitous universal principles.",
                    "TOEIC Business Communication: Workplace negotiations and strategic agreements" to
                            "During the quarterly conference, the executive gave an articulate presentation regarding international logistics. The team emphasized that customer loyalty is ephemeral unless companies maintain authentic engagement.",
                    "Academic TOEFL Essay: Environmental biology and ecological adaptations" to
                            "Organisms exhibit remarkable physiological plasticity. Species that demonstrate meticulous foraging strategies thrive even in ephemeral water sources across arid desert ecosystems."
                )

                itemsIndexed(samplePassages) { index, (title, passage) ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { startSampleTest(passage) }
                            .testTag("sample_test_$index"),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Quiz,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = passage.take(75) + "...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        } else if (!isTestCompleted) {
            // ACTIVE TEST (NO HINTS TYPING MODE)
            val currentQ = questions[currentQuestionIndex]
            val progress = (currentQuestionIndex + 1).toFloat() / questions.size

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Mode: Strict Typing (No Hints)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Question Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Fill in the blank by typing the exact English word:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sentence with blank
                        Text(
                            text = currentQ.sentenceWithBlank,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Audio button to listen to context sentence
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val sentenceSpoken = currentQ.sentenceWithBlank.replace("____", "blank")
                                    ttsManager.speak(sentenceSpoken)
                                }
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Listen sentence", tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = "Listen to context audio",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // User Typing Input (Strict, no hints)
                        OutlinedTextField(
                            value = currentQ.userAnswer,
                            onValueChange = { newAnswer ->
                                if (!currentQ.isSubmitted) {
                                    questions[currentQuestionIndex] = currentQ.copy(userAnswer = newAnswer)
                                }
                            },
                            label = { Text("Type target English word here") },
                            placeholder = { Text("Type word...") },
                            singleLine = true,
                            enabled = !currentQ.isSubmitted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("typing_test_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        // If already submitted this item, show result
                        AnimatedVisibility(visible = currentQ.isSubmitted) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                val isCorrect = currentQ.isCorrect
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCorrect) TopiaGreenSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                                contentDescription = null,
                                                tint = if (isCorrect) TopiaGreenSuccess else MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isCorrect) "Correct! Excellent recall." else "Incorrect typing.",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCorrect) TopiaGreenSuccess else MaterialTheme.colorScheme.error
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Correct Word: ${currentQ.targetWord}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Korean: ${currentQ.koreanMeaning}",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "English Definition: ${currentQ.englishDefinition}",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    repository.saveWord(
                                                        VocabularyWord(
                                                            word = currentQ.targetWord,
                                                            koreanMeaning = currentQ.koreanMeaning,
                                                            englishDefinition = currentQ.englishDefinition,
                                                            exampleSentence = currentQ.sentenceWithBlank.replace("____", currentQ.targetWord)
                                                        )
                                                    )
                                                    savedWordsCount++
                                                }
                                            },
                                            modifier = Modifier.testTag("add_to_voca_button")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save to Voca Book", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentQuestionIndex > 0) currentQuestionIndex--
                        },
                        enabled = currentQuestionIndex > 0,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Previous")
                    }

                    if (!currentQ.isSubmitted) {
                        Button(
                            onClick = {
                                val userAns = currentQ.userAnswer.trim().lowercase()
                                val correctAns = currentQ.targetWord.trim().lowercase()
                                val isCorrect = userAns == correctAns
                                questions[currentQuestionIndex] = currentQ.copy(
                                    isSubmitted = true,
                                    isCorrect = isCorrect
                                )
                            },
                            enabled = currentQ.userAnswer.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("check_answer_button")
                        ) {
                            Text("Check Typing")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (currentQuestionIndex < questions.size - 1) {
                                    currentQuestionIndex++
                                } else {
                                    // Complete test and record score in Room DB
                                    val correct = questions.count { it.isCorrect }
                                    val percent = ((correct.toFloat() / questions.size) * 100).toInt()
                                    coroutineScope.launch {
                                        repository.saveTestRecord(
                                            TestRecord(
                                                title = "Photo Typing Test",
                                                totalQuestions = questions.size,
                                                correctCount = correct,
                                                scorePercentage = percent
                                            )
                                        )
                                    }
                                    isTestCompleted = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("next_or_finish_button")
                        ) {
                            Text(if (currentQuestionIndex < questions.size - 1) "Next Question" else "See Test Results")
                        }
                    }
                }
            }
        } else {
            // TEST SUMMARY REPORT
            val total = questions.size
            val correct = questions.count { it.isCorrect }
            val scorePercent = ((correct.toFloat() / total) * 100).toInt()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (scorePercent >= 70) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Test Report Card",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "$scorePercent%",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$correct out of $total questions answered correctly without hints",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        // Save all incorrect words to voca book
                                        val missedWords = questions.filter { !it.isCorrect }.map {
                                            VocabularyWord(
                                                word = it.targetWord,
                                                koreanMeaning = it.koreanMeaning,
                                                englishDefinition = it.englishDefinition,
                                                exampleSentence = it.sentenceWithBlank.replace("____", it.targetWord)
                                            )
                                        }
                                        if (missedWords.isNotEmpty()) {
                                            repository.saveWords(missedWords)
                                            savedWordsCount += missedWords.size
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("save_all_missed_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add All Missed Words to Voca Book")
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Question-by-Question Review:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                itemsIndexed(questions) { idx, q ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Q${idx + 1}: ${q.targetWord}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (q.isCorrect) TopiaGreenSuccess.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = if (q.isCorrect) "CORRECT" else "MISSED",
                                        color = if (q.isCorrect) TopiaGreenSuccess else MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your typing: \"${q.userAnswer}\"",
                                fontSize = 13.sp,
                                color = if (q.isCorrect) TopiaGreenSuccess else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Meaning: ${q.koreanMeaning}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Definition: ${q.englishDefinition}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
