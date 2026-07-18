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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleService
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
    private lateinit var composeView: ComposeView
    private var tts: TextToSpeech? = null

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    
    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

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
                setSpeechRate(1.1f)
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

        val params = WindowManager.LayoutParams(
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

        composeView = ComposeView(this).apply {
            setContent {
                val darkTheme = isSystemInDarkTheme()
                val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
                
                MaterialTheme(colorScheme = colorScheme) {
                    val allFlashcards by repository.getAllFlashcards().collectAsState(initial = emptyList())
                    val selectedCategory by userPreferencesRepository.selectedCategoryFlow.collectAsState(initial = null)
                    
                    val filteredFlashcards = remember(allFlashcards, selectedCategory) {
                        if (selectedCategory == null) allFlashcards else allFlashcards.filter { it.category == selectedCategory }
                    }
                    
                    var isMinimized by remember { mutableStateOf(false) }

                    FloatingCard(
                        flashcards = filteredFlashcards,
                        currentGroupName = selectedCategory ?: "All Groups",
                        isMinimized = isMinimized,
                        onMinimizeToggle = { isMinimized = !isMinimized },
                        onSpeak = { speak(it, selectedCategory ?: "General") },
                        onClose = { stopSelf() }
                    )
                }
            }
        }

        // Draggable Logic
        var initialX = 100
        var initialY = 100
        var initialTouchX = 0f
        var initialTouchY = 0f

        composeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(composeView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    composeView.performClick()
                    true
                }
                else -> false
            }
        }

        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)

        windowManager.addView(composeView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        if (::composeView.isInitialized) {
            windowManager.removeView(composeView)
            composeView.disposeComposition()
        }
        store.clear()
    }
}

@Composable
fun FloatingCard(
    flashcards: List<Flashcard>, 
    currentGroupName: String, 
    isMinimized: Boolean,
    onMinimizeToggle: () -> Unit,
    onSpeak: (String) -> Unit,
    onClose: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    LaunchedEffect(flashcards.size) {
        currentIndex = 0
        showAnswer = false
    }

    if (isMinimized) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), CircleShape)
                .clickable { onMinimizeToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = "Show Flashcard",
                tint = Color.White
            )
        }
    } else {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .width(250.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentGroupName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onMinimizeToggle, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Minimize", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable {
                            if (flashcards.isNotEmpty()) {
                                showAnswer = !showAnswer
                            }
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (flashcards.isEmpty()) {
                        Text(
                            "No cards available.", 
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val currentCard = flashcards[currentIndex % flashcards.size]
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (showAnswer) "ANSWER" else "QUESTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = if (showAnswer) currentCard.answer else currentCard.question,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        IconButton(
                            onClick = { onSpeak(if (showAnswer) currentCard.answer else currentCard.question) },
                            modifier = Modifier.align(Alignment.BottomEnd).size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak", modifier = Modifier.size(18.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (flashcards.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (currentIndex > 0) currentIndex--
                                showAnswer = false
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Prev", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                currentIndex++
                                showAnswer = false
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Next", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
