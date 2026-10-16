package app.subtle.player.feature.onboarding

import android.content.Intent
import androidx.lifecycle.ViewModel
import app.subtle.player.data.storage.StorageAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val storageAccess: StorageAccess,
) : ViewModel() {

    /** Settings screen for All files access, or null below API 30. */
    fun allFilesIntent(): Intent? = storageAccess.allFilesIntent()

    /** `READ_EXTERNAL_STORAGE` below API 30, or null on API 30+. */
    fun requiredRuntimePermission(): String? = storageAccess.requiredRuntimePermission()
}
