package com.nexus.iptv.data.repository

import android.util.Log
import com.nexus.iptv.domain.model.ContentType
import com.nexus.iptv.domain.model.RemoteFavorite
import com.nexus.iptv.domain.model.Result
import com.nexus.iptv.domain.repository.RemoteFavoriteRepository
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class RemoteFavoriteRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) : RemoteFavoriteRepository {

    override suspend fun fetchFavorites(username: String): Result<List<RemoteFavorite>> = withContext(Dispatchers.IO) {
        val normalized = username.trim().lowercase()
        if (normalized.isEmpty()) return@withContext Result.success(emptyList())

        val filter = URLEncoder.encode("username=\"$normalized\"", "UTF-8")
        val request = Request.Builder()
            .url("$BASE_URL/api/collections/$COLLECTION/records?perPage=500&filter=$filter")
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
                items.mapNotNull { parseRecord(it.jsonObject) }
            }
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                Log.w(TAG, "Failed to fetch remote favorites", error)
                Result.error(error.message ?: "Failed to fetch favorites", error)
            }
        )
    }

    override suspend fun addFavorite(
        username: String,
        contentType: ContentType,
        externalId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val normalized = username.trim().lowercase()
        if (normalized.isEmpty() || externalId.isBlank()) {
            return@withContext Result.success(Unit)
        }

        val payload = buildJsonObject {
            put("username", normalized)
            put("content_type", contentType.name)
            put("external_id", externalId)
            put("added_at", System.currentTimeMillis())
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/collections/$COLLECTION/records")
            .header("Accept", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA))
            .build()

        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                // 400 with "validation_not_unique" means the row already exists — treat as success.
                if (!response.isSuccessful && response.code != 400) {
                    throw IOException("HTTP ${response.code} adding favorite")
                }
            }
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { error ->
                Log.w(TAG, "Failed to add remote favorite", error)
                Result.error(error.message ?: "Failed to add favorite", error)
            }
        )
    }

    override suspend fun removeFavorite(
        username: String,
        contentType: ContentType,
        externalId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val normalized = username.trim().lowercase()
        if (normalized.isEmpty() || externalId.isBlank()) {
            return@withContext Result.success(Unit)
        }

        // Find the record ID for the (username, type, external_id) triple, then DELETE it.
        val filter = URLEncoder.encode(
            "username=\"$normalized\" && content_type=\"${contentType.name}\" && external_id=\"$externalId\"",
            "UTF-8"
        )
        val lookupRequest = Request.Builder()
            .url("$BASE_URL/api/collections/$COLLECTION/records?perPage=1&filter=$filter")
            .header("Accept", "application/json")
            .build()

        runCatching {
            val recordIds = okHttpClient.newCall(lookupRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code} looking up favorite")
                }
                val bodyText = response.body?.string().orEmpty()
                val items = json.parseToJsonElement(bodyText).jsonObject["items"] as? JsonArray
                items?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull } ?: emptyList()
            }
            for (id in recordIds) {
                val deleteRequest = Request.Builder()
                    .url("$BASE_URL/api/collections/$COLLECTION/records/$id")
                    .delete()
                    .build()
                okHttpClient.newCall(deleteRequest).execute().use { response ->
                    if (!response.isSuccessful && response.code != 404) {
                        throw IOException("HTTP ${response.code} deleting favorite $id")
                    }
                }
            }
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { error ->
                Log.w(TAG, "Failed to remove remote favorite", error)
                Result.error(error.message ?: "Failed to remove favorite", error)
            }
        )
    }

    private fun parseRecord(obj: JsonObject): RemoteFavorite? {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val username = obj["username"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
        val contentTypeRaw = obj["content_type"]?.jsonPrimitive?.contentOrNull ?: return null
        val contentType = runCatching { ContentType.valueOf(contentTypeRaw.uppercase()) }.getOrNull() ?: return null
        val externalId = obj["external_id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
        val addedAt = obj["added_at"]?.jsonPrimitive?.longOrNull ?: 0L

        return RemoteFavorite(
            recordId = id,
            username = username,
            contentType = contentType,
            externalId = externalId,
            addedAt = addedAt
        )
    }

    private companion object {
        const val TAG = "RemoteFavoriteRepo"
        const val BASE_URL = "https://nexus.nexgrid.cc"
        const val COLLECTION = "user_favorites"
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
