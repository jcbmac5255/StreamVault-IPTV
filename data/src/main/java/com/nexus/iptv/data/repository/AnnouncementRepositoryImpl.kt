package com.nexus.iptv.data.repository

import android.util.Log
import com.nexus.iptv.domain.model.Announcement
import com.nexus.iptv.domain.model.AnnouncementAudienceType
import com.nexus.iptv.domain.model.Result
import com.nexus.iptv.domain.repository.AnnouncementRepository
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class AnnouncementRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) : AnnouncementRepository {

    override suspend fun fetchAnnouncements(): Result<List<Announcement>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/api/collections/$COLLECTION/records?perPage=$PAGE_SIZE&sort=-priority,-updated")
            .header("Accept", "application/json")
            .build()

        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code} from PocketBase")
                }
                val bodyText = response.body?.string().orEmpty()
                val root = json.parseToJsonElement(bodyText).jsonObject
                val items = root["items"] as? JsonArray ?: return@use emptyList()
                items.mapNotNull { element -> parseRecord(element.jsonObject) }
            }
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                Log.w(TAG, "Failed to fetch announcements", error)
                Result.error(error.message ?: "Failed to fetch announcements", error)
            }
        )
    }

    private fun parseRecord(obj: JsonObject): Announcement? {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val body = obj["body"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (title.isBlank() && body.isBlank()) return null

        val imageUrl = obj["image_url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val audienceType = when (obj["audience_type"]?.jsonPrimitive?.contentOrNull?.lowercase()) {
            "specific" -> AnnouncementAudienceType.SPECIFIC
            else -> AnnouncementAudienceType.GLOBAL
        }
        val audienceUsernames = obj["audience_usernames"]?.jsonPrimitive?.contentOrNull
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val priority = obj["priority"]?.jsonPrimitive?.intOrNull ?: 0
        val active = obj["active"]?.jsonPrimitive?.booleanOrNull ?: true
        val startsAt = parseTimestamp(obj["starts_at"]?.jsonPrimitive?.contentOrNull)
        val expiresAt = parseTimestamp(obj["expires_at"]?.jsonPrimitive?.contentOrNull)
        val updatedAt = parseTimestamp(obj["updated"]?.jsonPrimitive?.contentOrNull) ?: 0L

        return Announcement(
            id = id,
            title = title,
            body = body,
            imageUrl = imageUrl,
            audienceType = audienceType,
            audienceUsernames = audienceUsernames,
            priority = priority,
            startsAt = startsAt,
            expiresAt = expiresAt,
            active = active,
            updatedAt = updatedAt
        )
    }

    private fun parseTimestamp(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            // PocketBase emits timestamps as "2026-05-17 14:32:00.000Z" (space, not T)
            val normalized = raw.replace(' ', 'T')
            OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    private companion object {
        const val TAG = "AnnouncementRepo"
        const val BASE_URL = "https://nexus.nexgrid.cc"
        const val COLLECTION = "announcements"
        const val PAGE_SIZE = 50
    }
}
