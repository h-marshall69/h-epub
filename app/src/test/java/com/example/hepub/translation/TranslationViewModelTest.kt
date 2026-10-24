package com.example.hepub.translation

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationViewModelTest {
    @Before fun setUp() { kotlinx.coroutines.Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { kotlinx.coroutines.Dispatchers.resetMain() }
    @Test fun unavailableModelCanBePreparedAndTextTranslated() = runTest {
        val engine = FakeTranslationEngine()
        val viewModel = TranslationViewModel(engine)
        advanceUntilIdle()
        viewModel.downloadModel()
        advanceUntilIdle()
        assertEquals(TranslationState.Ready, viewModel.state.value)
        viewModel.translate("Hello world")
        advanceUntilIdle()
        assertEquals(TranslationState.Completed("Hello world", "Hola mundo"), viewModel.state.value)
    }
}

private class FakeTranslationEngine : TranslationEngine {
    var available = false
    var closed = false
    override suspend fun isModelAvailable() = available
    override suspend fun prepareModel(requireWifi: Boolean) { available = true }
    override suspend fun translate(text: String) = "Hola mundo"
    override fun close() { closed = true }
}
