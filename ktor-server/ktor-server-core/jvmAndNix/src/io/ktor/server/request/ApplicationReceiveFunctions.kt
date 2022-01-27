/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

package io.ktor.server.request

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.application.internal.*
import io.ktor.server.plugins.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import kotlin.native.concurrent.*
import kotlin.reflect.*

/**
 * Represents a subject for [ApplicationReceivePipeline]
 * @param typeInfo specifies the desired type for a receiving operation
 * @param value specifies current value being processed by the pipeline
 * @param reusableValue indicates whether the [value] instance can be reused. For example, a stream can't.
 */
public class ApplicationReceiveRequest constructor(
    public val typeInfo: TypeInfo,
    public val value: Any,
    public val reusableValue: Boolean = false
)

/**
 * Pipeline for processing incoming content
 *
 * When executed, this pipeline starts with an instance of [ByteReadChannel] and should finish with the requested type.
 */
public open class ApplicationReceivePipeline(
    override val developmentMode: Boolean = false
) : Pipeline<ApplicationReceiveRequest, ApplicationCall>(Before, Transform, After) {
    /**
     * Pipeline phases
     */
    @Suppress("PublicApiImplicitType")
    public companion object Phases {
        /**
         * Executes before any transformations are made
         */
        public val Before: PipelinePhase = PipelinePhase("Before")

        /**
         * Executes transformations
         */
        public val Transform: PipelinePhase = PipelinePhase("Transform")

        /**
         * Executes after all transformations
         */
        public val After: PipelinePhase = PipelinePhase("After")
    }
}

/**
 * Receives content for this request.
 * @return instance of [T] received from this call, or `null` if content cannot be transformed to the requested type.
 */
@OptIn(ExperimentalStdlibApi::class)
public suspend inline fun <reified T : Any> ApplicationCall.receiveOrNull(): T? = receiveOrNull(typeInfo<T>())

/**
 * Receives content for this request.
 * @return instance of [T] received from this call.
 * @throws ContentTransformationException when content cannot be transformed to the requested type.
 */
@OptIn(ExperimentalStdlibApi::class)
public suspend inline fun <reified T : Any> ApplicationCall.receive(): T = receive(typeInfo<T>())

/**
 * Receives content for this request.
 * @param type instance of `KClass` specifying type to be received.
 * @return instance of [T] received from this call.
 * @throws ContentTransformationException when content cannot be transformed to the requested type.
 */
public suspend fun <T : Any> ApplicationCall.receive(type: KClass<T>): T {
    val kotlinType = starProjectedTypeBridge(type)
    return receive(TypeInfo(type, kotlinType.platformType, kotlinType))
}

/**
 * Receives content for this request.
 * @param type instance of `KClass` specifying type to be received.
 * @return instance of [T] received from this call.
 * @throws ContentTransformationException when content cannot be transformed to the requested type.
 */
public suspend fun <T : Any> ApplicationCall.receive(typeInfo: TypeInfo): T {
    require(typeInfo.type != ApplicationReceiveRequest::class) { "ApplicationReceiveRequest can't be received" }

    val token = attributes.getOrNull(DoubleReceivePreventionTokenKey)

    if (token == null) {
        attributes.put(DoubleReceivePreventionTokenKey, DoubleReceivePreventionToken)
    }

    val incomingContent = token ?: request.receiveChannel()
    val receiveRequest = ApplicationReceiveRequest(typeInfo, incomingContent)
    val finishedRequest = request.pipeline.execute(this, receiveRequest)
    val transformed = finishedRequest.value

    when {
        transformed === DoubleReceivePreventionToken -> throw RequestAlreadyConsumedException()
        !typeInfo.type.isInstance(transformed) -> throw CannotTransformContentToTypeException(typeInfo.kotlinType!!)
    }

    @Suppress("UNCHECKED_CAST")
    return transformed as T
}

/**
 * Receives content for this request.
 * @param type instance of `KClass` specifying type to be received.
 * @return instance of [T] received from this call, or `null` if content cannot be transformed to the requested type..
 */
public suspend fun <T : Any> ApplicationCall.receiveOrNull(typeInfo: TypeInfo): T? {
    return try {
        receive(typeInfo)
    } catch (cause: ContentTransformationException) {
        application.log.debug("Conversion failed, null returned", cause)
        null
    }
}

/**
 * Receives content for this request.
 * @param type instance of `KClass` specifying type to be received.
 * @return instance of [T] received from this call, or `null` if content cannot be transformed to the requested type..
 */
public suspend fun <T : Any> ApplicationCall.receiveOrNull(type: KClass<T>): T? {
    return try {
        receive(type)
    } catch (cause: ContentTransformationException) {
        application.log.debug("Conversion failed, null returned", cause)
        null
    }
}

/**
 * Receives incoming content for this call as [String].
 * @return text received from this call.
 * @throws ContentTransformationException when content cannot be transformed to the [String].
 */
@Suppress("NOTHING_TO_INLINE")
public suspend inline fun ApplicationCall.receiveText(): String = receive()

/**
 * Receives channel content for this call.
 * @return instance of [ByteReadChannel] to read incoming bytes for this call.
 * @throws ContentTransformationException when content cannot be transformed to the [ByteReadChannel].
 */
@Suppress("NOTHING_TO_INLINE")
public suspend inline fun ApplicationCall.receiveChannel(): ByteReadChannel = receive()

/**
 * Receives multipart data for this call.
 * @return instance of [MultiPartData].
 * @throws ContentTransformationException when content cannot be transformed to the [MultiPartData].
 */
@Suppress("NOTHING_TO_INLINE")
public suspend inline fun ApplicationCall.receiveMultipart(): MultiPartData = receive()

/**
 * Receives form parameters for this call.
 * @return instance of [Parameters].
 * @throws ContentTransformationException when content cannot be transformed to the [Parameters].
 */
@Suppress("NOTHING_TO_INLINE")
public suspend inline fun ApplicationCall.receiveParameters(): Parameters = receive()

/**
 * Thrown when content cannot be transformed to the desired type.
 */
public typealias ContentTransformationException = io.ktor.server.plugins.ContentTransformationException

/**
 * This object is attached to an [ApplicationCall] with [DoubleReceivePreventionTokenKey] when
 * the receive function is invoked. It is used to detect double receive invocation
 * that causes [RequestAlreadyConsumedException] to be thrown unless [DoubleReceive] plugin installed.
 */
private object DoubleReceivePreventionToken

private val DoubleReceivePreventionTokenKey = AttributeKey<DoubleReceivePreventionToken>("DoubleReceivePreventionToken")

/**
 * Thrown when a request body has already been received.
 * Usually it is caused by double [ApplicationCall.receive] invocation.
 */
public class RequestAlreadyConsumedException : IllegalStateException(
    "Request body has already been consumed (received)."
)