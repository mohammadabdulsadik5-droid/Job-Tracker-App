package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ApplicationStatus
import com.example.domain.JobApplication
import com.example.domain.JobTrackerRules
import com.example.domain.Region
import com.example.domain.StatFilter
import com.example.domain.StatusFilterOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onSelectTab: (AppTab) -> Unit,
    onOpenStatInApplications: (StatFilter) -> Unit,
    onToggleTheme: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteDemoData: () -> Unit,
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
    onOpenAiDialog: () -> Unit,
    onResetFilters: () -> Unit,
    onCopied: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    BackHandler(enabled = uiState.selectedTab != AppTab.HOME) {
        onSelectTab(AppTab.HOME)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        val topTitle = when (uiState.selectedTab) {
            AppTab.HOME -> "Job Application Tracker"
            AppTab.APPLICATIONS -> "সব আবেদন (${JobTrackerRules.toBengaliDigits(uiState.filteredApplications.size)})"
            AppTab.FOLLOWUPS -> "ফলো-আপ ও ইন্টারভিউ"
            AppTab.SETTINGS -> "সেটিংস ও ব্যাকআপ"
        }

        Scaffold(
            contentWindowInsets = WindowInsets.navigationBars,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = topTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (uiState.selectedTab == AppTab.HOME) {
                                Text(
                                    text = "Dubai & Europe আবেদনসমূহ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onOpenAiDialog,
                            modifier = Modifier.testTag("ai_add_top_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "AI দিয়ে যোগ",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                imageVector = if (uiState.isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "থিম পরিবর্তন",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = uiState.selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        dueCount = uiState.stats.due,
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.bnLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (uiState.selectedTab == AppTab.HOME || uiState.selectedTab == AppTab.APPLICATIONS) {
                    FloatingActionButton(
                        onClick = onOpenAddForm,
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(58.dp)
                            .testTag("add_application_fab"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "নতুন আবেদন যোগ করুন",
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = uiState.selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        dueCount = uiState.stats.due,
                                    )
                                },
                                label = { Text(tab.bnLabel) },
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    Crossfade(
                        targetState = uiState.selectedTab,
                        label = "tab_crossfade",
                    ) { tab ->
                        when (tab) {
                            AppTab.HOME -> HomeTab(
                                uiState = uiState,
                                onSelectStat = onOpenStatInApplications,
                                onDeleteDemoData = onDeleteDemoData,
                                onOpenAiDialog = onOpenAiDialog,
                                onMarkFollowUpSent = onMarkFollowUpSent,
                                onOpenAllApplications = { onSelectTab(AppTab.APPLICATIONS) },
                                onOpenFollowUpsTab = { onSelectTab(AppTab.FOLLOWUPS) },
                                onStatusChanged = onStatusChanged,
                                onEditApplication = onEditApplication,
                                onDeleteApplication = onDeleteApplication,
                                onCopied = onCopied,
                                onMessage = onMessage,
                            )

                            AppTab.APPLICATIONS -> ApplicationsTab(
                                uiState = uiState,
                                onSelectStat = onSelectStat,
                                onSelectRegion = onSelectRegion,
                                onSelectCountry = onSelectCountry,
                                onSelectCity = onSelectCity,
                                onSearchChange = onSearchChange,
                                onSelectStatusFilter = onSelectStatusFilter,
                                onStatusChanged = onStatusChanged,
                                onMarkFollowUpSent = onMarkFollowUpSent,
                                onEditApplication = onEditApplication,
                                onDeleteApplication = onDeleteApplication,
                                onOpenAddForm = onOpenAddForm,
                                onResetFilters = onResetFilters,
                                onCopied = onCopied,
                                onMessage = onMessage,
                            )

                            AppTab.FOLLOWUPS -> FollowUpsTab(
                                uiState = uiState,
                                onStatusChanged = onStatusChanged,
                                onMarkFollowUpSent = onMarkFollowUpSent,
                                onEditApplication = onEditApplication,
                                onDeleteApplication = onDeleteApplication,
                                onCopied = onCopied,
                                onMessage = onMessage,
                            )

                            AppTab.SETTINGS -> SettingsTab(
                                uiState = uiState,
                                onToggleTheme = onToggleTheme,
                                onExportJson = onExportJson,
                                onImportJson = onImportJson,
                                onDeleteDemoData = onDeleteDemoData,
                                onOpenAiDialog = onOpenAiDialog,
                                onSignOut = onSignOut,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: AppTab,
    selected: Boolean,
    dueCount: Int,
) {
    val icon = when (tab) {
        AppTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
        AppTab.APPLICATIONS -> if (selected) Icons.Filled.Work else Icons.Outlined.WorkOutline
        AppTab.FOLLOWUPS -> if (selected) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications
        AppTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
    }

    if (tab == AppTab.FOLLOWUPS && dueCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(JobTrackerRules.toBengaliDigits(dueCount))
                }
            },
        ) {
            Icon(imageVector = icon, contentDescription = tab.bnLabel)
        }
    } else {
        Icon(imageVector = icon, contentDescription = tab.bnLabel)
    }
}
