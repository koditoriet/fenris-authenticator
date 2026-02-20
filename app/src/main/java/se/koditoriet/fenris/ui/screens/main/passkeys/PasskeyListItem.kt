package se.koditoriet.fenris.ui.screens.main.passkeys

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import se.koditoriet.fenris.AppStrings
import se.koditoriet.fenris.ui.components.listview.ReorderableListItem
import se.koditoriet.fenris.ui.primaryHint
import se.koditoriet.fenris.ui.theme.LIST_ITEM_FONT_SIZE
import se.koditoriet.fenris.ui.theme.PADDING_M
import se.koditoriet.fenris.vault.Passkey

class PasskeyListItem(
    val passkey: Passkey,
    private val appStrings: AppStrings,
    private val onUpdatePasskey: (Passkey) -> Unit,
    private val onLongClickPasskey: (PasskeyListItem) -> Unit,
) : ReorderableListItem {
    override val key: String
        get() = passkey.credentialId.string

    override val sortOrder: Long
        get() = passkey.sortOrder

    override val onClickLabel: String
        get() = ""

    override val onLongClickLabel: String
        get() = appStrings.generic.selectItem

    override val visibleDataHash: Int by lazy {
        listOf(
            passkey.credentialId.hashCode(),
            passkey.displayName.hashCode(),
        ).hashCode()
    }

    override fun filterPredicate(filter: String): Boolean = (
            filter in passkey.displayName.lowercase() ||
                    filter in passkey.userName.lowercase() ||
                    filter in passkey.rpId.lowercase()
            )

    override fun onUpdateSortOrder(sortOrder: Long) {
        onUpdatePasskey(passkey.copy(sortOrder = sortOrder))
    }

    override fun onClick() { }

    override fun onLongClick() {
        onLongClickPasskey(this)
    }

    @Composable
    override fun RowScope.Render() {
        Column(
            modifier = Modifier
                .weight(1.0f)
                .padding(start = PADDING_M)
        ) {
            Text(
                text = passkey.displayName,
                fontSize = LIST_ITEM_FONT_SIZE,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = passkey.rpId,
                fontSize = LIST_ITEM_FONT_SIZE,
                color = MaterialTheme.colorScheme.primaryHint,
            )
            Text(
                text = passkey.userName,
                fontSize = LIST_ITEM_FONT_SIZE,
                color = MaterialTheme.colorScheme.primaryHint,
            )
        }
    }
}
