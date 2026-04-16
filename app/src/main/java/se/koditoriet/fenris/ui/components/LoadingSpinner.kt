package se.koditoriet.fenris.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import se.koditoriet.fenris.ui.theme.LocalAccentColors

@Composable
fun LoadingSpinner(state: LoadingSpinnerState) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (state) {
            LoadingSpinnerState.LOADING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp).testTag("LoadingSpinner"),
                    color = LocalAccentColors.current.accent,
                )
            }

            LoadingSpinnerState.SUCCESS -> {
                Icon(
                    modifier = Modifier.size(48.dp).testTag("LoadingCheckbox"),
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = LocalAccentColors.current.accent,
                )
            }

            LoadingSpinnerState.FAILED -> {
                Icon(
                    modifier = Modifier.size(48.dp).testTag("LoadingWarningTriangle"),
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

enum class LoadingSpinnerState {
    LOADING,
    SUCCESS,
    FAILED
}