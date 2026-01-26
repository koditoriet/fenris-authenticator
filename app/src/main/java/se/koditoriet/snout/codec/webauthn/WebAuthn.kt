package se.koditoriet.snout.codec.webauthn

import androidx.credentials.provider.CallingAppInfo
import com.upokecenter.cbor.CBOREncodeOptions
import com.upokecenter.cbor.CBORObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import se.koditoriet.snout.codec.Base64Url
import se.koditoriet.snout.codec.Base64Url.Companion.toBase64Url
import se.koditoriet.snout.credentialprovider.appInfoToOrigin
import java.math.BigInteger
import java.security.MessageDigest
import java.security.interfaces.ECPublicKey









