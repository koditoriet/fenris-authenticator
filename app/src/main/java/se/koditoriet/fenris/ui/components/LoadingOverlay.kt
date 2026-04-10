package se.koditoriet.fenris.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import se.koditoriet.fenris.ui.theme.SPACING_XL

private const val TAG = "LoadingOverlay"

sealed interface LoadingOverlay {
    fun show(text: String? = null)
    fun hide()

    fun update(text: String? = null)
    fun done(dismissButtonText: String, text: String? = null, onDismissed: () -> Unit = {})
}

val LocalLoadingOverlay = staticCompositionLocalOf { LoadingOverlayImpl }

object LoadingOverlayImpl : LoadingOverlay {
    private val state = mutableStateOf<LoadingOverlayState>(LoadingOverlayState.Inactive)

    override fun show(text: String?) {
        Log.d(TAG, "Showing loading overlay with text '$text'")
        state.value = LoadingOverlayState.Loading(text)
    }

    override fun update(text: String?) {
        Log.d(TAG, "Updating loading overlay with text '$text'")
        state.value = state.value.let { state ->
            when (state) {
                LoadingOverlayState.Inactive -> state
                is LoadingOverlayState.Done -> state.copy(text = text)
                is LoadingOverlayState.Loading -> state.copy(text = text)
            }
        }
    }

    override fun done(dismissButtonText: String, text: String?, onDismissed: () -> Unit) {
        Log.d(TAG, "Marking loading overlay as done with text '$text'")
        state.value = when (state.value) {
            LoadingOverlayState.Inactive -> state.value
            is LoadingOverlayState.Done -> state.value
            is LoadingOverlayState.Loading -> LoadingOverlayState.Done(dismissButtonText, text, onDismissed)
        }
    }

    override fun hide() {
        Log.d(TAG, "Hiding loading overlay")
        state.value = LoadingOverlayState.Inactive
    }

    @Composable
    fun Render() = state.value.let { state ->
        when (state) {
            LoadingOverlayState.Inactive -> { /* NOP! */ }
            else -> {
                Dialog(
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    onDismissRequest = {},
                ) {
                    Box {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.67f))
                                .wrapContentSize(Alignment.Center)
                                .testTag("LoadingScreen")
                        ) {
                            LoadingSpinner(state is LoadingOverlayState.Loading)

                            state.text?.let {
                                Spacer(Modifier.height(SPACING_XL))
                                Text(
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    text = it,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        if (state is LoadingOverlayState.Done) {
                            MainButton(
                                text = state.dismissButtonText,
                                onClick = {
                                    hide()
                                    state.onDismissed()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private sealed interface LoadingOverlayState {
    object Inactive : LoadingOverlayState {
        override val text: String? = null
    }

    data class Loading(
        override val text: String?,
    ) : LoadingOverlayState

    data class Done(
        val dismissButtonText: String,
        override val text: String?,
        val onDismissed: () -> Unit,
    ) : LoadingOverlayState

    val text: String?
}
