package se.koditoriet.fenris.credentialprovider

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.PublicKeyCredentialEntry
import se.koditoriet.fenris.PRIVILEGED_BROWSERS_ASSET_NAME
import se.koditoriet.fenris.R
import se.koditoriet.fenris.TLD_LIST_ASSET_NAME
import se.koditoriet.fenris.credentialprovider.activities.AuthenticateActivity
import se.koditoriet.fenris.credentialprovider.webauthn.PublicKeyCredentialRequestOptions
import se.koditoriet.fenris.credentialprovider.webauthn.WebAuthnValidator
import se.koditoriet.fenris.vault.CredentialId
import se.koditoriet.fenris.vault.Vault
import java.time.Instant
import kotlin.random.Random

const val CREDENTIAL_DATA = "CREDENTIAL_DATA"
const val CREDENTIAL_ID = "CREDENTIAL_ID"
private const val TAG = "CredentialProviderUtil"

suspend fun createBeginGetCredentialResponse(
    vault: Vault,
    context: Context,
    request: BeginGetCredentialRequest,
): BeginGetCredentialResponse {
    Log.i(TAG, "Fetching passkeys from vault")
    val credentialEntries = request.beginGetCredentialOptions.flatMap {
        when (it) {
            is BeginGetPublicKeyCredentialOption -> getPasskeys(
                vault = vault,
                context = context,
                callingAppInfo = request.callingAppInfo,
                option = it,
                validator = context.webAuthnValidator,
            )
            else -> emptyList()
        }
    }
    Log.i(TAG, "Presenting ${credentialEntries.size} passkeys")
    return BeginGetCredentialResponse(credentialEntries)
}

private suspend fun getPasskeys(
    vault: Vault,
    context: Context,
    callingAppInfo: CallingAppInfo?,
    option: BeginGetPublicKeyCredentialOption,
    validator: WebAuthnValidator,
): List<CredentialEntry> {
    Log.i(TAG, "Listing eligible passkeys")
    val request = PublicKeyCredentialRequestOptions.fromJSON(option.requestJson)
    val allowedCredentials = request.allowCredentials.map { CredentialId(it.id) }

    if (allowedCredentials.isNotEmpty()) {
        val allowed = allowedCredentials.joinToString(", ") { it.toString() }
        Log.i(TAG, "RP lists the following credentials as allowed: $allowed")
    } else {
        Log.i(TAG, "RP did not specify allowedCredentials")
    }

    val passkeyIcon = Icon.createWithResource(context, R.drawable.passkey_fenris)
    val rpId = request.rpId ?: callingAppInfo?.let { validator.appInfoToRpId(it) }
    val passkeys = rpId?.let { vault.getPasskeys(it) } ?: emptyList()
    return passkeys.flatMap { passkey ->
        if (allowedCredentials.isEmpty() || allowedCredentials.contains(passkey.credentialId)) {
            val data = Bundle().apply { putString(CREDENTIAL_ID, passkey.credentialId.string) }
            listOf(
                PublicKeyCredentialEntry(
                    context = context,
                    username = passkey.userName,
                    pendingIntent = createPendingIntent(context, AuthenticateActivity::class.java, data),
                    beginGetPublicKeyCredentialOption = option,
                    displayName = passkey.displayName,
                    icon = passkeyIcon,
                    isAutoSelectAllowed = true,
                    lastUsedTime = passkey.timeOfLastUse?.let { Instant.ofEpochMilli(it) },
                )
            )
        } else {
            emptyList()
        }
    }
}

fun createPendingIntent(context: Context, cls: Class<*>, extra: Bundle? = null): PendingIntent =
    Intent(context, cls).run {
        Log.i(TAG, "Creating pending intent for ${cls.simpleName}")
        setPackage(context.packageName)
        if (extra != null) {
            Log.d(TAG, "Setting extra: $extra")
            putExtra(CREDENTIAL_DATA, extra)
        }
        PendingIntent.getActivity(
            context,
            Random.nextInt(),
            this,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

private var _webAuthnValidator: WebAuthnValidator? = null

val Context.webAuthnValidator: WebAuthnValidator
    get() {
        if (_webAuthnValidator == null) {
            val privilegedBrowserList = assets.open(PRIVILEGED_BROWSERS_ASSET_NAME).use { stream ->
                stream.bufferedReader().use { it.readText() }
            }
            val tlds = assets.open(TLD_LIST_ASSET_NAME).use { stream ->
                stream.bufferedReader().use { it.readLines() }.filter { it.isNotEmpty() && !it.startsWith('#') }
            }
            _webAuthnValidator = WebAuthnValidator(
                tlds = tlds,
                privilegedBrowserList = privilegedBrowserList,
            )
        }
        return _webAuthnValidator!!
    }
