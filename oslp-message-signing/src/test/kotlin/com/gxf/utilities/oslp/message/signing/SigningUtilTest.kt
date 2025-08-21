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

    val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance("EC").apply { initialize(256) }
    val keyPair1: KeyPair = keyPairGenerator.generateKeyPair()
    val keyPair2: KeyPair = keyPairGenerator.generateKeyPair()

    val mockKeyProvider1: KeyProvider =
        mock<KeyProvider> {
            on { getPrivateKey() } doReturn keyPair1.private
            on { getPublicKey() } doReturn keyPair1.public
        }

    val mockKeyProvider2: KeyProvider =
        mock<KeyProvider> {
            on { getPrivateKey() } doReturn keyPair2.private
            on { getPublicKey() } doReturn keyPair2.public
        }

    val mockKeyProvider3: KeyProvider =
        mock<KeyProvider> {
            on { getPrivateKey() } doReturn keyPair1.private
            on { getPublicKey() } doReturn keyPair1.public
        }

    private val signingUtil1: SigningUtil =
        SigningUtil(
            signingConfiguration = SigningProperties(securityProvider = "SunEC", securityAlgorithm = "SHA256withECDSA"),
            keyProvider = mockKeyProvider1,
        )

    private val signingUtil2: SigningUtil =
        SigningUtil(
            signingConfiguration = SigningProperties(securityProvider = "SunEC", securityAlgorithm = "SHA256withECDSA"),
            keyProvider = mockKeyProvider2,
        )

    private val signingUtil3: SigningUtil =
        SigningUtil(
            signingConfiguration = SigningProperties(securityProvider = "SunEC", securityAlgorithm = "SHA256withECDSA"),
            keyProvider = mockKeyProvider3,
        )

    @Test
    fun `should sign and verify message from same SingingUtil`() {
        val message = "test-message".toByteArray()
        val signature = signingUtil1.createSignature(message)
        verify(mockKeyProvider1).getPrivateKey()
        assertThat(signingUtil1.verifySignature(message, signature)).isTrue()
        verify(mockKeyProvider1).getPublicKey()
    }

    @Test
    fun `should sign and verify message from different SigningUtils with same keys`() {
        val message = "test-message".toByteArray()
        val signature = signingUtil1.createSignature(message)
        verify(mockKeyProvider1).getPrivateKey()
        assertThat(signingUtil3.verifySignature(message, signature)).isTrue()
        verify(mockKeyProvider3).getPublicKey()
    }

    @Test
    fun `should not verify tampered message`() {
        var message = "test-message".toByteArray()
        val signature = signingUtil1.createSignature(message)
        verify(mockKeyProvider1).getPrivateKey()
        message = "tampered-message".toByteArray()
        assertThat(signingUtil1.verifySignature(message, signature)).isFalse()
        verify(mockKeyProvider1).getPublicKey()
    }

    @Test
    fun `should not verify tampered keys`() {
        val message = "test-message".toByteArray()
        val signature = signingUtil1.createSignature(message)
        verify(mockKeyProvider1).getPrivateKey()
        assertThat(signingUtil2.verifySignature(message, signature)).isFalse()
        verify(mockKeyProvider2).getPublicKey()
    }
}
