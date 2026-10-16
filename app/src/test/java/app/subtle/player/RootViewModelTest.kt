package app.subtle.player

import android.content.Intent
import app.subtle.player.data.storage.StorageAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RootViewModelTest {

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
    fun notGrantedStartsOnboarding() = runTest {
        val viewModel = RootViewModel(FakeStorageAccess(granted = false))

        advanceUntilIdle()

        assertEquals(AppStart.Onboarding, viewModel.start.value)
    }

    @Test
    fun grantedStartsOnMain() = runTest {
        val viewModel = RootViewModel(FakeStorageAccess(granted = true))

        advanceUntilIdle()

        assertEquals(AppStart.Main, viewModel.start.value)
    }

    @Test
    fun refreshAfterGrantMovesToMain() = runTest {
        val access = FakeStorageAccess(granted = false)
        val viewModel = RootViewModel(access)
        advanceUntilIdle()
        assertEquals(AppStart.Onboarding, viewModel.start.value)

        access.granted = true
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(AppStart.Main, viewModel.start.value)
    }
}

class FakeStorageAccess(
    var granted: Boolean,
    private val runtime: String? = null,
) : StorageAccess {
    override fun isGranted(): Boolean = granted
    override fun allFilesIntent(): Intent? = null
    override fun requiredRuntimePermission(): String? = runtime
}
