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
    private val userPreferencesRepository: UserPreferencesRepository
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

    fun deleteFlashcard(flashcard: Flashcard) {
        viewModelScope.launch {
            repository.deleteFlashcard(flashcard)
        }
    }
}
