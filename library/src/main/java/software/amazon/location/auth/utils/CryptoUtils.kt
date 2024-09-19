package software.amazon.location.auth.utils

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import software.amazon.location.auth.utils.Constants.DEFAULT_ENCODING

const val HASHING_ALGORITHM = "SHA-256"
const val MAC_ALGORITHM = "HmacSHA256"
const val ENCODED_CHARACTER_REGEX = "%[0-9A-Fa-f]{2}"

fun hash(value: String): String {
    val bytes = value.toByteArray()
    val md = MessageDigest.getInstance(HASHING_ALGORITHM)
    val digest = md.digest(bytes)
    return digest.toHexString()
}

@Throws(Exception::class)
fun hmacSha256(key: ByteArray, data: String): ByteArray {
    val sha256Hmac = Mac.getInstance(MAC_ALGORITHM)
    val secretKey = SecretKeySpec(key, MAC_ALGORITHM)
    sha256Hmac.init(secretKey)

    return sha256Hmac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
}

@Throws(Exception::class)
fun hmacSha256(key: String, data: String) =
    hmacSha256(key.toByteArray(Charset.forName("utf-8")), data)

fun ByteArray.toHexString(): String{
    return joinToString("") { "%02x".format(it) }
}

fun urlEncode(value: String?, path: Boolean): String {
    val encodedCharactersPattern = Regex(ENCODED_CHARACTER_REGEX)
    if (value == null) {
        return ""
    }

    return try {
        val encoded = URLEncoder.encode(value, DEFAULT_ENCODING)

        val buffer = StringBuffer(encoded.length)
        val matcher = encodedCharactersPattern.findAll(encoded)
        var lastIndex = 0

        for (result in matcher) {
            buffer.append(encoded, lastIndex, result.range.first)

            var replacement = result.value
            when (replacement) {
                "+" -> replacement = "%20"
                "%2A" -> replacement = "*"
                "%7E" -> replacement = "~"
                "%2F" -> if (path) replacement = "/"
            }

            buffer.append(replacement)
            lastIndex = result.range.last + 1
        }

        buffer.append(encoded, lastIndex, encoded.length)
        buffer.toString()

    } catch (ex: UnsupportedEncodingException) {
        throw RuntimeException(ex)
    }
}

