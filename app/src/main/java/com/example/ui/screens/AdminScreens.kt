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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.Assignment
import com.example.data.model.AuditLog
import com.example.data.model.DistrictItem
import com.example.data.model.EmployerOrganization
import com.example.data.model.EngagementStatus
import com.example.data.model.Opportunity
import com.example.data.model.OpportunityCategory
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.VerificationStatus
import com.example.data.model.VolunteerHours
import com.example.data.model.VolunteerRequest
import com.example.data.model.VolunteerStats
import com.example.data.model.VolunteerType
import com.example.domain.FairAllocationEngine
import com.example.ui.components.BarChartCard
import com.example.ui.components.ResponsiveGrid
import com.example.ui.components.ResponsivePair
import com.example.ui.components.ResponsiveTabBar
import com.example.ui.components.ShareUtils
import com.example.ui.components.StatSummaryCard
import com.example.ui.components.StatusPill
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MaatramBlue
import com.example.ui.theme.MaatramGreen
import com.example.ui.theme.MaatramOrange

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    settings: AppSettings,
    allStats: List<VolunteerStats>,
    allProfiles: List<UserProfile>,
    opportunities: List<Opportunity>,
    requests: List<VolunteerRequest>,
    assignments: List<Assignment>,
    volunteerHours: List<VolunteerHours>,
    districts: List<DistrictItem>,
    organizations: List<EmployerOrganization>,
    auditLogs: List<AuditLog>,
    isBusy: Boolean,
    onAddDistrict: (String, String) -> Unit,
    onAddOrganization: (String, String, String) -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onPromoteRole: (UserProfile, UserRole) -> Unit,
    onSeedDemoData: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Analytics", "Engagement", "Employers", "Settings & Audit")

    Column(modifier = Modifier.fillMaxSize()) {
        ResponsiveTabBar(
            tabs = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 1040.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        var districtFilter by remember { mutableStateOf("All") }
                        val filteredStats = remember(allStats, districtFilter) {
                            allStats.filter { districtFilter == "All" || it.district == districtFilter }
                        }
                        val approvedHours = remember(volunteerHours, districtFilter) {
                            volunteerHours.filter {
                                it.verificationStatus == VerificationStatus.APPROVED.name &&
                                    (districtFilter == "All" || it.district == districtFilter)
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = districtFilter == "All", onClick = { districtFilter = "All" }, label = { Text("All Districts") })
                            districts.forEach { d ->
                                FilterChip(selected = districtFilter == d.name, onClick = { districtFilter = d.name }, label = { Text(d.name) })
                            }
                        }

                        val totalVols = filteredStats.size
                        val students = filteredStats.count { it.volunteerType == VolunteerType.STUDENT.name }
                        val alumni = filteredStats.count { it.volunteerType == VolunteerType.ALUMNI.name }
                        val totalHours = filteredStats.sumOf { it.verifiedHours }.takeIf { it > 0 }
                            ?: approvedHours.sumOf { it.submittedHours }
                        val completedAssigns = filteredStats.sumOf { it.completedActivities.toInt() }

                        val adminKpis = listOf(
                            Triple("Volunteers", "$totalVols", "$students Students • $alumni Alumni") to (Icons.Filled.Groups to MaatramBlue),
                            Triple("Verified Hours", String.format("%.1fh", totalHours), "Across ${opportunities.size} opps") to (Icons.Filled.AccessTimeFilled to MaatramGreen),
                            Triple("Completed Work", "$completedAssigns", "${requests.size} total requests") to (Icons.Filled.CheckCircle to MaatramBlue),
                            Triple("Employer Orgs", "${organizations.size}", "Active CSR partners") to (Icons.Filled.Business to MaatramOrange)
                        )
                        ResponsiveGrid(
                            items = adminKpis,
                            compactColumns = 2,
                            mediumColumns = 2,
                            expandedColumns = 4
                        ) { (info, visual), mod ->
                            StatSummaryCard(info.first, info.second, info.third, visual.first, visual.second, mod)
                        }

                        val distChart = allStats.groupBy { it.district }
                            .entries.sortedByDescending { it.value.size }.take(6)
                            .map { it.key.take(6) to it.value.size.toFloat() }
                        BarChartCard("District-Wise Volunteer Distribution", "Registered volunteers per district", distChart, MaatramBlue)

                        val catChart = OpportunityCategory.entries.map { cat ->
                            val hrs = approvedHours.filter { it.category == cat.name }.sumOf { it.submittedHours }.toFloat()
                            val fallbackCount = opportunities.count { it.category == cat.name }.toFloat()
                            cat.shortLabel to (if (hrs > 0f) hrs else fallbackCount)
                        }
                        BarChartCard("Category-Wise Contributions", "Verified hours / active opportunities by category", catChart, MaatramGreen)
                    }

                    1 -> {
                        val engagementList = remember(allStats, requests, assignments, settings) {
                            FairAllocationEngine.classifyEngagement(allStats, requests, assignments, settings)
                        }
                        var statusFilter by remember { mutableStateOf<EngagementStatus?>(null) }

                        Text(
                            text = "Thresholds: Active <= ${settings.activeThresholdDays}d | Less Active <= ${settings.lessActiveThresholdDays}d | Inactive <= ${settings.inactiveThresholdDays}d",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("All (${engagementList.size})") })
                            EngagementStatus.entries.forEach { st ->
                                val count = engagementList.count { it.status == st }
                                FilterChip(
                                    selected = statusFilter == st,
                                    onClick = { statusFilter = if (statusFilter == st) null else st },
                                    label = { Text("${st.label} ($count)") }
                                )
                            }
                        }

                        engagementList
                            .filter { statusFilter == null || it.status == statusFilter }
                            .forEach { rec ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(rec.stats.displayName, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = "${rec.stats.district} • Last verified: ${rec.daysSinceLastVerified?.let { "${it}d ago" } ?: "Never"} • Pending req: ${rec.pendingRequestCount}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        StatusPill(rec.status.label)
                                    }
                                }
                            }
                    }

                    2 -> {
                        var orgName by remember { mutableStateOf("") }
                        var orgIndustry by remember { mutableStateOf("Technology") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Employer Organization Reports", style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(onClick = {
                                val csv = FairAllocationEngine.exportEmployerSummaryCsv(allStats, volunteerHours)
                                ShareUtils.shareText(context, "Maatram Employer Organization Report CSV", csv)
                            }) { Text("Export Employer CSV") }
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Add Employer Organization", style = MaterialTheme.typography.titleMedium)
                                ResponsivePair(
                                    first = { mod ->
                                        OutlinedTextField(value = orgName, onValueChange = { orgName = it }, label = { Text("Org Name") }, singleLine = true, modifier = mod)
                                    },
                                    second = { mod ->
                                        OutlinedTextField(value = orgIndustry, onValueChange = { orgIndustry = it }, label = { Text("Industry") }, singleLine = true, modifier = mod)
                                    }
                                )
                                Button(
                                    onClick = {
                                        if (orgName.trim().length >= 2) {
                                            onAddOrganization(orgName, orgIndustry, "HR Outreach")
                                            orgName = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White)
                                ) { Text("Save Organization") }
                            }
                        }

                        val orgNames = (organizations.map { it.name } + allStats.map { it.employerOrgName }).filter { it.isNotBlank() }.distinct().sorted()
                        orgNames.forEach { org ->
                            val linked = allStats.filter { it.employerOrgName == org }
                            val hrs = linked.sumOf { it.verifiedHours }
                            val acts = linked.sumOf { it.completedActivities }
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(org, style = MaterialTheme.typography.titleMedium)
                                        StatusPill("${linked.size} VOLUNTEERS")
                                    }
                                    Text("Verified Hours: ${String.format("%.1f", hrs)}h • Completed Activities: $acts")
                                }
                            }
                        }
                    }

                    3 -> {
                        // Seed Demo Workspace button
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Demo Dataset & Initial Workspace Setup", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Populates 20 clearly labelled fictional volunteers, 5 Office Team coordinators, 8 Tamil Nadu districts, 10 opportunities across all 4 categories, PV cases, and calling tasks.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = onSeedDemoData,
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth().testTag("seed_demo_button")
                                ) {
                                    Text("Seed Fictional Demo Workspace", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Configurable Allocation & Inactivity Rules
                        var maxPv by remember(settings) { mutableStateOf(settings.maxActivePvCases.toString()) }
                        var activeDays by remember(settings) { mutableStateOf(settings.activeThresholdDays.toString()) }
                        var lessActiveDays by remember(settings) { mutableStateOf(settings.lessActiveThresholdDays.toString()) }
                        var inactiveDays by remember(settings) { mutableStateOf(settings.inactiveThresholdDays.toString()) }
                        var emailConfigured by remember(settings) { mutableStateOf(settings.emailProviderConfigured) }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Allocation & Engagement Thresholds", style = MaterialTheme.typography.titleMedium)
                                ResponsivePair(
                                    first = { mod -> OutlinedTextField(value = maxPv, onValueChange = { maxPv = it }, label = { Text("Max Active PV") }, singleLine = true, modifier = mod) },
                                    second = { mod -> OutlinedTextField(value = activeDays, onValueChange = { activeDays = it }, label = { Text("Active (d)") }, singleLine = true, modifier = mod) }
                                )
                                ResponsivePair(
                                    first = { mod -> OutlinedTextField(value = lessActiveDays, onValueChange = { lessActiveDays = it }, label = { Text("Less Active (d)") }, singleLine = true, modifier = mod) },
                                    second = { mod -> OutlinedTextField(value = inactiveDays, onValueChange = { inactiveDays = it }, label = { Text("Inactive (d)") }, singleLine = true, modifier = mod) }
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "External Email API Configured (else Demo Preview)",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(checked = emailConfigured, onCheckedChange = { emailConfigured = it })
                                }
                                Button(
                                    onClick = {
                                        onUpdateSettings(
                                            settings.copy(
                                                maxActivePvCases = maxPv.toLongOrNull() ?: 3L,
                                                activeThresholdDays = activeDays.toLongOrNull() ?: 30L,
                                                lessActiveThresholdDays = lessActiveDays.toLongOrNull() ?: 60L,
                                                inactiveThresholdDays = inactiveDays.toLongOrNull() ?: 90L,
                                                emailProviderConfigured = emailConfigured
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White)
                                ) { Text("Save Settings") }
                            }
                        }

                        // Dynamic Districts
                        var distName by remember { mutableStateOf("") }
                        var distCode by remember { mutableStateOf("") }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("District Management (${districts.size} Active)", style = MaterialTheme.typography.titleMedium)
                                ResponsivePair(
                                    first = { mod -> OutlinedTextField(value = distName, onValueChange = { distName = it }, label = { Text("District Name") }, singleLine = true, modifier = mod) },
                                    second = { mod -> OutlinedTextField(value = distCode, onValueChange = { distCode = it }, label = { Text("Code") }, singleLine = true, modifier = mod) }
                                )
                                Button(
                                    onClick = {
                                        if (distName.trim().length >= 2 && distCode.trim().length >= 2) {
                                            onAddDistrict(distName, distCode)
                                            distName = ""; distCode = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White)
                                ) { Text("Add District") }
                            }
                        }

                        // Role Management
                        if (allProfiles.isNotEmpty()) {
                            Text("User & Role Management", style = MaterialTheme.typography.titleMedium)
                            allProfiles.take(10).forEach { prof ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prof.fullName, style = MaterialTheme.typography.titleMedium)
                                            Text("${prof.volunteerCode} • ${prof.role}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        if (prof.role == UserRole.VOLUNTEER.name) {
                                            OutlinedButton(onClick = { onPromoteRole(prof, UserRole.OFFICE_TEAM) }) {
                                                Text("Make Office")
                                            }
                                        } else {
                                            StatusPill(prof.role)
                                        }
                                    }
                                }
                            }
                        }

                        // Immutable Audit Logs
                        Text("Immutable Audit Logs (${auditLogs.size})", style = MaterialTheme.typography.titleMedium)
                        auditLogs.sortedByDescending { it.createdAt?.seconds ?: 0L }.take(20).forEach { log ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(log.action, style = MaterialTheme.typography.labelLarge)
                                        StatusPill(log.actorRole)
                                    }
                                    Text(log.details, style = MaterialTheme.typography.bodyMedium)
                                    if (log.reason.isNotBlank()) {
                                        Text("Reason: ${log.reason}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                                    }
                                    Text("By ${log.actorName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
