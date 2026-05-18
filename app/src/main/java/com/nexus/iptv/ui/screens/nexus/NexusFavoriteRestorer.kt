package com.nexus.iptv.ui.screens.nexus

import android.util.Log
import com.nexus.iptv.data.local.dao.ChannelDao
import com.nexus.iptv.data.local.dao.MovieDao
import com.nexus.iptv.data.local.dao.SeriesDao
import com.nexus.iptv.domain.model.ContentType
import com.nexus.iptv.domain.model.Result
import com.nexus.iptv.domain.repository.FavoriteRepository
import com.nexus.iptv.domain.repository.RemoteFavoriteRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pulls server-side favorites from PocketBase for the just-signed-in user and inserts
 * them into the local favorites table. Maps remote external IDs (xtream stream_id /
 * series_id) back to the local Channel/Movie/Series rows that were just indexed.
 *
 * Idempotent: rows that already exist locally are skipped (FavoriteRepository.addFavorite
 * is a no-op when the favorite is already present).
 */
@Singleton
class NexusFavoriteRestorer @Inject constructor(
    private val remoteFavoriteRepository: RemoteFavoriteRepository,
    private val favoriteRepository: FavoriteRepository,
    private val channelDao: ChannelDao,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao
) {

    suspend fun restoreFor(providerId: Long, username: String): Int {
        val normalized = username.trim().lowercase()
        if (normalized.isEmpty()) return 0

        val remoteFavorites = when (val result = remoteFavoriteRepository.fetchFavorites(normalized)) {
            is Result.Success -> result.data
            is Result.Error -> {
                Log.w(TAG, "Skipping favorites restore: ${result.message}")
                return 0
            }
            Result.Loading -> return 0
        }

        var restored = 0
        for (remote in remoteFavorites) {
            val localContentId = resolveLocalContentId(providerId, remote.contentType, remote.externalId)
                ?: continue
            val addResult = favoriteRepository.addFavorite(
                providerId = providerId,
                contentId = localContentId,
                contentType = remote.contentType,
                groupId = null,
                // We just pulled these rows from PocketBase — pushing them straight back
                // is wasted I/O. The unique server-side index would dedup anyway, but
                // skipping the round-trip is cleaner.
                syncToRemote = false
            )
            if (addResult is Result.Success) restored++
        }
        if (restored > 0) {
            Log.i(TAG, "Restored $restored favorite(s) for $normalized")
        }
        return restored
    }

    private suspend fun resolveLocalContentId(
        providerId: Long,
        contentType: ContentType,
        externalId: String
    ): Long? {
        return when (contentType) {
            ContentType.LIVE -> {
                val streamId = externalId.toLongOrNull() ?: return null
                channelDao.findIdByStreamId(providerId, streamId)
            }
            ContentType.MOVIE -> {
                val streamId = externalId.toLongOrNull() ?: return null
                movieDao.getByStreamId(providerId, streamId)?.id
            }
            ContentType.SERIES -> {
                seriesDao.getByProviderSeriesId(providerId, externalId)?.id
            }
            ContentType.SERIES_EPISODE -> null
        }
    }

    private companion object {
        const val TAG = "NexusFavoriteRestorer"
    }
}
