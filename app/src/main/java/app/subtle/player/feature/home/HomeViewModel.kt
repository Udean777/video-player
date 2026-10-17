package app.subtle.player.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.subtle.player.core.common.DefaultDispatcher
import app.subtle.player.domain.model.Video
import app.subtle.player.domain.repository.PlaybackProgressRepository
import app.subtle.player.domain.repository.StorageRepository
import app.subtle.player.domain.usecase.SeriesGrouping
import app.subtle.player.domain.usecase.VideoSort
import app.subtle.player.domain.usecase.filterAndSortVideos
import app.subtle.player.feature.common.BrowserViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: StorageRepository,
    private val progressRepository: PlaybackProgressRepository,
    private val savedStateHandle: SavedStateHandle,
    @DefaultDispatcher private val background: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            viewMode = savedStateHandle.get<String>(KEY_VIEW_MODE)
                ?.let { runCatching { BrowserViewMode.valueOf(it) }.getOrNull() }
                ?: BrowserViewMode.List,
            sort = savedStateHandle.get<String>(KEY_SORT)
                ?.let { runCatching { VideoSort.valueOf(it) }.getOrNull() }
                ?: VideoSort.Name,
            groupSeries = savedStateHandle.get<Boolean>(KEY_GROUP_SERIES) ?: false,
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var allVideos: List<Video> = emptyList()
    private var scanJob: Job? = null
    private var filterJob: Job? = null
    private var lastEmittedFolders = 0

    init {
        viewModelScope.launch {
            progressRepository.progress.collect { map ->
                _uiState.update { it.copy(progress = map) }
                applyFilterAndSort()
            }
        }
        refresh()
    }

    fun refresh() {
        scanJob?.cancel()
        lastEmittedFolders = -PROGRESS_EVERY_FOLDERS
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scannedFolders = 0, videosFound = 0) }
            allVideos = repository.allVideos { progress ->
                // Writing on every folder recomposes the whole screen; a few updates a second is plenty.
                if (progress.foldersScanned - lastEmittedFolders >= PROGRESS_EVERY_FOLDERS) {
                    lastEmittedFolders = progress.foldersScanned
                    _uiState.update {
                        it.copy(scannedFolders = progress.foldersScanned, videosFound = progress.videosFound)
                    }
                }
            }
            _uiState.update { it.copy(isScanning = false) }
            applyFilterAndSort()
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        _uiState.update { it.copy(isScanning = false) }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        applyFilterAndSort()
    }

    fun setSort(sort: VideoSort) {
        savedStateHandle[KEY_SORT] = sort.name
        _uiState.update { it.copy(sort = sort) }
        applyFilterAndSort()
    }

    fun setViewMode(mode: BrowserViewMode) {
        savedStateHandle[KEY_VIEW_MODE] = mode.name
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setGroupSeries(enabled: Boolean) {
        savedStateHandle[KEY_GROUP_SERIES] = enabled
        _uiState.update { it.copy(groupSeries = enabled) }
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        val state = _uiState.value
        // Filtering and sorting a large library is O(n log n); keep it off the main thread so the
        // search field stays responsive while typing.
        filterJob?.cancel()
        filterJob = viewModelScope.launch(background) {
            val filtered = filterAndSortVideos(allVideos, state.query, state.sort, state.progress)
            val groups = if (state.groupSeries) SeriesGrouping.group(filtered) else emptyList()
            ensureActive()
            _uiState.update { it.copy(videos = filtered, groups = groups) }
        }
    }

    companion object {
        private const val KEY_VIEW_MODE = "home_view_mode"
        private const val KEY_SORT = "home_sort"
        private const val KEY_GROUP_SERIES = "home_group_series"
        private const val PROGRESS_EVERY_FOLDERS = 25
    }
}
