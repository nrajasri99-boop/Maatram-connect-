package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.example.data.model.DistrictItem
import com.example.data.model.EmployerOrganization
import com.example.data.model.UserProfile
import com.example.data.model.VolunteerStats
import com.example.data.model.VolunteerType
import androidx.compose.ui.graphics.Color
import com.example.ui.components.ResponsivePair
import com.example.ui.theme.MaatramBlue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileOnboardingScreen(
    uid: String,
    email: String,
    defaultName: String,
    existingProfile: UserProfile?,
    existingStats: VolunteerStats?,
    districts: List<DistrictItem>,
    organizations: List<EmployerOrganization>,
    isBusy: Boolean,
    onSaveProfile: (UserProfile, Boolean, Boolean) -> Unit
) {
    val isNew = existingProfile == null
    val districtNames = remember(districts) {
        if (districts.isNotEmpty()) districts.map { it.name }.sorted()
        else listOf("Tiruchirappalli", "Chennai", "Coimbatore", "Madurai", "Salem", "Thanjavur", "Erode", "Vellore")
    }
    val orgList = remember(organizations) {
        if (organizations.isNotEmpty()) organizations.map { it.id to it.name }
        else listOf(
            "org_maatram_stu" to "Maatram Student Chapter",
            "org_zoho" to "Zoho Outreach Volunteers",
            "org_tcs" to "TCS Maitree Alumni Wing",
            "org_tvs" to "TVS Motor CSR Network"
        )
    }

    var fullName by remember(existingProfile) { mutableStateOf(existingProfile?.fullName ?: defaultName) }
    var phone by remember(existingProfile) { mutableStateOf(existingProfile?.phone ?: "") }
    var volunteerCode by remember(existingProfile) {
        mutableStateOf(existingProfile?.volunteerCode ?: "MF-2026-${uid.take(4).uppercase()}")
    }
    var volunteerType by remember(existingProfile) {
        mutableStateOf(VolunteerType.fromRaw(existingProfile?.volunteerType))
    }
    var college by remember(existingProfile) { mutableStateOf(existingProfile?.college ?: "") }
    var department by remember(existingProfile) { mutableStateOf(existingProfile?.department ?: "") }
    var yearOfStudy by remember(existingProfile) { mutableStateOf(existingProfile?.yearOfStudy ?: "3rd Year") }
    var selectedDistrict by remember(existingProfile) {
        mutableStateOf(existingProfile?.district ?: districtNames.first())
    }
    var city by remember(existingProfile) { mutableStateOf(existingProfile?.city ?: "") }
    var skills by remember(existingProfile) { mutableStateOf(existingProfile?.skills ?: "Mentoring, Communication") }
    var interests by remember(existingProfile) { mutableStateOf(existingProfile?.interests ?: "Physical Verification, Calling") }
    var availability by remember(existingProfile) { mutableStateOf(existingProfile?.availability ?: "Weekends") }
    var preferredContact by remember(existingProfile) { mutableStateOf(existingProfile?.preferredContact ?: "WhatsApp") }
    var selectedOrgId by remember(existingProfile) { mutableStateOf(existingProfile?.employerOrgId ?: orgList.first().first) }
    var selectedOrgName by remember(existingProfile) { mutableStateOf(existingProfile?.employerOrgName ?: orgList.first().second) }
    var consentData by remember(existingProfile) { mutableStateOf(existingProfile?.consentDataProcessing ?: true) }
    var consentLinkedIn by remember(existingProfile) { mutableStateOf(existingProfile?.consentLinkedInPublic ?: false) }
    var availableForPv by remember(existingStats) { mutableStateOf(existingStats?.availableForPv ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 780.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isNew) "Volunteer Registration & Profile Setup" else "My Volunteer Profile",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Your contact details remain private and visible only to you and authorized Maatram Office coordinators.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                    )
                    ResponsivePair(
                        first = { mod ->
                            OutlinedTextField(
                                value = volunteerCode,
                                onValueChange = { if (isNew) volunteerCode = it },
                                enabled = isNew,
                                label = { Text("Volunteer ID") },
                                singleLine = true,
                                modifier = mod
                            )
                        },
                        second = { mod ->
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Phone Number") },
                                singleLine = true,
                                modifier = mod
                            )
                        }
                    )

                    Text("Volunteer Category", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VolunteerType.entries.forEach { type ->
                            FilterChip(
                                selected = volunteerType == type,
                                onClick = { volunteerType = type },
                                label = { Text(type.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = college,
                        onValueChange = { college = it },
                        label = { Text("College / Institution") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ResponsivePair(
                        first = { mod ->
                            OutlinedTextField(
                                value = department,
                                onValueChange = { department = it },
                                label = { Text("Department") },
                                singleLine = true,
                                modifier = mod
                            )
                        },
                        second = { mod ->
                            OutlinedTextField(
                                value = yearOfStudy,
                                onValueChange = { yearOfStudy = it },
                                label = { Text("Year / Alumni") },
                                singleLine = true,
                                modifier = mod
                            )
                        }
                    )

                    Text("Primary District (for Physical Verification Allocation)", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        districtNames.forEach { dist ->
                            FilterChip(
                                selected = selectedDistrict == dist,
                                onClick = { selectedDistrict = dist },
                                label = { Text(dist) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City / Town") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Employer / Chapter Organization", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        orgList.forEach { (id, name) ->
                            FilterChip(
                                selected = selectedOrgId == id,
                                onClick = {
                                    selectedOrgId = id
                                    selectedOrgName = name
                                },
                                label = { Text(name) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = skills,
                        onValueChange = { skills = it },
                        label = { Text("Skills") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = interests,
                        onValueChange = { interests = it },
                        label = { Text("Areas of Interest") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    ResponsivePair(
                        first = { mod ->
                            OutlinedTextField(
                                value = availability,
                                onValueChange = { availability = it },
                                label = { Text("General Availability") },
                                singleLine = true,
                                modifier = mod
                            )
                        },
                        second = { mod ->
                            OutlinedTextField(
                                value = preferredContact,
                                onValueChange = { preferredContact = it },
                                label = { Text("Preferred Contact") },
                                singleLine = true,
                                modifier = mod
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Available for District Physical Verification", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Turn off if temporarily unavailable (never penalized as inactive)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = availableForPv, onCheckedChange = { availableForPv = it })
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = consentData, onCheckedChange = { consentData = it })
                        Text(
                            text = "I consent to Maatram Foundation processing my volunteer records for opportunity coordination.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = consentLinkedIn, onCheckedChange = { consentLinkedIn = it })
                        Text(
                            text = "Consent to include my name in generated LinkedIn appreciation posts.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (validationError != null) {
                        Text(
                            text = validationError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(
                        onClick = {
                            if (fullName.trim().length < 2) {
                                validationError = "Please enter your full name (at least 2 characters)."
                                return@Button
                            }
                            if (!consentData) {
                                validationError = "Data processing consent is required to register."
                                return@Button
                            }
                            validationError = null
                            onSaveProfile(
                                UserProfile(
                                    id = uid,
                                    userId = uid,
                                    fullName = fullName.trim(),
                                    email = email,
                                    phone = phone.trim(),
                                    volunteerCode = volunteerCode.trim(),
                                    volunteerType = volunteerType.name,
                                    college = college.trim(),
                                    department = department.trim(),
                                    yearOfStudy = yearOfStudy.trim(),
                                    district = selectedDistrict,
                                    city = city.trim(),
                                    skills = skills.trim(),
                                    interests = interests.trim(),
                                    availability = availability.trim(),
                                    preferredContact = preferredContact.trim(),
                                    employerOrgId = selectedOrgId,
                                    employerOrgName = selectedOrgName,
                                    consentDataProcessing = consentData,
                                    consentLinkedInPublic = consentLinkedIn,
                                    profileCompleted = true,
                                    role = existingProfile?.role ?: "VOLUNTEER"
                                ),
                                isNew,
                                availableForPv
                            )
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaatramBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_profile_button")
                    ) {
                        Text(
                            text = if (isNew) "Complete Registration" else "Save Profile Changes",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
