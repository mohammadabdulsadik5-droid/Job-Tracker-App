package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.ApplicationStatus
import com.example.domain.CvSet
import com.example.domain.GeographyCatalog
import com.example.domain.JobTrackerRules
import com.example.domain.Region

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ApplicationFormSheet(
    state: FormSheetState,
    onDismiss: () -> Unit,
    onRegionChange: (Region) -> Unit,
    onCountryChange: (String) -> Unit,
    onFormUpdate: ((FormSheetState) -> FormSheetState) -> Unit,
    onCompanyRoleChange: (String, String) -> Unit,
    onDateChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var countryMenuExpanded by remember { mutableStateOf(false) }
    var cityMenuExpanded by remember { mutableStateOf(false) }
    var showAppDatePicker by remember { mutableStateOf(false) }
    var showFollowDatePicker by remember { mutableStateOf(false) }

    val countries = remember(state.region) { GeographyCatalog.countriesForRegion(state.region) }
    val cities = remember(state.country) { GeographyCatalog.citiesForCountry(state.country) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Text(
                text = if (state.editingId == null) "নতুন আবেদন যোগ করুন" else "আবেদন এডিট করুন",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )

            if (state.errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Region selector chips
                Text(
                    text = "অঞ্চল (Region)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Region.selectableRegions.forEach { reg ->
                        FilterChip(
                            selected = state.region == reg,
                            onClick = { onRegionChange(reg) },
                            label = { Text(reg.bnLabel) },
                        )
                    }
                }

                // Country & City selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = state.country,
                            onValueChange = { onFormUpdate { s -> s.copy(country = it) } },
                            label = { Text("দেশ (Country)") },
                            trailingIcon = {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "দেশ বাছুন")
                            },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { countryMenuExpanded = true },
                        )
                        DropdownMenu(
                            expanded = countryMenuExpanded,
                            onDismissRequest = { countryMenuExpanded = false },
                        ) {
                            countries.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c) },
                                    onClick = {
                                        countryMenuExpanded = false
                                        onCountryChange(c)
                                    },
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        val displayCity = if (state.customCityMode) {
                            GeographyCatalog.CUSTOM_CITY_LABEL
                        } else {
                            state.city.ifBlank { GeographyCatalog.CUSTOM_CITY_LABEL }
                        }
                        OutlinedTextField(
                            value = displayCity,
                            onValueChange = {},
                            label = { Text("শহর (City)") },
                            trailingIcon = {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "শহর বাছুন")
                            },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { cityMenuExpanded = true },
                        )
                        DropdownMenu(
                            expanded = cityMenuExpanded,
                            onDismissRequest = { cityMenuExpanded = false },
                        ) {
                            cities.forEach { cityName ->
                                DropdownMenuItem(
                                    text = { Text(cityName) },
                                    onClick = {
                                        cityMenuExpanded = false
                                        onFormUpdate {
                                            it.copy(
                                                city = cityName,
                                                customCityMode = false,
                                                customCityInput = "",
                                            )
                                        }
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = GeographyCatalog.CUSTOM_CITY_LABEL,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                },
                                onClick = {
                                    cityMenuExpanded = false
                                    onFormUpdate { it.copy(customCityMode = true, city = "") }
                                },
                            )
                        }
                    }
                }

                if (state.customCityMode) {
                    OutlinedTextField(
                        value = state.customCityInput,
                        onValueChange = { txt -> onFormUpdate { it.copy(customCityInput = txt) } },
                        label = { Text("শহরের নাম লিখুন") },
                        placeholder = { Text("যেমন: Bergamo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Company* and Role*
                OutlinedTextField(
                    value = state.company,
                    onValueChange = { onCompanyRoleChange(it, state.role) },
                    label = { Text("কোম্পানির নাম *") },
                    placeholder = { Text("যেমন: Al Futtaim Group") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_company_input"),
                )

                OutlinedTextField(
                    value = state.role,
                    onValueChange = { onCompanyRoleChange(state.company, it) },
                    label = { Text("পদের নাম (Role) *") },
                    placeholder = { Text("যেমন: Room Attendant / Machine Operator") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_role_input"),
                )

                // Website & Email
                OutlinedTextField(
                    value = state.website,
                    onValueChange = { txt -> onFormUpdate { it.copy(website = txt) } },
                    label = { Text("ওয়েবসাইট (Website)") },
                    placeholder = { Text("company.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.email,
                    onValueChange = { txt -> onFormUpdate { it.copy(email = txt) } },
                    label = { Text("ইমেইল (কমা দিয়ে একাধিক দেওয়া যাবে)") },
                    placeholder = { Text("hr@company.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_email_input"),
                )

                OutlinedTextField(
                    value = state.confirmCode,
                    onValueChange = { txt -> onFormUpdate { it.copy(confirmCode = txt) } },
                    label = { Text("নিশ্চিতকরণ কোড (Confirmation Code)") },
                    placeholder = { Text("যেমন: REF-2026-44") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Dates Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = JobTrackerRules.formatBengaliDate(state.date),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("আবেদনের তারিখ") },
                            trailingIcon = {
                                Icon(Icons.Filled.CalendarToday, contentDescription = "তারিখ বাছুন")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showAppDatePicker = true },
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = JobTrackerRules.formatBengaliDate(state.followDate),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("ফলো-আপ তারিখ") },
                            trailingIcon = {
                                Icon(Icons.Filled.CalendarToday, contentDescription = "ফলো-আপ তারিখ")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showFollowDatePicker = true },
                        )
                    }
                }

                // CV Set chips
                Text(
                    text = "CV সেট",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CvSet.entries.forEach { set ->
                        FilterChip(
                            selected = state.cvSet == set,
                            onClick = { onFormUpdate { it.copy(cvSet = set) } },
                            label = { Text(set.label) },
                            leadingIcon = if (state.cvSet == set) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }

                OutlinedTextField(
                    value = state.cvFile,
                    onValueChange = { txt ->
                        onFormUpdate { it.copy(cvFile = txt, cvFileManuallyEdited = txt.isNotBlank()) }
                    },
                    label = { Text("CV ফাইলের নাম (Hotel/Textile অটো-সাজেস্ট)") },
                    placeholder = { Text("যেমন: Hotel CV বা Textile CV") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Status chips
                Text(
                    text = "স্ট্যাটাস",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ApplicationStatus.entries.forEach { st ->
                        FilterChip(
                            selected = state.status == st,
                            onClick = { onFormUpdate { it.copy(status = st) } },
                            label = { Text(st.bnLabel) },
                        )
                    }
                }

                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { txt -> onFormUpdate { it.copy(notes = txt) } },
                    label = { Text("নোটস (Notes)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(6.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Sticky Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("form_cancel_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("বাতিল")
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("form_save_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("সেভ করুন")
                }
            }
        }
    }

    if (showAppDatePicker) {
        IsoDatePickerModal(
            initialIso = state.date,
            onDismiss = { showAppDatePicker = false },
            onConfirm = { iso ->
                showAppDatePicker = false
                onDateChange(iso)
            },
        )
    }

    if (showFollowDatePicker) {
        IsoDatePickerModal(
            initialIso = state.followDate,
            onDismiss = { showFollowDatePicker = false },
            onConfirm = { iso ->
                showFollowDatePicker = false
                onFormUpdate { it.copy(followDate = iso, followDateManuallyEdited = true) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IsoDatePickerModal(
    initialIso: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val initialMillis = remember(initialIso) {
        JobTrackerRules.isoToMillis(initialIso) ?: System.currentTimeMillis()
    }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis ?: initialMillis
                    onConfirm(JobTrackerRules.millisToIso(millis))
                },
            ) {
                Text("ঠিক আছে")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}
