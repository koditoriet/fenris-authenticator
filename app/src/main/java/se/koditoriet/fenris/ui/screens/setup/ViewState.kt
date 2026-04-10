package se.koditoriet.fenris.ui.screens.setup

sealed class ViewState(val previousViewState: ViewState?) {
    object InitialSetup : ViewState(null)
    object ShowBackupSeed : ViewState(InitialSetup)
    object RestoreBackup : ViewState(InitialSetup)
    object RestoreBackupFailed : ViewState(InitialSetup)
}
