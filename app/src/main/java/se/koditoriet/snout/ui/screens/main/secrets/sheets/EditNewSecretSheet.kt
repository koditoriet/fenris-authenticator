package se.koditoriet.snout.ui.screens.main.secrets.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import se.koditoriet.snout.appStrings
import se.koditoriet.snout.ui.components.TotpSecretForm
import se.koditoriet.snout.ui.components.TotpSecretFormResult
import se.koditoriet.snout.ui.components.sheet.BottomSheetGlobalHeader
import se.koditoriet.snout.vault.NewTotpSecret

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNewSecretSheet(
    prefilledSecret: NewTotpSecret?,
    hideSecretsFromAccessibility: Boolean,
    onSave: (NewTotpSecret) -> Unit,
) {
    val screenStrings = appStrings.secretsScreen
    BottomSheetGlobalHeader(
        heading = screenStrings.editNewSecretSheetHeading,
        details = if (prefilledSecret == null) {
            screenStrings.editNewSecretSheetDescriptionManual
        } else {
            screenStrings.editNewSecretSheetDescription
        },
    )

    if (prefilledSecret == null) {
        TotpSecretForm<TotpSecretFormResult.TotpSecret>(
            padding = null,
            hideSecretsFromAccessibility = hideSecretsFromAccessibility,
        ) {
            onSave(it.secret)
        }
    } else {
        TotpSecretForm<TotpSecretFormResult.TotpMetadata>(
            padding = null,
            metadata = prefilledSecret.metadata,
            hideSecretsFromAccessibility = hideSecretsFromAccessibility,
        ) {
            onSave(prefilledSecret.copy(metadata = it.metadata))
        }
    }
}
