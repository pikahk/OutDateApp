package ru.pikahk.outdateapp.ui.family

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.pikahk.outdateapp.data.dao.CategoryDao
import ru.pikahk.outdateapp.data.dao.ItemDao
import ru.pikahk.outdateapp.data.model.Category
import ru.pikahk.outdateapp.data.model.Item
import ru.pikahk.outdateapp.data.model.Session
import ru.pikahk.outdateapp.data.repository.CategoryRepository
import ru.pikahk.outdateapp.data.repository.FakeSyncDao
import ru.pikahk.outdateapp.data.repository.FamilyRepository
import ru.pikahk.outdateapp.data.repository.ItemRepository
import ru.pikahk.outdateapp.data.repository.SyncRepository
import ru.pikahk.outdateapp.ui.auth.FakeSessionStore
import ru.pikahk.outdateapp.ui.auth.json
import ru.pikahk.outdateapp.ui.auth.mockApi

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val family = """{"id":"f1","name":"Квартира","isOwner":true,""" +
        """"members":[{"id":"u1","name":"Миша","isOwner":true,"isMe":true,"joinedAt":0}]}"""

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(routes: Map<String, Pair<String, HttpStatusCode>>): FamilyViewModel {
        val store = FakeSessionStore(Session("token", "misha@mail.ru", emailVerified = true))
        val api = mockApi(dispatcher) { request ->
            val (body, status) = checkNotNull(routes[request.url.encodedPath]) { request.url }
            json(body, status)
        }
        return FamilyViewModel(
            session = store.session,
            families = FamilyRepository(api, store, SyncRepository(api, store, FakeSyncDao())),
            items = ItemRepository(NoItems),
            categories = CategoryRepository(NoCategories)
        )
    }

    @Test
    fun `код приглашения загружается сразу после создания группы`() = runTest(dispatcher) {
        val viewModel = viewModel(
            mapOf(
                "/family" to (family to HttpStatusCode.Created),
                "/family/invite" to ("""{"code":"K7PQX4","expiresAt":0}""" to HttpStatusCode.OK)
            )
        )
        viewModel.create("Квартира", "Миша") { viewModel.loadInvite() }
        advanceUntilIdle()
        assertEquals("K7PQX4", viewModel.invite.value?.code)
        assertFalse(viewModel.busy.value)
    }

    @Test
    fun `при ошибке дальше не переходит`() = runTest(dispatcher) {
        val viewModel = viewModel(
            mapOf("/family/join" to ("""{"error":"invalid_code"}""" to HttpStatusCode.BadRequest))
        )
        var done = false
        viewModel.join("AAAAAA", "Аня") { done = true }
        advanceUntilIdle()
        assertFalse(done)
        assertEquals(FamilyError.INVALID_CODE, viewModel.error.value)
        assertFalse(viewModel.busy.value)
        assertNull(viewModel.invite.value)
    }

    private object NoItems : ItemDao {
        override fun observeAll(): Flow<List<Item>> = flowOf(emptyList())
        override fun observeFamily(familyId: String): Flow<List<Item>> = flowOf(emptyList())
        override fun observeWithFamily(): Flow<List<Item>> = flowOf(emptyList())
        override fun observeByCategory(categoryId: String): Flow<List<Item>> = flowOf(emptyList())
        override fun observeDeletedSince(since: Long): Flow<List<Item>> = flowOf(emptyList())
        override fun observeById(id: String): Flow<Item?> = flowOf(null)
        override suspend fun findById(id: String): Item? = null
        override suspend fun upsert(item: Item) = Unit
        override suspend fun upsertAll(items: List<Item>) = Unit
        override suspend fun markDeleted(id: String, now: Long) = Unit
        override suspend fun deleteForever(item: Item) = Unit
    }

    private object NoCategories : CategoryDao {
        override fun observeAll(): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun findById(id: String): Category? = null
        override suspend fun upsert(category: Category) = Unit
        override suspend fun upsertAll(categories: List<Category>) = Unit
        override suspend fun insertIfAbsent(categories: List<Category>) = Unit
        override suspend fun markDeleted(id: String, now: Long) = Unit
        override suspend fun detachItems(id: String, now: Long) = Unit
        override suspend fun detachProducts(id: String, now: Long) = Unit
    }
}
