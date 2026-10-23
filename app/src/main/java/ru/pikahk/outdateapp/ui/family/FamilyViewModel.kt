package ru.pikahk.outdateapp.ui.family

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.pikahk.outdateapp.data.DatabaseProvider
import ru.pikahk.outdateapp.data.FamilyProvider
import ru.pikahk.outdateapp.data.SyncProvider
import ru.pikahk.outdateapp.data.model.Family
import ru.pikahk.outdateapp.data.model.Session
import ru.pikahk.outdateapp.data.remote.ApiException
import ru.pikahk.outdateapp.data.repository.CategoryRepository
import ru.pikahk.outdateapp.data.repository.FamilyRepository
import ru.pikahk.outdateapp.data.repository.ItemRepository
import ru.pikahk.outdateapp.domain.effectiveExpiryDate
import ru.pikahk.outdateapp.ui.items.toItemGroups
import ru.pikahk.outdateapp.ui.toUi

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyViewModel(
    session: Flow<Session?>,
    private val families: FamilyRepository,
    private val items: ItemRepository,
    private val categories: CategoryRepository
) : ViewModel() {

    private val selectedCategory = MutableStateFlow<String?>(null)

    private val busyState = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = busyState.asStateFlow()

    private val errorState = MutableStateFlow<FamilyError?>(null)
    val error: StateFlow<FamilyError?> = errorState.asStateFlow()

    private val inviteState = MutableStateFlow<InviteUi?>(null)
    val invite: StateFlow<InviteUi?> = inviteState.asStateFlow()

    val state: StateFlow<FamilyUiState> = combine(session, families.family, ::Pair)
        .flatMapLatest { (current, family) ->
            when {
                current?.emailVerified != true -> flowOf(FamilyUiState.SignedOut)
                family == null -> flowOf(FamilyUiState.NoFamily)
                else -> activeState(family)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FamilyUiState.Loading)

    fun selectCategory(id: String?) {
        selectedCategory.value = id
    }

    fun clearError() {
        errorState.value = null
    }

    fun create(name: String, memberName: String, onDone: () -> Unit) {
        launchAction(onDone) { families.create(name, memberName) }
    }

    fun join(code: String, memberName: String, onDone: () -> Unit) {
        launchAction(onDone) { families.join(normalizeInviteCode(code), memberName) }
    }

    fun loadInvite() {
        inviteState.value = null
        launchAction {
            val response = families.invite()
            inviteState.value = InviteUi(response.code, Instant.ofEpochMilli(response.expiresAt))
        }
    }

    fun removeMember(memberId: String) {
        launchAction { families.removeMember(memberId) }
    }

    fun leave(onDone: () -> Unit) {
        launchAction(onDone) { families.leave() }
    }

    fun delete(onDone: () -> Unit) {
        launchAction(onDone) { families.delete() }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                families.refresh()
            } catch (e: ApiException) {
                return@launch
            }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch { items.markDeleted(id) }
    }

    private fun activeState(family: Family): Flow<FamilyUiState> =
        combine(items.observeFamily(family.id), categories.observeAll(), selectedCategory) { list, all, selected ->
            val shared = all.map { it.toUi() }.filter { it.builtIn != null }
            val visible = list
                .filter { selected == null || it.categoryId == selected }
                .sortedBy { effectiveExpiryDate(it) }
            FamilyUiState.Active(
                family = family,
                categories = shared,
                selectedCategoryId = selected,
                groups = visible.toItemGroups(LocalDate.now(), shared),
                hasItems = list.isNotEmpty()
            )
        }

    private fun launchAction(onDone: () -> Unit = {}, action: suspend () -> Unit) {
        if (busyState.value) return
        viewModelScope.launch {
            busyState.value = true
            errorState.value = null
            val succeeded = try {
                action()
                true
            } catch (e: ApiException) {
                errorState.value = familyError(e.code)
                false
            } finally {
                busyState.value = false
            }
            if (succeeded) onDone()
        }
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer {
                val database = DatabaseProvider.get(context)
                FamilyViewModel(
                    session = SyncProvider.get(context).session,
                    families = FamilyProvider.get(context),
                    items = ItemRepository(database.itemDao()),
                    categories = CategoryRepository(database.categoryDao())
                )
            }
        }
    }
}
