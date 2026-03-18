package se.koditoriet.fenris.credentialprovider.webauthn

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.credentials.provider.CallingAppInfo
import se.koditoriet.fenris.codec.Base64Url.Companion.toBase64Url
import java.security.MessageDigest

private const val TAG = "WebAuthnValidation"
private const val ANDROID_ORIGIN_SCHEME = "android:apk-key-hash"

class WebAuthnValidator(
    tlds: Iterable<String>,
    private val privilegedBrowserList: String,
) {
    private val tldSet = tlds.map { it.lowercase() }.toSet()

    fun originIsValid(callingAppInfo: CallingAppInfo): Boolean {
        val origin = try {
            appInfoToOrigin(callingAppInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to get origin from calling app info", e)
            return false
        }

        if (origin.startsWith("$ANDROID_ORIGIN_SCHEME:")) {
            // If we're dealing with an Android origin, there's nothing more for us to verify
            return true
        }

        val originUri = origin.toUri()
        if (originUri.scheme != "https" && !originUri.isHttpLocalhost) {
            Log.e(TAG, "Bad origin URI scheme (must be https): ${originUri.scheme}")
            return false
        }

        // We intentionally do NOT validate rpId against origin, as Credential Manager does it for us and
        // explicitly requests that we don't.

        return true
    }

    fun rpIsValid(rpId: String): Boolean {
        if (rpId.lowercase() in tldSet) {
            // The RP ID must not be a TLD.
            return false
        }

        val labels = rpId.split('.')
        if (labels.isEmpty()) {
            return false
        }

        if (labels.any { !it.isValidLabel }) {
            return false
        }

        return true
    }

    fun appInfoToOrigin(callingAppInfo: CallingAppInfo): String {
        if (callingAppInfo.isOriginPopulated()) {
            return callingAppInfo.getOrigin(privilegedBrowserList)!!
        }

        // If origin is not populated, we're dealing with an app origin
        val cert = callingAppInfo.signingInfo.apkContentsSigners[0].toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val certHash = md.digest(cert)
        return "$ANDROID_ORIGIN_SCHEME:${certHash.toBase64Url().string}"
    }

    /**
     * Derives an rpId from a CallingAppInfo.
     * Only web origins are supported, and we approximate the rpId by taking the host of the origin URI.
     */
    fun appInfoToRpId(callingAppInfo: CallingAppInfo): String {
        require(callingAppInfo.isOriginPopulated())

        val origin = callingAppInfo.getOrigin(privilegedBrowserList)
        require(origin != null)

        val host = origin.toUri().host
        require(host != null)

        return host
    }
}

private val Uri.isHttpLocalhost: Boolean
    get() = scheme?.lowercase() == "http" && host?.lowercase() == "localhost"

private val String.isValidLabel: Boolean
    get() = !isEmpty() && this[0] != '-' && lowercase().all { it in LABEL_CHARS }

private val LABEL_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789-".toSet()
