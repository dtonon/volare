package com.dluvian.voyage.ui.views.nonMain.profile

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dluvian.voyage.core.ComposableContent
import com.dluvian.voyage.core.OnUpdate
import com.dluvian.voyage.core.viewModel.ProfileViewModel
import com.dluvian.voyage.ui.components.scaffold.VoyageScaffold

@Composable
fun ProfileScaffold(
    vm: ProfileViewModel,
    snackbar: SnackbarHostState,
    onUpdate: OnUpdate,
    content: ComposableContent
) {
    val outerProfile by vm.profile
    val profile by outerProfile.collectAsState()

    VoyageScaffold(
        snackbar = snackbar,
        topBar = {
            ProfileTopAppBar(
                profile = profile,
                onUpdate = onUpdate
            )
        }
    ) {
        content()
    }
}
