package com.fiatjaf.volare.ui.components.icon

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.ui.theme.CollapseIcon
import com.fiatjaf.volare.ui.theme.ExpandIcon

@Composable
fun ExpandOrCollapseIcon(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    if (isExpanded) {
        Icon(
            modifier = modifier,
            imageVector = CollapseIcon,
            tint = tint,
            contentDescription = stringResource(R.string.collapse)
        )
    } else {
        Icon(
            modifier = modifier,
            imageVector = ExpandIcon,
            tint = tint,
            contentDescription = stringResource(R.string.expand)
        )
    }
}
