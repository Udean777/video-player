package app.subtle.player.feature.home

import androidx.lifecycle.SavedStateHandle
import app.subtle.player.domain.model.DirectoryContent
import app.subtle.player.domain.model.PlaybackProgress
import app.subtle.player.domain.model.ScanProgress
import app.subtle.player.domain.model.SubtitleFile
import app.subtle.player.domain.model.Video
import app.subtle.player.domain.repository.StorageRepository
import app.subtle.player.domain.usecase.VideoSort
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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
    fun loadsEveryVideoOnStart() = runTest {
        val viewModel = HomeViewModel(FakeStorageRepository(), FakePlaybackProgressRepository(), SavedStateHandle(), testDispatcher)

        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.videos.size)
    }

    @Test
    fun refreshReloads() = runTest {
        val repository = FakeStorageRepository()
        val viewModel = HomeViewModel(repository, FakePlaybackProgressRepository(), SavedStateHandle(), testDispatcher)
        advanceUntilIdle()

        repository.videos = listOf(Video("file:///sdcard/c.mp4", "c.mp4", 1L, 0L))
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.videos.size)
    }

    @Test
    fun viewModeSwitches() = runTest {
        val viewModel = HomeViewModel(FakeStorageRepository(), FakePlaybackProgressRepository(), SavedStateHandle(), testDispatcher)
        advanceUntilIdle()

        viewModel.setViewMode(BrowserViewMode.Grid)
        advanceUntilIdle()

        assertEquals(BrowserViewMode.Grid, viewModel.uiState.value.viewMode)
    }

    @Test
    fun queryFiltersByName() = runTest {
        val viewModel = HomeViewModel(FakeStorageRepository(), FakePlaybackProgressRepository(), SavedStateHandle(), testDispatcher)
        advanceUntilIdle()

        viewModel.setQuery("a.")
        advanceUntilIdle()

        assertEquals(listOf("a.mp4"), viewModel.uiState.value.videos.map { it.displayName })
    }

    @Test
    fun scanReportsProgressAndFinishes() = runTest {
        val viewModel = HomeViewModel(FakeStorageRepository(), FakePlaybackProgressRepository(), SavedStateHandle(), testDispatcher)

        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.videosFound)
        assertFalse(viewModel.uiState.value.isScanning)
    }

    @Test
    fun groupSeriesBuildsGroups() = runTest {
        val viewModel = HomeViewModel(FakeStorageRepository(), FakePlaybackProgressRepository(), SavedStateHandle(), testDispatcher)
        advanceUntilIdle()

        viewModel.setGroupSeries(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.groupSeries)
        assertEquals(2, viewModel.uiState.value.groups.size)
    }

    @Test
    fun sortByLastPlayedPutsRecentlyPlayedFirst() = runTest {
        val progress = FakePlaybackProgressRepository()
        val viewModel = HomeViewModel(FakeStorageRepository(), progress, SavedStateHandle(), testDispatcher)
        advanceUntilIdle()

        viewModel.setSort(VideoSort.LastPlayed)
        progress.set("file:///sdcard/b.mkv", PlaybackProgress(1_000L, 2_000L, lastPlayedMs = 500L))
        advanceUntilIdle()

        assertEquals(listOf("b.mkv", "a.mp4"), viewModel.uiState.value.videos.map { it.displayName })
    }
}

private class FakeStorageRepository : StorageRepository {
    var videos: List<Video> = listOf(
        Video("file:///sdcard/a.mp4", "a.mp4", 100L, 0L),
        Video("file:///sdcard/b.mkv", "b.mkv", 200L, 0L),
    )

    override suspend fun allVideos(onProgress: (ScanProgress) -> Unit): List<Video> {
        onProgress(ScanProgress(foldersScanned = 1, videosFound = videos.size))
        return videos
    }

    override suspend fun subtitlesFor(videoUri: String): List<SubtitleFile> = emptyList()

    override suspend fun listDirectory(path: String?): DirectoryContent =
        DirectoryContent(displayName = "", folders = emptyList(), videos = emptyList())
}
