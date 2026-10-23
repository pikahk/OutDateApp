package ru.pikahk.outdateapp.data.remote

import kotlinx.serialization.Serializable
import ru.pikahk.outdateapp.data.model.Family

@Serializable
data class CreateFamilyRequest(val name: String, val memberName: String)

@Serializable
data class JoinFamilyRequest(val code: String, val memberName: String)

@Serializable
data class FamilyStateResponse(val family: Family? = null)

@Serializable
data class InviteResponse(val code: String, val expiresAt: Long)

@Serializable
data class FamilySyncRequest(val familyId: String, val since: Long, val items: List<ItemDto>)

@Serializable
data class FamilySyncResponse(val familyId: String, val version: Long, val items: List<ItemDto>)
