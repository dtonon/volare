package com.dluvian.volare.ui.components.indicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.dluvian.volare.ui.theme.OnBgLight
import com.dluvian.volare.ui.theme.SearchIcon
import com.dluvian.volare.ui.theme.sizing
import com.dluvian.volare.ui.theme.spacing

@Composable
fun BaseHint(text: String) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            modifier = Modifier
                .size(sizing.baseHint)
                .aspectRatio(1f),
            imageVector = SearchIcon,
            contentDescription = text,
            tint = OnBgLight
        )
        Spacer(modifier = Modifier.height(spacing.small))
        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = OnBgLight
        )
    }
}