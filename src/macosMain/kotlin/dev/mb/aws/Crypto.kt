package dev.mb.aws

import kotlinx.cinterop.*
import openssl.*

fun hmacSha256(data: String, key: ByteArray): ByteArray = memScoped {
    val result = UByteArray(32)

    val ctx = HMAC_CTX_new()
    HMAC_Init_ex(ctx, key.refTo(0), key.size, EVP_sha256(), null)
    HMAC_Update(ctx, data.cstr.ptr.reinterpret(), data.length.convert())
    HMAC_Final(ctx, result.refTo(0), UIntArray(1) { result.size.toUInt() }.refTo(0))
    HMAC_CTX_free(ctx)

    result.toByteArray()
}

private const val emptySHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
fun ByteArray.sha256(): String = memScoped {
    if (isEmpty()) {
        return emptySHA256
    }

    val result = UByteArray(SHA256_DIGEST_LENGTH)

    val sha256 = alloc<SHA256_CTX>()
    SHA256_Init(sha256.ptr)
    SHA256_Update(sha256.ptr, refTo(0), size.convert())
    SHA256_Final(result.refTo(0), sha256.ptr)

    result.toByteArray().hexEncoded()
}

private val hexArray = "0123456789abcdef".toCharArray()
fun ByteArray.hexEncoded(): String {
    val hexChars = CharArray(size * 2)
    for (i in indices) {
        val value = this[i].toInt() and 0xFF
        val valueLeft = value and 0xF0 ushr 4
        val valueRight = value and 0x0F
        hexChars[i * 2] = hexArray[valueLeft]
        hexChars[i * 2 + 1] = hexArray[valueRight]
    }
    return String(hexChars)
}
