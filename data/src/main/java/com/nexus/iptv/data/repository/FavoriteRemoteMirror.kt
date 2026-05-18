package com.nexus.iptv.data.repository

import com.nexus.iptv.domain.model.ContentType
import com.nexus.iptv.domain.model.ProviderType
import com.nexus.iptv.domain.repository.ChannelRepository
import com.nexus.iptv.domain.repository.MovieRepository
import com.nexus.iptv.domain.repository.ProviderRepository
import com.nexus.iptv.domain.repository.RemoteFavoriteRepository
import com.nexus.iptv.domain.repository.SeriesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Pushes local global-favorite mutations to the PocketBase user_favorites collection
 * so favorites survive uninstall / sign-out for Xtream-backed providers. Mirrors only
 * fire when the local mutation is on the *global* favorite (groupId == null); virtual
 * groups remain a local-only concept.
 */
@Singleton
class FavoriteRemoteMirror @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val channelRepository: ChannelRepository,
    private val movieRepository: MovieRepository,
    private val seriesRepository: SeriesRepository,
    private val remoteFavoriteRepository: RemoteFavoriteRepository,
    private val scope: CoroutineScope
) {

    fun pushAdd(providerId: Long, contentId: Long, contentType: ContentType) {
        scope.launch { sync(providerId, contentId, contentType, add = true) }
    }

    fun pushRemove(providerId: Long, contentId: Long, contentType: ContentType) {
        scope.launch { sync(providerId, contentId, contentType, add = false) }
    }

    private suspend fun sync(providerId: Long, contentId: Long, contentType: ContentType, add: Boolean) {
        val provider = providerRepository.getProvider(providerId) ?: return
        if (provider.type != ProviderType.XTREAM_CODES) return
        val username = provider.username.trim().lowercase()
        if (username.isEmpty()) return

        val externalId = resolveExternalId(contentType, contentId) ?: return
        if (add) {
            remoteFavoriteRepository.addFavorite(username, contentType, externalId)
        } else {
            remoteFavoriteRepository.removeFavorite(username, contentType, externalId)
        }
    }

    private suspend fun resolveExternalId(contentType: ContentType, contentId: Long): String? {
        return when (contentType) {
            ContentType.LIVE -> {
                val streamId = channelRepository.getChannel(contentId)?.streamId ?: 0L
                streamId.takeIf { it > 0L }?.toString()
            }
            ContentType.MOVIE -> {
                val streamId = movieRepository.getMovie(contentId)?.streamId ?: 0L
                streamId.takeIf { it > 0L }?.toString()
            }
            ContentType.SERIES -> {
                seriesRepository.getSeriesById(contentId)?.providerSeriesId?.takeIf { it.isNotBlank() }
            }
            ContentType.SERIES_EPISODE -> null
        }
    }
}
