package com.nexus.iptv.domain.model

data class RemoteFavorite(
    val recordId: String,
    val username: String,
    val contentType: ContentType,
    val externalId: String,
    val addedAt: Long
)
