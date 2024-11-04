// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package com.gxf.utilities.kafka.message.wrapper

import java.io.IOException
import java.nio.ByteBuffer
import java.util.function.Consumer
import java.util.function.Function

/**
 * Wrapper for signable messages. Because these messages are generated from Avro schemas, they can't be changed. This
 * wrapper unifies them for the MessageSigner.
 */
class SignableMessageWrapper<T>(
    val message: T,
    private val messageGetter: Function<T, ByteBuffer>,
    private val signatureGetter: Function<T, ByteBuffer?>,
    private val signatureSetter: Consumer<ByteBuffer?>,
) {

    /** @return ByteBuffer of the whole message */
    @Throws(IOException::class) fun toByteBuffer(): ByteBuffer = messageGetter.apply(message)

    /** @return ByteBuffer of the signature in the message */
    fun getSignature(): ByteBuffer? = signatureGetter.apply(message)

    /** @param signature The signature in ByteBuffer form to be set on the message */
    fun setSignature(signature: ByteBuffer?) {
        signatureSetter.accept(signature)
    }
}
