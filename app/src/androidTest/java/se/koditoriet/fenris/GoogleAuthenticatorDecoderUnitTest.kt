package se.koditoriet.fenris

import android.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test
import se.koditoriet.fenris.importformat.GoogleAuthenticatorDecoder
import se.koditoriet.fenris.vault.TotpAlgorithm

class GoogleAuthenticatorDecoderUnitTest {
    @Test
    fun `google authenticator decoder can decode export qr code`() {
        val result = GoogleAuthenticatorDecoder.decode(Base64.decode(EXPORT_QR_CODE_PNG, Base64.DEFAULT))
        assertEquals(2, result.totpSecrets.size)
        assertEquals(0, result.passkeys.size)
        assertEquals(0, result.incompatible.size)

        assertEquals(TotpAlgorithm.SHA1, result.totpSecrets[0].secretData.algorithm)
        assertEquals(6, result.totpSecrets[0].secretData.digits)
        assertEquals(30, result.totpSecrets[0].secretData.period)
        assertEquals("HVR4CFHAFOWFGGFAGSA5JVTIMMPG6GMT", result.totpSecrets[0].secretData.secret.concatToString())
        assertEquals("", result.totpSecrets[0].metadata.issuer)
        assertEquals("1", result.totpSecrets[0].metadata.account)

        assertEquals(TotpAlgorithm.SHA512, result.totpSecrets[1].secretData.algorithm)
        assertEquals(8, result.totpSecrets[1].secretData.digits)
        assertEquals(30, result.totpSecrets[1].secretData.period)
        assertEquals("HVR4CFHAFHWFGGFAGSA5JVTIMMPG6GMT", result.totpSecrets[1].secretData.secret.concatToString())
        assertEquals("TOTPgenerator", result.totpSecrets[1].metadata.issuer)
        assertEquals("TOTPgenerator", result.totpSecrets[1].metadata.account)
    }
}

private val EXPORT_QR_CODE_PNG = """
    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
""".trimIndent()