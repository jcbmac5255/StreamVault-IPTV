package com.nexus.iptv.domain.repository

import com.nexus.iptv.domain.model.ContentType
import com.nexus.iptv.domain.model.RemoteFavorite
import com.nexus.iptv.domain.model.Result

interface RemoteFavoriteRepository {
    suspend fun fetchFavorites(username: String): Result<List<RemoteFavorite>>
    suspend fun addFavorite(username: String, contentType: ContentType, externalId: String): Result<Unit>
    suspend fun removeFavorite(username: String, contentType: ContentType, externalId: String): Result<Unit>
}
