package ru.pikahk.outdate.server.routes

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import ru.pikahk.outdate.server.ServerTest
import ru.pikahk.outdate.server.family.FamilyDto
import ru.pikahk.outdate.server.family.FamilyState
import ru.pikahk.outdate.server.family.FamilySyncRequest
import ru.pikahk.outdate.server.family.FamilySyncResponse
import ru.pikahk.outdate.server.family.InviteResponse
import ru.pikahk.outdate.server.field
import ru.pikahk.outdate.server.serverTest
import ru.pikahk.outdate.server.sync.ItemDto
import ru.pikahk.outdate.server.sync.SyncRequest
import ru.pikahk.outdate.server.sync.SyncResponse
import ru.pikahk.outdate.server.uniqueEmail

class FamilyRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun item(name: String = "Сыр") = ItemDto(
        id = UUID.randomUUID().toString(),
        name = name,
        categoryId = "food",
        expiresAt = LocalDate.of(2026, 11, 1),
        notifyDaysBefore = 3,
        createdAt = 500,
        updatedAt = 1_000
    )

    private suspend fun ServerTest.family(token: String): FamilyDto? =
        json.decodeFromString<FamilyState>(client.get("/family") { bearerAuth(token) }.bodyAsText()).family

    private suspend fun ServerTest.create(token: String, name: String = "Квартира"): FamilyDto {
        val response = post("/family", "name" to name, "memberName" to "Миша", token = token)
        assertEquals(HttpStatusCode.Created, response.status)
        return json.decodeFromString(response.bodyAsText())
    }

    private suspend fun ServerTest.invite(token: String): String {
        val response = post("/family/invite", token = token)
        assertEquals(HttpStatusCode.OK, response.status)
        return json.decodeFromString<InviteResponse>(response.bodyAsText()).code
    }

    private suspend fun ServerTest.join(token: String, code: String, member: String = "Аня"): HttpResponse =
        post("/family/join", "code" to code, "memberName" to member, token = token)

    private suspend fun ServerTest.sendFamilySync(token: String, request: FamilySyncRequest): HttpResponse =
        client.post("/family/sync") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

    private suspend fun ServerTest.familySync(
        token: String,
        request: FamilySyncRequest = FamilySyncRequest()
    ): FamilySyncResponse {
        val response = sendFamilySync(token, request)
        assertEquals(HttpStatusCode.OK, response.status)
        return json.decodeFromString(response.bodyAsText())
    }

    private suspend fun ServerTest.removeMember(token: String, id: String): HttpResponse =
        client.delete("/family/members/$id") { bearerAuth(token) }

    private suspend fun ServerTest.twoMembers(): Pair<String, String> {
        val owner = registerVerified()
        create(owner)
        val member = registerVerified()
        assertEquals(HttpStatusCode.OK, join(member, invite(owner)).status)
        return owner to member
    }

    @Test
    fun `без группы приходит пустое состояние`() = serverTest {
        assertNull(family(registerVerified()))
    }

    @Test
    fun `создатель группы становится её первым участником`() = serverTest {
        val token = registerVerified()
        val created = create(token, name = "  Квартира  ")
        assertEquals("Квартира", created.name)
        assertTrue(created.isOwner)
        assertEquals(listOf("Миша"), created.members.map { it.name })
        assertTrue(created.members.single().isMe && created.members.single().isOwner)
        assertEquals(created, family(token))
    }

    @Test
    fun `по коду можно присоединиться к группе`() = serverTest {
        val owner = registerVerified()
        create(owner)
        val code = invite(owner)
        assertEquals(6, code.length)
        val member = registerVerified()
        val response = join(member, " ${code.lowercase().chunked(3).joinToString("-")} ")
        assertEquals(HttpStatusCode.OK, response.status)
        val joined = json.decodeFromString<FamilyDto>(response.bodyAsText())
        assertEquals(listOf("Миша", "Аня"), joined.members.map { it.name })
        assertTrue(!joined.isOwner && joined.members.last().isMe)
        assertEquals(listOf("Миша", "Аня"), family(owner)?.members?.map { it.name })
    }

    @Test
    fun `неверный или устаревший код отклоняется`() = serverTest {
        val owner = registerVerified()
        create(owner)
        val code = invite(owner)
        val member = registerVerified()
        assertEquals("invalid_code", join(member, "AAAAAA").field("error"))
        clock.advance(Duration.ofHours(25))
        val expired = join(member, code)
        assertEquals(HttpStatusCode.BadRequest, expired.status)
        assertEquals("invalid_code", expired.field("error"))
        assertNull(family(member))
    }

    @Test
    fun `новый код отменяет предыдущий`() = serverTest {
        val owner = registerVerified()
        create(owner)
        val first = invite(owner)
        val second = invite(owner)
        assertNotEquals(first, second)
        assertEquals("invalid_code", join(registerVerified(), first).field("error"))
        assertEquals(HttpStatusCode.OK, join(registerVerified(), second).status)
    }

    @Test
    fun `можно состоять только в одной группе`() = serverTest {
        val (owner, member) = twoMembers()
        val second = post("/family", "name" to "Дача", "memberName" to "Аня", token = member)
        assertEquals("already_in_family", second.field("error"))
        assertEquals(HttpStatusCode.Conflict, join(member, invite(owner)).status)
    }

    @Test
    fun `пригласить может только создатель`() = serverTest {
        val (_, member) = twoMembers()
        val response = post("/family/invite", token = member)
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("not_owner", response.field("error"))
    }

    @Test
    fun `пустое или слишком длинное имя отклоняется`() = serverTest {
        val token = registerVerified()
        val blank = post("/family", "name" to " ", "memberName" to "Миша", token = token)
        assertEquals("invalid_name", blank.field("error"))
        val long = post("/family", "name" to "Дом", "memberName" to "М".repeat(25), token = token)
        assertEquals("invalid_name", long.field("error"))
        assertNull(family(token))
    }

    @Test
    fun `общий список виден всем участникам с именем добавившего`() = serverTest {
        val (owner, member) = twoMembers()
        val cheese = item()
        val sent = familySync(owner, FamilySyncRequest(items = listOf(cheese)))
        assertEquals(1, sent.version)
        val received = familySync(member)
        assertEquals(listOf(cheese.copy(addedBy = "Миша")), received.items)
        assertEquals(sent.familyId, received.familyId)
    }

    @Test
    fun `изменение другим участником не меняет того, кто добавил`() = serverTest {
        val (owner, member) = twoMembers()
        val cheese = item()
        familySync(owner, FamilySyncRequest(items = listOf(cheese)))
        val opened = cheese.copy(openedAt = LocalDate.of(2026, 10, 25), updatedAt = 2_000)
        familySync(member, FamilySyncRequest(items = listOf(opened)))
        assertEquals(listOf(opened.copy(addedBy = "Миша")), familySync(owner).items)
    }

    @Test
    fun `свои категории в общем списке не сохраняются`() = serverTest {
        val (owner, member) = twoMembers()
        familySync(owner, FamilySyncRequest(items = listOf(item().copy(categoryId = "my-category"))))
        assertEquals(listOf(null), familySync(member).items.map { it.categoryId })
    }

    @Test
    fun `общий список не смешивается с личным`() = serverTest {
        val (owner, _) = twoMembers()
        familySync(owner, FamilySyncRequest(items = listOf(item())))
        val personal = client.post("/sync") {
            bearerAuth(owner)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SyncRequest()))
        }
        assertTrue(json.decodeFromString<SyncResponse>(personal.bodyAsText()).items.isEmpty())
    }

    @Test
    fun `номер от другой группы приводит к полной синхронизации`() = serverTest {
        val (owner, _) = twoMembers()
        val cheese = item()
        val sent = familySync(owner, FamilySyncRequest(items = listOf(cheese)))
        assertTrue(familySync(owner, FamilySyncRequest(familyId = sent.familyId, since = 1)).items.isEmpty())
        val other = UUID.randomUUID().toString()
        assertEquals(1, familySync(owner, FamilySyncRequest(familyId = other, since = 1)).items.size)
    }

    @Test
    fun `создатель удаляет участника, а его продукты остаются`() = serverTest {
        val (owner, member) = twoMembers()
        familySync(member, FamilySyncRequest(items = listOf(item(name = "Йогурт"))))
        val memberId = family(owner)!!.members.single { !it.isMe }.id
        assertEquals("not_owner", removeMember(member, family(member)!!.members.first().id).field("error"))
        assertEquals(HttpStatusCode.NoContent, removeMember(owner, memberId).status)
        assertNull(family(member))
        val response = sendFamilySync(member, FamilySyncRequest())
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("no_family", response.field("error"))
        assertEquals(listOf("Йогурт"), familySync(owner).items.map { it.name })
        assertEquals(listOf("Аня"), familySync(owner).items.map { it.addedBy })
    }

    @Test
    fun `создатель не может удалить себя или чужого пользователя`() = serverTest {
        val (owner, _) = twoMembers()
        val me = family(owner)!!.members.single { it.isMe }.id
        assertEquals(HttpStatusCode.NotFound, removeMember(owner, me).status)
        assertEquals(HttpStatusCode.NotFound, removeMember(owner, UUID.randomUUID().toString()).status)
        assertEquals(HttpStatusCode.NotFound, removeMember(owner, "не-id").status)
        assertEquals(2, family(owner)?.members?.size)
    }

    @Test
    fun `участник выходит сам, а создатель может только удалить группу`() = serverTest {
        val (owner, member) = twoMembers()
        val ownerLeave = post("/family/leave", token = owner)
        assertEquals(HttpStatusCode.Conflict, ownerLeave.status)
        assertEquals("owner_cannot_leave", ownerLeave.field("error"))
        assertEquals(HttpStatusCode.NoContent, post("/family/leave", token = member).status)
        assertNull(family(member))
        assertEquals(listOf("Миша"), family(owner)?.members?.map { it.name })
    }

    @Test
    fun `после удаления группы она пропадает у всех`() = serverTest {
        val (owner, member) = twoMembers()
        familySync(owner, FamilySyncRequest(items = listOf(item())))
        assertEquals(HttpStatusCode.Forbidden, client.delete("/family") { bearerAuth(member) }.status)
        assertEquals(HttpStatusCode.NoContent, client.delete("/family") { bearerAuth(owner) }.status)
        assertNull(family(owner))
        assertNull(family(member))
        create(owner, name = "Новая")
        assertTrue(familySync(owner).items.isEmpty())
    }

    @Test
    fun `без подтверждённой почты группа недоступна`() = serverTest {
        val response = client.get("/family") { bearerAuth(register(uniqueEmail())) }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("email_not_verified", response.field("error"))
    }
}
