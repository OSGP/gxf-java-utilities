// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing

import com.gxf.utilities.oslp.message.signing.configuration.SigningConfiguration
import java.io.File
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import org.springframework.stereotype.Component

@Component
class PublicKeyTestProvider(private val signingConfiguration: SigningConfiguration) : PublicKeyProvider {
    override fun getPublicKey(): PublicKey {
        return KeyFactory.getInstance(signingConfiguration.securityKeyType, signingConfiguration.securityProvider)
            .generatePublic(X509EncodedKeySpec(File(config.publicKeyPath).readBytes()))
    }
}
