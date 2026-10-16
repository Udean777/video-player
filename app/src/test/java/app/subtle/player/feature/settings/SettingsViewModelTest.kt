package app.subtle.player.feature.settings

import app.subtle.player.domain.model.ThemeMode
import app.subtle.player.feature.common.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setThemeUpdatesThemeOnly() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.setTheme(ThemeMode.Dark)
        advanceUntilIdle()

        assertEquals(ThemeMode.Dark, repository.current.theme)
        assertTrue(repository.current.dynamicColor)
    }

    @Test
    fun setForceSoftwareDecodingUpdatesSetting() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.setForceSoftwareDecoding(true)
        advanceUntilIdle()

        assertTrue(repository.current.forceSoftwareDecoding)
    }
}
