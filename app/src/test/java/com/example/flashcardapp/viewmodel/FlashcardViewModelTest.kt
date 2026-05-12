package com.example.flashcardapp.viewmodel

import app.cash.turbine.test
import com.example.flashcardapp.data.Flashcard
import com.example.flashcardapp.repository.FlashcardRepository
import com.example.flashcardapp.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@ExperimentalCoroutinesApi
class FlashcardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: FlashcardViewModel
    private val repository: FlashcardRepository = mock()
    private val userPreferencesRepository: UserPreferencesRepository = mock()

    private val testCards = listOf(
        Flashcard(1, "Q1", "A1", "General"),
        Flashcard(2, "Q2", "A2", "Math"),
        Flashcard(3, "Q3", "A3", "General")
    )

    @Before
    fun setup() {
        whenever(repository.getAllFlashcards()).thenReturn(flowOf(testCards))
        whenever(repository.getCategories()).thenReturn(flowOf(listOf("General", "Math")))
        whenever(userPreferencesRepository.selectedCategoryFlow).thenReturn(flowOf(null))
        viewModel = FlashcardViewModel(repository, userPreferencesRepository)
    }

    @Test
    fun `flashcards flow emits all cards initially`() = runTest {
        viewModel.flashcards.test {
            val emission = awaitItem()
            assertEquals(3, emission.size)
            assertEquals(testCards, emission)
        }
    }

    @Test
    fun `setSelectedCategory calls userPreferencesRepository`() = runTest {
        viewModel.setSelectedCategory("Math")
        verify(userPreferencesRepository).saveSelectedCategory("Math")
    }

    @Test
    fun `insertFlashcard calls repository`() = runTest {
        viewModel.insertFlashcard("New Q", "New A", "Science")
        verify(repository).insertFlashcard(Flashcard(question = "New Q", answer = "New A", category = "Science"))
    }

    @Test
    fun `deleteFlashcard calls repository`() = runTest {
        val card = testCards[0]
        viewModel.deleteFlashcard(card)
        verify(repository).deleteFlashcard(card)
    }
}
