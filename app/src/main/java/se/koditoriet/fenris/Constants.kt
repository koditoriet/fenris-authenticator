package se.koditoriet.fenris

import se.koditoriet.fenris.credentialprovider.webauthn.AuthDataFlag
import se.koditoriet.fenris.crypto.types.EncryptionAlgorithm
import se.koditoriet.fenris.crypto.types.KeyIdentifier

/**
 * The AAGUID is a UUID uniquely identifying a WebAuthn authenticator.
 * Specifying an all-zero AAGUID, which indicates "unknown authenticator" is allowed by the spec,
 * but having our own means that apps and websites can suggest proper names for credentials and
 * generally improve the UX around credential management.
 */
val AAGUID: ByteArray = "071320aec08443fd94e6987933b23e1f".hexToByteArray()

/**
 * All our symmetric encryption is 256 bit AES-GCM.
 */
const val SYMMETRIC_KEY_SIZE: Int = 32
val SYMMETRIC_KEY_ALGORITHM = EncryptionAlgorithm.AES_GCM

/**
 * Use 32 byte salt whenever we hash a password.
 */
const val PASSWORD_SALT_SIZE: Int = 32


/**
 * Number of seconds for which a symmetric key can be used after successful authentication.
 * Due to how AndroidKeyStore is implemented, it is unfortunately not possible to authenticate symmetric keys
 * for only a specific operation, as each "operation" from a user perspective involves multiple calls to keymaster.
 */
const val SYMMETRIC_KEY_AUTHENTICATION_LIFETIME: Int = 5

/**
 * Name of privileged browser list JSON asset file.
 * Fetched from https://www.gstatic.com/gpm-passkeys-privileged-apps/apps.json
 */
const val PRIVILEGED_BROWSERS_ASSET_NAME = "privileged_browsers_google.json"

/**
 * Name of exhaustive TLD list asset file.
 * Fetched from https://data.iana.org/TLD/tlds-alpha-by-domain.txt
 */
const val TLD_LIST_ASSET_NAME = "tlds.txt"

/**
 * Scheme used to identify Fenris URIs.
 */
const val FENRIS_URI_SCHEME = "fenris"

/**
 * Host part of backup seed URI.
 */
const val BACKUP_SEED_URI_HOST: String = "seed"

/**
 * Number of words in BIP-39 encoding of backup seed.
 */
const val BACKUP_SEED_MNEMONIC_LENGTH_WORDS: Int = 24

/**
 * Default flags for passkey creation.
 */
val PASSKEY_CREATE_FLAGS = setOf(AuthDataFlag.UP, AuthDataFlag.UV, AuthDataFlag.BE)

/**
 * Default flags for passkey authentication.
 */
val PASSKEY_AUTH_FLAGS = setOf(AuthDataFlag.UP, AuthDataFlag.UV, AuthDataFlag.BE)

/**
 * Identifier for SQLCipher key encryption key.
 */
val DB_KEK_IDENTIFIER = KeyIdentifier.Internal("db_kek")

/**
 * Identifier for backup data encryption key.
 */
val BACKUP_DEK_IDENTIFIER = KeyIdentifier.Internal("backup_secret_dek")

/**
 * Valid backup MIME types.
 */
const val BACKUP_MIME_TYPE = "application/json"
