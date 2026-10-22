package ru.pikahk.outdate.server.family

import kotlinx.serialization.Serializable
import ru.pikahk.outdate.server.sync.ItemDto

@Serializable
data class CreateFamilyRequest(val name: String, val memberName: String)

@Serializable
data class JoinFamilyRequest(val code: String, val memberName: String)

@Serializable
data class MemberDto(val id: String, val name: String, val isOwner: Boolean, val isMe: Boolean, val joinedAt: Long)

@Serializable
data class FamilyDto(val id: String, val name: String, val isOwner: Boolean, val members: List<MemberDto>)

@Serializable
data class FamilyState(val family: FamilyDto? = null)

@Serializable
data class InviteResponse(val code: String, val expiresAt: Long)

@Serializable
data class FamilySyncRequest(val familyId: String? = null, val since: Long = 0, val items: List<ItemDto> = emptyList())

@Serializable
data class FamilySyncResponse(val familyId: String, val version: Long, val items: List<ItemDto>)
