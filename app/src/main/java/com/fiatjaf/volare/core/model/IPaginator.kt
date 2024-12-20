package com.fiatjaf.volare.core.model

import androidx.compose.runtime.State
import com.fiatjaf.volare.ui.components.row.mainEvent.MainEventCtx
import kotlinx.coroutines.flow.StateFlow

interface IPaginator {
    val isInitialized: State<Boolean>
    val isRefreshing: State<Boolean>
    val isAppending: State<Boolean>
    val hasMoreRecentItems: State<Boolean>
    val hasPage: State<StateFlow<Boolean>>
    val pageTimestamps: State<List<Long>>
    val filteredPage: State<StateFlow<List<MainEventCtx>>>
}
