package ru.pikahk.outdateapp.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CredentialsRequest(val email: String, val password: String)

@Serializable
data class EmailRequest(val email: String)

@Serializable
data class CodeRequest(val code: String)

@Serializable
data class ResetPasswordRequest(val email: String, val code: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val email: String)

@Serializable
data class MeResponse(val email: String, val emailVerified: Boolean)

@Serializable
data class ErrorResponse(val error: String)
