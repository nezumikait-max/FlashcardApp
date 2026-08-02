package com.example.flashcardapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcardapp.data.Flashcard
import com.example.flashcardapp.repository.FlashcardRepository
import com.example.flashcardapp.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val repository: FlashcardRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val selectedCategory: StateFlow<String?> = userPreferencesRepository.selectedCategoryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val categories: StateFlow<List<String>> = repository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcards: StateFlow<List<Flashcard>> = repository.getAllFlashcards()
        .combine(selectedCategory) { cards, category ->
            if (category == null) cards else cards.filter { it.category == category }
        }
        .combine(_searchQuery) { cards, query ->
            if (query.isBlank()) cards else {
                cards.filter { it.question.contains(query, ignoreCase = true) || it.answer.contains(query, ignoreCase = true) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    val trashedFlashcards: StateFlow<List<Flashcard>> = repository.getTrashedFlashcards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val autoCloseSeconds: StateFlow<Int> = userPreferencesRepository.autoCloseSecondsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val appearanceIntervalMinutes: StateFlow<Int> = userPreferencesRepository.appearanceIntervalMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val sidebarSide: StateFlow<String> = userPreferencesRepository.sidebarSideFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Right")

    val sidebarHeight: StateFlow<Int> = userPreferencesRepository.sidebarHeightFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    val sidebarVerticalOffset: StateFlow<Int> = userPreferencesRepository.sidebarVerticalOffsetFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setAutoCloseSeconds(seconds: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveAutoCloseSeconds(seconds)
        }
    }

    fun setAppearanceIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveAppearanceIntervalMinutes(minutes)
        }
    }

    fun setSidebarSide(side: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveSidebarSide(side)
        }
    }

    fun setSidebarHeight(height: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveSidebarHeight(height)
        }
    }

    fun setSidebarVerticalOffset(offset: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveSidebarVerticalOffset(offset)
        }
    }

    fun setSelectedCategory(category: String?) {
        viewModelScope.launch {
            userPreferencesRepository.saveSelectedCategory(category)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun insertFlashcard(question: String, answer: String, category: String = "General") {
        viewModelScope.launch {
            repository.insertFlashcard(Flashcard(question = question, answer = answer, category = category))
        }
    }

    fun updateFlashcard(flashcard: Flashcard) {
        viewModelScope.launch {
            repository.insertFlashcard(flashcard)
        }
    }

    fun moveToTrash(flashcard: Flashcard) {
        viewModelScope.launch {
            repository.moveToTrash(flashcard)
        }
    }

    fun restoreFromTrash(flashcard: Flashcard) {
        viewModelScope.launch {
            repository.restoreFromTrash(flashcard)
        }
    }

    fun deletePermanently(flashcard: Flashcard) {
        viewModelScope.launch {
            repository.deleteFlashcard(flashcard)
        }
    }
    
    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }
}
