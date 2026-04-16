package se.koditoriet.fenris.ui.components.backupseed

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.isSensitiveData
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import se.koditoriet.fenris.BACKUP_SEED_MNEMONIC_LENGTH_WORDS
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.ui.components.MAIN_BUTTON_HEIGHT_WITH_SECONDARY
import se.koditoriet.fenris.ui.components.MainButton
import se.koditoriet.fenris.ui.components.SecondaryButton
import se.koditoriet.fenris.ui.components.dialogs.LocalDialogHost
import se.koditoriet.fenris.ui.components.dialogs.showBadInput
import se.koditoriet.fenris.ui.primaryHint
import se.koditoriet.fenris.ui.theme.PADDING_S
import se.koditoriet.fenris.ui.theme.PADDING_XL
import se.koditoriet.fenris.ui.theme.SPACING_M
import se.koditoriet.fenris.ui.theme.SPACING_S

private const val TAG = "SeedPhraseInput"

@Composable
fun SeedPhraseInput(
    confirmButtonText: String,
    seedWords: Set<String>,
    modifier: Modifier = Modifier,
    seedPhraseInputState: SnapshotStateList<String>,
    onScanQRClick: () -> Unit,
    onContinue: (BackupSeed) -> Unit,
) {
    require(seedPhraseInputState.size == BACKUP_SEED_MNEMONIC_LENGTH_WORDS)
    val screenStrings = appStrings.seedInputScreen
    val dialogHost = LocalDialogHost.current
    val focusRequesters = remember { List(BACKUP_SEED_MNEMONIC_LENGTH_WORDS) { FocusRequester() } }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(
                    top = PADDING_S,
                    start = PADDING_XL,
                    end = PADDING_XL,
                    bottom = MAIN_BUTTON_HEIGHT_WITH_SECONDARY,
                )
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SPACING_M),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(SPACING_S),
                horizontalArrangement = Arrangement.spacedBy(SPACING_M),
            ) {
                items(BACKUP_SEED_MNEMONIC_LENGTH_WORDS) { index ->
                    SeedWordInput(
                        index = index,
                        words = seedPhraseInputState[index],
                        isLastWord = index == BACKUP_SEED_MNEMONIC_LENGTH_WORDS - 1,
                        seedWords = seedWords,
                        onValueChange = { seedPhraseInputState[index] = it },
                        onNextWord = { focusRequesters[index + 1].requestFocus() },
                        focusRequester = focusRequesters[index],
                    )
                }
            }
        }

        val seedPhraseIsValid = seedPhraseInputState.all { it in seedWords }
        var confirmButtonEnabled by remember { mutableStateOf(true) }
        MainButton(
            text = confirmButtonText,
            enabled = seedPhraseIsValid && confirmButtonEnabled,
            onClick = {
                confirmButtonEnabled = false
                if (seedPhraseIsValid) {
                    try {
                        onContinue(BackupSeed.fromMnemonic(seedPhraseInputState))
                    } catch (e: Exception) {
                        Log.w(TAG, "Invalid seed phrase", e)
                        dialogHost.showBadInput(
                            title = screenStrings.invalidSeedPhrase,
                            text = screenStrings.invalidSeedPhraseDescription,
                        )
                    }
                    seedPhraseInputState.forEachIndexed { index, _ -> seedPhraseInputState[index] = "" }
                }
                confirmButtonEnabled = true
            },
            secondaryButton = SecondaryButton(
                text = screenStrings.scanQRCode,
                onClick = onScanQRClick,
            ),
        )
    }
}

@Composable
private fun SeedWordInput(
    index: Int,
    words: String,
    isLastWord: Boolean,
    seedWords: Set<String>,
    onValueChange: (String) -> Unit,
    onNextWord: () -> Unit,
    focusRequester: FocusRequester,
) {
    Column {
        Text(
            text = (index + 1).toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primaryHint,
        )

        OutlinedTextField(
            value = words,
            onValueChange = {
                val trimmedWord = it.lowercase().trim()
                onValueChange(trimmedWord)
                if (it.endsWith(" ") && !isLastWord && trimmedWord in seedWords) {
                    onNextWord()
                }
            },
            modifier = Modifier
                .semantics {
                    isSensitiveData = true
                }
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            singleLine = true,
            isError = words.isNotBlank() && words !in seedWords,
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = if (isLastWord) ImeAction.Done else ImeAction.Next,
                keyboardType = KeyboardType.Password,
            )
        )
    }
}
