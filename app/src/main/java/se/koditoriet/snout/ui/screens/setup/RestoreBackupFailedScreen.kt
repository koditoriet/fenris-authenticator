package se.koditoriet.snout.ui.screens.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import se.koditoriet.snout.appStrings
import se.koditoriet.snout.ui.components.MAIN_BUTTON_HEIGHT
import se.koditoriet.snout.ui.components.MainButton
import se.koditoriet.snout.ui.theme.BACKGROUND_ICON_SIZE
import se.koditoriet.snout.ui.theme.SPACING_XXL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreBackupFailedScreen(onDismiss: () -> Unit) {
    val screenStrings = appStrings.restoreBackupFailedScreen

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screenStrings.heading) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = MAIN_BUTTON_HEIGHT)
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = screenStrings.centerCopy,
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(Modifier.height(SPACING_XXL))

                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(BACKGROUND_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            MainButton(
                text = appStrings.generic.ok,
                onClick = onDismiss,
            )
        }
    }
}
