package app.subtle.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.subtle.player.data.storage.StorageAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class AppStart { Onboarding, Main }

/** Onboarding until storage access is granted, then the main tabs. */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val storageAccess: StorageAccess,
) : ViewModel() {

    private val _granted = MutableStateFlow(storageAccess.isGranted())

    val start: StateFlow<AppStart> = _granted
        .map { if (it) AppStart.Main else AppStart.Onboarding }
        .stateIn(viewModelScope, SharingStarted.Eagerly, if (_granted.value) AppStart.Main else AppStart.Onboarding)

    /** Re-reads the permission; call when the app returns to the foreground. */
    fun refresh() {
        _granted.value = storageAccess.isGranted()
    }
}
