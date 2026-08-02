package com.example.flashcardapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE isInTrash = 0")
    fun getAllFlashcards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE isInTrash = 1")
    fun getTrashedFlashcards(): Flow<List<Flashcard>>

    @Query("SELECT DISTINCT category FROM flashcards WHERE isInTrash = 0")
    fun getCategories(): Flow<List<String>>

    @Query("SELECT * FROM flashcards WHERE category = :category AND isInTrash = 0")
    fun getFlashcardsByCategory(category: String): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard)

    @Query("UPDATE flashcards SET isInTrash = 1 WHERE id = :id")
    suspend fun moveToTrash(id: Int)

    @Query("UPDATE flashcards SET isInTrash = 0 WHERE id = :id")
    suspend fun restoreFromTrash(id: Int)

    @Delete
    suspend fun deleteFlashcard(flashcard: Flashcard)

    @Query("DELETE FROM flashcards WHERE isInTrash = 1")
    suspend fun emptyTrash()
}
