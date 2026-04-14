package se.koditoriet.fenris.ui.screens.main.secrets.sheets

import se.koditoriet.fenris.ui.components.sheet.BottomSheetAction
import BottomSheetContextualHeader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.vault.TotpSecret

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretActionsSheet(
    totpSecret: TotpSecret,
    onEditMetadata: (TotpSecret.Id) -> Unit,
    onDeleteSecret: (TotpSecret.Id) -> Unit,
) {
    val screenStrings = appStrings.secretsScreen
    BottomSheetContextualHeader(
        heading = totpSecret.issuer,
        subheading = totpSecret.account ?: screenStrings.actionsSheetNoAccount,
        icon = Icons.Default.AccountBox,
    )
    BottomSheetAction(
        icon = Icons.Default.Edit,
        text = screenStrings.actionsSheetEdit,
        onClick = { onEditMetadata(totpSecret.id) },
    )
    BottomSheetAction(
        icon = Icons.Default.DeleteForever,
        text = screenStrings.actionsSheetDelete,
        onClick = { onDeleteSecret(totpSecret.id) },
    )
}
