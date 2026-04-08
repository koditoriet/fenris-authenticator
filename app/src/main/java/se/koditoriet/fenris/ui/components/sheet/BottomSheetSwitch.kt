package se.koditoriet.fenris.ui.components.sheet

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import se.koditoriet.fenris.ui.theme.LocalAccentColors
import se.koditoriet.fenris.ui.theme.PADDING_XS
import se.koditoriet.fenris.ui.theme.SPACING_L

@Composable
fun BottomSheetSwitch(
    text: String,
    checked: Boolean,
    verticalPadding: Dp = PADDING_XS,
    onCheckedChange: (Boolean) -> Unit,
) {
    BottomSheetItem(verticalPadding = verticalPadding) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors().copy(
                checkedThumbColor = LocalAccentColors.current.onForeground,
                checkedTrackColor = LocalAccentColors.current.onBackground,
            ),
        )
        Spacer(modifier = Modifier.width(SPACING_L))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
