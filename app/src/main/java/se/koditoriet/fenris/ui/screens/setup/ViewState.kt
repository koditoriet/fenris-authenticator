package se.koditoriet.fenris.ui.screens.setup

sealed class ViewState(val previousViewState: ViewState?) {
    object InitialSetup : ViewState(null)
    object ShowBackupSeed : ViewState(InitialSetup)
    object RestoreBackup : ViewState(InitialSetup)
    object RestoringBackup : ViewState(null)
    object RestoreBackupFailed : ViewState(InitialSetup)
}
