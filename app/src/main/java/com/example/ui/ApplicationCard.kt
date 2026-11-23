package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ApplicationStatus
import com.example.domain.JobApplication
import com.example.domain.JobTrackerRules
import com.example.ui.theme.DueYellowDark
import com.example.ui.theme.DueYellowLight
import com.example.ui.theme.DueYellowTextDark
import com.example.ui.theme.DueYellowTextLight
import com.example.ui.theme.HrBadgeDark
import com.example.ui.theme.HrBadgeLight
import com.example.ui.theme.StatusAppliedBgDark
import com.example.ui.theme.StatusAppliedBgLight
import com.example.ui.theme.StatusAppliedDark
import com.example.ui.theme.StatusAppliedLight
import com.example.ui.theme.StatusFollowupBgDark
import com.example.ui.theme.StatusFollowupBgLight
import com.example.ui.theme.StatusFollowupDark
import com.example.ui.theme.StatusFollowupLight
import com.example.ui.theme.StatusInterviewBgDark
import com.example.ui.theme.StatusInterviewBgLight
import com.example.ui.theme.StatusInterviewDark
import com.example.ui.theme.StatusInterviewLight
import com.example.ui.theme.StatusOfferBgDark
import com.example.ui.theme.StatusOfferBgLight
import com.example.ui.theme.StatusOfferDark
import com.example.ui.theme.StatusOfferLight
import com.example.ui.theme.StatusRejectedBgDark
import com.example.ui.theme.StatusRejectedBgLight
import com.example.ui.theme.StatusRejectedDark
import com.example.ui.theme.StatusRejectedLight
import kotlinx.coroutines.delay

