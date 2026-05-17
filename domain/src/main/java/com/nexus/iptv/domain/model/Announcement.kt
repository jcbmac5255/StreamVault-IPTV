package com.nexus.iptv.domain.model

data class Announcement(
    val id: String,
    val title: String,
    val body: String,
    val imageUrl: String?,
    val audienceType: AnnouncementAudienceType,
    val audienceUsernames: List<String>,
    val priority: Int,
    val startsAt: Long?,
    val expiresAt: Long?,
    val active: Boolean,
    val updatedAt: Long
)

enum class AnnouncementAudienceType {
    GLOBAL,
    SPECIFIC
}
