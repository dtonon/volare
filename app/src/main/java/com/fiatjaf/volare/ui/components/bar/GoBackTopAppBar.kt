package com.fiatjaf.volare.ui.components.bar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.fiatjaf.volare.core.ComposableContent
import com.fiatjaf.volare.core.ComposableRowContent
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.ui.components.button.GoBackIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoBackTopAppBar(
    title: ComposableContent = {},
    actions: ComposableRowContent = {},
    onUpdate: OnUpdate
) {
    TopAppBar(
        title = title,
        actions = actions,
        navigationIcon = {
            GoBackIconButton(onUpdate = onUpdate)
        },
    )
}
