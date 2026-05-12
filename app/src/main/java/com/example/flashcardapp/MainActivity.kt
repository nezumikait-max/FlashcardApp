package com.example.flashcardapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flashcardapp.data.Flashcard
import com.example.flashcardapp.viewmodel.FlashcardViewModel
import dagger.hilt.android.AndroidEntryPoint

enum class AppScreen {
    Dashboard,
    Study
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.Dashboard) }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        AppScreen.Dashboard -> FlashcardScreen(
                            onStartStudy = { currentScreen = AppScreen.Study }
                        )
                        AppScreen.Study -> StudyScreen(
                            onExit = { currentScreen = AppScreen.Dashboard }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(viewModel: FlashcardViewModel = viewModel(), onStartStudy: () -> Unit) {
    val flashcards by viewModel.flashcards.collectAsState()
    val context = LocalContext.current

    var questionText by remember { mutableStateOf("") }
    var answerText by remember { mutableStateOf("") }
    var editingFlashcard by remember { mutableStateOf<Flashcard?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Flashcards Dashboard") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Permission and Launch Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    if (!Settings.canDrawOverlays(context)) {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    }
                }) {
                    Text("Grant Overlay")
                }

                Button(onClick = {
                    if (Settings.canDrawOverlays(context)) {
                        context.startService(Intent(context, com.example.flashcardapp.service.FloatingFlashcardService::class.java))
                    }
                }) {
                    Text("Launch Floating UI")
                }

                Button(
                    onClick = onStartStudy,
                    enabled = flashcards.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Study Mode")
                }
            }

            // Input Form
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("Question") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = answerText,
                    onValueChange = { answerText = it },
                    label = { Text("Answer") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (questionText.isNotBlank() && answerText.isNotBlank()) {
                                if (editingFlashcard == null) {
                                    viewModel.insertFlashcard(questionText, answerText)
                                } else {
                                    viewModel.updateFlashcard(editingFlashcard!!.copy(question = questionText, answer = answerText))
                                    editingFlashcard = null
                                }
                                questionText = ""
                                answerText = ""
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (editingFlashcard == null) "Save Flashcard" else "Update Flashcard")
                    }
                    if (editingFlashcard != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                editingFlashcard = null
                                questionText = ""
                                answerText = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }

            // Flashcard List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(flashcards) { flashcard ->
                    FlashcardItem(
                        flashcard,
                        onDelete = { viewModel.deleteFlashcard(flashcard) },
                        onEdit = {
                            editingFlashcard = flashcard
                            questionText = flashcard.question
                            answerText = flashcard.answer
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FlashcardItem(flashcard: Flashcard, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Q: ${flashcard.question}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "A: ${flashcard.answer}", style = MaterialTheme.typography.bodyMedium)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Flashcard", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Flashcard", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(viewModel: FlashcardViewModel = viewModel(), onExit: () -> Unit) {
    val flashcards by viewModel.flashcards.collectAsState()
    var currentIndex by remember { mutableStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    // Shuffle cards once when the screen is entered
    val shuffledCards = remember(flashcards) { flashcards.shuffled() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Mode") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Filled.Close, contentDescription = "Exit Study Mode")
                    }
                }
            )
        }
    ) { padding ->
        if (shuffledCards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No flashcards to study!")
            }
        } else {
            val currentCard = shuffledCards[currentIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Card ${currentIndex + 1} of ${shuffledCards.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clickable { showAnswer = !showAnswer },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showAnswer) currentCard.answer else currentCard.question,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (showAnswer) "Answer (Tap to hide)" else "Question (Tap to reveal)",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = {
                            if (currentIndex > 0) {
                                currentIndex--
                                showAnswer = false
                            }
                        },
                        enabled = currentIndex > 0
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Previous Card")
                    }
                    
                    IconButton(
                        onClick = {
                            if (currentIndex < shuffledCards.size - 1) {
                                currentIndex++
                                showAnswer = false
                            }
                        },
                        enabled = currentIndex < shuffledCards.size - 1
                    ) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Next Card")
                    }
                }
            }
        }
    }
}
