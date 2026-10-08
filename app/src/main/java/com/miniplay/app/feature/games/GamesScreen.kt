package com.miniplay.app.feature.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miniplay.app.R
import com.miniplay.app.core.ui.components.EmptyState
import com.miniplay.app.core.ui.components.GameGridCard
import com.miniplay.app.core.ui.label
import com.miniplay.app.core.ui.theme.MiniPlayTheme
import com.miniplay.app.di.rememberViewModel
import com.miniplay.app.domain.model.GameCategory
import com.miniplay.app.feature.home.GameCardUi

@Composable
fun GamesScreen(onOpenGame: (String) -> Unit) {
    val viewModel = rememberViewModel { container ->
        GamesViewModel(container.gameRegistry, container.gameStatsRepository)
    }
    val games by viewModel.games.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<GameCategory?>(null) }
    val gutter = MiniPlayTheme.spacing.screen

    // Resolve titles once for searching (tiny catalogue).
    val withTitles = games.map { it to stringResource(it.metadata.titleRes) }
    val filtered = withTitles.filter { (card, title) ->
        (category == null || card.metadata.category == category) &&
            (query.isBlank() || title.contains(query.trim(), ignoreCase = true))
    }.map { it.first }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = gutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.games_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.games_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.game_close))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            CategoryFilterRow(selected = category, onSelect = { category = it })
            Text(
                stringResource(R.string.games_count, filtered.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (filtered.isEmpty()) {
            EmptyState(
                emoji = "🔍",
                title = stringResource(R.string.games_empty),
                message = stringResource(R.string.games_search_hint),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filtered, key = { it.metadata.id }) { card ->
                    GameGridCard(
                        metadata = card.metadata,
                        bestScore = card.bestScore,
                        onPlay = { onOpenGame(card.metadata.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selected: GameCategory?,
    onSelect: (GameCategory?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.games_filter_all)) },
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
        items(GameCategory.entries.toList()) { cat ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(if (selected == cat) null else cat) },
                label = { Text(cat.label()) },
            )
        }
    }
}
