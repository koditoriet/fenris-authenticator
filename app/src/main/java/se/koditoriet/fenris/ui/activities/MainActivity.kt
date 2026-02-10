package se.koditoriet.fenris.ui.activities

import android.content.ComponentName
import android.credentials.CredentialManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.koditoriet.fenris.BiometricPromptAuthenticator
import se.koditoriet.fenris.Config
import se.koditoriet.fenris.credentialprovider.FenrisCredentialProviderService
import se.koditoriet.fenris.ui.ignoreAuthFailure
import se.koditoriet.fenris.ui.onIOThread
import se.koditoriet.fenris.ui.screens.EnablePasskeysScreen
import se.koditoriet.fenris.ui.screens.LockedScreen
import se.koditoriet.fenris.ui.screens.main.MainScreen
import se.koditoriet.fenris.ui.screens.setup.SetupScreen
import se.koditoriet.fenris.ui.theme.FenrisTheme
import se.koditoriet.fenris.vault.Vault
import se.koditoriet.fenris.viewmodel.FenrisViewModel

private const val TAG = "MainActivity"

class MainActivity : FragmentActivity() {
    val viewModel: FenrisViewModel by viewModels()

    private var _isEnabledCredentialProvider = MutableStateFlow(false)
    val isEnabledCredentialProvider: StateFlow<Boolean> = _isEnabledCredentialProvider.asStateFlow()

    private var _isBackgrounded = MutableStateFlow(true)
    var isBackgrounded: StateFlow<Boolean> = _isBackgrounded.asStateFlow()

    private val foregroundObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            _isBackgrounded.value = true
        }

        override fun onStart(owner: LifecycleOwner) {
            val credentialProviderEnabled = credentialProviderEnabled()
            if (credentialProviderEnabled != _isEnabledCredentialProvider.value) {
                _isEnabledCredentialProvider.value = credentialProviderEnabled
            }
            _isBackgrounded.value = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "Main activity created")
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Registering unlock lifecycle observer")
        lifecycle.addObserver(foregroundObserver)

        enableEdgeToEdge()
        setContent {
            MainActivityContent()
        }
    }
}

@Composable
fun MainActivity.MainActivityContent() {
    val vaultState by viewModel.vaultState.collectAsState(Vault.State.Uninitialized)
    val config by viewModel.config.collectAsState(Config.default)
    val credentialProviderEnabled by isEnabledCredentialProvider.collectAsState()
    val isBackgrounded by isBackgrounded.collectAsState()

    LaunchedEffect(isBackgrounded) {
        if (!isBackgrounded) {
            ignoreAuthFailure {
                if (vaultState != Vault.State.Uninitialized) {
                    val authFactory = BiometricPromptAuthenticator.Factory(this@MainActivityContent)
                    viewModel.unlockVault(authFactory)
                }
            }
        }
    }

    LaunchedEffect(config.screenSecurityEnabled) {
        if (config.screenSecurityEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
    }

    FenrisTheme {
        when (vaultState) {
            Vault.State.Unlocked -> { UnlockedScreens(config, credentialProviderEnabled) }
            Vault.State.Uninitialized -> { SetupScreen() }
            Vault.State.Locked -> {
                LockedScreen(
                    onUnlock = onIOThread {
                        val authFactory = BiometricPromptAuthenticator.Factory(this)
                        ignoreAuthFailure { viewModel.unlockVault(authFactory) }
                    },
                )
            }
        }
    }
}

@Composable
private fun MainActivity.UnlockedScreens(
    config: Config,
    credentialProviderEnabled: Boolean,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        !config.passkeyScreenDismissed &&
        !credentialProviderEnabled
    ) {
        EnablePasskeysScreen(
            onDismiss = {
                lifecycleScope.launch {
                    Log.i(TAG, "Dismissing credential provider nudge screen")
                    viewModel.setPasskeyScreenDismissed()
                }
            }
        )
    } else {
        MainScreen(
            credentialProviderEnabled = credentialProviderEnabled,
            viewModel = viewModel,
        )
    }
}

private fun MainActivity.credentialProviderEnabled(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return false
    }
    val credentialManager = getSystemService(CredentialManager::class.java)
    val componentName = ComponentName(
        this.applicationContext,
        FenrisCredentialProviderService::class.java
    )
    return credentialManager.isEnabledCredentialProviderService(componentName)
}
