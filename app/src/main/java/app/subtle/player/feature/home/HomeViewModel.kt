package app.subtle.player.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.subtle.player.domain.repository.PlaybackProgressRepository
import app.subtle.player.domain.repository.StorageRepository
import app.subtle.player.feature.common.BrowserViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            viewMode = savedStateHandle.get<String>(KEY_VIEW_MODE)
                ?.let { runCatching { BrowserViewMode.valueOf(it) }.getOrNull() }
                ?: BrowserViewMode.List,
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            progressRepository.progress.collect { map -> _uiState.update { it.copy(progress = map) } }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val videos = repository.allVideos()
            _uiState.update { it.copy(videos = videos, isLoading = false) }
        }
    }

    fun setViewMode(mode: BrowserViewMode) {
        savedStateHandle[KEY_VIEW_MODE] = mode.name
        _uiState.update { it.copy(viewMode = mode) }
    }

    companion object {
        private const val KEY_VIEW_MODE = "home_view_mode"
    }
}
