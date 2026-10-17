package app.subtle.player.feature.browse

import androidx.lifecycle.SavedStateHandle
import app.subtle.player.domain.model.DirectoryContent
import app.subtle.player.domain.model.FolderEntry
import app.subtle.player.domain.model.ScanProgress
import app.subtle.player.domain.model.SubtitleFile
import app.subtle.player.domain.model.Video
import app.subtle.player.domain.repository.StorageRepository
import app.subtle.player.feature.common.BrowserViewMode
import app.subtle.player.feature.common.FakePlaybackProgressRepository
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
class BrowseViewModelTest {

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
    fun rootIsLoadedOnStart() = runTest {
        val viewModel = BrowseViewModel(FakeStorageRepository(), FakePlaybackProgressRepository(), SavedStateHandle())

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Internal storage"), state.path.map { it.name })
        assertEquals(1, state.folders.size)
        assertEquals(1, state.videos.size)
    }

    @Test
    fun openingFolderPushesBreadcrumb() = runTest {
        val viewModel = BrowseViewModel(FakeStorageRepository(), FakePlaybackProgressRepository(), SavedStateHandle())
        advanceUntilIdle()

        viewModel.openFolder(FolderEntry("Movies", "/sdcard/Movies", 2))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Internal storage", "Movies"), state.path.map { it.name })
        assertEquals(2, state.videos.size)
        assertEquals(true, state.canGoUp)
    }

    @Test
    fun goUpReturnsToRoot() = runTest {
        val viewModel = BrowseViewModel(FakeStorageRepository(), FakePlaybackProgressRepository(), SavedStateHandle())
        advanceUntilIdle()
        viewModel.openFolder(FolderEntry("Movies", "/sdcard/Movies", 2))
        advanceUntilIdle()

        viewModel.goUp()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.path.size)
    }

    @Test
    fun viewModeSwitches() = runTest {
        val viewModel = BrowseViewModel(FakeStorageRepository(), FakePlaybackProgressRepository(), SavedStateHandle())
        advanceUntilIdle()

        viewModel.setViewMode(BrowserViewMode.Grid)
        advanceUntilIdle()

        assertEquals(BrowserViewMode.Grid, viewModel.uiState.value.viewMode)
    }
}

private class FakeStorageRepository : StorageRepository {
    override suspend fun allVideos(onProgress: (ScanProgress) -> Unit): List<Video> = emptyList()

    override suspend fun subtitlesFor(videoUri: String): List<SubtitleFile> = emptyList()

    override suspend fun listDirectory(path: String?): DirectoryContent =
        if (path == null) {
            DirectoryContent(
                displayName = "Internal storage",
                folders = listOf(FolderEntry("Movies", "/sdcard/Movies", 2)),
                videos = listOf(Video("file:///sdcard/top.mp4", "top.mp4", 100L, 0L)),
            )
        } else {
            DirectoryContent(
                displayName = "Movies",
                folders = emptyList(),
                videos = listOf(
                    Video("file:///sdcard/Movies/a.mkv", "a.mkv", 200L, 0L),
                    Video("file:///sdcard/Movies/b.mp4", "b.mp4", 300L, 0L),
                ),
            )
        }
}