@Composable
fun statusColors(status: ApplicationStatus, isDark: Boolean): Pair<Color, Color> = when (status) {
    ApplicationStatus.APPLIED -> if (isDark) StatusAppliedDark to StatusAppliedBgDark else StatusAppliedLight to StatusAppliedBgLight
    ApplicationStatus.FOLLOWUP -> if (isDark) StatusFollowupDark to StatusFollowupBgDark else StatusFollowupLight to StatusFollowupBgLight
    ApplicationStatus.INTERVIEW -> if (isDark) StatusInterviewDark to StatusInterviewBgDark else StatusInterviewLight to StatusInterviewBgLight
    ApplicationStatus.REJECTED -> if (isDark) StatusRejectedDark to StatusRejectedBgDark else StatusRejectedLight to StatusRejectedBgLight
    ApplicationStatus.OFFER -> if (isDark) StatusOfferDark to StatusOfferBgDark else StatusOfferLight to StatusOfferBgLight
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApplicationCard(
    app: JobApplication,
    isDarkTheme: Boolean,
    onStatusChanged: (ApplicationStatus) -> Unit,
    onMarkFollowUpSent: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopied: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val (accentColor, badgeBg) = statusColors(app.status, isDarkTheme)
    val isHr = remember(app.email) { JobTrackerRules.isHrEmail(app.email) }
    val isDue = remember(app.status, app.followDate) {
        JobTrackerRules.isFollowUpDue(app.status, app.followDate)
    }
    val emails = remember(app.email) { JobTrackerRules.extractEmails(app.email) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var copiedEmail by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiedEmail) {
        if (copiedEmail != null) {
            delay(2000L)
            copiedEmail = null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("application_card_${app.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Solid colored top bar by status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(accentColor),
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Header Row: Company + Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = app.company,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = app.role,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    ) {
                        Text(
                            text = app.status.bnLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                // HR Email tag + Demo tag
                if (isHr || app.isDemo) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (isHr) {
                            val hrColor = if (isDarkTheme) HrBadgeDark else HrBadgeLight
                            Surface(
                                color = hrColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = "👥 HR ইমেইল",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = hrColor,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                        if (app.isDemo) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = "ডেমো",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }

                // Location Row
                val locationParts = buildString {
                    if (app.country.isNotBlank()) append(app.country)
                    if (app.city.isNotBlank()) {
                        if (isNotEmpty()) append(", ")
                        append(app.city)
                    }
                    append(" (${app.region.bnLabel})")
                }
                InfoRow(
                    icon = Icons.Outlined.LocationOn,
                    text = locationParts,
                )

                // Date Row
                InfoRow(
                    icon = Icons.Outlined.CalendarToday,
                    text = "আবেদন: ${JobTrackerRules.formatBengaliDate(app.date)}",
                )

                // CV Set & File
                val cvText = listOf(app.cvSet.label, app.cvFile)
                    .filter { it.isNotBlank() }
                    .joinToString(" — ")
                if (cvText.isNotBlank()) {
                    InfoRow(
                        icon = Icons.Outlined.Description,
                        text = cvText,
                    )
                }

                // Website link
                if (app.website.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.website))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    onMessage("ওয়েবসাইট খোলা যায়নি")
                                }
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = "Website",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = app.website,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Emails: mailto link + copy button
                emails.forEach { emailAddress ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = emailAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_SENDTO,
                                            Uri.parse("mailto:$emailAddress"),
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        onMessage("ইমেইল অ্যাপ পাওয়া যায়নি")
                                    }
                                },
                        )
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(emailAddress))
                                copiedEmail = emailAddress
                                onCopied("ইমেইল")
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("copy_email_button"),
                        ) {
                            Icon(
                                imageVector = if (copiedEmail == emailAddress) {
                                    Icons.Filled.Check
                                } else {
                                    Icons.Filled.ContentCopy
                                },
                                contentDescription = "Copy email",
                                tint = if (copiedEmail == emailAddress) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                // Confirmation code
                if (app.confirmCode.isNotBlank()) {
                    InfoRow(
                        icon = Icons.Outlined.Key,
                        text = "নিশ্চিতকরণ কোড: ${app.confirmCode}",
                    )
                }

                // Follow-up date with yellow highlight if due
                if (app.followDate.isNotBlank()) {
                    val dueBg = if (isDarkTheme) DueYellowDark else DueYellowLight
                    val dueText = if (isDarkTheme) DueYellowTextDark else DueYellowTextLight
                    Surface(
                        color = if (isDue) dueBg else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .then(if (isDue) Modifier.padding(horizontal = 10.dp, vertical = 6.dp) else Modifier),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = "Follow-up date",
                                tint = if (isDue) dueText else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = buildString {
                                    append("ফলো-আপ: ")
                                    append(JobTrackerRules.formatBengaliDate(app.followDate))
                                    if (isDue) append(" — বাকি আছে!")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isDue) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDue) dueText else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Notes
                if (app.notes.isNotBlank()) {
                    InfoRow(
                        icon = Icons.Outlined.Notes,
                        text = app.notes,
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                )

                // Action Bar
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { statusDropdownExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(40.dp),
                        ) {
                            Text(
                                text = app.status.bnLabel,
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Change status",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = statusDropdownExpanded,
                            onDismissRequest = { statusDropdownExpanded = false },
                        ) {
                            ApplicationStatus.entries.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st.bnLabel) },
                                    onClick = {
                                        statusDropdownExpanded = false
                                        onStatusChanged(st)
                                    },
                                )
                            }
                        }
                    }

                    if (isDue) {
                        Button(
                            onClick = onMarkFollowUpSent,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("followup_sent_button_${app.id}"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ফলো-আপ পাঠিয়েছি",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("edit_button_${app.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "এডিট করুন",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("delete_button_${app.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "মুছে ফেলুন",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("আবেদন মুছে ফেলবেন?") },
            text = {
                Text("\"${app.company}\" (${app.role}) তালিকা থেকে স্থায়ীভাবে মুছে যাবে।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("হ্যাঁ, মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("না")
                }
            },
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
