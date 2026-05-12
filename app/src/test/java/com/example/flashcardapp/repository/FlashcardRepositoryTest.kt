package com.example.flashcardapp.repository

import com.example.flashcardapp.data.Flashcard
import com.example.flashcardapp.data.FlashcardDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class FlashcardRepositoryTest {

    private lateinit var repository: FlashcardRepository
    private val dao: FlashcardDao = mock()

    @Before
    fun setup() {
        repository = FlashcardRepository(dao)
    }

    @Test
    fun `getAllFlashcards calls dao`() = runTest {
        val testCards = listOf(Flashcard(1, "Q", "A", "Cat"))
        whenever(dao.getAllFlashcards()).thenReturn(flowOf(testCards))

        val result = repository.getAllFlashcards().first()
        
        assertEquals(testCards, result)
        verify(dao).getAllFlashcards()
    }

    @Test
    fun `insertFlashcard calls dao`() = runTest {
        val card = Flashcard(question = "Q", answer = "A")
        repository.insertFlashcard(card)
        verify(dao).insertFlashcard(card)
    }

    @Test
    fun `deleteFlashcard calls dao`() = runTest {
        val card = Flashcard(1, "Q", "A")
        repository.deleteFlashcard(card)
        verify(dao).deleteFlashcard(card)
    }
}
