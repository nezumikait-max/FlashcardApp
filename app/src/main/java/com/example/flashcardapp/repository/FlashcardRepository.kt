package com.example.flashcardapp.repository

import com.example.flashcardapp.data.Flashcard
import com.example.flashcardapp.data.FlashcardDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashcardRepository @Inject constructor(
    private val flashcardDao: FlashcardDao,
) {
    fun getAllFlashcards(): Flow<List<Flashcard>> = flashcardDao.getAllFlashcards()
    
    fun getTrashedFlashcards(): Flow<List<Flashcard>> = flashcardDao.getTrashedFlashcards()

    fun getCategories(): Flow<List<String>> = flashcardDao.getCategories()

    suspend fun insertFlashcard(flashcard: Flashcard) {
        flashcardDao.insertFlashcard(flashcard)
    }

    suspend fun moveToTrash(flashcard: Flashcard) {
        flashcardDao.moveToTrash(flashcard.id)
    }

    suspend fun restoreFromTrash(flashcard: Flashcard) {
        flashcardDao.restoreFromTrash(flashcard.id)
    }

    suspend fun deleteFlashcard(flashcard: Flashcard) {
        flashcardDao.deleteFlashcard(flashcard)
    }
    
    suspend fun emptyTrash() {
        flashcardDao.emptyTrash()
    }
}
