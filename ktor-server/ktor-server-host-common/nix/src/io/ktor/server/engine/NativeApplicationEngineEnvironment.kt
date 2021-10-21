/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.engine

import io.ktor.events.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.util.logging.Logger
import kotlin.coroutines.*

public class NativeApplicationEngineEnvironment(
    override val log: Logger,
    override val config: ApplicationConfig,
    override val connectors: MutableList<EngineConnectorConfig>,
    private val modules: MutableList<Application.() -> Unit>,
    private val watchPaths: List<String>,
    override val parentCoroutineContext: CoroutineContext,
    override val rootPath: String,
    override val developmentMode: Boolean
) : ApplicationEngineEnvironment {

    override val monitor: Events
        get() = TODO("Not yet implemented")
    override val application: Application
        get() = TODO("Not yet implemented")

    override fun start() {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

}
