package app.subtle.player.feature.player

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import app.subtle.player.core.player.MediaSource
import app.subtle.player.core.player.PlayerConfig
import app.subtle.player.core.player.PlayerController
import app.subtle.player.core.player.PlayerControllerFactory
import app.subtle.player.core.player.PlayerError
import app.subtle.player.core.player.PlayerState
import app.subtle.player.core.player.SubtitleSource
import app.subtle.player.core.player.Track
import app.subtle.player.domain.model.AppSettings
import app.subtle.player.domain.model.DirectoryContent
import app.subtle.player.domain.model.PlaybackProgress
import app.subtle.player.domain.model.SubtitleFile
import app.subtle.player.domain.model.Video
import app.subtle.player.domain.model.VideoPreferences
import app.subtle.player.domain.repository.StorageRepository
import app.subtle.player.domain.repository.SubtitleRepository
import app.subtle.player.domain.repository.VideoPreferencesRepository
import app.subtle.player.engine.vlc.buildVlcArgs
import app.subtle.player.feature.common.FakePlaybackProgressRepository
import app.subtle.player.feature.common.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeController: FakePlayerController
    private lateinit var storage: FakeStorageRepository
    private lateinit var progress: FakePlaybackProgressRepository
    private lateinit var subtitles: FakeSubtitleRepository
    private lateinit var preferences: FakeVideoPreferencesRepository
    private lateinit var settings: FakeSettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeController = FakePlayerController()
        storage = FakeStorageRepository()
        progress = FakePlaybackProgressRepository()
        subtitles = FakeSubtitleRepository()
        preferences = FakeVideoPreferencesRepository()
        settings = FakeSettingsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun openLoadsContentSourceFromUri() = runTest {
        val viewModel = viewModel()
        val uri = "content://media/video/1".toUri()

        viewModel.open(uri)
        advanceUntilIdle()

        assertEquals(uri, (fakeController.loadedSource as MediaSource.Content).uri)
        assertEquals(uri, viewModel.uiState.value.videoUri)
    }

    @Test
    fun playPauseAndSeekDelegateToController() = runTest {
        val viewModel = viewModel()
        viewModel.open("content://media/video/1".toUri())
        advanceUntilIdle()

        viewModel.play()
        viewModel.pause()
        viewModel.seekTo(12_000L)
        advanceUntilIdle()

        assertTrue(fakeController.playCalls >= 1)
        assertTrue(fakeController.pauseCalls >= 1)
        assertEquals(12_000L, fakeController.lastSeekMs)
    }

    @Test
    fun playerStateFlowsIntoUiState() = runTest {
        val viewModel = viewModel()
        viewModel.open("content://media/video/1".toUri())
        advanceUntilIdle()

        fakeController.emit(PlayerState(status = PlayerState.Status.Playing, positionMs = 5_000L))
        advanceUntilIdle()

        assertEquals(PlayerState.Status.Playing, viewModel.uiState.value.player.status)
        assertEquals(5_000L, viewModel.uiState.value.player.positionMs)
    }

    @Test
    fun savedUriRestoresAndLoadsOnInit() = runTest {
        val uri = "content://media/video/7".toUri()
        val viewModel = viewModel(SavedStateHandle(mapOf("video_uri" to uri.toString())))
        advanceUntilIdle()

        assertEquals(uri, (fakeController.loadedSource as MediaSource.Content).uri)
        assertEquals(uri, viewModel.uiState.value.videoUri)
    }

    @Test
    fun controllerFactoryFailureSurfacesFatalError() = runTest {
        val failingFactory = PlayerControllerFactory { throw IllegalStateException("no engine") }
        val viewModel = PlayerViewModel(
            failingFactory,
            storage,
            progress,
            subtitles,
            preferences,
            settings,
            SavedStateHandle(),
            testDispatcher,
        )
        viewModel.open("content://media/video/1".toUri())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.fatalError is PlayerError.Engine)
    }

    @Test
    fun skipForwardAndBackwardSeekRelativeToPosition() = runTest {
        val viewModel = viewModel()
        viewModel.open("content://media/video/1".toUri())
        advanceUntilIdle()
        fakeController.emit(PlayerState(status = PlayerState.Status.Playing, positionMs = 30_000L, durationMs = 600_000L))
        advanceUntilIdle()

        viewModel.skipForward()
        viewModel.skipBackward()
        advanceUntilIdle()

        assertEquals(20_000L, fakeController.lastSeekMs)
    }

    @Test
    fun nextOpensFollowingVideoInSameFolder() = runTest {
        val first = "file:///storage/emulated/0/Movies/a.mp4"
        val second = "file:///storage/emulated/0/Movies/b.mp4"
        storage.directoryContent = DirectoryContent(
            displayName = "Movies",
            folders = emptyList(),
            videos = listOf(video(first, "a.mp4"), video(second, "b.mp4")),
        )
        val viewModel = viewModel()
        viewModel.open(first.toUri())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasNext)
        assertTrue(!viewModel.uiState.value.hasPrevious)

        viewModel.next()
        advanceUntilIdle()

        assertEquals(second.toUri(), (fakeController.loadedSource as MediaSource.Content).uri)
    }

    @Test
    fun stopReleasesController() = runTest {
        val viewModel = viewModel()
        viewModel.open("content://media/video/1".toUri())
        advanceUntilIdle()

        viewModel.stop()
        advanceUntilIdle()

        assertTrue(fakeController.released)
    }

    @Test
    fun reOpeningSameVideoAfterStopLoadsAgain() = runTest {
        val uri = "content://media/video/1".toUri()
        val viewModel = viewModel()
        viewModel.openInitial(uri)
        advanceUntilIdle()

        viewModel.stop()
        advanceUntilIdle()
        fakeController.loadedSource = null

        viewModel.openInitial(uri)
        advanceUntilIdle()

        assertEquals(uri, (fakeController.loadedSource as MediaSource.Content).uri)
    }

    @Test
    fun openInitialWhilePlayingSameVideoDoesNotReload() = runTest {
        val uri = "content://media/video/1".toUri()
        val viewModel = viewModel()
        viewModel.openInitial(uri)
        advanceUntilIdle()

        viewModel.openInitial(uri)
        advanceUntilIdle()

        assertEquals(1, fakeController.loadCalls)
    }

    @Test
    fun resumesFromSavedPosition() = runTest {
        val uri = "content://media/video/1".toUri()
        progress.saved[uri.toString()] = PlaybackProgress(50_000L, 600_000L)
        val viewModel = viewModel()

        viewModel.open(uri)
        advanceUntilIdle()

        assertEquals(50_000L, fakeController.lastStartMs)
    }

    @Test
    fun pauseSavesCurrentProgress() = runTest {
        val uri = "content://media/video/1".toUri()
        val viewModel = viewModel()
        viewModel.open(uri)
        advanceUntilIdle()
        fakeController.emit(PlayerState(status = PlayerState.Status.Playing, positionMs = 20_000L, durationMs = 600_000L))
        advanceUntilIdle()

        viewModel.pause()
        advanceUntilIdle()

        assertEquals(PlaybackProgress(20_000L, 600_000L), progress.saved[uri.toString()])
    }

    @Test
    fun endingClearsSavedProgress() = runTest {
        val uri = "content://media/video/1".toUri()
        progress.saved[uri.toString()] = PlaybackProgress(20_000L, 600_000L)
        val viewModel = viewModel()
        viewModel.open(uri)
        advanceUntilIdle()

        fakeController.emit(PlayerState(status = PlayerState.Status.Ended, positionMs = 600_000L, durationMs = 600_000L))
        advanceUntilIdle()

        assertFalse(progress.saved.containsKey(uri.toString()))
    }

    @Test
    fun restartSeeksToStartAndPlays() = runTest {
        val uri = "content://media/video/1".toUri()
        val viewModel = viewModel()
        viewModel.open(uri)
        advanceUntilIdle()

        viewModel.restart()
        advanceUntilIdle()

        assertEquals(0L, fakeController.lastSeekMs)
        assertEquals(1, fakeController.playCalls)
    }

    @Test
    fun selectSubtitleTrackDelegatesToController() = runTest {
        val viewModel = viewModel()
        viewModel.open("content://media/video/1".toUri())
        advanceUntilIdle()

        viewModel.selectSubtitleTrack(3)
        advanceUntilIdle()

        assertEquals(3, fakeController.selectedSubtitleId)
    }

    @Test
    fun adjustSubtitleDelayAppliesAndPersists() = runTest {
        val uri = "content://media/video/1".toUri()
        val viewModel = viewModel()
        viewModel.open(uri)
        advanceUntilIdle()

        viewModel.adjustSubtitleDelay(100L)
        advanceUntilIdle()

        assertEquals(100L, fakeController.lastSubtitleDelayMs)
        assertEquals(100L, preferences.saved[uri.toString()]?.subtitleDelayMs)
    }

    @Test
    fun hardSubsDisablesSubtitlesAndPersists() = runTest {
        val uri = "content://media/video/1".toUri()
        val viewModel = viewModel()
        viewModel.open(uri)
        advanceUntilIdle()

        viewModel.setHardSubs(true)
        advanceUntilIdle()

        assertEquals(Track.DISABLED_ID, fakeController.selectedSubtitleId)
        assertTrue(preferences.saved[uri.toString()]?.hasHardSubs == true)
    }

    @Test
    fun loadSubtitlePreparesAndAddsToController() = runTest {
        subtitles.prepared = "file:///cache/subs/x.srt"
        val viewModel = viewModel()
        viewModel.open("content://media/video/1".toUri())
        advanceUntilIdle()

        viewModel.loadSubtitle(SubtitleFile("file:///x.srt", "x.srt", null))
        advanceUntilIdle()

        assertEquals("file:///cache/subs/x.srt", fakeController.addedSubtitleUri)
    }

    @Test
    fun contentUriHasNoSiblings() = runTest {        val viewModel = viewModel()
        viewModel.open("content://media/video/1".toUri())
        advanceUntilIdle()

        assertTrue(!viewModel.uiState.value.hasNext)
        assertTrue(!viewModel.uiState.value.hasPrevious)
    }

    @Test
    fun softwareDecodingAddsLibVlcFlag() {
        assertTrue(buildVlcArgs(PlayerConfig(forceSoftwareDecoding = true)).contains("--avcodec-hw=none"))
        assertTrue(buildVlcArgs(PlayerConfig()).isEmpty())
    }

    @Test
    fun forceSoftwareDecodingSettingReachesEngine() = runTest {
        settings = FakeSettingsRepository(AppSettings(forceSoftwareDecoding = true))
        var captured: PlayerConfig? = null
        val viewModel = PlayerViewModel(
            PlayerControllerFactory { config -> captured = config; fakeController },
            storage,
            progress,
            subtitles,
            preferences,
            settings,
            SavedStateHandle(),
            testDispatcher,
        )
        viewModel.open("content://media/video/1".toUri())
        advanceUntilIdle()

        assertEquals(true, captured?.forceSoftwareDecoding)
    }

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): PlayerViewModel {
        val factory = PlayerControllerFactory { fakeController }
        return PlayerViewModel(
            factory,
            storage,
            progress,
            subtitles,
            preferences,
            settings,
            savedStateHandle,
            testDispatcher,
        )
    }
}

