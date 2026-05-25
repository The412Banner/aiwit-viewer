package com.the412banner.aiwitviewer.data

import android.net.Uri
import android.util.Base64
import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object OssSigner {

    /**
     * Build a presigned GET URL for Alibaba OSS using STS credentials.
     *
     * Reverse-engineered from the AIWIT app's `setPKey` flow: `security-token`
     * is an OSS sub-resource that MUST be included (un-URL-encoded) in the
     * canonical resource for the signature to match. Confirmed against
     * a 403 SignatureDoesNotMatch response that quoted OSS's expected
     * StringToSign verbatim.
     */
    fun presignedGet(
        bucket: String,
        endpoint: String,
        key: String,
        token: OssToken,
        expiresInSeconds: Long = 300L,
    ): String {
        val expiresAt = System.currentTimeMillis() / 1000L + expiresInSeconds
        val canonicalResource = "/$bucket/$key?security-token=${token.securityToken}"
        val stringToSign = "GET\n\n\n$expiresAt\n$canonicalResource"

        val mac = Mac.getInstance("HmacSHA1").apply {
            init(SecretKeySpec(token.accessKeySecret.toByteArray(), "HmacSHA1"))
        }
        val signature = Base64.encodeToString(
            mac.doFinal(stringToSign.toByteArray()),
            Base64.NO_WRAP,
        )

        val encodedKey = key.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        val params = listOf(
            "OSSAccessKeyId" to token.accessKeyId,
            "Expires" to expiresAt.toString(),
            "Signature" to signature,
            "security-token" to token.securityToken,
        ).joinToString("&") { (k, v) -> "$k=${Uri.encode(v)}" }

        return "https://$bucket.$endpoint/$encodedKey?$params"
    }
}
