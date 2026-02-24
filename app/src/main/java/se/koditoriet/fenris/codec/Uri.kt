package se.koditoriet.fenris.codec

import android.net.Uri

val Uri.totpIssuer: String
    get() {
        val qpIssuer = totpIssuerFromQueryParam
        val pathIssuer = totpIssuerFromPath
        if (qpIssuer != null && pathIssuer != null) {
            require(qpIssuer == pathIssuer)
        }
        return qpIssuer ?: pathIssuer ?: throw IllegalArgumentException("path component is missing")
    }

val Uri.totpAccount: String?
    get() = path
        ?.split(':', limit = 2)
        ?.drop(1)
        ?.firstOrNull()

val Uri.totpSecret: String
    get() = queryParameter("secret")

val Uri.totpDigits: Int
    get() = queryParameter("digits", "6")
        .toIntOrNull() ?: throw IllegalArgumentException("query parameter 'digits' is not an integer")

val Uri.totpPeriod: Int
    get() = queryParameter("period", "30")
        .toIntOrNull() ?: throw IllegalArgumentException("query parameter 'period' is not an integer")

val Uri.totpAlgorithm: String
    get() = queryParameter("algorithm", "SHA1")

private fun Uri.queryParameter(name: String, default: String? = null): String =
    getQueryParameter(name)
        ?: default
        ?: throw IllegalArgumentException("query parameter '$name' is missing")

private val Uri.totpIssuerFromQueryParam: String?
    get() = getQueryParameter("issuer")

private val Uri.totpIssuerFromPath: String?
    get() = path
        ?.split(':', limit = 2)
        ?.first()
        ?.trimStart('/')
