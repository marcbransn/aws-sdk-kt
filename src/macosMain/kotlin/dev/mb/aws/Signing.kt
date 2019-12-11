package dev.mb.aws

data class HttpRequest(
    val method: Method,
    val url: String,
    val headers: List<Header>,
    val payload: String
)

enum class Method {
    GET, POST
}

data class Header(val name: String, val value: String)

data class Param(val name: String, val value: String)

data class Credentials(val accessKeyId: String, val secretAccessKey: String)

data class DateTimeStamp(val dateStamp: String, val timeStamp: String)

private fun HttpRequest.canonicalURI(): String =
    url.substringAfter("://").substringAfter("/").substringBefore("?").takeUnless { it.isEmpty() } ?: "/"

private fun HttpRequest.canonicalQueryString(): String =
    url.substringAfter("?", "")
        .takeUnless { it.isBlank() }
        ?.split("&")
        ?.map { it.split("=").let { Param(it[0], it[1]) } }
        ?.sortedBy { it.value }
        ?.sortedBy { it.name }
        ?.map { "${it.name}=${it.value}" }
        ?.joinToString("&") ?: ""

private fun HttpRequest.canonicalHeaders(): String =
    headers
        .groupBy { it.name }
        .map { "${it.key.toLowerCase()}:${it.value.map { it.value.trimAll() }.joinToString(",")}\n" }
        .joinToString("")

private fun String.trimAll(): String =
    split("\n")
        .map { it.trimStart().trimEnd().replace("\\s+".toRegex(), " ") }
        .joinToString(",")

private fun HttpRequest.signedHeaders(): String =
    headers.map { it.name.toLowerCase() }.sorted().distinct().joinToString(";")

private fun HttpRequest.hashedPayload(): String =
    payload.encodeToByteArray().sha256()

private fun canonicalRequest(
    method: Method,
    canonicalURI: String,
    canonicalQueryString: String,
    canonicalHeaders: String,
    signedHeaders: String,
    hashedPayload: String
): String = "$method\n$canonicalURI\n$canonicalQueryString\n$canonicalHeaders\n$signedHeaders\n$hashedPayload"

private fun hashedCanonicalRequest(
    method: Method,
    canonicalURI: String,
    canonicalQueryString: String,
    canonicalHeaders: String,
    signedHeaders: String,
    hashedPayload: String
): String = canonicalRequest(
    method,
    canonicalURI,
    canonicalQueryString,
    canonicalHeaders,
    signedHeaders,
    hashedPayload
)
    .encodeToByteArray().sha256()

private fun stringToSign(
    algorithm: String,
    requestDateTime: String,
    credentialScope: String,
    hashedCanonicalRequest: String
): String = "$algorithm\n$requestDateTime\n$credentialScope\n$hashedCanonicalRequest"

private fun signatureKey(
    key: String,
    dateStamp: String,
    regionName: String,
    serviceName: String
): ByteArray {
    val kSecret = "AWS4$key".encodeToByteArray()
    val kDate = hmacSha256(dateStamp, kSecret)
    val kRegion = hmacSha256(regionName, kDate)
    val kService = hmacSha256(serviceName, kRegion)
    val kSigning = hmacSha256("aws4_request", kService)
    return kSigning
}

private fun signature(stringToSign: String, signatureKey: ByteArray): String =
    hmacSha256(stringToSign, signatureKey)
        .hexEncoded()

private fun authorization(
    algorithm: String,
    accessKeyId: String,
    credentialScope: String,
    signedHeaders: String,
    signature: String
) = "$algorithm Credential=${accessKeyId}/${credentialScope}, SignedHeaders=${signedHeaders}, Signature=${signature}"

private fun requestDateTime(dateStamp: String, timeStamp: String) = "${dateStamp}T${timeStamp}Z"

private fun credentialScope(dateStamp: String, regionName: String, serviceName: String) =
    "$dateStamp/$regionName/$serviceName/aws4_request"

private const val algorithm = "AWS4-HMAC-SHA256"

fun canonicalRequest(request: HttpRequest): String {
    val method = request.method
    val canonicalURI = request.canonicalURI()
    val canonicalQueryString = request.canonicalQueryString()
    val canonicalHeaders = request.canonicalHeaders()
    val signedHeaders = request.signedHeaders()
    val hashedPayload = request.hashedPayload()

    return canonicalRequest(
        method, canonicalURI, canonicalQueryString, canonicalHeaders, signedHeaders, hashedPayload
    )
}

fun authorization(
    request: HttpRequest,
    credentials: Credentials,
    dateTimeStamp: DateTimeStamp,
    regionName: String,
    serviceName: String
): String {
    val method = request.method
    val canonicalURI = request.canonicalURI()
    val canonicalQueryString = request.canonicalQueryString()
    val canonicalHeaders = request.canonicalHeaders()
    val signedHeaders = request.signedHeaders()
    val hashedPayload = request.hashedPayload()

    val hashedCanonicalRequest = hashedCanonicalRequest(
        method, canonicalURI, canonicalQueryString, canonicalHeaders, signedHeaders, hashedPayload
    )

    val requestDateTime =
        requestDateTime(dateTimeStamp.dateStamp, dateTimeStamp.timeStamp)
    val credentialScope =
        credentialScope(dateTimeStamp.dateStamp, regionName, serviceName)
    val stringToSign = stringToSign(
        algorithm,
        requestDateTime,
        credentialScope,
        hashedCanonicalRequest
    )
    val signatureKey = signatureKey(
        credentials.secretAccessKey,
        dateTimeStamp.dateStamp,
        regionName,
        serviceName
    )
    val signature = signature(stringToSign, signatureKey)
    return authorization(
        algorithm,
        credentials.accessKeyId,
        credentialScope,
        signedHeaders,
        signature
    )
}

fun authorizedRequest(
    request: HttpRequest,
    credentials: Credentials,
    dateTimeStamp: DateTimeStamp,
    regionName: String,
    serviceName: String
): HttpRequest = request.copy(
    headers = request.headers + Header(
        "Authorization",
        authorization(request, credentials, dateTimeStamp, regionName, serviceName)
    )
)
