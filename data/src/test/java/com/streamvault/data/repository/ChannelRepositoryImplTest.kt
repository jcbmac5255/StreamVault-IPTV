package com.streamvault.data.repository

import com.google.common.truth.Truth.assertThat
import com.streamvault.data.local.dao.CategoryDao
import com.streamvault.data.local.dao.ChannelDao
import com.streamvault.data.local.entity.CategoryCount
import com.streamvault.data.local.entity.CategoryEntity
import com.streamvault.data.preferences.PreferencesRepository
import com.streamvault.data.remote.xtream.XtreamStreamUrlResolver
import com.streamvault.domain.manager.ParentalControlManager
import com.streamvault.domain.model.ContentType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ChannelRepositoryImplTest {

    private val channelDao: ChannelDao = mock()
    private val categoryDao: CategoryDao = mock()
    private val preferencesRepository: PreferencesRepository = mock()
    private val parentalControlManager: ParentalControlManager = mock()
    private val xtreamStreamUrlResolver: XtreamStreamUrlResolver = mock()

    @Test
    fun `getCategories uses grouped counts without loading all channels`() = runTest {
        whenever(categoryDao.getByProviderAndType(7L, ContentType.LIVE.name)).thenReturn(
            flowOf(
                listOf(
                    categoryEntity(id = 10L, name = "News"),
                    categoryEntity(id = 20L, name = "Sports")
                )
            )
        )
        whenever(channelDao.getGroupedCategoryCounts(7L)).thenReturn(
            flowOf(
                listOf(
                    CategoryCount(categoryId = 10L, item_count = 4),
                    CategoryCount(categoryId = 20L, item_count = 6)
                )
            )
        )
        whenever(preferencesRepository.parentalControlLevel).thenReturn(flowOf(0))
        whenever(parentalControlManager.unlockedCategoriesForProvider(7L)).thenReturn(flowOf(emptySet()))

        val repository = createRepository()

        val result = repository.getCategories(7L).first()

        assertThat(result.map { it.name to it.count }).containsExactly(
            "All Channels" to 10,
            "News" to 4,
            "Sports" to 6
        ).inOrder()
        verify(channelDao).getGroupedCategoryCounts(7L)
        verify(channelDao, never()).getByProvider(any())
    }

    @Test
    fun `getCategories keeps unlocked protected category visible at hidden level`() = runTest {
        whenever(categoryDao.getByProviderAndType(7L, ContentType.LIVE.name)).thenReturn(
            flowOf(
                listOf(
                    categoryEntity(id = 10L, name = "Kids"),
                    categoryEntity(id = 20L, name = "Adults", isUserProtected = true)
                )
            )
        )
        whenever(channelDao.getGroupedCategoryCounts(7L)).thenReturn(
            flowOf(
                listOf(
                    CategoryCount(categoryId = 10L, item_count = 3),
                    CategoryCount(categoryId = 20L, item_count = 5)
                )
            )
        )
        whenever(preferencesRepository.parentalControlLevel).thenReturn(flowOf(2))
        whenever(parentalControlManager.unlockedCategoriesForProvider(eq(7L))).thenReturn(flowOf(setOf(20L)))

        val repository = createRepository()

        val result = repository.getCategories(7L).first()

        assertThat(result.map { it.name to it.count }).containsExactly(
            "All Channels" to 8,
            "Kids" to 3,
            "Adults" to 5
        ).inOrder()
        assertThat(result.first { it.id == 20L }.isUserProtected).isFalse()
    }

    private fun createRepository() = ChannelRepositoryImpl(
        channelDao = channelDao,
        categoryDao = categoryDao,
        preferencesRepository = preferencesRepository,
        parentalControlManager = parentalControlManager,
        xtreamStreamUrlResolver = xtreamStreamUrlResolver
    )

    private fun categoryEntity(
        id: Long,
        name: String,
        isUserProtected: Boolean = false
    ) = CategoryEntity(
        categoryId = id,
        name = name,
        type = ContentType.LIVE,
        providerId = 7L,
        isUserProtected = isUserProtected
    )
}
