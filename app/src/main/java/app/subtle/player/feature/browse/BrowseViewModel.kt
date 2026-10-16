package app.subtle.player.feature.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.subtle.player.domain.model.Breadcrumb
import app.subtle.player.domain.model.FolderEntry
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
class BrowseViewModel @Inject constructor(
    private val repository: StorageRepository,
    private val progressRepository: PlaybackProgressRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BrowseUiState(
            viewMode = savedStateHandle.get<String>(KEY_VIEW_MODE)
                ?.let { runCatching { BrowserViewMode.valueOf(it) }.getOrNull() }
                ?: BrowserViewMode.List,
        ),
    )
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    private val crumbs = mutableListOf<Breadcrumb>()

    init {
        viewModelScope.launch {
            progressRepository.progress.collect { map -> _uiState.update { it.copy(progress = map) } }
        }
        load(null)
    }

    fun openFolder(entry: FolderEntry) {
        crumbs.add(Breadcrumb(entry.name, entry.path))
        load(entry.path)
    }

    fun openPath(index: Int) {
        if (index < 0 || index >= crumbs.size) return
        while (crumbs.size > index + 1) crumbs.removeAt(crumbs.lastIndex)
        load(crumbs.lastOrNull()?.path)
    }

    fun goUp() {
        if (crumbs.size > 1) {
            crumbs.removeAt(crumbs.lastIndex)
            load(crumbs.last().path)
        }
    }

    fun refresh() {
        load(crumbs.lastOrNull()?.path)
    }

    fun setViewMode(mode: BrowserViewMode) {
        savedStateHandle[KEY_VIEW_MODE] = mode.name
        _uiState.update { it.copy(viewMode = mode) }
    }

    private fun load(dirPath: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val content = repository.listDirectory(dirPath)
            if (crumbs.isEmpty()) {
                crumbs.add(Breadcrumb(content.displayName, null))
            } else if (dirPath == null) {
                crumbs[0] = Breadcrumb(content.displayName, null)
            }
            _uiState.update {
                it.copy(
                    path = crumbs.toList(),
                    folders = content.folders,
                    videos = content.videos,
                    isLoading = false,
                )
            }
        }
    }

    companion object {
        private const val KEY_VIEW_MODE = "browse_view_mode"
    }
}
