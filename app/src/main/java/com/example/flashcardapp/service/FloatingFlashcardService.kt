package com.example.flashcardapp.service

import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.flow.combine
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
            gravity = Gravity.CENTER
        }

        composeView = ComposeView(this).apply {
            setContent {
                MaterialTheme {
                    val allFlashcards by repository.getAllFlashcards().collectAsState(initial = emptyList())
                    val selectedCategory by userPreferencesRepository.selectedCategoryFlow.collectAsState(initial = null)
                    
                    val filteredFlashcards = remember(allFlashcards, selectedCategory) {
                        if (selectedCategory == null) allFlashcards else allFlashcards.filter { it.category == selectedCategory }
                    }
                    
                    FloatingCard(flashcards = filteredFlashcards, onClose = { stopSelf() })
                }
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

    // Reset index if cards change significantly (e.g. filter change)
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
