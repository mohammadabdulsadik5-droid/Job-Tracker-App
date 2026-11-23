package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.ApplicationStatus
import com.example.domain.JobApplication
import com.example.domain.JobTrackerRules

private enum class FollowUpSegment(val bnLabel: String) {
    DUE("বাকি আছে"),
    SENT("পাঠানো হয়েছে"),
    INTERVIEW("ইন্টারভিউ"),
}

@Composable
fun FollowUpsTab(
    uiState: DashboardUiState,
    onStatusChanged: (JobApplication, ApplicationStatus) -> Unit,
    onMarkFollowUpSent: (JobApplication) -> Unit,
    onEditApplication: (JobApplication) -> Unit,
    onDeleteApplication: (JobApplication) -> Unit,
    onCopied: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    var segment by remember { mutableStateOf(FollowUpSegment.DUE) }
    val context = LocalContext.current

    val sentList = remember(uiState.allApplications) {
        uiState.allApplications.filter { it.status == ApplicationStatus.FOLLOWUP }
    }
    val interviewList = remember(uiState.allApplications) {
        uiState.allApplications.filter { it.status == ApplicationStatus.INTERVIEW }
    }

    val displayed = when (segment) {
        FollowUpSegment.DUE -> uiState.dueApplications
        FollowUpSegment.SENT -> sentList
        FollowUpSegment.INTERVIEW -> interviewList
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "followup_segments") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FollowUpSegment.entries.forEach { seg ->
                    val count = when (seg) {
                        FollowUpSegment.DUE -> uiState.dueApplications.size
                        FollowUpSegment.SENT -> sentList.size
                        FollowUpSegment.INTERVIEW -> interviewList.size
                    }
                    FilterChip(
                        selected = segment == seg,
                        onClick = { segment = seg },
                        label = {
                            Text(
                                text = "${seg.bnLabel} (${JobTrackerRules.toBengaliDigits(count)})",
                                maxLines = 1,
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

        if (displayed.isEmpty()) {
            item(key = "followup_empty") {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = when (segment) {
                                FollowUpSegment.DUE -> "🎉 এই মুহূর্তে কোনো ফলো-আপ বাকি নেই!"
                                FollowUpSegment.SENT -> "📭 এখনো কোনো ফলো-আপ পাঠানো হয়নি"
                                FollowUpSegment.INTERVIEW -> "📭 এখনো কোনো ইন্টারভিউ ডাক তালিকাভুক্ত নেই"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "নতুন আবেদন যোগ করলে ১৫ দিন পর স্বয়ংক্রিয়ভাবে এখানে ফলো-আপ রিমাইন্ডার দেখাবে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(
                items = displayed,
                key = { "fu_${it.id}" },
            ) { app ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

                    if (segment == FollowUpSegment.DUE) {
                        val primaryEmail = JobTrackerRules.extractEmails(app.email).firstOrNull()
                        if (primaryEmail != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val subject = Uri.encode("Follow-up on my application for ${app.role} - ${app.company}")
                                            val body = Uri.encode(
                                                "Dear Hiring Team,\n\nI applied for the ${app.role} position at ${app.company}. " +
                                                    "I am writing to politely follow up on my application.\n\nBest regards,"
                                            )
                                            val intent = Intent(
                                                Intent.ACTION_SENDTO,
                                                Uri.parse("mailto:$primaryEmail?subject=$subject&body=$body"),
                                            )
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            onMessage("ইমেইল অ্যাপ পাওয়া যায়নি")
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("অটো-ড্রাফট ইমেইল")
                                }
                                Button(
                                    onClick = { onMarkFollowUpSent(app) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("পাঠিয়েছি")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
