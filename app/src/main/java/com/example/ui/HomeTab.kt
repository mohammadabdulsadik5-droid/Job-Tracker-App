package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FiberNew
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ApplicationStatus
import com.example.domain.JobApplication
import com.example.domain.JobTrackerRules
import com.example.domain.StatFilter
import com.example.ui.theme.DueYellowDark
import com.example.ui.theme.DueYellowLight
import com.example.ui.theme.DueYellowTextDark
import com.example.ui.theme.DueYellowTextLight
import com.example.ui.theme.HrBadgeDark
import com.example.ui.theme.HrBadgeLight
import com.example.ui.theme.StatusAppliedDark
import com.example.ui.theme.StatusAppliedLight
import com.example.ui.theme.StatusFollowupDark
import com.example.ui.theme.StatusFollowupLight
import com.example.ui.theme.StatusInterviewDark
import com.example.ui.theme.StatusInterviewLight
import com.example.ui.theme.StatusOfferDark
import com.example.ui.theme.StatusOfferLight

@Composable
fun HomeTab(
    uiState: DashboardUiState,
    onSelectStat: (StatFilter) -> Unit,
    onDeleteDemoData: () -> Unit,
    onOpenAiDialog: () -> Unit,
    onMarkFollowUpSent: (JobApplication) -> Unit,
    onOpenAllApplications: () -> Unit,
    onOpenFollowUpsTab: () -> Unit,
    onStatusChanged: (JobApplication, ApplicationStatus) -> Unit,
    onEditApplication: (JobApplication) -> Unit,
    onDeleteApplication: (JobApplication) -> Unit,
    onCopied: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val isDark = uiState.isDarkTheme
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val recentApps = uiState.allApplications.take(3)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!uiState.streamErrorMessage.isNullOrBlank()) {
            item(key = "stream_error") {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "⚠️ ক্লাউড সিঙ্ক সমস্যা: ${uiState.streamErrorMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        // Welcome & quick actions card
        item(key = "welcome_card") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "স্বাগতম, ${uiState.currentUser?.displayName ?: "বন্ধু"}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Dubai & Europe চাকরির আবেদন ট্র্যাকার",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onOpenAiDialog,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier.testTag("ai_add_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🤖 AI দিয়ে যোগ", style = MaterialTheme.typography.labelMedium)
                        }

                        if (uiState.hasDemoData) {
                            OutlinedButton(
                                onClick = onDeleteDemoData,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                                modifier = Modifier.testTag("delete_demo_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ডেমো মুছুন", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // 6 Tappable Stat Boxes
        item(key = "home_stats") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "এক নজরে সারসংক্ষেপ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                StatBoxesGrid(
                    uiState = uiState,
                    isDark = isDark,
                    onSelectStat = onSelectStat,
                )
            }
        }

        // Urgent Follow-up Due Section
        if (uiState.dueApplications.isNotEmpty()) {
            item(key = "due_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "⏰ জরুরি ফলো-আপ বাকি (${JobTrackerRules.toBengaliDigits(uiState.dueApplications.size)})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    TextButton(onClick = onOpenFollowUpsTab) {
                        Text("সব দেখুন")
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            items(
                items = uiState.dueApplications.take(2),
                key = { "due_${it.id}" },
            ) { app ->
                val dueBg = if (isDark) DueYellowDark else DueYellowLight
                val dueText = if (isDark) DueYellowTextDark else DueYellowTextLight
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, dueText.copy(alpha = 0.45f)),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.company,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${app.role} • ${app.country}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Surface(
                                color = dueBg,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = "বাকি আছে!",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = dueText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val primaryEmail = JobTrackerRules.extractEmails(app.email).firstOrNull()
                            if (primaryEmail != null) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val subject = Uri.encode("Follow-up: ${app.role} - ${app.company}")
                                            val intent = Intent(
                                                Intent.ACTION_SENDTO,
                                                Uri.parse("mailto:$primaryEmail?subject=$subject"),
                                            )
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            onMessage("ইমেইল অ্যাপ পাওয়া যায়নি")
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ইমেইল পাঠান", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onMarkFollowUpSent(app)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("পাঠিয়েছি", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // Recent Applications Preview
        item(key = "recent_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "সাম্প্রতিক আবেদন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = onOpenAllApplications) {
                    Text("সব আবেদন (${JobTrackerRules.toBengaliDigits(uiState.allApplications.size)})")
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        if (recentApps.isEmpty()) {
            item(key = "home_empty") {
                EmptyApplicationsState(
                    hasAnyData = false,
                    onAddClick = onOpenAllApplications,
                    onResetFilters = {},
                )
            }
        } else {
            items(
                items = recentApps,
                key = { "recent_${it.id}" },
            ) { app ->
                ApplicationCard(
                    app = app,
                    isDarkTheme = isDark,
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

@Composable
fun StatBoxesGrid(
    uiState: DashboardUiState,
    isDark: Boolean,
    onSelectStat: (StatFilter) -> Unit,
) {
    val stats = uiState.stats
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatBoxCard(
                label = StatFilter.ALL.bnLabel,
                count = stats.total,
                accentColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Outlined.FolderCopy,
                isActive = uiState.activeStatFilter == StatFilter.ALL,
                onClick = { onSelectStat(StatFilter.ALL) },
                modifier = Modifier.weight(1f),
            )
            StatBoxCard(
                label = StatFilter.INTERVIEW.bnLabel,
                count = stats.interview,
                accentColor = if (isDark) StatusInterviewDark else StatusInterviewLight,
                icon = Icons.Outlined.RecordVoiceOver,
                isActive = uiState.activeStatFilter == StatFilter.INTERVIEW,
                onClick = { onSelectStat(StatFilter.INTERVIEW) },
                modifier = Modifier.weight(1f),
            )
            StatBoxCard(
                label = StatFilter.DUE.bnLabel,
                count = stats.due,
                accentColor = if (isDark) StatusFollowupDark else StatusFollowupLight,
                icon = Icons.Outlined.Alarm,
                isActive = uiState.activeStatFilter == StatFilter.DUE,
                onClick = { onSelectStat(StatFilter.DUE) },
                modifier = Modifier.weight(1f),
            )
            StatBoxCard(
                label = StatFilter.FOLLOWUP.bnLabel,
                count = stats.followup,
                accentColor = if (isDark) StatusOfferDark else StatusOfferLight,
                icon = Icons.Outlined.MarkEmailRead,
                isActive = uiState.activeStatFilter == StatFilter.FOLLOWUP,
                onClick = { onSelectStat(StatFilter.FOLLOWUP) },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatBoxCard(
                label = StatFilter.NEW.bnLabel,
                count = stats.newApps,
                accentColor = if (isDark) StatusAppliedDark else StatusAppliedLight,
                icon = Icons.Outlined.FiberNew,
                isActive = uiState.activeStatFilter == StatFilter.NEW,
                onClick = { onSelectStat(StatFilter.NEW) },
                modifier = Modifier.weight(1f),
            )
            StatBoxCard(
                label = StatFilter.HR.bnLabel,
                count = stats.hrEmails,
                accentColor = if (isDark) HrBadgeDark else HrBadgeLight,
                icon = Icons.Outlined.Badge,
                isActive = uiState.activeStatFilter == StatFilter.HR,
                onClick = { onSelectStat(StatFilter.HR) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatBoxCard(
    label: String,
    count: Int,
    accentColor: Color,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                accentColor.copy(alpha = 0.11f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) accentColor else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = JobTrackerRules.toBengaliDigits(count),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
