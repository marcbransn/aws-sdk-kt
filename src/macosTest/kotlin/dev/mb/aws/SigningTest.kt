package dev.mb.aws

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.perror
import kotlin.test.Test
import kotlin.test.assertTrue

class SigningTest {

    @Test
    fun `test input`() {
        listOf(
            "get-header-key-duplicate",
            "get-header-value-multiline",
            "get-header-value-order",
            "get-header-value-trim",
            "get-vanilla",
            "get-vanilla-empty-query-key",
            "get-vanilla-query",
            "get-vanilla-query",
            "get-vanilla-query-order-key",
            "get-vanilla-query-order-key-case",
            "get-vanilla-query-order-value"
        )
            .map { Pair(it, isCorrect(it)) }
            .forEach { assertTrue(it.second) }
    }

    private fun isCorrect(name: String): Boolean {
        val request = readRequest(name)
        val authorizationHeader = readAuthorizationHeader(name)

        val credentials =
            Credentials("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY")
        val dateTimeStamp = DateTimeStamp("20150830", "123600")
        val regionName = "us-east-1"
        val serviceName = "service"

        val actualAuthorizationHeader =
            authorization(request, credentials, dateTimeStamp, regionName, serviceName)

        return authorizationHeader == actualAuthorizationHeader
    }

}

fun readRequest(name: String): HttpRequest {
    val lines =
        readLines("$basePath/$name/$name.req") // TODO double consumption of sequence
    val requestLine = lines.first()
    val requestLineToken = requestLine.split(" ")
    val method = requestLineToken[0]
    val url = requestLineToken[1]
    val headers = lines
        .drop(1)
        .fold(listOf<Header>()) { list, line ->
            if (line.contains(":")) {
                list + line.removeSuffix("\n").split(":").let { headerLineToken ->
                    Header(headerLineToken[0], headerLineToken[1])
                }
            } else {
                list.dropLast(1) + list.last().let {
                    it.copy(value = "${it.value}\n${line.removeSuffix("\n")}")
                }
            }
        }

    return HttpRequest(Method.valueOf(method), url, headers, "") // TODO body?
}

fun readCanonicalRequest(name: String) =
    readText("$basePath/$name/$name.creq")

fun readStringToSign(name: String) =
    readText("$basePath/$name/$name.sts")

fun readAuthorizationHeader(name: String) =
    readText("$basePath/$name/$name.authz")

fun readSignedRequest(name: String) =
    readText("$basePath/$name/$name.sreq")

data class TestGroup(
    val request: String,
    val canonicalRequest: String,
    val stringToSign: String,
    val authorizationHeader: String,
    val signedRequest: String
)

const val basePath = "src/macosTest/resources/aws-sig-v4-test-suite"
fun readTestGroup(name: String): TestGroup {
    return TestGroup(
        readText("$basePath/$name/$name.req"),
        readText("$basePath/$name/$name.creq"),
        readText("$basePath/$name/$name.sts"),
        readText("$basePath/$name/$name.authz"),
        readText("$basePath/$name/$name.sreq")
    )
}

fun readText(fileName: String): String = readLines(fileName).joinToString("\n")

fun readLines(fileName: String): Sequence<String> = sequence {
    val file = fopen(fileName, "r")
    if (file == null) {
        perror("cannot open input file $fileName")
    }

    try {
        memScoped {
            val bufferLength = 64 * 1024
            val buffer = allocArray<ByteVar>(bufferLength)

            while (true) {
                val nextLine = fgets(buffer, bufferLength, file)?.toKString()
                if (nextLine == null || nextLine.isEmpty()) break
                @Suppress("UNNECESSARY_SAFE_CALL")
                nextLine?.let { yield(it) }
            }
        }
    } finally {
        fclose(file)
    }
}
