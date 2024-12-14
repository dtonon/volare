package com.dluvian.volare.ui.components.text

import android.text.format.DateUtils
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.dluvian.volare.ui.theme.OnBgLight

@Composable
fun RelativeTime(from: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val time = remember(from) {
        DateUtils.getRelativeTimeSpanString(context, from * 1000).toString()
    }
    Text(
        modifier = modifier,
        text = time,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = OnBgLight
    )
}
