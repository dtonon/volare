package com.dluvian.voyage.ui.views.nonMain.thread

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dluvian.voyage.R
import com.dluvian.voyage.core.Fn
import com.dluvian.voyage.core.OnUpdate
import com.dluvian.voyage.core.ThreadViewRefresh
import com.dluvian.voyage.core.ThreadViewShowReplies
import com.dluvian.voyage.core.ThreadViewToggleCollapse
import com.dluvian.voyage.core.model.CommentUI
import com.dluvian.voyage.core.model.ThreadUI
import com.dluvian.voyage.core.viewModel.ThreadViewModel
import com.dluvian.voyage.ui.components.CommentRow
import com.dluvian.voyage.ui.components.PostRow
import com.dluvian.voyage.ui.components.PullRefreshBox
import com.dluvian.voyage.ui.components.indicator.BaseHint
import com.dluvian.voyage.ui.theme.spacing

@Composable
fun ThreadView(vm: ThreadViewModel, snackbar: SnackbarHostState, onUpdate: OnUpdate) {
    val isRefreshing by vm.isRefreshing
    val thread by vm.thread.collectAsState()

    ThreadScaffold(snackbar = snackbar, onUpdate = onUpdate) {
        thread?.let {
            ThreadViewContent(
                thread = it,
                isRefreshing = isRefreshing,
                onUpdate = onUpdate
            )
        }
    }
}

@Composable
private fun ThreadViewContent(
    thread: ThreadUI,
    isRefreshing: Boolean,
    onUpdate: OnUpdate
) {
    PullRefreshBox(isRefreshing = isRefreshing, onRefresh = { onUpdate(ThreadViewRefresh) }) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (thread.rootPost != null) {
                item {
                    PostRow(post = thread.rootPost, isDetailed = true, onUpdate = onUpdate)
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = spacing.tiny)
                }
            }
            items(thread.comments) { comment ->
                Comment(comment = comment, onUpdate = onUpdate)
                HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = spacing.tiny)
            }
            if (thread.comments.isEmpty()) {
                item {
                    Column(modifier = Modifier.fillParentMaxHeight(0.5f)) {
                        BaseHint(text = stringResource(id = R.string.no_comments_found))
                    }
                }
            }
        }
    }
}

@Composable
private fun Comment(comment: CommentUI, onUpdate: OnUpdate) {
    // TODO: Collapse immediately
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUpdate(ThreadViewToggleCollapse(id = comment.id)) }) {
        CommentRow(comment = comment, onUpdate = onUpdate)
        if (comment.showReplies()) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = spacing.xl, bottom = spacing.medium, end = spacing.medium)
                )
                if (comment.replies.isEmpty()) MoreRepliesTextButton(
                    commentCount = comment.commentCount,
                    onShowReplies = { onUpdate(ThreadViewShowReplies(id = comment.id)) })
                else Column {
                    comment.replies.forEach { Comment(comment = it, onUpdate = onUpdate) }
                }
            }
        }
    }
}

@Composable
fun MoreRepliesTextButton(commentCount: Int, onShowReplies: Fn) {
    TextButton(onClick = onShowReplies) {
        if (commentCount == 1) Text(text = stringResource(id = R.string.one_reply))
        else Text(text = "$commentCount ${stringResource(id = R.string.replies)}")
    }
}
