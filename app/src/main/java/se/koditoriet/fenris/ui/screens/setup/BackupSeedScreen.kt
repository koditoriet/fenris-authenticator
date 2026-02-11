package se.koditoriet.fenris.ui.screens.setup

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.ui.components.backupseed.BackupSeedDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSeedScreen(
    backupSeed: BackupSeed,
    onContinue: () -> Unit
) {
    val screenStrings = appStrings.seedDisplayScreen
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screenStrings.recoveryPhrase) }
            )
        }
    ) { padding ->
        BackupSeedDisplay(
            modifier = Modifier.padding(padding),
            text = screenStrings.writeThisDown,
            backupSeed = backupSeed,
            confirmButtonText = appStrings.generic.next,
            onContinue = onContinue,
        )
    }
}
