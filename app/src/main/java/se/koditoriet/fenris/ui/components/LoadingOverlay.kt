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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.koditoriet.fenris.ui.theme.SPACING_XL

private const val TAG = "LoadingOverlay"

sealed interface LoadingOverlay {
    suspend fun show(text: String? = null)
    suspend fun hide()
    suspend fun update(text: String? = null)
    suspend fun done(
        dismissButtonText: String,
        text: String? = null,
        success: Boolean = true,
        onDismissed: () -> Unit = {},
    )

    fun show(scope: CoroutineScope, text: String? = null) {
        scope.launch { show(text) }
    }

    fun hide(scope: CoroutineScope) {
        scope.launch { hide() }
    }

    fun update(scope: CoroutineScope, text: String?) {
        scope.launch { update(text) }
    }

    fun done(
        scope: CoroutineScope,
        dismissButtonText: String,
        text: String? = null,
        success: Boolean = true,
        onDismissed: () -> Unit = {},
    ) {
        scope.launch { done(dismissButtonText, text, success, onDismissed) }
    }
}

val LocalLoadingOverlay = staticCompositionLocalOf<LoadingOverlay> { LoadingOverlayImpl }

object LoadingOverlayImpl : LoadingOverlay {
    private val state = mutableStateOf<LoadingOverlayState>(LoadingOverlayState.Inactive)

    override suspend fun show(text: String?) = withContext(Dispatchers.Main) {
        Log.d(TAG, "Showing loading overlay with text '$text'")
        state.value = LoadingOverlayState.Loading(text)
    }

    override suspend fun update(text: String?) = withContext(Dispatchers.Main) {
        Log.d(TAG, "Updating loading overlay with text '$text'")
        state.value = state.value.let { state ->
            when (state) {
                LoadingOverlayState.Inactive -> state
                is LoadingOverlayState.Done -> state.copy(text = text)
                is LoadingOverlayState.Loading -> state.copy(text = text)
            }
        }
    }

    override suspend fun done(
        dismissButtonText: String,
        text: String?,
        success: Boolean,
        onDismissed: () -> Unit,
    ) = withContext(Dispatchers.Main) {
        Log.d(TAG, "Marking loading overlay as done with text '$text'")
        state.value = when (state.value) {
            LoadingOverlayState.Inactive -> state.value
            is LoadingOverlayState.Done -> state.value
            is LoadingOverlayState.Loading -> LoadingOverlayState.Done(dismissButtonText, text, success, onDismissed)
        }
    }

    override suspend fun hide() = withContext(Dispatchers.Main) {
        Log.d(TAG, "Hiding loading overlay")
        state.value = LoadingOverlayState.Inactive
    }

    @Composable
    fun Render() = state.value.let { state ->
        val scope = LocalLifecycleOwner.current.lifecycleScope
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
                            val spinnerState = when (state) {
                                is LoadingOverlayState.Done if state.success -> LoadingSpinnerState.SUCCESS
                                is LoadingOverlayState.Done -> LoadingSpinnerState.FAILED
                                is LoadingOverlayState.Loading -> LoadingSpinnerState.LOADING
                            }
                            LoadingSpinner(spinnerState)

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
                                    hide(scope)
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
        val success: Boolean,
        val onDismissed: () -> Unit,
    ) : LoadingOverlayState

    val text: String?
}
