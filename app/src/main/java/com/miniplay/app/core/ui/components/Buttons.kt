package com.miniplay.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.miniplay.app.core.ui.theme.MiniPlayCorner
import com.miniplay.app.core.ui.theme.MiniPlayTheme

/** Filled, high-emphasis action. 48dp min height for an accessible touch target. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(MiniPlayCorner.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        ButtonContent(text, leadingIcon)
    }
}

/** Low-emphasis outlined action for secondary choices. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(MiniPlayCorner.button),
        border = BorderStroke(1.dp, MiniPlayTheme.colors.hairline),
    ) {
        ButtonContent(text, leadingIcon)
    }
}

@Composable
private fun ButtonContent(text: String, leadingIcon: ImageVector?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.padding(end = 0.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
