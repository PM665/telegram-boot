package io.github.pm665.telegramboot.admin.bot

import com.fasterxml.jackson.databind.json.JsonMapper
import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class TelegramApiDebugInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val method = chain.request().url.pathSegments.lastOrNull().orEmpty()
        if (!observes(method)) return chain.proceed(chain.request())

        val startedAt = System.nanoTime()
        try {
            val response = chain.proceed(chain.request())
            val body = runCatching { json.readTree(response.peekBody(MAX_RESPONSE_BYTES).string()) }.getOrNull()
            log.info(
                "Telegram API {} status={} ok={} errorCode={} messageId={} richBlocks={} durationMs={}",
                method,
                response.code,
                body?.path("ok")?.takeIf { !it.isMissingNode }?.asBoolean(),
                body?.path("error_code")?.takeIf { !it.isMissingNode }?.asInt(),
                body?.path("result")?.path("message_id")?.takeIf { !it.isMissingNode }?.asInt(),
                body?.path("result")?.path("rich_message")?.path("blocks")?.takeIf { it.isArray }?.size(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt),
            )
            return response
        } catch (exception: IOException) {
            log.warn(
                "Telegram API {} failed after {}ms: {}",
                method,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt),
                exception.javaClass.simpleName,
            )
            throw exception
        }
    }

    companion object {
        private const val MAX_RESPONSE_BYTES = 8L * 1024L
        private val json = JsonMapper.builder().build()
        private val log = LoggerFactory.getLogger(TelegramApiDebugInterceptor::class.java)
        private val observedMethods = setOf("sendRichMessage", "editMessageText")

        internal fun observes(method: String): Boolean = method in observedMethods
    }
}
