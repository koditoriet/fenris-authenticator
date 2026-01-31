package se.koditoriet.snout.ui.screens.setup

import se.koditoriet.snout.vault.ImportFailedException

sealed class ViewState(val previousViewState: ViewState?) {
    object InitialSetup : ViewState(null)
    object ShowBackupSeed : ViewState(InitialSetup)
    object RestoreBackup : ViewState(InitialSetup)
    object RestoringBackup : ViewState(null)
    class RestoreBackupFailed(exception: ImportFailedException) : ViewState(InitialSetup)
}
