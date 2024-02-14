// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package com.gxf.utilities.kafka.message.signing

import com.gxf.utilities.kafka.message.signing.MessageSigner.Companion.generateKeyPair
import com.gxf.utilities.kafka.message.signing.MessageSigner.Companion.newBuilder
import com.gxf.utilities.kafka.message.wrapper.SignableMessageWrapper
import org.apache.avro.Schema
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors

class MessageSignerTest {

    private val signingEnabled = true
    private val stripAvroHeader = true

    private val signatureAlgorithm = "SHA256withRSA"
    private val signatureProvider = "SunRsaSign"
    private val signatureKeyAlgorithm = "RSA"
    private val signatureKeySize = 2048
    private val signatureKeySizeBytes = signatureKeySize / 8

    private val keyPair = generateKeyPair(
        signatureKeyAlgorithm, signatureProvider, signatureKeySize
    )

    private val random: Random = SecureRandom()

    private val messageSigner = newBuilder()
        .signingEnabled(signingEnabled)
        .stripAvroHeader(stripAvroHeader)
        .signatureAlgorithm(signatureAlgorithm)
        .signatureProvider(signatureProvider)
        .signatureKeyAlgorithm(signatureKeyAlgorithm)
        .signatureKeySize(signatureKeySize)
        .keyPair(keyPair)
        .build()

    @Test
    fun signsMessageWithoutSignature() {
        val messageWrapper: SignableMessageWrapper<*> = this.messageWrapper()

        messageSigner.sign(messageWrapper)

        assertThat(messageWrapper.getSignature()).isNotNull()
    }

