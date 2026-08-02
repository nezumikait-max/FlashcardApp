package com.example.flashcardapp

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flashcardapp.data.Flashcard
import com.example.flashcardapp.viewmodel.FlashcardViewModel
import dagger.hilt.android.AndroidEntryPoint

enum class AppScreen {
    Dashboard,
    Study,
    Settings,
    Trash
}

@Composable
fun FlashcardTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF60A5FA),
            secondary = Color(0xFF8B5CF6),
            tertiary = Color(0xFF34D399),
            background = Color(0xFF09090B),
            surface = Color(0xFF0F172A),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF1E293B)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF2563EB),
            secondary = Color(0xFF7C3AED),
            tertiary = Color(0xFF10B981),
            background = Color(0xFFF8FAFC),
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF64748B),
            outline = Color(0xFFE2E8F0)
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
            FlashcardTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.Dashboard) }
                val context = LocalContext.current

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        AppScreen.Dashboard -> FlashcardScreen(
                            onStartStudy = { currentScreen = AppScreen.Study },
                            onOpenSettings = { currentScreen = AppScreen.Settings },
                            onOpenTrash = { currentScreen = AppScreen.Trash }
                        )
                        AppScreen.Study -> StudyScreen {
                            currentScreen = AppScreen.Dashboard
                        }
                        AppScreen.Settings -> {
                            LaunchedEffect(Unit) {
                                val intent = Intent(context, com.example.flashcardapp.service.FloatingFlashcardService::class.java).apply {
                                    putExtra("EXTRA_SETTINGS_MODE", true)
                                }
                                context.startService(intent)
                            }
                            DisposableEffect(Unit) {
                                onDispose {
                                    val intent = Intent(context, com.example.flashcardapp.service.FloatingFlashcardService::class.java).apply {
                                        putExtra("EXTRA_SETTINGS_MODE", false)
                                    }
                                    context.startService(intent)
                                }
                            }
                            SettingsScreen(currentScreen = currentScreen) {
                                currentScreen = AppScreen.Dashboard
                            }
                        }
                        AppScreen.Trash -> TrashScreen(
                            onBack = { currentScreen = AppScreen.Dashboard }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FlashcardViewModel = viewModel(),
    currentScreen: AppScreen,
    onBack: () -> Unit
) {
    val autoCloseSeconds by viewModel.autoCloseSeconds.collectAsState()
    val appearanceIntervalMinutes by viewModel.appearanceIntervalMinutes.collectAsState()
    val sidebarSide by viewModel.sidebarSide.collectAsState()
    val sidebarHeight by viewModel.sidebarHeight.collectAsState()
    val sidebarVerticalOffset by viewModel.sidebarVerticalOffset.collectAsState()
    val sidebarEnabled by viewModel.sidebarEnabled.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF09090B), Color(0xFF0F172A))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "SETTINGS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overlay Permissions & Toggle
                GlassCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "FLOATING UI PERMISSIONS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassButton(
                                onClick = {
                                    if (!Settings.canDrawOverlays(context)) {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                text = "GRANT",
                                icon = Icons.Default.Security
                            )

                            GlassButton(
                                onClick = {
                                    if (Settings.canDrawOverlays(context)) {
                                        val intent = Intent(context, com.example.flashcardapp.service.FloatingFlashcardService::class.java).apply {
                                            action = "ACTION_SHOW_CARDS"
                                        }
                                        context.startService(intent)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                text = "START",
                                icon = Icons.Default.Layers,
                                accentColor = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                GlassCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "AUTO-CLOSE TIMER",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Close floating card after ${autoCloseSeconds}s of inactivity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Slider(
                            value = autoCloseSeconds.toFloat(),
                            onValueChange = { viewModel.setAutoCloseSeconds(it.toInt()) },
                            valueRange = 5f..120f,
                            steps = 23
                        )
                    }
                }

                GlassCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "APPEARANCE INTERVAL",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Show a flashcard every $appearanceIntervalMinutes minute(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Slider(
                            value = appearanceIntervalMinutes.toFloat(),
                            onValueChange = { viewModel.setAppearanceIntervalMinutes(it.toInt()) },
                            valueRange = 1f..60f,
                            steps = 59
                        )
                    }
                }

                GlassCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Dock, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "QUICK-CREATE SIDEBAR",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Switch(
                                checked = sidebarEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setSidebarEnabled(enabled)
                                    if (enabled && Settings.canDrawOverlays(context)) {
                                        val intent = Intent(context, com.example.flashcardapp.service.FloatingFlashcardService::class.java).apply {
                                            action = "ACTION_START_SIDEBAR"
                                            putExtra("EXTRA_SETTINGS_MODE", currentScreen == AppScreen.Settings)
                                        }
                                        context.startService(intent)
                                    }
                                }
                            )
                        }

                        if (sidebarEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Side position",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                listOf("Left", "Right").forEach { side ->
                                    OutlinedButton(
                                        onClick = { viewModel.setSidebarSide(side) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp, 
                                            if (sidebarSide == side) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (sidebarSide == side) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                                                            else Color.Transparent
                                        )
                                    ) {
                                        Text(
                                            side, 
                                            color = if (sidebarSide == side) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Sidebar Height: ${sidebarHeight}dp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = sidebarHeight.toFloat(),
                                onValueChange = { viewModel.setSidebarHeight(it.toInt()) },
                                valueRange = 50f..400f,
                                steps = 34
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Vertical Offset: ${sidebarVerticalOffset}dp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = sidebarVerticalOffset.toFloat(),
                                onValueChange = { viewModel.setSidebarVerticalOffset(it.toInt()) },
                                valueRange = -600f..600f,
                                steps = 120
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    "Note: Settings apply to the Floating UI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    viewModel: FlashcardViewModel = viewModel(),
    onOpenSettings: () -> Unit,
    onOpenTrash: () -> Unit,
    onStartStudy: () -> Unit
) {
    val flashcards by viewModel.flashcards.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var questionText by remember { mutableStateOf("") }
    var answerText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("General") }
    var editingFlashcard by remember { mutableStateOf<Flashcard?>(null) }
    
    var categoryExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF09090B), Color(0xFF0F172A))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "FLASHCARDS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenTrash) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Recycle Bin")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header & Search
                item {
                    GlassTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = "Search decks...",
                        leadingIcon = Icons.Default.Search,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Input Form
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = if (editingFlashcard == null) "CREATE NEW" else "EDIT CARD",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GlassTextField(
                                value = questionText,
                                onValueChange = { questionText = it },
                                placeholder = "Question",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            GlassTextField(
                                value = answerText,
                                onValueChange = { answerText = it },
                                placeholder = "Answer",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Box(modifier = Modifier.fillMaxWidth()) {
                                GlassTextField(
                                    value = categoryText,
                                    onValueChange = { 
                                        categoryText = it
                                        categoryExpanded = true
                                    },
                                    placeholder = "Study Group (e.g., French, Math)",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { if (it.isFocused) categoryExpanded = true }
                                )
                                
                                val filteredCategories = if (categoryText.isBlank()) categories else categories.filter {
                                    it.contains(categoryText, ignoreCase = true) 
                                }
                                
                                if (categoryExpanded && filteredCategories.isNotEmpty()) {
                                    DropdownMenu(
                                        expanded = categoryExpanded,
                                        onDismissRequest = { categoryExpanded = false },
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
                                            .background(Color(0xFF1E293B)),
                                        properties = PopupProperties(focusable = false)
                                    ) {
                                        filteredCategories.forEach { category ->
                                            DropdownMenuItem(
                                                text = { Text(category, color = Color.White) },
                                                onClick = {
                                                    categoryText = category
                                                    categoryExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(if (editingFlashcard == null) "SAVE" else "UPDATE", fontWeight = FontWeight.Bold)
                                }
                                if (editingFlashcard != null) {
                                    OutlinedButton(
                                        onClick = {
                                            editingFlashcard = null
                                            questionText = ""
                                            answerText = ""
                                            categoryText = "General"
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("CANCEL", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Important Start Button
                item {
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            if (Settings.canDrawOverlays(context)) {
                                context.startService(Intent(context, com.example.flashcardapp.service.FloatingFlashcardService::class.java))
                            } else {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "START FLOATING CARDS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                // Study Groups Section
                item {
                    Text(
                        text = "STUDY GROUPS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        letterSpacing = 1.sp
                    )
                }

                item {
                    val selectedTabIndex = if (selectedCategory == null) 0 else {
                        val index = categories.indexOf(selectedCategory)
                        if (index == -1) 0 else index + 1
                    }

                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        edgePadding = 0.dp,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedCategory == null,
                            onClick = { viewModel.setSelectedCategory(null) },
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("ALL", fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal)
                                    if (selectedCategory == null) {
                                        Badge(
                                            modifier = Modifier.padding(start = 6.dp),
                                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ) { Text(flashcards.size.toString()) }
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
                                        Text(category.uppercase(), fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal)
                                        if (selectedCategory == category) {
                                            Badge(
                                                modifier = Modifier.padding(start = 6.dp),
                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ) { Text(flashcards.size.toString()) }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Flashcard Items
                items(
                    items = flashcards,
                    key = { it.id }
                ) { flashcard ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if ((it == SwipeToDismissBoxValue.EndToStart) || (it == SwipeToDismissBoxValue.StartToEnd)) {
                                viewModel.moveToTrash(flashcard)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                                    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                },
                                label = "Dismiss Color"
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    ) {
                        FlashcardItem(
                            flashcard,
                            onDelete = { viewModel.moveToTrash(flashcard) },
                            onEdit = {
                                editingFlashcard = flashcard
                                questionText = flashcard.question
                                answerText = flashcard.answer
                                categoryText = flashcard.category
                            }
                        )
                    }
                }
                
                // Bottom spacing for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
        
        // Floating Study FAB
        FloatingActionButton(
            onClick = onStartStudy,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary),
            containerColor = Color.Transparent,
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Study", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.5f)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor.copy(alpha = 0.1f),
            contentColor = accentColor
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FlashcardItem(
    flashcard: Flashcard,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = flashcard.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = flashcard.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF09090B), Color(0xFF0F172A))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            if (isFinished) "COMPLETE" else "STUDYING",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onExit) {
                            Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            if (shuffledCards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No flashcards to study!", color = Color.White)
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
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("WELL DONE!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("Reviewed ${shuffledCards.size} cards", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = {
                            currentIndex = 0
                            rotated = false
                            isFinished = false
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(56.dp).width(200.dp)
                    ) {
                        Text("STUDY AGAIN", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onExit) {
                        Text("BACK TO DASHBOARD", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress Bar
                    val progress = (currentIndex.toFloat() / shuffledCards.size.toFloat())
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
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
                                .height(400.dp)
                                .graphicsLayer {
                                    rotationY = rotation
                                    cameraDistance = 12f * density
                                }
                                .shadow(
                                    elevation = 20.dp,
                                    shape = RoundedCornerShape(32.dp),
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                .clickable { rotated = !rotated },
                            contentAlignment = Alignment.Center
                        ) {
                            if (rotation <= 90f) {
                                GlassCard(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = card.question,
                                            style = MaterialTheme.typography.headlineMedium,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(32.dp)
                                        )
                                    }
                                }
                            } else {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { rotationY = 180f }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                        Color.Transparent
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = card.answer,
                                            style = MaterialTheme.typography.headlineMedium,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "TAP TO FLIP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                if (currentIndex > 0) {
                                    currentIndex--
                                    rotated = false
                                }
                            },
                            enabled = currentIndex > 0,
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${currentIndex + 1} / ${shuffledCards.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text("CARDS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        FilledIconButton(
                            onClick = {
                                if (currentIndex < shuffledCards.size - 1) {
                                    currentIndex++
                                    rotated = false
                                } else {
                                    isFinished = true
                                }
                            },
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                if (currentIndex < shuffledCards.size - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Filled.Check, 
                                contentDescription = "Next",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(viewModel: FlashcardViewModel = viewModel(), onBack: () -> Unit) {
    val trashedCards by viewModel.trashedFlashcards.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF09090B), Color(0xFF0F172A))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "RECYCLE BIN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (trashedCards.isNotEmpty()) {
                            IconButton(onClick = { viewModel.emptyTrash() }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Empty Trash", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            if (trashedCards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Your recycle bin is empty", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trashedCards, key = { it.id }) { card ->
                        GlassCard {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(card.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(card.answer, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
                                }
                                IconButton(onClick = { viewModel.restoreFromTrash(card) }) {
                                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deletePermanently(card) }) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete Permanently", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
