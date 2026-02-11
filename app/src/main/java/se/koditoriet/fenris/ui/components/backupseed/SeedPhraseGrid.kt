package se.koditoriet.fenris.ui.components.backupseed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.koditoriet.fenris.ui.theme.PADDING_S
import se.koditoriet.fenris.ui.theme.PADDING_XXS
import se.koditoriet.fenris.ui.theme.SPACING_S
import se.koditoriet.fenris.ui.theme.SPACING_XL
import se.koditoriet.fenris.ui.theme.SPACING_XS

@Composable
fun SeedPhraseGrid(
    mnemonic: List<String>,
    columns: Int = 3,
) {
    val rows = (mnemonic.size + columns - 1) / columns
    Column {
        for (rowIndex in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SPACING_S)
            ) {
                for (colIndex in 0 until columns) {
                    val wordIndex = rowIndex * columns + colIndex
                    if (wordIndex < mnemonic.size) {
                        MnemonicWordCard(
                            modifier = Modifier.weight(1.0f),
                            index = wordIndex + 1,
                            word = mnemonic[wordIndex]
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(SPACING_S))
        }
    }
}

@Composable
private fun MnemonicWordCard(index: Int, word: String, modifier: Modifier) {
    Card(
        modifier = modifier.padding(PADDING_XXS),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(PADDING_S)
        ) {
            Text(
                "$index.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(SPACING_XL),
            )
            Spacer(Modifier.width(SPACING_XS))
            Text(
                text = word,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
