package se.koditoriet.fenris.repository.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val V3toV4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `passkeys_new` (
                `credentialId` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `userId` TEXT NOT NULL,
                `userName` TEXT NOT NULL,
                `displayName` TEXT NOT NULL,
                `rpId` TEXT NOT NULL,
                `keyAlias` TEXT NOT NULL,
                `publicKey` TEXT NOT NULL,
                `encryptedBackupPrivateKey` TEXT,
                `timeOfCreation` INTEGER NOT NULL,
                `timeOfLastUse` INTEGER,
                `algorithm` TEXT NOT NULL,
                PRIMARY KEY(`credentialId`)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `passkeys_new` (
                `credentialId`,
                `sortOrder`,
                `userId`,
                `userName`,
                `displayName`,
                `rpId`,
                `keyAlias`,
                `publicKey`,
                `encryptedBackupPrivateKey`,
                `timeOfCreation`,
                `timeOfLastUse`,
                `algorithm`
            )
            SELECT
                `credentialId`,
                `sortOrder`,
                `userId`,
                `userName`,
                `displayName`,
                `rpId`,
                `keyAlias`,
                `publicKey`,
                `encryptedBackupPrivateKey`,
                `timeOfCreation`,
                `timeOfLastUse`,
                'ES256'
            FROM `passkeys`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `passkeys`")
        db.execSQL("ALTER TABLE `passkeys_new` RENAME TO `passkeys`")
    }
}
