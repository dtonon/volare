package com.dluvian.voyage.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimplePager(
    headers: List<String>,
    index: MutableIntState,
    pagerState: PagerState,
    scope: CoroutineScope = rememberCoroutineScope(),
    pageContent: @Composable PagerScope.(page: Int) -> Unit
) {
    require(headers.size == pagerState.pageCount)

    LaunchedEffect(key1 = pagerState.currentPage) {
        index.intValue = pagerState.currentPage
    }
    Column {
        PagerTabRow(
            headers = headers,
            index = index,
            onClickPage = { i -> scope.launch { pagerState.animateScrollToPage(i) } })
        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = pagerState
        ) {
            pageContent(it)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PagerTabRow(
    headers: List<String>,
    index: MutableIntState,
    onClickPage: (Int) -> Unit
) {
    // Set higher zIndex to hide resting refresh indicator
    PrimaryTabRow(modifier = Modifier.zIndex(2f), selectedTabIndex = index.intValue) {
        headers.forEachIndexed { i, header ->
            Tab(
                selected = index.intValue == i,
                onClick = {
                    index.intValue = i
                    onClickPage(i)
                },
                text = { Text(header) }
            )
        }
    }
}
