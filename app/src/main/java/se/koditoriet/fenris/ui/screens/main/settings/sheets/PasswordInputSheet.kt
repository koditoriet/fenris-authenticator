package se.koditoriet.fenris.ui.screens.main.settings.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.ui.components.MAIN_BUTTON_HEIGHT
import se.koditoriet.fenris.ui.components.MainButton
import se.koditoriet.fenris.ui.components.sheet.BottomSheetGlobalHeader
import se.koditoriet.fenris.ui.theme.INPUT_FIELD_PADDING
import se.koditoriet.fenris.ui.theme.PADDING_XXL
import se.koditoriet.fenris.ui.theme.SPACING_S

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordInputSheet(
    heading: String,
    confirmButtonText: String,
    confirmPassword: Boolean = false,
    onSubmit: (password: String) -> Unit,
) {
    val passwordFieldState = rememberTextFieldState()
    val confirmPasswordFieldState = rememberTextFieldState()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    val fieldModifier = Modifier
        .fillMaxWidth()
        .padding(INPUT_FIELD_PADDING)

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MAIN_BUTTON_HEIGHT)
                .padding(bottom = PADDING_XXL),
            verticalArrangement = Arrangement.spacedBy(SPACING_S),
        ) {
            BottomSheetGlobalHeader(heading = heading)
            OutlinedSecureTextField(
                state = passwordFieldState,
                modifier = fieldModifier.focusRequester(focusRequester),
                label = { Text(appStrings.generic.password) },
            )

            if (confirmPassword) {
                OutlinedSecureTextField(
                    state = confirmPasswordFieldState,
                    modifier = fieldModifier,
                    label = { Text(appStrings.generic.confirmPassword) },
                    isError = passwordFieldState.text != confirmPasswordFieldState.text,
                )
            }
        }

        MainButton(
            text = confirmButtonText,
            oneshot = true,
            enabled = passwordFieldState.text.isNotEmpty() &&
                        (!confirmPassword || passwordFieldState.text == confirmPasswordFieldState.text),
            onClick = { onSubmit(passwordFieldState.text.toString()) },
        )
    }
}
