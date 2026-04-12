package se.koditoriet.fenris.repository

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import se.koditoriet.fenris.codec.Base64Url
import se.koditoriet.fenris.repository.migrations.V3toV4
import se.koditoriet.fenris.vault.CredentialId
import se.koditoriet.fenris.vault.Passkey
import se.koditoriet.fenris.vault.TotpSecret
import se.koditoriet.fenris.vault.UserId

@Database(
    version = 4,
    entities = [TotpSecret::class, Passkey::class],
)
@TypeConverters(
    TotpSecret.Id.TypeConverters::class,
    UserId.TypeConverters::class,
    CredentialId.TypeConverters::class,
    Base64Url.TypeConverters::class,
)
abstract class VaultRepository : RoomDatabase() {
    companion object {
        fun open(ctx: Context, path: String, dbKey: ByteArray): VaultRepository {
            return Room.databaseBuilder(ctx, VaultRepository::class.java, path).apply {
                // we only use this to set the SQLCipher key immediately upon opening the database
                allowMainThreadQueries()

                // We need to do a manual migration from v3 to v4 to add passkeys.algorithm without cluttering up
                // the schema with default values.
                addMigrations(V3toV4)
            }.build().apply {
                query("PRAGMA key = \"x'${dbKey.toHexString()}'\";", arrayOf())
            }
        }
    }

    abstract fun totpSecrets(): TotpSecrets
    abstract fun passkeys(): Passkeys
}