private fun video(uri: String, name: String) = Video(uri = uri, displayName = name, sizeBytes = 0L, lastModified = 0L)

private class FakeStorageRepository : StorageRepository {
    var directoryContent = DirectoryContent(displayName = "", folders = emptyList(), videos = emptyList())
    var subtitleFiles: List<SubtitleFile> = emptyList()
    override suspend fun allVideos(): List<Video> = directoryContent.videos
    override suspend fun listDirectory(path: String?): DirectoryContent = directoryContent
    override suspend fun subtitlesFor(videoUri: String): List<SubtitleFile> = subtitleFiles
}

private class FakeSubtitleRepository : SubtitleRepository {
    var prepared = "file:///cache/subs/prepared.srt"
    override suspend fun prepareForPlayback(subtitle: SubtitleFile, charsetOverride: String?): String = prepared
}

private class FakeVideoPreferencesRepository : VideoPreferencesRepository {
    val saved = mutableMapOf<String, VideoPreferences>()
    override suspend fun get(videoUri: String): VideoPreferences = saved[videoUri] ?: VideoPreferences()
    override suspend fun save(videoUri: String, preferences: VideoPreferences) {
        saved[videoUri] = preferences
    }
}

private class FakePlayerController : PlayerController {
    private val backing = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = backing.asStateFlow()

