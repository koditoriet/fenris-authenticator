package se.koditoriet.snout.ui.screens.main

import se.koditoriet.snout.vault.NewTotpSecret

sealed class ViewState(val previousViewState: ViewState?) {
    object ListSecrets : ViewState(null)
    object Settings : ViewState(ListSecrets)
    object ManagePasskeys: ViewState(Settings)
}
