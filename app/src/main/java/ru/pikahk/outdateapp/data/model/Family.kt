package ru.pikahk.outdateapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FamilyMember(val id: String, val name: String, val isOwner: Boolean, val isMe: Boolean, val joinedAt: Long)

@Serializable
data class Family(val id: String, val name: String, val isOwner: Boolean, val members: List<FamilyMember>)
