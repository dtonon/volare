package com.fiatjaf.volare.core.model

import com.fiatjaf.volare.core.Topic

data class TopicFollowState(val topic: Topic, val isFollowed: Boolean)
data class TopicMuteState(val topic: Topic, val isMuted: Boolean)
