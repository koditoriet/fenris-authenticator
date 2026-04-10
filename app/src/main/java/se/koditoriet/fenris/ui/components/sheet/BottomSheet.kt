package se.koditoriet.fenris.ui.components.sheet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.koditoriet.fenris.ui.theme.PADDING_L
import se.koditoriet.fenris.ui.theme.ROUNDED_CORNER_PADDING
import se.koditoriet.fenris.ui.theme.SPACING_L
import se.koditoriet.fenris.ui.theme.SPACING_S

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <S> BottomSheet(
    hideSheet: () -> Unit,
    sheetState: SheetState,
    sheetViewState: S,
    content: @Composable ColumnScope.(state: S) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = hideSheet,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = ROUNDED_CORNER_PADDING, topEnd = ROUNDED_CORNER_PADDING)
    ) {
        BoxWithConstraints {
            AnimatedContent(
                modifier = Modifier.heightIn(max = maxHeight * 0.6f),
                targetState = sheetViewState,
                transitionSpec = {
                    (fadeIn()).togetherWith(fadeOut()).using(
                        SizeTransform(clip = false)
                    )
                }
            ) { state ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PADDING_L),
                    verticalArrangement = Arrangement.spacedBy(SPACING_L),
                ) {
                    content(state)
                    Spacer(Modifier.height(SPACING_S))
                }
            }
        }
    }
}
