@file:OptIn(ExperimentalMaterial3Api::class)

package se.koditoriet.snout.ui.screens.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.koditoriet.snout.appStrings
import se.koditoriet.snout.ui.components.MAIN_BUTTON_HEIGHT
import se.koditoriet.snout.ui.components.MainButton
import se.koditoriet.snout.ui.theme.PADDING_L
import se.koditoriet.snout.ui.theme.PADDING_XL
import se.koditoriet.snout.ui.theme.SPACING_L
import se.koditoriet.snout.ui.theme.SPACING_M
import se.koditoriet.snout.ui.theme.SPACING_XXL

@Composable
fun InitialSetupScreen(
    onEnableBackups: () -> Unit,
    onSkipBackups: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    val screenStrings = appStrings.setupScreen
    var backupChoice by remember { mutableStateOf(BackupChoice.EnableBackups) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screenStrings.welcome) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier
                    .padding(PADDING_XL)
                    .padding(bottom = MAIN_BUTTON_HEIGHT)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                item {
                    Column {
                        Text(
                            text = screenStrings.enableBackups,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(SPACING_L))
                        Text(
                            text = screenStrings.enableBackupsDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(Modifier.height(SPACING_XXL))

                        BackupChoiceCard(
                            title = screenStrings.enableBackupsCardEnable,
                            description = screenStrings.enableBackupsCardEnableDescription,
                            selected = backupChoice == BackupChoice.EnableBackups,
                            onClick = { backupChoice = BackupChoice.EnableBackups }
                        )

                        Spacer(Modifier.height(SPACING_M))

                        BackupChoiceCard(
                            title = screenStrings.enableBackupsCardDisable,
                            description = screenStrings.enableBackupsCardDisableDescription,
                            selected = backupChoice == BackupChoice.DisableBackups,
                            onClick = { backupChoice = BackupChoice.DisableBackups }
                        )

                        Spacer(Modifier.height(SPACING_M))

                        BackupChoiceCard(
                            title = screenStrings.enableBackupsCardImport,
                            description = screenStrings.enableBackupsCardImportDescription,
                            selected = backupChoice == BackupChoice.ImportAndEnableBackups,
                            onClick = { backupChoice = BackupChoice.ImportAndEnableBackups }
                        )
                    }
                }
            }
            MainButton(
                text = appStrings.generic.next,
                onClick = {
                    when (backupChoice) {
                        BackupChoice.DisableBackups -> onSkipBackups()
                        BackupChoice.EnableBackups -> onEnableBackups()
                        BackupChoice.ImportAndEnableBackups -> onRestoreBackup()
                    }
                }
            )
        }
    }
}

@Composable
private fun BackupChoiceCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        }  else {
            MaterialTheme.colorScheme.outline
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(PADDING_L),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )

            Spacer(Modifier.width(SPACING_M))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private enum class BackupChoice {
    DisableBackups,
    EnableBackups,
    ImportAndEnableBackups,
}
