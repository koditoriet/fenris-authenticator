package se.koditoriet.snout.synchronization

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

private const val TAG = "Sync"

class Sync<T>(private val name: String? = null, itemFactory: () -> T) {
    private val mutex = Mutex()
    private val item by lazy { itemFactory() }

    suspend fun <U> withLock(action: suspend T.() -> U): U {
        val id = Random.nextInt()
        Log.d("Sync", "Attempting to acquire lock '$name' with correlation id $id")
        try {
            return mutex.withLock {
                Log.d("Sync", "Acquired lock '$name' with correlation id $id")
                item.action()
            }
        } finally {
            Log.d("Sync", "Released lock '$name' with correlation id $id")
        }
    }

    fun <U> unsafeReadOnly(read: T.() -> U): U =
        item.read()
}
