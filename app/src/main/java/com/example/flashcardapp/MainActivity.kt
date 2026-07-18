package com.example.flashcardapp

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flashcardapp.data.Flashcard
import com.example.flashcardapp.viewmodel.FlashcardViewModel
import dagger.hilt.android.AndroidEntryPoint

enum class AppScreen {
    Dashboard,
    Study
}

@Composable
fun FlashcardTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFD0BCFF),
            onPrimary = Color(0xFF381E72),
            primaryContainer = Color(0xFF4F378B),
            onPrimaryContainer = Color(0xFFEADDFF),
            secondary = Color(0xFFCCC2DC),
            onSecondary = Color(0xFF332D41),
            secondaryContainer = Color(0xFF4A4458),
            onSecondaryContainer = Color(0xFFE8DEF8),
            tertiary = Color(0xFFEFB8C8),
            onTertiary = Color(0xFF492532),
            tertiaryContainer = Color(0xFF633B48),
            onTertiaryContainer = Color(0xFFFFD8E4),
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC),
            background = Color(0xFF1C1B1F),
            onBackground = Color(0xFFE6E1E5),
            surface = Color(0xFF1C1B1F),
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = Color(0xFF49454F),
            onSurfaceVariant = Color(0xFFCAC4D0),
            outline = Color(0xFF938F99),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6750A4),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFEADDFF),
            onPrimaryContainer = Color(0xFF21005D),
            secondary = Color(0xFF625B71),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFE8DEF8),
            onSecondaryContainer = Color(0xFF1D192B),
            tertiary = Color(0xFF7D5260),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFD8E4),
            onTertiaryContainer = Color(0xFF31111D),
            error = Color(0xFFB3261E),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            background = Color(0xFFFFFBFE),
            onBackground = Color(0xFF1C1B1F),
            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFFE7E0EC),
            onSurfaceVariant = Color(0xFF49454F),
            outline = Color(0xFF79747E),
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.Dashboard) }

            FlashcardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        AppScreen.Dashboard -> FlashcardScreen {
                            currentScreen = AppScreen.Study
                        }
                        AppScreen.Study -> StudyScreen {
                            currentScreen = AppScreen.Dashboard
                        }
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
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current

    var questionText by remember { mutableStateOf("") }
    var answerText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("General") }
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
                Button(
                    onClick = {
                        if (!Settings.canDrawOverlays(context)) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Grant Overlay")
                }

                Button(
                    onClick = {
                        if (Settings.canDrawOverlays(context)) {
                            context.startService(Intent(context, com.example.flashcardapp.service.FloatingFlashcardService::class.java))
                        }
                    }
                ) {
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

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search flashcards...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // Input Form
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (editingFlashcard == null) "Add New Flashcard" else "Edit Flashcard",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                    OutlinedTextField(
                        value = categoryText,
                        onValueChange = { categoryText = it },
                        label = { Text("Study Group (e.g., French, Math)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (questionText.isNotBlank() && answerText.isNotBlank()) {
                                    if (editingFlashcard == null) {
                                        viewModel.insertFlashcard(questionText, answerText, categoryText)
                                    } else {
                                        viewModel.updateFlashcard(editingFlashcard!!.copy(question = questionText, answer = answerText, category = categoryText))
                                        editingFlashcard = null
                                    }
                                    questionText = ""
                                    answerText = ""
                                    categoryText = "General"
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
                                    categoryText = "General"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // Study Group Filter
            val categories by viewModel.categories.collectAsState()
            val selectedCategory by viewModel.selectedCategory.collectAsState()

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    text = "Active Study Group (shown in Floating UI):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            ScrollableTabRow(
                selectedTabIndex = if (selectedCategory == null) 0 else categories.indexOf(selectedCategory) + 1,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth(),
                divider = {}
            ) {
                Tab(
                    selected = selectedCategory == null,
                    onClick = { viewModel.setSelectedCategory(null) },
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("All")
                            if (selectedCategory == null) {
                                Badge(modifier = Modifier.padding(start = 4.dp)) { Text("${flashcards.size}") }
                            }
                        }
                    }
                )
                categories.forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { viewModel.setSelectedCategory(category) },
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(category)
                                if (selectedCategory == category) {
                                    Badge(modifier = Modifier.padding(start = 4.dp)) { Text(flashcards.size.toString()) }
                                }
                            }
                        }
                    )
                }
            }

            // Flashcard List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = flashcards,
                    key = { it.id }
                ) { flashcard ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if ((it == SwipeToDismissBoxValue.EndToStart) || (it == SwipeToDismissBoxValue.StartToEnd)) {
                                viewModel.deleteFlashcard(flashcard)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val direction = dismissState.dismissDirection
                            val color by animateColorAsState(
                                when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                                    else -> MaterialTheme.colorScheme.errorContainer
                                },
                                label = "Dismiss Color"
                            )
                            val alignment = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }
                            val icon = Icons.Default.Delete
                            val scale by animateFloatAsState(
                                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
                                label = "Dismiss Icon Scale"
                            )

                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = alignment
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = "Delete Icon",
                                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    ) {
                        FlashcardItem(
                            flashcard,
                            onDelete = { viewModel.deleteFlashcard(flashcard) },
                            onEdit = {
                                editingFlashcard = flashcard
                                questionText = flashcard.question
                                answerText = flashcard.answer
                                categoryText = flashcard.category
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                Spacer(modifier = Modifier.height(4.dp))
                SuggestionChip(
                    onClick = { },
                    label = { Text(flashcard.category, style = MaterialTheme.typography.labelSmall) }
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Flashcard", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Flashcard", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(viewModel: FlashcardViewModel = viewModel(), onExit: () -> Unit) {
    val flashcards by viewModel.flashcards.collectAsState()
    var currentIndex by remember { mutableIntStateOf(0) }
    var rotated by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (rotated) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "Card Rotation"
    )

    val shuffledCards = remember(flashcards) { flashcards.shuffled() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isFinished) "Study Complete" else "Study Mode") },
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
        } else if (isFinished) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.CheckCircle, 
                    contentDescription = null, 
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Deck Finished!", style = MaterialTheme.typography.headlineMedium)
                Text("You've reviewed ${shuffledCards.size} cards.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = {
                    currentIndex = 0
                    rotated = false
                    isFinished = false
                }) {
                    Text("Study Again")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onExit) {
                    Text("Back to Dashboard")
                }
            }
        } else {
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
                
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        val transform = if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                        }
                        transform.using(SizeTransform(clip = false))
                    },
                    label = "Card Transition"
                ) { targetIndex ->
                    val card = shuffledCards[targetIndex]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12f * density
                            }
                            .clickable { rotated = !rotated },
                        contentAlignment = Alignment.Center
                    ) {
                        if (rotation <= 90f) {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = card.question,
                                        style = MaterialTheme.typography.headlineMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationY = 180f },
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = card.answer,
                                        style = MaterialTheme.typography.headlineMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tap to flip",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            if (currentIndex > 0) {
                                currentIndex--
                                rotated = false
                            }
                        },
                        enabled = currentIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Card")
                    }
                    
                    Text(
                        text = "${currentIndex + 1} / ${shuffledCards.size}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    FilledTonalIconButton(
                        onClick = {
                            if (currentIndex < shuffledCards.size - 1) {
                                currentIndex++
                                rotated = false
                            } else {
                                isFinished = true
                            }
                        }
                    ) {
                        Icon(
                            if (currentIndex < shuffledCards.size - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Filled.Check,
                            contentDescription = if (currentIndex < shuffledCards.size - 1) "Next Card" else "Finish"
                        )
                    }
                }
            }
        }
    }
}
