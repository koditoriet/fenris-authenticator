package se.koditoriet.snout.ui.screens.main.secrets

import android.content.ClipData
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isSensitiveData
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import se.koditoriet.snout.AppStrings
import se.koditoriet.snout.appStrings
import se.koditoriet.snout.ui.components.listview.ReorderableListItem
import se.koditoriet.snout.ui.ignoreAuthFailure
import se.koditoriet.snout.ui.primaryDisabled
import se.koditoriet.snout.ui.primaryHint
import se.koditoriet.snout.ui.theme.LIST_ITEM_FONT_SIZE
import se.koditoriet.snout.ui.theme.PADDING_M
import se.koditoriet.snout.ui.theme.PADDING_S
import se.koditoriet.snout.ui.theme.SECRET_FONT_SIZE
import se.koditoriet.snout.vault.TotpSecret
import kotlin.time.Clock

class TotpSecretListItem(
    val totpSecret: TotpSecret,
    private val hideSecretsFromAccessibility: Boolean,
    private val clock: Clock,
    private val environment: ListItemEnvironment,
    private val getTotpCodes: suspend (TotpSecret) -> List<String>,
    private val onLongClickSecret: (TotpSecretListItem) -> Unit,
    private val onUpdateSecret: (TotpSecret) -> Unit,
) : ReorderableListItem {
    override val key: String
        get() = totpSecret.id.toString()

    override val sortOrder: Long
        get() = totpSecret.sortOrder

    override val onClickLabel: String
        get() = environment.screenStrings.generateOneTimeCode

    override val onLongClickLabel: String
        get() = environment.appStrings.generic.selectItem

    override fun filterPredicate(filter: String): Boolean =
        filter in totpSecret.issuer.lowercase() || filter in (totpSecret.account?.lowercase() ?: "")

    override fun onUpdateSortOrder(sortOrder: Long) {
        onUpdateSecret(totpSecret.copy(sortOrder = sortOrder))
    }

    override fun onClick() {
        when (viewState) {
            ListRowViewState.CodeHidden -> environment.scope.launch {
                ignoreAuthFailure {
                    val codes = getTotpCodes(totpSecret)
                    viewState = ListRowViewState.CodeVisible(codes)
                }
            }
            is ListRowViewState.CodeVisible -> environment.scope.launch {
                val clipEntry = ClipEntry(ClipData.newPlainText("", totpCode))
                environment.clipboard.setClipEntry(clipEntry)
                environment.view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

                codeRecentlyCopied = true
                delay(1000)
                codeRecentlyCopied = false
            }
        }
    }

    override fun onLongClick() {
        onLongClickSecret(this)
    }

    fun update(updatedSecret: TotpSecret): TotpSecretListItem =
        TotpSecretListItem(
            totpSecret = updatedSecret,
            hideSecretsFromAccessibility = hideSecretsFromAccessibility,
            clock = clock,
            environment = environment,
            getTotpCodes = getTotpCodes,
            onLongClickSecret = onLongClickSecret,
            onUpdateSecret = onUpdateSecret,
        )

    private val dots = "\u2022".repeat(totpSecret.digits)
    private var viewState by mutableStateOf<ListRowViewState>(ListRowViewState.CodeHidden)
    private var codeRecentlyCopied by mutableStateOf(false)
    private var totpCode by mutableStateOf(dots)
    private var progress by mutableFloatStateOf(0.0f)

    @Composable
    override fun RowScope.Render() {
        LaunchedEffect(viewState) {
            animateTimerProgressbar()
        }

        Column(
            modifier = Modifier
                .padding(start = PADDING_M)
                .weight(1.0f)
        ) {
            AccountDetails()
            Row {
                TOTPCode()
                CopyIcon()
            }
        }

        if (viewState is ListRowViewState.CodeVisible) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(PADDING_S)
                    .size(36.dp),
                progress = { progress },
            )
        }
    }

    private suspend fun animateTimerProgressbar() {
        val state = viewState
        if (state is ListRowViewState.CodeVisible) {
            for (code in state.codes) {
                totpCode = code
                val now = clock.now()
                val deciSecondsIntoPeriod = (now.epochSeconds % totpSecret.period) * 10
                val deciSecondsPeriod = totpSecret.period * 10
                for (step in deciSecondsIntoPeriod..deciSecondsPeriod) {
                    progress = 1 - step.toFloat() / deciSecondsPeriod
                    delay(100)
                }
            }
            viewState = ListRowViewState.CodeHidden
            totpCode = dots
            progress = 0.0f
        }
    }

    @Composable
    private fun AccountDetails() {
        Row {
            Text(
                text = totpSecret.issuer,
                fontSize = LIST_ITEM_FONT_SIZE,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = totpSecret.account?.let { "(${it})" } ?: "",
                fontSize = LIST_ITEM_FONT_SIZE,
                color = MaterialTheme.colorScheme.primaryHint,
                modifier = Modifier.padding(start = PADDING_S),
            )
        }
    }

    @Composable
    private fun TOTPCode() {
        Text(
            text = totpCode.chunked(3).joinToString("\u202F"),
            fontSize = SECRET_FONT_SIZE,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .padding(top = PADDING_S)
                .height(36.dp)
                .semantics {
                    isSensitiveData = true
                    if (hideSecretsFromAccessibility) {
                        hideFromAccessibility()
                    }
                },
            color = if (viewState is ListRowViewState.CodeVisible) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.primaryDisabled
            },
        )
    }

    @Composable
    private fun CopyIcon() {
        if (viewState is ListRowViewState.CodeVisible) {
            val (icon, description) = if (codeRecentlyCopied) {
                Pair(Icons.Default.Check, environment.screenStrings.codeCopied)
            } else {
                Pair(Icons.Default.ContentCopy, environment.screenStrings.copyCode)
            }
            Icon(
                modifier = Modifier
                    // Vertical padding + size should be equal to vertical padding + size of code font
                    .padding(PADDING_S, PADDING_S + 10.dp)
                    .size(16.dp),
                imageVector = icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.primaryHint,
            )
        }
    }
}

private sealed interface ListRowViewState {
    object CodeHidden : ListRowViewState
    class CodeVisible(val codes: List<String>) : ListRowViewState
}

class ListItemEnvironment(
    val scope: CoroutineScope,
    val clipboard: Clipboard,
    val view: View,
    val appStrings: AppStrings,
    val screenStrings: AppStrings.SecretsScreen,
) {
    companion object {
        @Composable
        fun remember(): ListItemEnvironment {
            val strings = appStrings
            val scope = rememberCoroutineScope()
            val clipboard = LocalClipboard.current
            val view = LocalView.current

            return androidx.compose.runtime.remember(clipboard, view) {
                ListItemEnvironment(
                    scope = scope,
                    clipboard = clipboard,
                    view = view,
                    appStrings = strings,
                    screenStrings = strings.secretsScreen,
                )
            }
        }
    }
}
