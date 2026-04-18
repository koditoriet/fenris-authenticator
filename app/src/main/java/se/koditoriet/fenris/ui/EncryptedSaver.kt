package se.koditoriet.fenris.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import se.koditoriet.fenris.crypto.EphemeralSymmetricKey
import se.koditoriet.fenris.crypto.types.EncryptedData

@OptIn(ExperimentalSerializationApi::class)
class EncryptedSaver<T>(
    private val key: EphemeralSymmetricKey,
    private val serializer: KSerializer<T>,
) : Saver<T, String> {
    override fun SaverScope.save(value: T): String {
        val encodedData = value?.let { ProtoBuf.encodeToByteArray(serializer, it) } ?: nullByteArray
        return key.encrypt(encodedData).encode()
    }

    override fun restore(value: String): T? {
        val decryptedData = key.decrypt(EncryptedData.decode(value))
        return if (decryptedData.isEmpty()) {
            null
        } else {
            ProtoBuf.decodeFromByteArray(serializer, decryptedData)
        }
    }

    companion object {
        private val nullByteArray: ByteArray =
            ByteArray(0)
    }
}

class EncryptedSnapshotStateListSaver<T>(
    key: EphemeralSymmetricKey,
    serializer: KSerializer<List<T>>,
) : Saver<SnapshotStateList<T>, String> {
    private val saver: EncryptedSaver<List<T>> = EncryptedSaver(key, serializer)

    override fun SaverScope.save(value: SnapshotStateList<T>): String = saver.run {
        save(value.toList())
    }

    override fun restore(value: String): SnapshotStateList<T>? =
        saver.restore(value)?.let { mutableStateListOf<T>().apply { addAll(it) } }

}

val LocalEphemeralKey = staticCompositionLocalOf {
    EphemeralSymmetricKey()
}

@Composable
@SuppressLint("ComposableNaming")
inline fun <reified T : Any> rememberSensitiveSerializable(
    vararg inputs: Any?,
    crossinline init: () -> T,
): T {
    val saver = EncryptedSaver(LocalEphemeralKey.current, serializer<T>())
    return rememberSaveable(*inputs, saver = saver) { init() }
}

@Composable
@SuppressLint("ComposableNaming")
inline fun <reified T> rememberSensitiveSerializableState(
    vararg inputs: Any?,
    crossinline init: () -> T,
): MutableState<T> {
    val saver = EncryptedSaver(LocalEphemeralKey.current, serializer<T>())
    return rememberSaveable(*inputs, stateSaver = saver) { mutableStateOf(init()) }
}

@Composable
@SuppressLint("ComposableNaming")
inline fun <reified T> rememberSensitiveSerializableList(
    vararg inputs: Any?,
    crossinline init: () -> SnapshotStateList<T>,
): SnapshotStateList<T> {
    val saver = EncryptedSnapshotStateListSaver(LocalEphemeralKey.current, serializer<List<T>>())
    return rememberSaveable(*inputs, saver = saver) { init() }
}
