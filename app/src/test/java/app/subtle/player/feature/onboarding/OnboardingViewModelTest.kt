package app.subtle.player.feature.onboarding

import app.subtle.player.FakeStorageAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingViewModelTest {

    @Test
    fun forwardsTheRuntimePermissionBelowApi30() {
        val access = FakeStorageAccess(granted = false, runtime = "android.permission.READ_EXTERNAL_STORAGE")

        val viewModel = OnboardingViewModel(access)

        assertNull(viewModel.allFilesIntent())
        assertEquals("android.permission.READ_EXTERNAL_STORAGE", viewModel.requiredRuntimePermission())
    }

    @Test
    fun withoutRuntimePermissionNothingIsRequested() {
        val access = FakeStorageAccess(granted = false)

        val viewModel = OnboardingViewModel(access)

        assertNull(viewModel.allFilesIntent())
        assertNull(viewModel.requiredRuntimePermission())
    }
}
