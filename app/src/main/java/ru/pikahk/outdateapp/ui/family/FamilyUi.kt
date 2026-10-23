package ru.pikahk.outdateapp.ui.family

import androidx.annotation.StringRes
import java.time.Instant
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.model.Family
import ru.pikahk.outdateapp.data.remote.ApiException
import ru.pikahk.outdateapp.ui.CategoryUi
import ru.pikahk.outdateapp.ui.items.ItemsGroup

const val FAMILY_NAME_LIMIT = 40
const val MEMBER_NAME_LIMIT = 24
const val INVITE_CODE_LENGTH = 6

sealed interface FamilyUiState {
    data object Loading : FamilyUiState

    data object SignedOut : FamilyUiState

    data object NoFamily : FamilyUiState

    data class Active(
        val family: Family,
        val categories: List<CategoryUi>,
        val selectedCategoryId: String?,
        val groups: List<ItemsGroup>,
        val hasItems: Boolean
    ) : FamilyUiState
}

data class InviteUi(val code: String, val expiresAt: Instant)

enum class FamilyError(@StringRes val message: Int) {
    INVALID_CODE(R.string.family_error_code),
    ALREADY_IN_FAMILY(R.string.family_error_already),
    INVALID_NAME(R.string.family_error_name),
    NETWORK(R.string.auth_error_network),
    SERVER(R.string.auth_error_server)
}

fun familyError(code: String): FamilyError = when (code) {
    "invalid_code" -> FamilyError.INVALID_CODE
    "already_in_family" -> FamilyError.ALREADY_IN_FAMILY
    "invalid_name" -> FamilyError.INVALID_NAME
    ApiException.NETWORK -> FamilyError.NETWORK
    else -> FamilyError.SERVER
}

fun normalizeInviteCode(text: String): String =
    text.uppercase().filter { it.isLetterOrDigit() }.take(INVITE_CODE_LENGTH)

fun formatInviteCode(code: String): String = code.chunked(INVITE_CODE_LENGTH / 2).joinToString(" ")
