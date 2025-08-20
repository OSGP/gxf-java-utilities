// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing

import com.gxf.utilities.oslp.message.signing.configuration.SigningConfiguration
import java.security.KeyPairGenerator
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SigningUtilTest {

    private val signingUtil: SigningUtil =
        SigningUtil(
            signingConfiguration =
                SigningConfiguration(securityProvider = "SunEC", securityAlgorithm = "SHA256withECDSA"),
            keyProvider =
                object : KeyProvider {
                    private val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()

                    override fun getPublicKey() = keyPair.public

                    override fun getPrivateKey() = keyPair.private
                },
        )

    @Test
    fun `should sign and verify message`() {
        val message = "test-message".toByteArray()
        val signature = signingUtil.createSignature(message)
        assertTrue(signingUtil.verifySignature(message, signature))
    }
}
