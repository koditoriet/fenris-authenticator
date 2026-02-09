package se.koditoriet.fenris.ui.screens.setup

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.crypto.BACKUP_SEED_PHRASE_WORDS
import se.koditoriet.fenris.crypto.BackupSeed
import se.koditoriet.fenris.ui.components.backupseed.SeedPhraseInput
import se.koditoriet.fenris.ui.components.backupseed.SeedQRCodeInput

private const val TAG = "RestoreBackupScreen"
private val BACKUP_MIME_TYPES = arrayOf("application/octet-stream")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreBackupScreen(
    wordCount: Int = BACKUP_SEED_PHRASE_WORDS,
    seedWords: Set<String>,
    onRestore: (BackupSeed, Uri) -> Unit
) {
    var scanSecretQRCode by remember { mutableStateOf(false) }
    var backupSeed by remember { mutableStateOf<BackupSeed?>(null) }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.run {
                backupSeed?.let { backupSeed ->
                    onRestore(backupSeed, uri)
                } ?: Log.e(TAG, "Backup seed was null when import file launcher completed!")
            } ?: backupSeed?.wipe()
        }
    )

    if (scanSecretQRCode) {
        SeedQRCodeInput(
            onCancel = { scanSecretQRCode = false },
            onContinue = {
                backupSeed = it
                importFileLauncher.launch(BACKUP_MIME_TYPES)
                scanSecretQRCode = false
            }
        )
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(appStrings.restoringBackupScreen.heading) },
                )
            }
        ) { padding ->
            SeedPhraseInput(
                confirmButtonText = appStrings.seedInputScreen.restoreVault,
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
        }
    }
}
