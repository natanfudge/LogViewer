package io.github.natanfudge.logs

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.objectbox.Box
import io.objectbox.query.PropertyQueryCondition
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.ceil

context(Routing)
internal fun routeApi(box: Box<ObjectBoxLogEvent>) {
    get("/__log_viewer__/endpoints") {
        val endpoints = box.query().build().use {
            it.property(ObjectBoxLogEvent_.name).distinct().findStrings()
        }.toList()

        addCorsHeader()

        call.respondText(json.encodeToString(ListSerializer(String.serializer()), endpoints))
    }

    get("/__log_viewer__/logs") {
        addCorsHeader()
        val endpoint = call.parameters["endpoint"]
        val dayString = call.parameters["day"]
        val page = call.parameters["page"]
        if (endpoint == null || dayString == null || page == null) {
            call.respondText("Missing parameter", status = HttpStatusCode.BadRequest)
            return@get
        }

        val pageInt = page.toIntOrNull() ?: run {
            call.respondText("Malformed page", status = HttpStatusCode.BadRequest)
            return@get
        }

        val day = try {
            json.decodeFromString(Day.serializer(), dayString)
        } catch (e: SerializationException) {
            e.printStackTrace()
            call.respondText("Malformed day", status = HttpStatusCode.BadRequest)
            return@get
        }

        // Get logs with the specified endpoint in the specified day
        val logs: List<ObjectBoxLogEvent> = box.query(
            ObjectBoxLogEvent_.name.equal(endpoint)
                .and(inDay(day))
        ).build().use { it.find() }

        val response = LogResponse(
            pageCount = ceil(logs.size.toDouble() / PageSize).toInt(),
            // Return only PageSize items, and skip pages before the requested page
            logs = logs.sortedByDescending { it.startTime }.drop(pageInt * PageSize).take(PageSize)
                .map { it.toLogEvent() }
        )


        call.respondText(json.encodeToString(LogResponse.serializer(), response))
    }
}


private fun PipelineContext<Unit, ApplicationCall>.addCorsHeader() {
    // This makes it easier to test out the api in development since the React app runs in port 3000
    call.response.header("Access-Control-Allow-Origin", "*")
}

private fun inDay(day: Day): PropertyQueryCondition<ObjectBoxLogEvent> {
//    val calendar = Calendar.getInstance()
//    calendar.set(day.year, day.month, day.day, 0, 0, 0) // set to the start of the day
//    val startOfDay = calendar.time.time
//    calendar.set(day.year, day.month, day.day, 23, 59, 59) // set to the end of the day
//    val endOfDay = calendar.time.time

    val startOfDay = ZonedDateTime.of(day.year, day.month, day.day, 0, 0, 0, 0, GMT)
        .toInstant().toEpochMilli()
    val endOfDay = ZonedDateTime.of(day.year, day.month, day.day, 23, 59, 59, 999_999_999, GMT)
        .toInstant().toEpochMilli()

    return ObjectBoxLogEvent_.startTime.between(startOfDay, endOfDay)
}

private val GMT = ZoneId.of("GMT")

//#1: 'amar', 1676755225068 -> 1676755225071 (1381 bytes)
private const val PageSize = 18


private val json = Json { encodeDefaults = true }

@Serializable
internal data class Day(val day: Int, val month: Int, val year: Int)

@Serializable
internal data class LogResponse(
    val pageCount: Int,
    val logs: List<LogEvent>
)