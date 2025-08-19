// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing

import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SigningUtilTest(private val signingUtil: SigningUtil) {

    fun contextLoads() {
        // This test will simply check if the application context loads successfully.
        // You can add more specific tests for SigningUtil here.
    }

    fun createSignatureTest() {
        signingUtil.createSignature("Test message".toByteArray())
    }

    fun verifySignatureTest() {
        val message = "Test message".toByteArray()
        val signature = signingUtil.createSignature(message)
        val isValid = signingUtil.verifySignature(message, signature)
        assert(isValid) { "Signature verification failed" }
    }
}
