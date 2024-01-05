package io.ktor.utils.io.core

import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.internal.*

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "DEPRECATION")
public fun Input.readShort(): Short {
    return readPrimitive(2, { memory, index -> memory.loadShortAt(index) }, { readShortFallback() })
}

@Suppress("DEPRECATION")
private fun Input.readShortFallback(): Short {
    return readPrimitiveFallback(2) { it.readShort() }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "DEPRECATION")
public fun Input.readInt(): Int {
    return readPrimitive(4, { memory, index -> memory.loadIntAt(index) }, { readIntFallback() })
}

@Suppress("DEPRECATION")
private fun Input.readIntFallback(): Int {
    return readPrimitiveFallback(4) { it.readInt() }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "DEPRECATION")
public fun Input.readLong(): Long {
    return readPrimitive(8, { memory, index -> memory.loadLongAt(index) }, { readLongFallback() })
}

@Suppress("DEPRECATION")
private fun Input.readLongFallback(): Long {
    return readPrimitiveFallback(8) { it.readLong() }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "DEPRECATION")
public fun Input.readFloat(): Float {
    return readPrimitive(4, { memory, index -> memory.loadFloatAt(index) }, { readFloatFallback() })
}

@Suppress("DEPRECATION")
public fun Input.readFloatFallback(): Float {
    return readPrimitiveFallback(4) { it.readFloat() }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "DEPRECATION")
public fun Input.readDouble(): Double {
    return readPrimitive(8, { memory, index -> memory.loadDoubleAt(index) }, { readDoubleFallback() })
}

@Suppress("DEPRECATION")
public fun Input.readDoubleFallback(): Double {
    return readPrimitiveFallback(8) { it.readDouble() }
}

@Suppress("DEPRECATION")
private inline fun <R> Input.readPrimitive(size: Int, main: (Memory, Int) -> R, fallback: () -> R): R {
    if (headRemaining > size) {
        val index = headPosition
        headPosition = index + size
        return main(headMemory, index)
    }

    return fallback()
}

@Suppress("DEPRECATION")
private inline fun <R> Input.readPrimitiveFallback(size: Int, read: (Buffer) -> R): R {
    val head = prepareReadFirstHead(size) ?: prematureEndOfStream(size)
    val value = read(head)
    completeReadHead(head)
    return value
}
