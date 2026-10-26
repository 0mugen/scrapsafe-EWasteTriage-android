package com.ewastetriage.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ewastetriage.app.guidance.GuidanceEntry
import com.ewastetriage.app.guidance.Severity
import com.ewastetriage.app.ui.components.GuidanceDetail
import com.ewastetriage.app.ui.components.SeverityBadge
import com.ewastetriage.app.ui.components.iconForCategory
import com.ewastetriage.app.ui.components.iconForHazardType
import com.ewastetriage.app.ui.theme.semanticColorForSeverity

enum class CategoryFilter(val label: String) {
    ALL("All"),
    HIGH_RISK("High risk"),
    BATTERIES("Batteries"),
    SCREENS("Screens"),
    DEVICES("Computers/Phones")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(vm: ScanViewModel) {
    val allGuidance = remember { vm.getAllGuidance() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CategoryFilter.ALL) }
    var activeDetailEntry by remember { mutableStateOf<Pair<String, GuidanceEntry>?>(null) }

    // Filter & Sort
    val filteredEntries = remember(searchQuery, selectedFilter, allGuidance) {
        allGuidance.entries
            .filter { (key, entry) ->
                val matchesFilter = when (selectedFilter) {
                    CategoryFilter.ALL -> true
                    CategoryFilter.HIGH_RISK -> entry.overallSeverity == Severity.HIGH
                    CategoryFilter.BATTERIES -> key.startsWith("battery")
                    CategoryFilter.SCREENS -> key == "crt" || key == "flat_panel_monitor"
                    CategoryFilter.DEVICES -> key == "smartphone" || key == "laptop"
                }
                val q = searchQuery.trim().lowercase()
                val matchesSearch = q.isEmpty() ||
                        entry.displayName.lowercase().contains(q) ||
                        entry.composition.lowercase().contains(q) ||
                        entry.harmful.any { it.name.lowercase().contains(q) || it.risk.lowercase().contains(q) }

                matchesFilter && matchesSearch
            }
            .sortedBy { it.value.overallSeverity.ordinal }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Hazard Guide",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Curated safety and component handling for supported e-waste",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search by name, hazard or component...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter chips row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryFilter.values().forEach { filter ->
                        val selected = selectedFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    filter.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No categories match your search",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Try clearing the search or choosing a different filter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                items(
                    items = filteredEntries,
                    key = { it.key }
                ) { (key, entry) ->
                    CategoryCard(
                        categoryKey = key,
                        entry = entry,
                        onClick = { activeDetailEntry = Pair(key, entry) }
                    )
                }
            }
        }
    }

    // ModalBottomSheet for category detail
    activeDetailEntry?.let { (key, entry) ->
        ModalBottomSheet(
            onDismissRequest = { activeDetailEntry = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            GuidanceDetail(
                guidance = entry,
                categoryKey = key,
                onDismiss = { activeDetailEntry = null }
            )
        }
    }
}

@Composable
fun CategoryCard(
    categoryKey: String,
    entry: GuidanceEntry,
    onClick: () -> Unit
) {
    val overallSeverity = entry.overallSeverity
    val sem = semanticColorForSeverity(overallSeverity)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 48dp rounded icon tile
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(sem.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForCategory(categoryKey),
                    contentDescription = null,
                    tint = sem.onContainer,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    SeverityBadge(severity = overallSeverity)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${entry.harmful.size} hazards, ${entry.reusable.size} reusable parts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Row of small hazard-type chips
                val uniqueTypes = remember(entry.harmful) {
                    entry.harmful.map { it.type }.distinct()
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    uniqueTypes.take(3).forEach { type ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = iconForHazardType(type),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = type.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (uniqueTypes.size > 3) {
                        Text(
                            text = "+${uniqueTypes.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
