package se.koditoriet.fenris.ui.screens.main

sealed class ViewState(val previousViewState: ViewState?) {
    object ListSecrets : ViewState(null)
    object Settings : ViewState(ListSecrets)
    object ManagePasskeys: ViewState(Settings)
}
