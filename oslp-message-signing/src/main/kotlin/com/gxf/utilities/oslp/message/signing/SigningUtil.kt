// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing

import com.gxf.utilities.oslp.message.signing.configuration.SigningConfiguration
import java.security.SecureRandom
import java.security.Signature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SigningUtil(private val signingConfiguration: SigningConfiguration, private val keyProvider: KeyProvider) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun createSignature(message: ByteArray): ByteArray {
        logger.debug("Creating signature for message of length: ${message.size}")
        return Signature.getInstance(signingConfiguration.securityAlgorithm, signingConfiguration.securityProvider)
            .apply {
                initSign(keyProvider.getPrivateKey(), SecureRandom())
                update(message)
            }
            .sign()
    }

    fun verifySignature(message: ByteArray, securityKey: ByteArray): Boolean {
        logger.debug("Verifying signature for message of length: ${message.size}")
        val builder =
            Signature.getInstance(signingConfiguration.securityAlgorithm, signingConfiguration.securityProvider).apply {
                initVerify(keyProvider.getPublicKey())
                update(message)
            }

        // Truncation needed for some signature types, including the used SHA256withECDSA
        val len = securityKey[1] + 2 and 0xff
        val truncatedKey = securityKey.copyOf(len)

        return builder.verify(truncatedKey)
    }

    companion object
}
