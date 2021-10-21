/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.shared.serializaion.jackson

import io.ktor.http.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import java.nio.charset.*

public data class SerializedData(
    val data: ByteReadChannel,
    val dataLength: Long
)

public interface BaseConverter {
    public suspend fun serialize(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any
    ): SerializedData?
}
