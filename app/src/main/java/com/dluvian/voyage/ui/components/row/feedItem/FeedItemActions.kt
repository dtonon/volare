package com.dluvian.voyage.ui.components.row.feedItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dluvian.voyage.core.ComposableContent
import com.dluvian.voyage.core.OnUpdate
import com.dluvian.voyage.core.OpenCrossPostCreation
import com.dluvian.voyage.core.UnbookmarkPost
import com.dluvian.voyage.core.model.FeedItemUI
import com.dluvian.voyage.ui.components.chip.BookmarkChip
import com.dluvian.voyage.ui.components.chip.CrossPostChip
import com.dluvian.voyage.ui.components.chip.UpvoteChip
import com.dluvian.voyage.ui.theme.spacing

@Composable
fun FeedItemActions(
    feedItem: FeedItemUI,
    onUpdate: OnUpdate,
    additionalStartAction: ComposableContent = {},
    additionalEndAction: ComposableContent = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        additionalStartAction()
        Spacer(modifier = Modifier.width(spacing.tiny))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isBookmarked) BookmarkChip(onClick = { onUpdate(UnbookmarkPost(postId = postId)) })
            CrossPostChip(onClick = { onUpdate(OpenCrossPostCreation(id = postId)) })
            additionalEndAction()
            UpvoteChip(
                upvoteCount = upvoteCount,
                isUpvoted = isUpvoted,
                postId = postId,
                authorPubkey = authorPubkey,
                onUpdate = onUpdate
            )
        }
    }
}