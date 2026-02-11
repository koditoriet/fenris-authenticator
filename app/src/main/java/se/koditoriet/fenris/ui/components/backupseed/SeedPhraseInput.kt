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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.isSensitiveData
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.ui.components.BadInputInformationDialog
import se.koditoriet.fenris.ui.components.MAIN_BUTTON_HEIGHT_WITH_SECONDARY
import se.koditoriet.fenris.ui.components.MainButton
import se.koditoriet.fenris.ui.components.SecondaryButton
import se.koditoriet.fenris.ui.primaryHint
import se.koditoriet.fenris.ui.theme.PADDING_S
import se.koditoriet.fenris.ui.theme.PADDING_XL
import se.koditoriet.fenris.ui.theme.SPACING_M
import se.koditoriet.fenris.ui.theme.SPACING_S

private const val TAG = "SeedPhraseInput"

@Composable
fun SeedPhraseInput(
    confirmButtonText: String,
    wordCount: Int,
    seedWords: Set<String>,
    modifier: Modifier = Modifier,
    onScanQRClick: () -> Unit,
    onContinue: (BackupSeed) -> Unit,
) {
    val screenStrings = appStrings.seedInputScreen
    var invalidBackupSeedPhrase by remember { mutableStateOf(false) }
    val focusRequesters = remember { List(wordCount) { FocusRequester() } }
    val words = remember { mutableStateListOf(*Array(wordCount) { "" }) }

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
            Text(
                text = screenStrings.enterRecoveryPhrase,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(SPACING_S),
                horizontalArrangement = Arrangement.spacedBy(SPACING_M),
            ) {
                items(wordCount) { index ->
                    SeedWordInput(
                        index = index,
                        words = words[index],
                        isLastWord = index == wordCount - 1,
                        seedWords = seedWords,
                        onValueChange = { words[index] = it },
                        onNextWord = { focusRequesters[index + 1].requestFocus() },
                        focusRequester = focusRequesters[index],
                    )
                }
            }
        }

        if (invalidBackupSeedPhrase) {
            BadInputInformationDialog(
                title = screenStrings.invalidSeedPhrase,
                text = screenStrings.invalidSeedPhraseDescription,
                onDismiss = { invalidBackupSeedPhrase = false }
            )
        }

        val seedPhraseIsValid = words.all { it in seedWords }
        MainButton(
            text = confirmButtonText,
            enabled = seedPhraseIsValid,
            onClick = {
                if (seedPhraseIsValid) {
                    try {
                        onContinue(BackupSeed.fromMnemonic(words))
                    } catch (e: Exception) {
                        invalidBackupSeedPhrase = true
                        Log.w(TAG, "Invalid seed phrase", e)
                    }
                }
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
