package app.lifeguide.core.data

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun httpEngine(): HttpClientEngineFactory<*> = OkHttp
