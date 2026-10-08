package com.miniplay.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miniplay.app.R

/**
 * The per-game "How to play" dialog. Every game screen exposes one from its menu
 * so the player never has to guess the rules.
 */
@Composable
fun HowToPlayDialog(
    titleEmoji: String,
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.game_got_it))
            }
        },
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(titleEmoji, style = MaterialTheme.typography.headlineSmall)
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(body, style = MaterialTheme.typography.bodyMedium)
        },
    )
}

/** Generic destructive/confirm dialog (e.g. reset progress, quit game). */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
    )
}

/** A vertical group of options rendered as the body of a bottom actions column. */
@Composable
fun DialogActions(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}
