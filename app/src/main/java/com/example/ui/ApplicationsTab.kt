package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ApplicationStatus
import com.example.domain.JobApplication
import com.example.domain.JobTrackerRules
import com.example.domain.Region
import com.example.domain.StatFilter
import com.example.domain.StatusFilterOption

@Composable
fun ApplicationsTab(
    uiState: DashboardUiState,
    onSelectStat: (StatFilter) -> Unit,
    onSelectRegion: (Region) -> Unit,
    onSelectCountry: (String?) -> Unit,
    onSelectCity: (String?) -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectStatusFilter: (StatusFilterOption) -> Unit,
    onStatusChanged: (JobApplication, ApplicationStatus) -> Unit,
    onMarkFollowUpSent: (JobApplication) -> Unit,
    onEditApplication: (JobApplication) -> Unit,
    onDeleteApplication: (JobApplication) -> Unit,
    onOpenAddForm: () -> Unit,
    onResetFilters: () -> Unit,
    onCopied: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Search + Status Filter
        item(key = "search_status") {
            SearchAndStatusRow(
                searchQuery = uiState.searchQuery,
                statusFilter = uiState.statusFilter,
                onSearchChange = onSearchChange,
                onSelectStatusFilter = onSelectStatusFilter,
            )
        }

        // Active Stat Filter pill if set
        if (uiState.activeStatFilter != StatFilter.ALL) {
            item(key = "active_stat_chip") {
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    InputChip(
                        selected = true,
                        onClick = { onSelectStat(StatFilter.ALL) },
                        label = { Text("ফিল্টার: ${uiState.activeStatFilter.bnLabel}") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "ফিল্টার সরান",
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }

        // Region Tabs
        item(key = "region_tabs") {
            RegionTabsRow(
                selectedRegion = uiState.selectedRegion,
                onSelectRegion = onSelectRegion,
            )
        }

        // Country Chips
        if (uiState.selectedRegion != Region.ALL && uiState.countryChips.isNotEmpty()) {
            item(key = "country_chips") {
                CountryOrCityChipRow(
                    prefixLabel = "দেশ:",
                    chips = uiState.countryChips,
                    selectedLabel = uiState.selectedCountry,
                    onSelect = onSelectCountry,
                )
            }
        }

        // City Chips
        if (!uiState.selectedCountry.isNullOrBlank()) {
            item(key = "city_chips") {
                CountryOrCityChipRow(
                    prefixLabel = "শহর:",
                    chips = uiState.cityChips,
                    selectedLabel = uiState.selectedCity,
                    onSelect = onSelectCity,
                )
            }
        }

        // Count line
        item(key = "count_line") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${JobTrackerRules.toBengaliDigits(uiState.filteredApplications.size)}টি আবেদন দেখাচ্ছে",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (uiState.activeStatFilter != StatFilter.ALL ||
                    uiState.selectedRegion != Region.ALL ||
                    uiState.searchQuery.isNotBlank() ||
                    uiState.statusFilter != StatusFilterOption.ALL
                ) {
                    TextButton(onClick = onResetFilters) {
                        Text("ফিল্টার রিসেট")
                    }
                }
            }
        }

        // Cards or Empty state
        if (uiState.filteredApplications.isEmpty()) {
            item(key = "empty_apps") {
                EmptyApplicationsState(
                    hasAnyData = uiState.allApplications.isNotEmpty(),
                    onAddClick = onOpenAddForm,
                    onResetFilters = onResetFilters,
                )
            }
        } else {
            items(
                items = uiState.filteredApplications,
                key = { it.id },
            ) { app ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ApplicationCard(
                        app = app,
                        isDarkTheme = uiState.isDarkTheme,
                        onStatusChanged = { st -> onStatusChanged(app, st) },
                        onMarkFollowUpSent = { onMarkFollowUpSent(app) },
                        onEdit = { onEditApplication(app) },
                        onDelete = { onDeleteApplication(app) },
                        onCopied = onCopied,
                        onMessage = onMessage,
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionTabsRow(
    selectedRegion: Region,
    onSelectRegion: (Region) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Region.entries.forEach { region ->
            val selected = selectedRegion == region
            FilterChip(
                selected = selected,
                onClick = { onSelectRegion(region) },
                label = {
                    Text(
                        text = region.bnLabel,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun CountryOrCityChipRow(
    prefixLabel: String,
    chips: List<ChipCount>,
    selectedLabel: String?,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = prefixLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        chips.forEach { chip ->
            val selected = selectedLabel.equals(chip.label, ignoreCase = true)
            val labelText = if (chip.count > 0) {
                "${chip.label} (${JobTrackerRules.toBengaliDigits(chip.count)})"
            } else {
                chip.label
            }
            FilterChip(
                selected = selected,
                onClick = { onSelect(chip.label) },
                label = { Text(labelText) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun SearchAndStatusRow(
    searchQuery: String,
    statusFilter: StatusFilterOption,
    onSearchChange: (String) -> Unit,
    onSelectStatusFilter: (StatusFilterOption) -> Unit,
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text(
                    text = "কোম্পানি, পদ বা দেশ",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "সার্চ")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "সার্চ মুছুন")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1.25f)
                .testTag("search_input"),
        )

        Box(modifier = Modifier.weight(0.95f)) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { statusMenuExpanded = true }
                    .testTag("status_filter_dropdown"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = statusFilter.bnLabel,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "স্ট্যাটাস ফিল্টার",
                    )
                }
            }

            DropdownMenu(
                expanded = statusMenuExpanded,
                onDismissRequest = { statusMenuExpanded = false },
            ) {
                StatusFilterOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.bnLabel) },
                        onClick = {
                            statusMenuExpanded = false
                            onSelectStatusFilter(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyApplicationsState(
    hasAnyData: Boolean,
    onAddClick: () -> Unit,
    onResetFilters: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_inbox),
            contentDescription = "কোনো আবেদন নেই",
            modifier = Modifier
                .size(144.dp)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = if (hasAnyData) "📭 এই ফিল্টারে কোনো আবেদন নেই" else "📭 এখনো কোনো আবেদন যোগ করা হয়নি",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = if (hasAnyData) {
                "ফিল্টার রিসেট করুন অথবা নিচের + বাটনে ট্যাপ করে নতুন আবেদন যোগ করুন।"
            } else {
                "নিচের + বাটনে ট্যাপ করে আপনার প্রথম চাকরির আবেদন যোগ করুন।"
            },
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (hasAnyData) {
                OutlinedButton(onClick = onResetFilters) {
                    Text("ফিল্টার রিসেট")
                }
            }
            OutlinedButton(onClick = onAddClick) {
                Text("+ আবেদন যোগ করুন")
            }
        }
    }
}
