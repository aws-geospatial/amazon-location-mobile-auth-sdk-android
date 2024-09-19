package software.amazon.location.auth.utils

import okhttp3.Headers
import okhttp3.Request
import okio.Buffer
import java.net.URLEncoder
import java.util.Locale
import software.amazon.location.auth.utils.Constants.SIGNING_ALGORITHM

/**
 * Sign the request with the aws authorization header
 */
fun Request.awsAuthorizationHeader(
    accessKeyId: String,
    accessKey: String,
    region: String,
    service: String,
    time: String
) =
    "$SIGNING_ALGORITHM Credential=$accessKeyId/${
        credentialScope(
            region,
            service,
            time
        )
    }, SignedHeaders=${signedHeaders()}, Signature=${signature(
        accessKey,
        region,
        service,
        time
    )}"

fun Request.signature(
    accessKey: String,
    region: String,
    service: String,
    time: String
) =
    hmacSha256(
        hmacSha256(
            hmacSha256(
                hmacSha256(hmacSha256("AWS4$accessKey", amazonDateHeaderShort(time)), region),
                service
            ), "aws4_request"
        ),
        stringToSign(region, service, time)
    ).toHexString()

fun Request.stringToSign(region: String, service: String, time: String) =
    """
    |$SIGNING_ALGORITHM
    |${time}
    |${credentialScope(region, service, time)}
    |${hash(canonicalRequest())}
    """.trimMargin("|")

fun Request.canonicalRequest() =
    """
    |$method
    |${canonicalUri()}
    |${canonicalQueryString()}
    |${canonicalHeaders()}
    |
    |${signedHeaders()}
    |${bodyDigest()}
    """.trimMargin("|")

private fun Request.canonicalUri():String {
    return urlEncode(url.encodedPath, true)
}

private fun Request.canonicalQueryString() =
    url.queryParameterNames.sorted()
        .takeIf { it.isNotEmpty() }
        ?.flatMap { name ->
            url.queryParameterValues(name)
                .filterNotNull()
                .sorted()
                .map { value ->
                    Pair(name.rfc3986Encode(), value.rfc3986Encode())
                }
        }
        ?.joinToString("&") { (name, value) ->
            "$name=$value"
        }
        ?: ""

private fun Request.canonicalHeaders() = headers.canonicalHeaders()

fun Request.signedHeaders() =
    headers.names()
        .map { it.trim().lowercase(Locale.ENGLISH) }
        .sorted()
        .joinToString(";")

private fun Request.bodyDigest() =
    hash(bodyAsString()).lowercase(Locale.ENGLISH)

private fun amazonDateHeaderShort(time: String) =
   time.substring(0..7)

private fun Request.bodyAsString() =
    body?.let {
        val buffer = Buffer()
        this.newBuilder().build().body!!.writeTo(buffer)
        buffer.readUtf8()
    } ?: ""

private fun Headers.canonicalHeaders() =
    names().joinToString("\n") {
        "${it.lowercase(Locale.ENGLISH)}:${values(it).trimmedAndJoined()}"
    }

private fun String.trimAll() = trim().replace(Regex("\\s+"), " ")

private fun List<String>.trimmedAndJoined() = joinToString(",") { it.trimAll() }

private fun String.rfc3986Encode() =
    URLEncoder.encode(this, "utf8")
        .replace("+", "%20")
        .replace("*", "%2A")
        .replace("%7E", "~")

fun credentialScope(region: String, service: String, time: String) =
    "${amazonDateHeaderShort(time)}/$region/$service/aws4_request"
