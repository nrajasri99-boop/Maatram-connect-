package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DistrictItem
import com.example.data.model.Opportunity
import com.example.data.model.OpportunityCategory
import com.example.data.model.OpportunityMode
import com.example.data.model.OpportunityStatus
import com.example.data.model.PhysicalVerificationCase
import com.example.data.model.PvCaseStatus
import com.example.data.model.RequestStatus
import com.example.data.model.VerificationStatus
import com.example.data.model.VolunteerHours
import com.example.data.model.VolunteerRequest
import com.example.data.model.VolunteerStats
import com.example.ui.components.BarChartCard
import com.example.ui.components.CategoryBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ResponsiveGrid
import com.example.ui.components.ResponsivePair
import com.example.ui.components.ResponsiveTabBar
import com.example.ui.components.StatSummaryCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.MaatramBlue
import com.example.ui.theme.MaatramGreen
import com.example.ui.theme.MaatramNavy
import com.example.ui.theme.MaatramOrange
import com.example.ui.theme.MaatramOutline

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OfficeDashboardAndRequestsScreen(
    allStats: List<VolunteerStats>,
    opportunities: List<Opportunity>,
    requests: List<VolunteerRequest>,
    pvCases: List<PhysicalVerificationCase>,
    volunteerHours: List<VolunteerHours>,
    districts: List<DistrictItem>,
    isBusy: Boolean,
    onCreateOpportunity: (Opportunity, Int) -> Unit,
    onToggleOppStatus: (Opportunity, OpportunityStatus) -> Unit,
    onReviewRequest: (VolunteerRequest, Opportunity, RequestStatus, String) -> Unit,
    onOpenFairAllocation: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val pendingReqsCount = requests.count { it.status == RequestStatus.PENDING.name || it.status == RequestStatus.UNDER_REVIEW.name }
    val tabs = listOf("Overview", "Works by Category", "Requests ($pendingReqsCount)")
    var selectedWorkCategory by remember { mutableStateOf(OpportunityCategory.PHYSICAL_VERIFICATION) }
    var showCreateOppDialog by remember { mutableStateOf(false) }
    var initialDialogCategory by remember { mutableStateOf(OpportunityCategory.PHYSICAL_VERIFICATION) }
    var rejectTarget by remember { mutableStateOf<Pair<VolunteerRequest, Opportunity>?>(null) }

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
                        val unassignedPv = pvCases.count { it.status == PvCaseStatus.UNASSIGNED.name }
                        val pendingHours = volunteerHours.count { it.verificationStatus == VerificationStatus.PENDING.name }
                        val availableVols = allStats.count { it.availableForPv }

                        val kpiItems = listOf(
                            Triple("Pending Requests", "$pendingReqsCount", "Need Office review") to (Icons.Filled.PendingActions to MaatramOrange),
                            Triple("Available Volunteers", "$availableVols", "Ready for PV & works") to (Icons.Filled.Groups to MaatramGreen),
                            Triple("Unassigned PV Cases", "$unassignedPv", "Ready for fair allocation") to (Icons.Filled.HomeWork to MaatramBlue),
                            Triple("Pending Hours", "$pendingHours", "Awaiting verification") to (Icons.Filled.Schedule to MaatramBlue)
                        )
                        ResponsiveGrid(
                            items = kpiItems,
                            compactColumns = 2,
                            mediumColumns = 2,
                            expandedColumns = 4
                        ) { (info, visual), mod ->
                            StatSummaryCard(info.first, info.second, info.third, visual.first, visual.second, mod)
                        }

                        ResponsivePair(
                            first = { mod ->
                                Button(
                                    onClick = {
                                        initialDialogCategory = OpportunityCategory.PHYSICAL_VERIFICATION
                                        showCreateOppDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White),
                                    modifier = mod.testTag("office_create_opp_btn")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Text(" Post Volunteer Work", maxLines = 1)
                                }
                            },
                            second = { mod ->
                                OutlinedButton(
                                    onClick = onOpenFairAllocation,
                                    modifier = mod.testTag("office_open_fair_alloc_btn")
                                ) {
                                    Text("Fair PV Allocation", maxLines = 1)
                                }
                            }
                        )

                        val distAvailability = allStats
                            .filter { it.availableForPv }
                            .groupBy { it.district }
                            .entries.sortedByDescending { it.value.size }.take(6)
                            .map { it.key.take(6) to it.value.size.toFloat() }
                        BarChartCard(
                            title = "District-Wise Available Volunteers",
                            subtitle = "Active volunteers available for district deployment",
                            items = distAvailability,
                            barColor = MaatramGreen
                        )
                    }

                    1 -> {
                        // Category-wise Pages matching the Student Volunteer Works categories
                        Text(
                            text = "Select Work Category Page",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaatramNavy
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                OpportunityCategory.PHYSICAL_VERIFICATION,
                                OpportunityCategory.CALLING_BGV,
                                OpportunityCategory.INTERVIEWS,
                                OpportunityCategory.EVENTS
                            ).forEach { cat ->
                                val count = opportunities.count { it.category == cat.name }
                                FilterChip(
                                    selected = selectedWorkCategory == cat,
                                    onClick = { selectedWorkCategory = cat },
                                    label = { Text("${cat.shortLabel} ($count)") },
                                    modifier = Modifier.testTag("office_cat_chip_${cat.name}")
                                )
                            }
                        }

                        Button(
                            onClick = {
                                initialDialogCategory = selectedWorkCategory
                                showCreateOppDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().testTag("office_create_category_work_btn")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text(" Create ${selectedWorkCategory.shortLabel} Work", fontWeight = FontWeight.Bold)
                        }

                        val catOpps = opportunities.filter { it.category == selectedWorkCategory.name }
                        if (catOpps.isEmpty()) {
                            EmptyStateCard(
                                title = "No ${selectedWorkCategory.label} Works Yet",
                                message = "Tap Create ${selectedWorkCategory.shortLabel} Work above to publish one for students."
                            )
                        } else {
                            catOpps.forEach { opp ->
                                val casesCount = pvCases.count { it.opportunityId == opp.id }
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, MaatramOutline)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            CategoryBadge(opp.category)
                                            StatusPill(opp.status)
                                        }
                                        Text(opp.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = buildString {
                                                append("${opp.district} • ${opp.eventDate} • Slots: ${opp.availableSlots}/${opp.requiredVolunteers}")
                                                if (casesCount > 0) append(" • $casesCount PV Cases")
                                            }
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (opp.status == OpportunityStatus.OPEN.name) {
                                                OutlinedButton(
                                                    onClick = { onToggleOppStatus(opp, OpportunityStatus.CLOSED) },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Close") }
                                            } else {
                                                OutlinedButton(
                                                    onClick = { onToggleOppStatus(opp, OpportunityStatus.OPEN) },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Reopen") }
                                            }
                                            if (opp.category == OpportunityCategory.PHYSICAL_VERIFICATION.name) {
                                                Button(
                                                    onClick = onOpenFairAllocation,
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Allocate PV") }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Student availability requests specific to this category
                        val catRequests = requests.filter {
                            it.category == selectedWorkCategory.name &&
                                (it.status == RequestStatus.PENDING.name || it.status == RequestStatus.UNDER_REVIEW.name)
                        }
                        if (catRequests.isNotEmpty()) {
                            Text(
                                text = "Pending ${selectedWorkCategory.shortLabel} Requests (${catRequests.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaatramNavy
                            )
                            catRequests.forEach { req ->
                                val opp = opportunities.firstOrNull { it.id == req.opportunityId }
                                RequestReviewCard(
                                    req = req,
                                    opp = opp,
                                    volStats = allStats.firstOrNull { it.userId == req.userId },
                                    isBusy = isBusy,
                                    onReviewRequest = onReviewRequest,
                                    onStartReject = { r, o -> rejectTarget = r to o }
                                )
                            }
                        }
                    }

                    2 -> {
                        var statusFilter by remember { mutableStateOf("PENDING") }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ALL", "PENDING", "APPROVED", "REJECTED", "WAITLISTED").forEach { st ->
                                FilterChip(
                                    selected = statusFilter == st,
                                    onClick = { statusFilter = st },
                                    label = { Text(st) }
                                )
                            }
                        }
                        val filtered = requests.filter { statusFilter == "ALL" || it.status == statusFilter }
                        if (filtered.isEmpty()) {
                            EmptyStateCard("No Requests in This Filter", "Student availability requests appear here in real time.")
                        } else {
                            filtered.forEach { req ->
                                val opp = opportunities.firstOrNull { it.id == req.opportunityId }
                                RequestReviewCard(
                                    req = req,
                                    opp = opp,
                                    volStats = allStats.firstOrNull { it.userId == req.userId },
                                    isBusy = isBusy,
                                    onReviewRequest = onReviewRequest,
                                    onStartReject = { r, o -> rejectTarget = r to o }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showCreateOppDialog) {
        val districtNames = remember(districts) {
            if (districts.isNotEmpty()) districts.map { it.name }.sorted()
            else listOf("Tiruchirappalli", "Chennai", "Coimbatore", "Madurai", "Salem")
        }
        var title by remember { mutableStateOf("") }
        var category by remember(initialDialogCategory) { mutableStateOf(initialDialogCategory) }
        var activityType by remember(initialDialogCategory) { mutableStateOf(initialDialogCategory.label) }
        var district by remember { mutableStateOf(districtNames.first()) }
        var location by remember { mutableStateOf("") }
        var mode by remember(initialDialogCategory) {
            mutableStateOf(
                if (initialDialogCategory == OpportunityCategory.CALLING_BGV) OpportunityMode.ONLINE
                else OpportunityMode.OFFLINE
            )
        }
        var eventDate by remember { mutableStateOf("2026-12-15") }
        var startTime by remember { mutableStateOf("09:00") }
        var endTime by remember { mutableStateOf("16:00") }
        var reqVols by remember { mutableStateOf("4") }
        var pvCount by remember { mutableStateOf("6") }
        var instructions by remember { mutableStateOf("") }
        var whatsappLink by remember { mutableStateOf("https://chat.whatsapp.com/maatram-invite") }

        AlertDialog(
            onDismissRequest = { showCreateOppDialog = false },
            title = { Text("Create Volunteer Work") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Work Title *") }, modifier = Modifier.fillMaxWidth())
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OpportunityCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = {
                                    category = cat
                                    activityType = cat.label
                                    if (cat == OpportunityCategory.CALLING_BGV) mode = OpportunityMode.ONLINE
                                },
                                label = { Text(cat.shortLabel) }
                            )
                        }
                    }
                    Text("District", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        districtNames.forEach { d ->
                            FilterChip(selected = district == d, onClick = { district = d }, label = { Text(d) })
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OpportunityMode.entries.forEach { m ->
                            FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.label) })
                        }
                    }
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location / Zones") }, modifier = Modifier.fillMaxWidth())
                    ResponsivePair(
                        breakpoint = 320.dp,
                        spacing = 8.dp,
                        first = { mod -> OutlinedTextField(value = eventDate, onValueChange = { eventDate = it }, label = { Text("Date") }, singleLine = true, modifier = mod) },
                        second = { mod -> OutlinedTextField(value = reqVols, onValueChange = { reqVols = it }, label = { Text("Volunteers") }, singleLine = true, modifier = mod) }
                    )
                    ResponsivePair(
                        breakpoint = 320.dp,
                        spacing = 8.dp,
                        first = { mod -> OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Start") }, singleLine = true, modifier = mod) },
                        second = { mod -> OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("End") }, singleLine = true, modifier = mod) }
                    )
                    if (category == OpportunityCategory.PHYSICAL_VERIFICATION) {
                        OutlinedTextField(value = pvCount, onValueChange = { pvCount = it }, label = { Text("PV Cases to Generate") }, modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(value = whatsappLink, onValueChange = { whatsappLink = it }, label = { Text("WhatsApp Invite Link (Optional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text("Instructions") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.trim().length >= 3) {
                            onCreateOpportunity(
                                Opportunity(
                                    title = title.trim(),
                                    category = category.name,
                                    activityType = activityType,
                                    description = instructions.ifBlank { "$activityType in $district" },
                                    district = district,
                                    location = location.ifBlank { district },
                                    mode = mode.name,
                                    eventDate = eventDate,
                                    startTime = startTime,
                                    endTime = endTime,
                                    deadline = eventDate,
                                    requiredVolunteers = (reqVols.toLongOrNull() ?: 4L).coerceAtLeast(1L),
                                    availableSlots = (reqVols.toLongOrNull() ?: 4L).coerceAtLeast(1L),
                                    instructions = instructions,
                                    whatsappGroupLink = whatsappLink,
                                    status = OpportunityStatus.OPEN.name
                                ),
                                if (category == OpportunityCategory.PHYSICAL_VERIFICATION) (pvCount.toIntOrNull() ?: 4).coerceIn(0, 50) else 0
                            )
                            showCreateOppDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White)
                ) { Text("Publish") }
            },
            dismissButton = { TextButton(onClick = { showCreateOppDialog = false }) { Text("Cancel") } }
        )
    }

    rejectTarget?.let { (req, opp) ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject Request") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (visible to student)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReviewRequest(req, opp, RequestStatus.REJECTED, reason.ifBlank { "Slots filled via fair priority" })
                        rejectTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Confirm Reject") }
            },
            dismissButton = { TextButton(onClick = { rejectTarget = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RequestReviewCard(
    req: VolunteerRequest,
    opp: Opportunity?,
    volStats: VolunteerStats?,
    isBusy: Boolean,
    onReviewRequest: (VolunteerRequest, Opportunity, RequestStatus, String) -> Unit,
    onStartReject: (VolunteerRequest, Opportunity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaatramOutline)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${req.volunteerName} (${req.volunteerCode})", style = MaterialTheme.typography.titleMedium)
                StatusPill(req.status)
            }
            Text("${req.opportunityTitle} • ${req.district}")
            if (volStats != null) {
                Text(
                    text = "History: ${String.format("%.1f", volStats.verifiedHours)}h • ${volStats.completedPvCases} PV done • ${volStats.activePvCases} active PV",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (req.availabilityNote.isNotBlank()) {
                Text("Availability: ${req.availabilityNote}", style = MaterialTheme.typography.bodyMedium)
            }
            if (opp != null && (req.status == RequestStatus.PENDING.name || req.status == RequestStatus.UNDER_REVIEW.name || req.status == RequestStatus.WAITLISTED.name)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onReviewRequest(req, opp, RequestStatus.APPROVED, "") },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = MaatramGreen)
                    ) { Text("Approve", maxLines = 1) }
                    OutlinedButton(
                        onClick = { onReviewRequest(req, opp, RequestStatus.WAITLISTED, "") },
                        enabled = !isBusy
                    ) { Text("Waitlist", maxLines = 1) }
                    OutlinedButton(
                        onClick = { onStartReject(req, opp) },
                        enabled = !isBusy
                    ) { Text("Reject", maxLines = 1) }
                }
            }
        }
    }
}
