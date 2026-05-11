package com.example.flashcardapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcardapp.data.Flashcard
import com.example.flashcardapp.repository.FlashcardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val repository: FlashcardRepository
) : ViewModel() {

    val flashcards: StateFlow<List<Flashcard>> = repository.getAllFlashcards()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addDummyFlashcard() {
        viewModelScope.launch {
            repository.insertFlashcard(
                Flashcard(
                    question = "Dummy Question ${System.currentTimeMillis().toString().takeLast(4)}",
                    answer = "Dummy Answer"
                )
            )
        }
    }

    fun insertFlashcard(question: String, answer: String) {
        viewModelScope.launch {
            repository.insertFlashcard(Flashcard(question = question, answer = answer))
        }
    }

    fun deleteFlashcard(flashcard: Flashcard) {
        viewModelScope.launch {
            repository.deleteFlashcard(flashcard)
        }
    }
}
