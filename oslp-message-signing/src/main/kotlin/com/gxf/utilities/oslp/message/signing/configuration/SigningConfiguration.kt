// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.oslp.message.signing.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties

@EnableConfigurationProperties(SigningConfiguration::class)
@ConfigurationProperties(prefix = "signing")
class SigningConfiguration(val securityProvider: String, val securityAlgorithm: String)
