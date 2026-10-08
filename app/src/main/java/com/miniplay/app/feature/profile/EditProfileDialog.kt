package com.miniplay.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miniplay.app.R
import com.miniplay.app.core.ui.modifier.bounceClick
import com.miniplay.app.data.repository.ProfileRepositoryImpl

@Composable
fun EditProfileDialog(
    initialName: String,
    initialAvatar: Int,
    onSave: (String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var avatar by remember { mutableIntStateOf(initialAvatar) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onSave(name.ifBlank { initialName }, avatar) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.profile_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) }
        },
        title = { Text(stringResource(R.string.profile_edit), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= ProfileRepositoryImpl.MAX_NICKNAME_LENGTH) name = it },
                    label = { Text(stringResource(R.string.profile_nickname)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.profile_avatar), style = MaterialTheme.typography.titleSmall)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.heightIn(max = 220.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(Avatars.emojis) { index, emoji ->
                        val selected = index == avatar
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                                )
                                .then(
                                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier,
                                )
                                .bounceClick { avatar = index },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(emoji, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        },
    )
}
