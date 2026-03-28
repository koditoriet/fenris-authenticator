package se.koditoriet.fenris.ui.screens.setup

import android.net.Uri
import android.util.Log
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import se.koditoriet.fenris.BACKUP_SEED_MNEMONIC_LENGTH_WORDS
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.ui.components.backupseed.SeedPhraseInput
import se.koditoriet.fenris.ui.components.backupseed.SeedQRCodeInput
import se.koditoriet.fenris.ui.components.sheet.BottomSheet
import se.koditoriet.fenris.ui.screens.main.settings.sheets.PasswordInputSheet

private const val TAG = "RestoreBackupScreen"
private val BACKUP_MIME_TYPES = arrayOf("application/octet-stream")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreBackupScreen(
    wordCount: Int = BACKUP_SEED_MNEMONIC_LENGTH_WORDS,
    seedWords: Set<String>,
    onRestore: (BackupSeed, String, Uri) -> Unit
) {
    var scanSecretQRCode by remember { mutableStateOf(false) }
    var backupSeed by remember { mutableStateOf<BackupSeed?>(null) }
    var showPasswordInput by remember { mutableStateOf(false) }
    var backupUri by remember { mutableStateOf<Uri?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val screenStrings = appStrings.seedInputScreen

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            backupUri = uri
            showPasswordInput = true
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = appStrings.generic.back,
                        )
                    }
                },
                title = { Text(screenStrings.heading) },
            )
        }
    ) { padding ->
        SeedPhraseInput(
            confirmButtonText = screenStrings.restoreVault,
            wordCount = wordCount,
            seedWords = seedWords,
            modifier = Modifier.padding(padding),
            onScanQRClick = {
                scanSecretQRCode = true
            },
            onContinue = {
                backupSeed = it
                importFileLauncher.launch(BACKUP_MIME_TYPES)
            }
        )

        if (showPasswordInput) {
            BottomSheet(
                hideSheet = { showPasswordInput = false },
                sheetState = sheetState,
                padding = padding,
                sheetViewState = showPasswordInput,
            ) {
                PasswordInputSheet(
                    heading = screenStrings.enterBackupPassword,
                    confirmButtonText = screenStrings.restoreVault,
                    onSubmit = { password ->
                        backupUri?.run {
                            backupSeed?.let { backupSeed ->
                                onRestore(backupSeed, password, this)
                            } ?: Log.e(TAG, "Backup seed was null when import file launcher completed!")
                        } ?: backupSeed?.wipe()
                    }
                )
            }
        }
    }

    if (scanSecretQRCode) {
        SeedQRCodeInput(
            onCancel = { scanSecretQRCode = false },
            onContinue = {
                backupSeed = it
                scanSecretQRCode = false
                importFileLauncher.launch(BACKUP_MIME_TYPES)
            }
        )
    }
}
