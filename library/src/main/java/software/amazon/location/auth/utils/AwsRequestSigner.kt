package software.amazon.location.auth.utils

import okhttp3.Headers
import okhttp3.Request
import okio.Buffer
import java.net.URLEncoder
import java.util.Locale
import software.amazon.location.auth.utils.Constants.HEADER_AUTHORIZATION

internal const val SIGNING_ALGORITHM = "AWS4-HMAC-SHA256"

/**
 * Sign the request with the aws authorization header
 */
internal fun Request.signed(accessKeyId: String, accessKey: String, region: String, service: String) =
    newBuilder()
        .header(HEADER_AUTHORIZATION, awsAuthorizationHeader(accessKeyId, accessKey, region, service))
        .build()

internal fun Request.awsAuthorizationHeader(accessKeyId: String, accessKey: String, region: String, service: String) =
    "$SIGNING_ALGORITHM Credential=$accessKeyId/${credentialScope(
        region,
        service
    )}, SignedHeaders=${signedHeaders()}, Signature=${signature(
        accessKey,
        region,
        service
    )}"

internal fun Request.signature(accessKey: String, region: String, service: String) =
    hmacSha256(
        hmacSha256(
            hmacSha256(
                hmacSha256(hmacSha256("AWS4$accessKey", amazonDateHeaderShort()), region),
                service
            ), "aws4_request"
        ),
        stringToSign(region, service)
    ).toHexString()

internal fun Request.stringToSign(region: String, service: String) =
    """
    |$SIGNING_ALGORITHM
    |${amazonDateHeader()}
    |${credentialScope(region, service)}
    |${hash(canonicalRequest())}
    """.trimMargin("|")

internal fun Request.canonicalRequest() =
    """
    |$method
    |${canonicalUri()}
    |${canonicalQueryString()}
    |${canonicalHeaders()}
    |
    |${signedHeaders()}
    |${bodyDigest()}
    """.trimMargin("|")

private fun Request.canonicalUri() =
    url.encodedPath.replace(Regex("/+"), "/")

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

private fun Request.signedHeaders() =
    headers.names()
        .map { it.trim().lowercase(Locale.ENGLISH) }
        .sorted()
        .joinToString(";")

private fun Request.bodyDigest() =
    hash(bodyAsString()).lowercase(Locale.ENGLISH)

private fun Request.amazonDateHeader() =
    header("x-amz-date")
        ?: throw NoSuchFieldException("Request cannot be signed without having the x-amz-date header")

private fun Request.amazonDateHeaderShort() =
    header("x-amz-date")?.substring(0..7)
        ?: throw NoSuchFieldException("Request cannot be signed without having the x-amz-date header")

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

private fun Request.credentialScope(region: String, service: String) =
    "${amazonDateHeaderShort()}/$region/$service/aws4_request"
