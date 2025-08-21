// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing

import com.gxf.utilities.oslp.message.signing.configuration.SigningProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SigningUtilTest {

    val keyPair: KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()

    val mockKeyProvider: KeyProvider =
        mock<KeyProvider> {
            on { getPrivateKey() } doReturn keyPair.private
            on { getPublicKey() } doReturn keyPair.public
        }

    private val signingUtil: SigningUtil =
        SigningUtil(
            signingConfiguration = SigningProperties(securityProvider = "SunEC", securityAlgorithm = "SHA256withECDSA"),
            keyProvider = mockKeyProvider,
        )

    @Test
    fun `should sign and verify message`() {
        val message = "test-message".toByteArray()
        val signature = signingUtil.createSignature(message)
        verify(mockKeyProvider).getPrivateKey()
        assertThat(signingUtil.verifySignature(message, signature)).isTrue()
        verify(mockKeyProvider).getPublicKey()
    }
}
