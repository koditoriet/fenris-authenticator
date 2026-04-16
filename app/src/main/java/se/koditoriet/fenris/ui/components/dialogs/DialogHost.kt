package se.koditoriet.fenris.ui.components.dialogs

sealed interface DialogHost {
    fun show(
        type: DialogType,
        title: String,
        text: String,
        buttonText: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit,
    )

    val visible: Boolean
}

fun DialogHost.showIrrevocableActionConfirmation(
    text: String,
    buttonText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {},
) {
    show(DialogType.IrrevocableActionConfirmation, "IGNORED", text, buttonText, onConfirm, onCancel)
}

fun DialogHost.showBadInput(
    title: String,
    text: String,
    onDismiss: () -> Unit = {},
) {
    showInformation(DialogType.BadInputInformation, title, text, onDismiss)
}

fun DialogHost.showWarning(
    title: String,
    text: String,
    onDismiss: () -> Unit = {},
) {
    showInformation(DialogType.WarningInformation, title, text, onDismiss)
}

fun DialogHost.showSuccess(
    title: String,
    text: String,
    onDismiss: () -> Unit = {},
) {
    showInformation(DialogType.SuccessInformation, title, text, onDismiss)
}

private fun DialogHost.showInformation(
    type: DialogType,
    title: String,
    text: String,
    onDismiss: () -> Unit,
) {
    show(type, title, text, "IGNORED", onDismiss, onDismiss)
}

enum class DialogType {
    IrrevocableActionConfirmation,
    BadInputInformation,
    WarningInformation,
    SuccessInformation,
}
