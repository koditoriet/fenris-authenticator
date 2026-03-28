package se.koditoriet.fenris.viewmodel

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import se.koditoriet.fenris.AppStrings
import se.koditoriet.fenris.Config
import se.koditoriet.fenris.FenrisApp
import se.koditoriet.fenris.appStrings
import se.koditoriet.fenris.vault.SynchronizedVault
import se.koditoriet.fenris.vault.Vault

/**
 * Shared functionality for all view models.
 */
abstract class ViewModelBase(private val app: Application) : AndroidViewModel(app) {
    protected val fenrisApp: FenrisApp by lazy { app as FenrisApp }
    protected val vault: SynchronizedVault by lazy { fenrisApp.vault }
    protected val configDatastore: DataStore<Config> by lazy { fenrisApp.configDatastore }

    val appStrings: AppStrings by lazy { app.appStrings }
    val config: StateFlow<Config> by lazy { fenrisApp.config }

    protected fun onIOThread(f: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { f() }
    }

    protected fun withVault(f: suspend Vault.() -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            vault.withLock(f)
        }
    }

    protected fun updateConfig(f: (Config) -> Config) = onIOThread {
        configDatastore.updateData(f)
    }
}
