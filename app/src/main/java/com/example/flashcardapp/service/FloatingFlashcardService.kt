package com.example.flashcardapp.service

import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.widget.Toast
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.flashcardapp.data.Flashcard
import com.example.flashcardapp.repository.FlashcardRepository
import com.example.flashcardapp.repository.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class FloatingFlashcardService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner, TextToSpeech.OnInitListener {

    @Inject
    lateinit var repository: FlashcardRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var sidebarView: ComposeView? = null
    private var quickCreateView: ComposeView? = null
    private var tts: TextToSpeech? = null

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    
    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private var appearanceJob: Job? = null
    private var isViewAdded = false

    private val flashcardParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 100
        y = 100
    }

    private val quickCreateParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.CENTER
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    private fun speak(text: String, category: String) {
        tts?.apply {
            if (category.equals("French", ignoreCase = true)) {
                language = Locale.FRENCH
                setPitch(1.0f)
                setSpeechRate(1.0f)
            } else {
                language = Locale.US
                setPitch(1.2f)
            }
            speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        savedStateRegistryController.performRestore(null)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        setupSidebarTrigger()
    }

    private fun setupSidebarTrigger() {
        lifecycleScope.launch {
            combine(
                userPreferencesRepository.sidebarSideFlow,
                userPreferencesRepository.sidebarHeightFlow,
                userPreferencesRepository.sidebarVerticalOffsetFlow,
                userPreferencesRepository.sidebarEnabledFlow
            ) { side: String, height: Int, offset: Int, enabled: Boolean ->
                DataBundle(side, height, offset, enabled)
            }.collectLatest { data ->
                if (data.enabled) {
                    updateSidebarPosition(data.side, data.height, data.offset)
                } else {
                    removeSidebar()
                }
            }
        }
    }

    private data class DataBundle(val side: String, val height: Int, val offset: Int, val enabled: Boolean)

    private fun removeSidebar() {
        sidebarView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        sidebarView = null
    }

    private fun updateSidebarPosition(side: String, height: Int, offset: Int) {
        sidebarView?.let { 
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        
        val sidebarParams = WindowManager.LayoutParams(
            30.toPx(), // Width of the trigger button
            height.toPx(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (if (side == "Left") Gravity.START else Gravity.END) or Gravity.CENTER_VERTICAL
            y = offset.toPx()
        }

        sidebarView = ComposeView(this).apply {
            setContent {
                val darkTheme = isSystemInDarkTheme()
                val color = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                        .clip(
                            if (side == "Left") RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            else RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        )
                        .background(
                            Brush.horizontalGradient(
                                colors = if (side == "Left") listOf(color, color.copy(alpha = 0.2f))
                                         else listOf(color.copy(alpha = 0.2f), color)
                            )
                        )
                        .clickable { showQuickCreateDialog() }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                val threshold = 10f
                                if (side == "Left" && dragAmount > threshold) {
                                    showQuickCreateDialog()
                                } else if (side == "Right" && dragAmount < -threshold) {
                                    showQuickCreateDialog()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (side == "Left") Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        sidebarView?.setViewTreeLifecycleOwner(this)
        sidebarView?.setViewTreeSavedStateRegistryOwner(this)
        sidebarView?.setViewTreeViewModelStoreOwner(this)
        
        try {
            windowManager.addView(sidebarView, sidebarParams)
        } catch (e: Exception) {}
    }

    private fun showQuickCreateDialog() {
        if (quickCreateView != null) return

        quickCreateView = ComposeView(this).apply {
            setContent {
                val darkTheme = isSystemInDarkTheme()
                val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
                
                MaterialTheme(colorScheme = colorScheme) {
                    val categories by repository.getCategories().collectAsState(initial = emptyList())
                    
                    QuickCreateCard(
                        categories = categories,
                        onSave = { q, a, cat ->
                            lifecycleScope.launch {
                                repository.insertFlashcard(Flashcard(question = q, answer = a, category = cat))
                            }
                            hideQuickCreateDialog()
                        },
                        onClose = { hideQuickCreateDialog() }
                    )
                }
            }
        }
        
        quickCreateView?.setViewTreeLifecycleOwner(this)
        quickCreateView?.setViewTreeSavedStateRegistryOwner(this)
        quickCreateView?.setViewTreeViewModelStoreOwner(this)
        windowManager.addView(quickCreateView, quickCreateParams)
    }

    private fun hideQuickCreateDialog() {
        quickCreateView?.let {
            windowManager.removeView(it)
            it.disposeComposition()
        }
        quickCreateView = null
    }

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        startAppearanceTimer()
        return result
    }

    private fun startAppearanceTimer() {
        appearanceJob?.cancel()
        appearanceJob = lifecycleScope.launch {
            val intervalMinutes = userPreferencesRepository.appearanceIntervalMinutesFlow.first()
            Toast.makeText(this@FloatingFlashcardService, "Floating cards will appear every $intervalMinutes min(s)", Toast.LENGTH_SHORT).show()
            
            while (isActive) {
                showFloatingCard()
                while (isViewAdded) {
                    delay(1000)
                }
                delay(intervalMinutes * 60 * 1000L)
            }
        }
    }

    private fun showFloatingCard() {
        if (isViewAdded) return

        composeView = ComposeView(this).apply {
            setContent {
                val darkTheme = isSystemInDarkTheme()
                val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
                
                MaterialTheme(colorScheme = colorScheme) {
                    val allFlashcards by repository.getAllFlashcards().collectAsState(initial = emptyList())
                    val selectedCategory by userPreferencesRepository.selectedCategoryFlow.collectAsState(initial = null)
                    val autoCloseSeconds by userPreferencesRepository.autoCloseSecondsFlow.collectAsState(initial = 30)
                    
                    val filteredFlashcards = remember(allFlashcards, selectedCategory) {
                        if (selectedCategory == null) allFlashcards else allFlashcards.filter { it.category == selectedCategory }
                    }
                    
                    var isMinimized by remember { mutableStateOf(false) }

                    FloatingCard(
                        flashcards = filteredFlashcards,
                        currentGroupName = selectedCategory ?: "All Groups",
                        isMinimized = isMinimized,
                        autoCloseSeconds = autoCloseSeconds,
                        onMinimizeToggle = { isMinimized = !isMinimized },
                        onClose = { hideFloatingCard() },
                        onMove = { dx, dy ->
                            flashcardParams.x += dx.toInt()
                            flashcardParams.y += dy.toInt()
                            windowManager.updateViewLayout(this@apply, flashcardParams)
                        }
                    )
                }
            }
        }

        composeView?.setViewTreeLifecycleOwner(this)
        composeView?.setViewTreeSavedStateRegistryOwner(this)
        composeView?.setViewTreeViewModelStoreOwner(this)

        windowManager.addView(composeView, flashcardParams)
        isViewAdded = true
    }

    private fun hideFloatingCard() {
        if (!isViewAdded) return
        
        composeView?.let {
            windowManager.removeView(it)
            it.disposeComposition()
        }
        composeView = null
        isViewAdded = false
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        hideFloatingCard()
        hideQuickCreateDialog()
        sidebarView?.let { windowManager.removeView(it) }
        appearanceJob?.cancel()
        store.clear()
    }
}

@Composable
fun QuickCreateCard(
    categories: List<String>,
    onSave: (String, String, String) -> Unit,
    onClose: () -> Unit
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("General") }
    var categoryExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(280.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "QUICK CREATE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                placeholder = { Text("Question") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                placeholder = { Text("Answer") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it; categoryExpanded = true },
                    placeholder = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                
                val filtered = categories.filter { it.contains(categoryText, true) }
                if (categoryExpanded && filtered.isNotEmpty()) {
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.width(200.dp)
                    ) {
                        filtered.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { categoryText = cat; categoryExpanded = false }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = { if (question.isNotBlank() && answer.isNotBlank()) onSave(question, answer, categoryText) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("SAVE CARD", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FloatingCard(
    flashcards: List<Flashcard>, 
    currentGroupName: String, 
    isMinimized: Boolean,
    autoCloseSeconds: Int,
    onMinimizeToggle: () -> Unit,
    onClose: () -> Unit,
    onMove: (Float, Float) -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }
    
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    var cardWidth by remember { mutableStateOf(200.dp) }
    var cardHeight by remember { mutableStateOf(140.dp) }
    
    val density = LocalDensity.current

    LaunchedEffect(flashcards.size) {
        currentIndex = 0
        showAnswer = false
    }
    
    // Auto-close logic
    LaunchedEffect(lastInteractionTime, autoCloseSeconds, isMinimized) {
        if (autoCloseSeconds > 0 && !isMinimized) {
            delay(autoCloseSeconds * 1000L)
            onClose()
        }
    }

    if (isMinimized) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { 
                        lastInteractionTime = System.currentTimeMillis()
                        onMinimizeToggle() 
                    })
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        lastInteractionTime = System.currentTimeMillis()
                        onMove(dragAmount.x, dragAmount.y)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = "Show Flashcard",
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    } else {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .size(cardWidth, cardHeight),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Header / Drag Area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    lastInteractionTime = System.currentTimeMillis()
                                    onMove(dragAmount.x, dragAmount.y)
                                }
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentGroupName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Close, 
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Card Content
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .pointerInput(flashcards.size) {
                                detectTapGestures(
                                    onTap = { 
                                        lastInteractionTime = System.currentTimeMillis()
                                        if (flashcards.isNotEmpty()) showAnswer = !showAnswer 
                                    },
                                    onDoubleTap = { 
                                        lastInteractionTime = System.currentTimeMillis()
                                        onMinimizeToggle() 
                                    }
                                )
                            }
                            .pointerInput(flashcards.size) {
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        lastInteractionTime = System.currentTimeMillis()
                                        if (totalDrag > 50) {
                                            if (currentIndex > 0) currentIndex--
                                            showAnswer = false
                                        } else if (totalDrag < -50) {
                                            currentIndex++
                                            showAnswer = false
                                        }
                                        totalDrag = 0f
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        totalDrag += dragAmount
                                    }
                                )
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (flashcards.isEmpty()) {
                            Text(
                                "No cards.", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            val currentCard = flashcards[currentIndex % flashcards.size]
                            Text(
                                text = if (showAnswer) currentCard.answer else currentCard.question,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                // Resize Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                lastInteractionTime = System.currentTimeMillis()
                                val deltaW = with(density) { dragAmount.x.toDp() }
                                val deltaH = with(density) { dragAmount.y.toDp() }
                                cardWidth = (cardWidth + deltaW).coerceIn(150.dp, 400.dp)
                                cardHeight = (cardHeight + deltaH).coerceIn(100.dp, 400.dp)
                            }
                        },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Resize",
                        modifier = Modifier.size(12.dp).graphicsLayer(rotationZ = 90f),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