    @Test
    fun signsRecordHeaderWithoutSignature() {
        val record = this.producerRecord()

        messageSigner.sign(record)

        assertThat(record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE)).isNotNull()
    }

    @Test
    fun signsMessageReplacingSignature() {
        val randomSignature = this.randomSignature()
        val messageWrapper = this.messageWrapper()
        messageWrapper.setSignature(ByteBuffer.wrap(randomSignature))

        val actualSignatureBefore = this.bytes(messageWrapper.getSignature())
        assertThat(actualSignatureBefore).isNotNull().isEqualTo(randomSignature)

        messageSigner.sign(messageWrapper)

        val actualSignatureAfter = this.bytes(messageWrapper.getSignature())
        assertThat(actualSignatureAfter).isNotNull().isNotEqualTo(randomSignature)
    }

    @Test
    fun signsRecordHeaderReplacingSignature() {
        val randomSignature = this.randomSignature()
        val record = this.producerRecord()
        record.headers().add(MessageSigner.RECORD_HEADER_KEY_SIGNATURE, randomSignature)

        val actualSignatureBefore = record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE).value()
        assertThat(actualSignatureBefore).isNotNull().isEqualTo(randomSignature)

        messageSigner.sign(record)

        val actualSignatureAfter = record.headers().lastHeader(MessageSigner.RECORD_HEADER_KEY_SIGNATURE).value()
        assertThat(actualSignatureAfter).isNotNull().isNotEqualTo(randomSignature)
    }

    @Test
    fun verifiesMessagesWithValidSignature() {
        val message = this.properlySignedMessage()

        val signatureWasVerified = messageSigner.verify(message)

        assertThat(signatureWasVerified).isTrue()
    }

    @Test
    fun verifiesRecordsWithValidSignature() {
        val signedRecord = this.properlySignedRecord()

        val signatureWasVerified: Boolean = messageSigner.verify(signedRecord)

        assertThat(signatureWasVerified).isTrue()
    }

    @Test
    fun doesNotVerifyMessagesWithoutSignature() {
        val messageWrapper = this.messageWrapper()

        val signatureWasVerified = messageSigner.verify(messageWrapper)

        assertThat(signatureWasVerified).isFalse()
    }

    @Test
    fun doesNotVerifyRecordsWithoutSignature() {
        val expectedMessage = "This ProducerRecord does not contain a signature header"
        val consumerRecord = this.consumerRecord()

        val exception: Exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException::class.java
        ) {
            messageSigner.verify(
                consumerRecord
            )
        }
        val actualMessage = exception.message

        org.junit.jupiter.api.Assertions.assertTrue(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun doesNotVerifyMessagesWithIncorrectSignature() {
        val randomSignature = this.randomSignature()
        val messageWrapper = this.messageWrapper(randomSignature)

        val signatureWasVerified = messageSigner.verify(messageWrapper)

        assertThat(signatureWasVerified).isFalse()
    }

    @Test
    fun verifiesMessagesPreservingTheSignatureAndItsProperties() {
        val message = this.properlySignedMessage()
        val originalSignature = message.getSignature()
        val originalPosition = originalSignature!!.position()
        val originalLimit = originalSignature.limit()
        val originalRemaining = originalSignature.remaining()

        messageSigner.verify(message)

        val verifiedSignature = message.getSignature()
        assertThat(verifiedSignature).isEqualTo(originalSignature)
        assertThat(verifiedSignature!!.position()).isEqualTo(originalPosition)
        assertThat(verifiedSignature.limit()).isEqualTo(originalLimit)
        assertThat(verifiedSignature.remaining()).isEqualTo(originalRemaining)
    }

    private fun fromPemResource(name: String): String {
        return BufferedReader(
            InputStreamReader(
                this.javaClass.getResourceAsStream(name), StandardCharsets.ISO_8859_1
            )
        )
            .lines()
            .collect(Collectors.joining(System.lineSeparator()))
    }

    @Test
    fun worksWithKeysFromPemEncodedResources() {
        val messageSignerWithKeysFromResources =
            newBuilder()
                .signingEnabled(signingEnabled)
                .signatureAlgorithm(signatureAlgorithm)
                .signatureProvider(signatureProvider)
                .signatureKeyAlgorithm(signatureKeyAlgorithm)
                .signatureKeySize(signatureKeySize)
                .signingKey(this.fromPemResource("/rsa-private.pem"))
                .verificationKey(this.fromPemResource("/rsa-public.pem"))
                .build()

        val messageWrapper = this.messageWrapper()
        messageSignerWithKeysFromResources.sign(messageWrapper)
        val signatureWasVerified = messageSignerWithKeysFromResources.verify(messageWrapper)

        assertThat(signatureWasVerified).isTrue()
    }

    @Test
    fun recordHeaderSigningWorksWithKeysFromPemEncodedResources() {
        val messageSignerWithKeysFromResources =
            newBuilder()
                .signingEnabled(signingEnabled)
                .signatureAlgorithm(signatureAlgorithm)
                .signatureProvider(signatureProvider)
                .signatureKeyAlgorithm(signatureKeyAlgorithm)
                .signatureKeySize(signatureKeySize)
                .signingKey(this.fromPemResource("/rsa-private.pem"))
                .verificationKey(this.fromPemResource("/rsa-public.pem"))
                .build()

        val producerRecord = this.producerRecord()
        messageSignerWithKeysFromResources.sign(producerRecord)
        val consumerRecord: ConsumerRecord<String, Message> = this.producerRecordToConsumerRecord(producerRecord)
        val signatureWasVerified: Boolean = messageSignerWithKeysFromResources.verify(consumerRecord)

        assertThat(signatureWasVerified).isTrue()
    }

    @Test
    fun signingCanBeDisabled() {
        val messageSignerSigningDisabled =
            newBuilder().signingEnabled(!signingEnabled).build()

        assertThat(messageSignerSigningDisabled.canSignMessages()).isFalse()
        assertThat(messageSignerSigningDisabled.canVerifyMessageSignatures()).isFalse()
    }

    private fun messageWrapper(): TestableWrapper {
        return TestableWrapper()
    }

    private fun messageWrapper(signature: ByteArray): TestableWrapper {
        val testableWrapper = TestableWrapper()
        testableWrapper.setSignature(ByteBuffer.wrap(signature))
        return testableWrapper
    }

    private fun properlySignedMessage(): TestableWrapper {
        val messageWrapper = this.messageWrapper()
        messageSigner.sign(messageWrapper)
        return messageWrapper
    }

    private fun properlySignedRecord(): ConsumerRecord<String, Message> {
        val producerRecord = this.producerRecord()
        messageSigner.sign(producerRecord)
        return this.producerRecordToConsumerRecord(producerRecord)
    }

    private fun <K, V> producerRecordToConsumerRecord(producerRecord: ProducerRecord<K, V>): ConsumerRecord<K, V> {
        val consumerRecord =
            ConsumerRecord(producerRecord.topic(), 0, 123L, producerRecord.key(), producerRecord.value())
        producerRecord.headers().forEach(Consumer { header: Header? ->
            consumerRecord.headers().add(header)
        })
        return consumerRecord
    }

    private fun randomSignature(): ByteArray {
        val signature = ByteArray(signatureKeySizeBytes)
        random.nextBytes(signature)
        return signature
    }

    private fun bytes(byteBuffer: ByteBuffer?): ByteArray? {
        if (byteBuffer == null) {
            return null
        }
        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer[bytes]
        return bytes
    }

    private fun producerRecord(): ProducerRecord<String, Message> {
        return ProducerRecord("topic", this.message())
    }

    private fun consumerRecord(): ConsumerRecord<String, Message> {
        return ConsumerRecord("topic", 0, 123L, null, this.message())
    }

    private fun message(): Message {
        return Message("super special message")
    }

    internal class Message : SpecificRecordBase {
        private var message: String? = null

        constructor()

        constructor(message: String?) {
            this.message = message
        }

        override fun getSchema(): Schema {
            return Schema.Parser()
                .parse("""{"type":"record","name":"Message","namespace":"com.alliander.osgp.kafka.message.signing","fields":[{"name":"message","type":{"type":"string","avro.java.string":"String"}}]}""")
        }

        override fun get(field: Int): Any {
            return message!!
        }

        override fun put(field: Int, value: Any) {
            this.message = value.toString()
        }
    }

    private class TestableWrapper : SignableMessageWrapper<String>("Some test message") {
        private var signature: ByteBuffer? = null

        override fun toByteBuffer(): ByteBuffer? {
            return ByteBuffer.wrap(message.toByteArray(StandardCharsets.UTF_8))
        }

        override fun getSignature(): ByteBuffer? {
            return this.signature
        }

        override fun setSignature(signature: ByteBuffer?) {
            this.signature = signature
        }
    }
}
