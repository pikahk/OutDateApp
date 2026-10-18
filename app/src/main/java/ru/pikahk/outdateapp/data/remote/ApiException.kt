package ru.pikahk.outdateapp.data.remote

class ApiException(val code: String, cause: Throwable? = null) : Exception(code, cause) {
    companion object {
        const val NETWORK = "network"
        const val SERVER = "server_error"
        const val UNAUTHORIZED = "unauthorized"
    }
}
