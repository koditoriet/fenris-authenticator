package se.koditoriet.snout.ui.screens.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import se.koditoriet.snout.appStrings
import se.koditoriet.snout.ui.components.LoadingSpinner
import se.koditoriet.snout.ui.theme.SPACING_XXL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreProgressScreen(
    importedSecrets: Int,
    secretsToImport: Int,
) {
    val screenStrings = appStrings.restoringBackupScreen

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screenStrings.heading) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .wrapContentSize(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoadingSpinner()
            Spacer(Modifier.size(SPACING_XXL))
            Text(
                text = screenStrings.restoredSecrets(importedSecrets, secretsToImport),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
