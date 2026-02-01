package se.koditoriet.snout.ui.activities

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import se.koditoriet.snout.BiometricPromptAuthenticator
import se.koditoriet.snout.Config
import se.koditoriet.snout.credentialprovider.SnoutCredentialProviderService
import se.koditoriet.snout.ui.ignoreAuthFailure
import se.koditoriet.snout.ui.onIOThread
import se.koditoriet.snout.ui.screens.EnablePasskeysScreen
import se.koditoriet.snout.ui.screens.LockedScreen
import se.koditoriet.snout.ui.screens.main.MainScreen
import se.koditoriet.snout.ui.screens.setup.SetupScreen
import se.koditoriet.snout.ui.theme.SnoutTheme
import se.koditoriet.snout.vault.Vault
import se.koditoriet.snout.viewmodel.SnoutViewModel

private const val TAG = "MainActivity"

class MainActivity : FragmentActivity() {
    val viewModel: SnoutViewModel by viewModels()
    var _isEnabledCredentialProvider = MutableStateFlow(false)
    val isEnabledCredentialProvider: StateFlow<Boolean> = _isEnabledCredentialProvider.asStateFlow()
    private var isBackgrounded: Boolean = true

    private val foregroundObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            isBackgrounded = true
        }

        override fun onStart(owner: LifecycleOwner) {
            val credentialProviderEnabled = credentialProviderEnabled()
            if (credentialProviderEnabled != _isEnabledCredentialProvider.value) {
                _isEnabledCredentialProvider.value = credentialProviderEnabled
            }

            if (isBackgrounded) {
                lifecycleScope.launch {
                    ignoreAuthFailure {
                        if (viewModel.vaultState.first() != Vault.State.Uninitialized) {
                            viewModel.unlockVault(BiometricPromptAuthenticator.Factory(this@MainActivity))
                        }
                    }
                }
            }
            isBackgrounded = false
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

    LaunchedEffect(config.screenSecurityEnabled) {
        if (config.screenSecurityEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
    }

    SnoutTheme {
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
            config = config,
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
        SnoutCredentialProviderService::class.java
    )
    return credentialManager.isEnabledCredentialProviderService(componentName)
}
