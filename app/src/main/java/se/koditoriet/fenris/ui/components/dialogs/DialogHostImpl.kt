package se.koditoriet.fenris.ui.components.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

val LocalDialogHost = staticCompositionLocalOf<DialogHost> { DialogHostImpl }

object DialogHostImpl : DialogHost {
    private val state: MutableState<DialogHostState?> = mutableStateOf(null)

    override val visible: Boolean
        get() = state.value != null

    override fun show(
        type: DialogType,
        title: String,
        text: String,
        buttonText: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit,
    ) {
        state.value = DialogHostState(
            type = type,
            title = title,
            text = text,
            buttonText = buttonText,
            onConfirm = onConfirm,
            onCancel = onCancel,
        )
    }

    @Composable
    fun Render() {
        state.value?.apply {
            val onConfirm = {
                state.value = null
                onConfirm()
            }

            val onCancel = {
                state.value = null
                onCancel()
            }

            when (type) {
                DialogType.IrrevocableActionConfirmation -> {
                    IrrevocableActionConfirmationDialog(text, buttonText, onConfirm, onCancel)
                }
                DialogType.BadInputInformation -> {
                    BadInputInformationDialog(title, text, onCancel)
                }
                DialogType.WarningInformation -> {
                    WarningInformationDialog(title, text, onCancel)
                }
                DialogType.SuccessInformation -> {
                    SuccessInformationDialog(title, text, onCancel)
                }
            }
        }
    }
}

private data class DialogHostState(
    val type: DialogType,
    val title: String,
    val text: String,
    val buttonText: String,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit,
)
