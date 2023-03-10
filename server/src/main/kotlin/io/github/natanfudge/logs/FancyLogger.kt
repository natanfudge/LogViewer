package io.github.natanfudge.logs

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import kotlinx.coroutines.*
import java.nio.file.Path
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*
import kotlin.concurrent.schedule
import kotlin.io.path.createDirectories

public class LoggingCredentials(
    internal val username: CharArray,
    internal val password: CharArray
)

public class FancyLogger(
    private val logToConsole: Boolean,
    logsDir: Path,
    private val credentials: LoggingCredentials
) {
    private val boxStore = MyObjectBox.builder()
        .directory(logsDir.toFile())
        .build()

    @PublishedApi
    internal val logsBox: Box<ObjectBoxLogEvent> = boxStore.boxFor<ObjectBoxLogEvent>()

    init {
        logsDir.createDirectories()
    }

    context(Application)
    public fun install() {
        installAuthentication(credentials)
        scheduleOldLogDeletion()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun scheduleOldLogDeletion() {
        val timer = Timer()
        // Runs once every day
        timer.schedule(DayMs) {
            GlobalScope.launch(Dispatchers.IO) {
                startCall("logViewer_cleanup") {
                    logData("Time") { Instant.now() }
                    evictOld()
                }
            }
        }.apply {
            // Run once at startup
            run()
        }
    }

    context(LogContext)
    private fun evictOld() {
        // Query for all logs more than a month old and remove them
        val monthAgo = ZonedDateTime.now().minusMonths(1).toEpochSecond() * 1000
        logsBox.query(ObjectBoxLogEvent_.startTime.less(monthAgo)).build().use {
            logData("Log Size") { (boxStore.sizeOnDisk() / 1000).toString() + "KB" }
            val removed = it.remove()
            logData("Logs Removed") { removed }
        }
    }

    context(Routing)
    public fun route() {
        routeAuthentication()
        authenticate(AuthSessionName) {
            routeApi(logsBox)
            routeReactApp()
        }
    }

    private fun Route.routeReactApp() {
        singlePageApplication {
            useResources = true
            filesPath = "__log_viewer__/static"
            defaultPage = "index.html"
            applicationRoute = "/logs"
        }
    }


    public inline fun <T> startCall(name: String, call: LogContext.() -> T): T {
        return startCallWithContextAsParam(name, call)
    }

    // Context receivers are bugging out, so we pass LogContext as a parameter for some use cases
    // (try removing this with K2)
    public inline fun <T> startCallWithContextAsParam(name: String, call: (LogContext) -> T): T {
        val context = LogContext(name.removePrefix("/").replace("/", "_"), Instant.now())
        val value = try {
            call(context)
        } catch (e: Throwable) {
            context.logError(e) { "Unexpected error handling '$name'" }
            throw e
        } finally {
            storeLog(context)
        }
        return value
    }

    @PublishedApi
    internal fun storeLog(context: LogContext) {
        val log = context.buildLog()
        if (log.logs.isNotEmpty()) {
            logsBox.put(log.toObjectBox())
            if (logToConsole) ConsoleLogRenderer.render(log)
        }
    }

}


internal const val LoginPath = "/_log_viewer/login"
private const val DayMs = 1000 * 60 * 60 * 24L

public class LogContext(private val name: String, private val startTime: Instant) {
    @PublishedApi
    internal val logDetails: MutableList<LogLine> = mutableListOf()

    public inline fun logInfo(message: () -> String) {
        logDetails.add(LogLine.Message.Normal(message(), Instant.now(), LogLine.Severity.Info))
    }

    public inline fun logWarn(message: () -> String) {
        logDetails.add(LogLine.Message.Normal(message(), Instant.now(), LogLine.Severity.Warn))
    }

    public inline fun logError(exception: Throwable, message: () -> String) {
        logDetails.add(LogLine.Message.Error(message(), Instant.now(), exception.toSerializable()))
    }

    public inline fun logData(key: String, value: () -> Any) {
        logDetails.add(LogLine.Detail(key, value().toString()))
    }

    @PublishedApi
    internal fun buildLog(): LogEvent = LogEvent(
        name, startTime = startTime, endTime = Instant.now(), logDetails
    )
}