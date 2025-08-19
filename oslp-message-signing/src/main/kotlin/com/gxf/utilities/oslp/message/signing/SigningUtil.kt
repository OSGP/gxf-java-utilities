// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing

import com.gxf.utilities.oslp.message.signing.configuration.SigningConfiguration
import java.io.File
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SigningUtil(
    private val signingConfiguration: SigningConfiguration,
    private val publicKeyProvider: PublicKeyProvider,
) {

    private val privateKey: PrivateKey =
        initializePrivateKey() ?: throw IllegalStateException("Failed to initialize private key")

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    // Define publicKey if needed
    // private val publicKey: PublicKey = ...

    private fun initializePrivateKey(): PrivateKey? {
        logger.info("Initializing private key: ${signingConfiguration.privateKeyFile}")
        return try {
            KeyFactory.getInstance(signingConfiguration.securityKeyType, signingConfiguration.securityProvider)
                .generatePrivate(PKCS8EncodedKeySpec(File(signingConfiguration.privateKeyFile).readBytes()))
        } catch (e: Exception) {
            logger.error("Invalid key: ${e.message}")
            null
        }
    }

    fun createSignature(message: ByteArray): ByteArray {
        logger.debug("Creating signature for message of length: ${message.size}")
        return Signature.getInstance(signingConfiguration.securityAlgorithm, signingConfiguration.securityProvider)
            .apply {
                initSign(privateKey, SecureRandom())
                update(message)
            }
            .sign()
    }

    fun verifySignature(message: ByteArray, securityKey: ByteArray): Boolean {
        logger.debug("Verifying signature for message of length: ${message.size}")
        val builder =
            Signature.getInstance(signingConfiguration.securityAlgorithm, signingConfiguration.securityProvider).apply {
                initVerify(publicKeyProvider.getPublicKey())
                update(message)
            }

        // Truncation needed for some signature types, including the used SHA256withECDSA
        val len = securityKey[1] + 2 and 0xff
        val truncatedKey = securityKey.copyOf(len)

        return builder.verify(truncatedKey)
    }

    companion object
}
