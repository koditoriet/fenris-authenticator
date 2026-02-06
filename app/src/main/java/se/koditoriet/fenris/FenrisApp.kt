package se.koditoriet.fenris

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.koditoriet.fenris.crypto.Cryptographer
import se.koditoriet.fenris.repository.VaultRepository
import se.koditoriet.fenris.vault.SynchronizedVault
import se.koditoriet.fenris.vault.Vault
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val TAG = "FenrisApp"

private val idleTimeoutScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class FenrisApp : Application() {
    val vault: SynchronizedVault
    val config: DataStore<Config> by dataStore("config", ConfigSerializer)
    private var idleTimeout: TimeoutJob? = null

    fun startIdleTimeout() {
        idleTimeoutScope.launch {
            val cfg = config.data.first()
            if (cfg.lockOnClose) {
                idleTimeout
                    ?.start(cfg.lockOnCloseGracePeriod.seconds)
                    ?: vault.withLock {
                        Log.w(TAG, "Idle timeout job is unavailable - locking vault immediately instead")
                        lock()
                    }
            }
        }
    }

    fun cancelIdleTimeout() {
        idleTimeoutScope.launch {
            idleTimeout?.cancel()
        }
    }

    init {
        Log.i(TAG, "Loading sqlcipher native libraries")
        System.loadLibrary("sqlcipher")
        val repositoryFactory = { dbName: String, key: ByteArray ->
            VaultRepository.open(this, dbName, key)
        }

        Log.i(TAG, "Creating vault")
        vault = SynchronizedVault {
            Vault(
                repositoryFactory = repositoryFactory,
                cryptographer = Cryptographer(),
                dbFile = lazy { getDatabasePath("vault")!! },
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        idleTimeout = TimeoutJob(
            name = "LockOnIdle",
            application = this,
            onTimeout = IdleTimeoutReceiver::class.java,
        )

        Log.i(TAG, "Registering automatic lock observers")
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        ProcessLifecycleOwner.get().lifecycle.addObserver(foregroundObserver)
    }

    private val foregroundObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            Log.i(TAG, "Lost focus")
            startIdleTimeout()
        }

        override fun onStart(owner: LifecycleOwner) {
            Log.i(TAG, "Got back focus")
            cancelIdleTimeout()
        }
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            idleTimeoutScope.launch {
                Log.i(TAG, "Screen off detected; locking vault immediately")
                cancelIdleTimeout()
                vault.withLock { lock() }
            }
        }
    }
}

/**
 * Sets up an action to be executed when a timeout expires.
 * The action can be canceled up until the point where the timeout expires. After that, it's unstoppable.
 */
class TimeoutJob(
    private val name: String,
    private val application: Application,
    private val onTimeout: Class<out BroadcastReceiver>,
) {
    private var timeoutPendingIntent: PendingIntent? = null
    private val mutex = Mutex()
    private val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun start(timeout: Duration) = mutex.withLock {
        timeoutPendingIntent?.cancel()

        Log.i(TAG, "Executing timeout job '$name' in $timeout")
        val pendingIntent = Intent(application, onTimeout).let { intent ->
            PendingIntent.getBroadcast(
                application,
                name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        timeoutPendingIntent = pendingIntent
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + timeout.inWholeMilliseconds,
            pendingIntent,
        )
    }

    suspend fun cancel() = mutex.withLock {
        timeoutPendingIntent?.let {
            Log.i(TAG, "Canceling timeout for job '$name'")
            alarmManager.cancel(it)
        }
    }
}

class IdleTimeoutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        (context?.applicationContext as? FenrisApp)?.apply {
            ProcessLifecycleOwner.get().lifecycleScope.launch {
                Log.d(TAG, "Idle timeout expired")
                vault.withLock { lock() }
            }
        }
    }
}