    var loadedSource: MediaSource? = null
    var loadCalls = 0
    var lastStartMs = 0L
    var playCalls = 0
    var pauseCalls = 0
    var lastSeekMs: Long? = null
    var selectedSubtitleId: Int? = null
    var selectedAudioId: Int? = null
    var lastSubtitleDelayMs: Long? = null
    var lastAudioDelayMs: Long? = null
    var addedSubtitleUri: String? = null
    var released = false

    fun emit(value: PlayerState) {
        backing.value = value
    }

    override suspend fun load(source: MediaSource, startPositionMs: Long) {
        loadedSource = source
        lastStartMs = startPositionMs
        loadCalls++
        backing.value = PlayerState(status = PlayerState.Status.Opening)
    }

    override fun play() {
        playCalls++
    }

    override fun pause() {
        pauseCalls++
    }

    override fun seekTo(positionMs: Long) {
        lastSeekMs = positionMs
    }

    override fun setSpeed(rate: Float) = Unit
    override fun selectAudioTrack(id: Int) {
        selectedAudioId = id
    }

    override fun selectSubtitleTrack(id: Int) {
        selectedSubtitleId = id
    }

    override suspend fun addExternalSubtitle(source: SubtitleSource, select: Boolean) {
        addedSubtitleUri = (source as SubtitleSource.Content).uri.toString()
    }

    override fun setSubtitleDelay(delayMs: Long) {
        lastSubtitleDelayMs = delayMs
    }

    override fun setAudioDelay(delayMs: Long) {
        lastAudioDelayMs = delayMs
    }

    override fun attachSurface(container: android.view.ViewGroup) = Unit
    override fun detachSurface() = Unit
    override fun release() {
        released = true
    }
}
