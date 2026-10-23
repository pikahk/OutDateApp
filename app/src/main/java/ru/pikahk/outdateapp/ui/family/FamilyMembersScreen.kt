package ru.pikahk.outdateapp.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import ru.pikahk.outdateapp.R
import ru.pikahk.outdateapp.data.model.Family
import ru.pikahk.outdateapp.data.model.FamilyMember
import ru.pikahk.outdateapp.ui.theme.OutDateAppTheme

private enum class Confirm { LEAVE, DELETE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMembersScreen(
    family: Family,
    error: FamilyError?,
    onInvite: () -> Unit,
    onRemove: (String) -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var removingId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirm by rememberSaveable { mutableStateOf<Confirm?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.family_members), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            family.members.forEach { member ->
                MemberRow(
                    member = member,
                    canRemove = family.isOwner && !member.isMe,
                    onRemove = { removingId = member.id }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (family.isOwner) {
                OutlinedButton(
                    onClick = onInvite,
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text(stringResource(R.string.family_invite))
                }
            }
            FamilyErrorText(error)
            Text(
                text = stringResource(if (family.isOwner) R.string.family_delete else R.string.family_leave),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { confirm = if (family.isOwner) Confirm.DELETE else Confirm.LEAVE }
                    .padding(vertical = 12.dp)
            )
        }
    }

    val removing = family.members.firstOrNull { it.id == removingId }
    if (removing != null) {
        ConfirmDialog(
            title = stringResource(R.string.family_remove_title, removing.name),
            text = stringResource(R.string.family_remove_text),
            action = stringResource(R.string.family_remove),
            onConfirm = {
                removingId = null
                onRemove(removing.id)
            },
            onDismiss = { removingId = null }
        )
    }

    when (confirm) {
        Confirm.LEAVE -> ConfirmDialog(
            title = stringResource(R.string.family_leave_title),
            text = stringResource(R.string.family_leave_text),
            action = stringResource(R.string.family_leave_confirm),
            onConfirm = {
                confirm = null
                onLeave()
            },
            onDismiss = { confirm = null }
        )
        Confirm.DELETE -> ConfirmDialog(
            title = stringResource(R.string.family_delete_title),
            text = stringResource(R.string.family_delete_text),
            action = stringResource(R.string.delete),
            onConfirm = {
                confirm = null
                onDelete()
            },
            onDismiss = { confirm = null }
        )
        null -> Unit
    }
}

@Composable
private fun MemberRow(member: FamilyMember, canRemove: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val colors = MaterialTheme.colorScheme
        val background = if (member.isMe) colors.primary else colors.surfaceVariant
        val content = if (member.isMe) colors.onPrimary else colors.onSurfaceVariant
        Box(
            modifier = Modifier.size(40.dp).background(background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = content
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (member.isMe) stringResource(R.string.family_you, member.name) else member.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (member.isOwner) {
                    stringResource(R.string.family_owner)
                } else {
                    stringResource(R.string.family_since, rememberJoinDate(member.joinedAt))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (canRemove) {
            TextButton(onClick = onRemove) {
                Text(stringResource(R.string.family_remove), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun rememberJoinDate(joinedAt: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, joinedAt) {
        val date = Instant.ofEpochMilli(joinedAt).atZone(ZoneId.systemDefault())
        date.format(DateTimeFormatter.ofPattern("d MMMM", locale))
    }
}

@Composable
private fun ConfirmDialog(title: String, text: String, action: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(action, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun FamilyMembersScreenPreview() {
    OutDateAppTheme {
        FamilyMembersScreen(
            family = Family(
                id = "f1",
                name = "Квартира на Ленина",
                isOwner = true,
                members = listOf(
                    FamilyMember("1", "Миша", isOwner = true, isMe = true, joinedAt = 0),
                    FamilyMember("2", "Мама", isOwner = false, isMe = false, joinedAt = 1_792_000_000_000),
                    FamilyMember("3", "Аня", isOwner = false, isMe = false, joinedAt = 1_792_000_000_000)
                )
            ),
            error = null,
            onInvite = {},
            onRemove = {},
            onLeave = {},
            onDelete = {},
            onBack = {}
        )
    }
}
