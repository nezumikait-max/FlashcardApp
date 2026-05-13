package com.example.flashcardapp.service

import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
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
import javax.inject.Inject

@AndroidEntryPoint
class FloatingFlashcardService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    @Inject
    lateinit var repository: FlashcardRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    
    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
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
                    
                    FloatingCard(flashcards = filteredFlashcards, onClose = { stopSelf() })
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

        ViewTreeLifecycleOwner.set(composeView, this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        ViewTreeViewModelStoreOwner.set(composeView, this)

        windowManager.addView(composeView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::composeView.isInitialized) {
            windowManager.removeView(composeView)
            composeView.disposeComposition()
        }
        store.clear()
    }
}

@Composable
fun FloatingCard(flashcards: List<Flashcard>, onClose: () -> Unit) {
    var currentIndex by remember { mutableStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    LaunchedEffect(flashcards.size) {
        currentIndex = 0
        showAnswer = false
    }

    Card(
        modifier = Modifier
            .padding(16.dp)
            .width(250.dp)
            .clickable {
                if (flashcards.isNotEmpty()) {
                    showAnswer = !showAnswer
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (flashcards.isEmpty()) {
                Text("No cards available in this category.", style = MaterialTheme.typography.bodyMedium)
            } else {
                val currentCard = flashcards[currentIndex % flashcards.size]
                if (showAnswer) {
                    Text("A: ${currentCard.answer}", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("Q: ${currentCard.question}", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onClose) {
                    Text("Close")
                }
                if (flashcards.isNotEmpty()) {
                    Button(onClick = {
                        currentIndex++
                        showAnswer = false
                    }) {
                        Text("Next")
                    }
                }
            }
        }
    }
}
