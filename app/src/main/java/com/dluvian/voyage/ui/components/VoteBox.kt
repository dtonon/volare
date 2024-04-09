package com.dluvian.voyage.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.dluvian.voyage.R
import com.dluvian.voyage.core.ClickDownvote
import com.dluvian.voyage.core.ClickNeutralizeVote
import com.dluvian.voyage.core.ClickUpvote
import com.dluvian.voyage.core.EventIdHex
import com.dluvian.voyage.core.Fn
import com.dluvian.voyage.core.OnUpdate
import com.dluvian.voyage.core.PubkeyHex
import com.dluvian.voyage.data.interactor.Downvote
import com.dluvian.voyage.data.interactor.Upvote
import com.dluvian.voyage.data.interactor.Vote
import com.dluvian.voyage.ui.theme.DenimBlue
import com.dluvian.voyage.ui.theme.DownvoteIcon
import com.dluvian.voyage.ui.theme.DownvoteOffIcon
import com.dluvian.voyage.ui.theme.LeftRoundedChip
import com.dluvian.voyage.ui.theme.Orange
import com.dluvian.voyage.ui.theme.RightRoundedChip
import com.dluvian.voyage.ui.theme.RoundedChip
import com.dluvian.voyage.ui.theme.UpvoteIcon
import com.dluvian.voyage.ui.theme.UpvoteOffIcon
import com.dluvian.voyage.ui.theme.spacing

@Composable
fun VoteBox(
    postId: EventIdHex,
    authorPubkey: PubkeyHex,
    myVote: Vote,
    tally: Int,
    onUpdate: OnUpdate,
) {
    VoteButtonsAndTally(
        postId = postId,
        authorPubkey = authorPubkey,
        myVote = myVote,
        tally = tally,
        onUpdate = onUpdate
    )
}

@Composable
private fun VoteButtonsAndTally(
    postId: EventIdHex,
    authorPubkey: PubkeyHex,
    myVote: Vote,
    tally: Int,
    onUpdate: OnUpdate,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = spacing.medium)
            .border(
                border = AssistChipDefaults.assistChipBorder(enabled = true),
                shape = RoundedChip
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VoteButton(
            isUpvote = true,
            isActive = myVote is Upvote,
            tint = if (myVote is Upvote) Orange else MaterialTheme.colorScheme.onSurfaceVariant,
            description = stringResource(id = R.string.upvote),
            onClick = {
                if (myVote is Upvote)
                    onUpdate(ClickNeutralizeVote(postId = postId, mention = authorPubkey))
                else
                    onUpdate(ClickUpvote(postId = postId, mention = authorPubkey))
            }
        )
        Text(
            text = "$tally",
            fontWeight = if (myVote.isNeutral()) FontWeight.Normal else FontWeight.SemiBold
        )
        VoteButton(
            isUpvote = false,
            isActive = myVote is Downvote,
            tint = if (myVote is Downvote) DenimBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            description = stringResource(id = R.string.downvote),
            onClick = {
                if (myVote is Downvote)
                    onUpdate(ClickNeutralizeVote(postId = postId, mention = authorPubkey))
                else
                    onUpdate(ClickDownvote(postId = postId, mention = authorPubkey))
            })
    }
}

@Composable
private fun VoteButton(
    isUpvote: Boolean,
    isActive: Boolean,
    tint: Color,
    description: String,
    onClick: Fn
) {
    Icon(
        modifier = Modifier
            .clip(if (isUpvote) LeftRoundedChip else RightRoundedChip)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.xl),
        imageVector = if (isUpvote) {
            if (isActive) UpvoteIcon else UpvoteOffIcon
        } else {
            if (isActive) DownvoteIcon else DownvoteOffIcon
        },
        contentDescription = description,
        tint = tint
    )
}
