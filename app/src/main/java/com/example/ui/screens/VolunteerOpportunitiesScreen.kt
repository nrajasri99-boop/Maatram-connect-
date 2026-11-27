package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Assignment
import com.example.data.model.CallStatus
import com.example.data.model.CallingTask
import com.example.data.model.DistrictItem
import com.example.data.model.Opportunity
import com.example.data.model.OpportunityCategory
import com.example.data.model.OpportunityStatus
import com.example.data.model.PhysicalVerificationCase
import com.example.data.model.PvCaseStatus
import com.example.data.model.RequestStatus
import com.example.data.model.VolunteerRequest
import com.example.ui.components.CategoryBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ResponsiveGrid
import com.example.ui.components.StatusPill
import com.example.ui.theme.MaatramBlue
import com.example.ui.theme.MaatramBlueContainer
import com.example.ui.theme.MaatramBlueSoft
import com.example.ui.theme.MaatramGreen
import com.example.ui.theme.MaatramGreenSoft
import com.example.ui.theme.MaatramNavy
import com.example.ui.theme.MaatramOutline

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VolunteerOpportunitiesScreen(
    opportunities: List<Opportunity>,
    myRequests: List<VolunteerRequest>,
    districts: List<DistrictItem>,
    userDistrict: String,
    currentUserId: String = "",
    availableForPv: Boolean = true,
    myAssignments: List<Assignment> = emptyList(),
    pvCases: List<PhysicalVerificationCase> = emptyList(),
    callingTasks: List<CallingTask> = emptyList(),
    isBusy: Boolean,
    onTogglePvAvailability: (Boolean) -> Unit = {},
    onRequestJoin: (Opportunity, String) -> Unit,
    onCancelRequest: (VolunteerRequest) -> Unit,
    onUpdatePvCase: (String, PvCaseStatus, String, String) -> Unit = { _, _, _, _ -> },
    onUpdateCallingTask: (String, CallStatus, Boolean, String) -> Unit = { _, _, _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<OpportunityCategory?>(null) }
    var selectedDistrict by remember { mutableStateOf("All") }
    var requestDialogOpportunity by remember { mutableStateOf<Opportunity?>(null) }
    var availabilityNote by remember { mutableStateOf("I am available as scheduled") }

    if (selectedCategory != null) {
        BackHandler {
            selectedCategory = null
            selectedDistrict = "All"
        }
    }

    val orderedCategories = remember {
        listOf(
            OpportunityCategory.PHYSICAL_VERIFICATION,
            OpportunityCategory.CALLING_BGV,
            OpportunityCategory.INTERVIEWS,
            OpportunityCategory.EVENTS
        )
    }

    val districtOptions = remember(districts) {
        listOf("All") + (if (districts.isNotEmpty()) districts.map { it.name }.sorted()
        else listOf("Tiruchirappalli", "Chennai", "Coimbatore", "Madurai", "Salem"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 1040.dp)
                .fillMaxSize()
        ) {
            val activeCat = selectedCategory
            if (activeCat == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                // LEVEL 1: Volunteer Works Hub (4 Dedicated Category Pages)
                Text(
                    text = "Volunteer Works",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaatramNavy
                )
                Text(
                    text = "Works posted by Admin and Office Team. Choose a category to open its dedicated page.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search works by title or district...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaatramBlue) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("opportunity_search_input")
                )

                val visibleCategories = orderedCategories.filter { cat ->
                    if (searchQuery.isBlank()) true
                    else cat.label.contains(searchQuery, ignoreCase = true) ||
                        opportunities.any {
                            it.category == cat.name && (
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                    it.district.contains(searchQuery, ignoreCase = true)
                                )
                        }
                }

                ResponsiveGrid(
                    items = visibleCategories,
                    compactColumns = 1,
                    mediumColumns = 2,
                    expandedColumns = 2
                ) { cat, mod ->
                    val catOpps = opportunities.filter { it.category == cat.name }
                    val openCount = catOpps.count { it.status == OpportunityStatus.OPEN.name }
                    val meta = categoryMeta(cat)
                    val subtitleExtra = when (cat) {
                        OpportunityCategory.PHYSICAL_VERIFICATION -> {
                            val assignedCount = pvCases.count { currentUserId.isNotBlank() && it.assignedToUid == currentUserId }
                            if (assignedCount > 0) "PV Available: $openCount open • $assignedCount assigned to you"
                            else if (availableForPv) "PV Available • $openCount open for request"
                            else "Mark PV available & send request"
                        }
                        OpportunityCategory.CALLING_BGV -> {
                            val assigned = callingTasks.count { currentUserId.isNotBlank() && it.assignedToUid == currentUserId }
                            if (assigned > 0) "$assigned assigned to you • $openCount open" else meta.second
                        }
                        else -> meta.second
                    }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, MaatramOutline),
                        modifier = mod
                            .testTag("category_card_${cat.name}")
                            .clickable { selectedCategory = cat }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                color = MaatramBlueSoft,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = meta.first,
                                        contentDescription = cat.label,
                                        tint = MaatramBlue,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = cat.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaatramNavy
                                    )
                                    Surface(
                                        color = MaatramBlueContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "$openCount Open",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaatramBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = subtitleExtra,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open ${cat.label}",
                                tint = MaatramBlue
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                }
            } else {
                // LEVEL 2: Pinned Category Header + Scrollable Dedicated Category Page
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            selectedCategory = null
                            selectedDistrict = "All"
                        },
                        modifier = Modifier.testTag("category_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Volunteer Works",
                            tint = MaatramNavy
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeCat.label,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaatramNavy
                        )
                        Text(
                            text = categoryMeta(activeCat).second,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaatramOutline)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                // Physical Verification Dedicated Controls: Availability Switch + PV Cases Inside Drive
                if (activeCat == OpportunityCategory.PHYSICAL_VERIFICATION) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (availableForPv) MaatramGreenSoft else Color.White
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (availableForPv) MaatramGreen else MaatramOutline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "I am Available for Physical Verification",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaatramNavy
                                )
                                Text(
                                    text = if (availableForPv) {
                                        "Available — Office Team can view your readiness and allocate PV cases."
                                    } else {
                                        "Not Available — Turn this on and send a request on any PV drive below."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = availableForPv,
                                onCheckedChange = onTogglePvAvailability,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaatramBlue
                                ),
                                modifier = Modifier.testTag("pv_availability_switch")
                            )
                        }
                    }
                }

                // District Filter for this Category Page
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    districtOptions.forEach { dist ->
                        FilterChip(
                            selected = selectedDistrict == dist,
                            onClick = { selectedDistrict = dist },
                            label = { Text(if (dist == userDistrict) "$dist (My District)" else dist) }
                        )
                    }
                }

                val categoryOpps = remember(opportunities, activeCat, selectedDistrict, searchQuery) {
                    opportunities.filter { opp ->
                        opp.category == activeCat.name &&
                            (selectedDistrict == "All" || opp.district.equals(selectedDistrict, ignoreCase = true)) &&
                            (searchQuery.isBlank() || opp.title.contains(searchQuery, ignoreCase = true) || opp.district.contains(searchQuery, ignoreCase = true))
                    }.sortedByDescending { it.status == OpportunityStatus.OPEN.name }
                }

                if (categoryOpps.isEmpty()) {
                    EmptyStateCard(
                        title = "No ${activeCat.label} Works Posted",
                        message = "Works posted by Admin or Office Team under ${activeCat.label} will appear here."
                    )
                } else {
                    categoryOpps.forEach { opp ->
                        val existingReq = myRequests.firstOrNull { it.opportunityId == opp.id }
                        val myOppAssignment = myAssignments.firstOrNull { it.opportunityId == opp.id }

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, MaatramOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CategoryBadge(opp.category)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        StatusPill(opp.mode)
                                        StatusPill(
                                            when {
                                                myOppAssignment != null -> "ASSIGNED"
                                                existingReq != null -> existingReq.status
                                                else -> opp.status
                                            }
                                        )
                                    }
                                }

                                Text(
                                    text = opp.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaatramNavy
                                )
                                Text(
                                    text = opp.description.ifBlank { opp.activityType },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = MaatramBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "${opp.district} • ${opp.location.ifBlank { opp.mode }} • ${opp.eventDate} (${opp.startTime}-${opp.endTime})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaatramNavy
                                    )
                                }

                                Surface(
                                    color = MaatramBlueSoft,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Office Availability Poll: ${opp.availableSlots} of ${opp.requiredVolunteers} slots available",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaatramNavy,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }

                                if (existingReq?.status == RequestStatus.REJECTED.name && existingReq.rejectionReason.isNotBlank()) {
                                    Text(
                                        text = "Reason: ${existingReq.rejectionReason}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                val canRequest = opp.status == OpportunityStatus.OPEN.name &&
                                    myOppAssignment == null &&
                                    (existingReq == null || existingReq.status == RequestStatus.CANCELLED.name || existingReq.status == RequestStatus.REJECTED.name)

                                if (myOppAssignment != null) {
                                    Surface(
                                        color = MaatramGreenSoft,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Assigned to You — See your assigned details below.",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaatramGreen,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        )
                                    }
                                } else if (canRequest) {
                                    Button(
                                        onClick = { requestDialogOpportunity = opp },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaatramBlue,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("request_join_${opp.id}")
                                    ) {
                                        Text(
                                            text = "Accept Availability — Send Request",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else if (existingReq?.status == RequestStatus.PENDING.name || existingReq?.status == RequestStatus.UNDER_REVIEW.name) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = MaatramGreenSoft,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Request Sent (Awaiting Assignment)",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaatramGreen,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { onCancelRequest(existingReq) },
                                            enabled = !isBusy
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Assigned Details Section: ONLY shown if Office Team has assigned this student
                val myCategoryAssignments = myAssignments.filter { it.category == activeCat.name }
                if (myCategoryAssignments.isNotEmpty()) {
                    HorizontalDivider(color = MaatramOutline)
                    Text(
                        text = "My Assigned Details (${myCategoryAssignments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaatramNavy
                    )
                    myCategoryAssignments.forEach { assign ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, MaatramGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = assign.opportunityTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaatramNavy,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatusPill(assign.status)
                                }
                                Text(
                                    text = "${assign.district}${if (assign.assignedCasesCount > 0) " • ${assign.assignedCasesCount} case(s) assigned" else ""}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (assign.progressNotes.isNotBlank()) {
                                    Text(
                                        text = "Notes: ${assign.progressNotes}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaatramNavy
                                    )
                                }
                                if (assign.whatsappGroupLink.isNotBlank()) {
                                    Text(
                                        text = "Coordination Group: ${assign.whatsappGroupLink}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaatramBlue
                                    )
                                }
                            }
                        }
                    }
                }

                // My Assigned PV Cases section inside Physical Verification Page
                if (activeCat == OpportunityCategory.PHYSICAL_VERIFICATION) {
                    val myAssignedPv = pvCases.filter { currentUserId.isNotBlank() && it.assignedToUid == currentUserId }
                    if (myAssignedPv.isNotEmpty()) {
                        HorizontalDivider(color = MaatramOutline)
                        Text(
                            text = "My Assigned PV Cases (${myAssignedPv.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaatramNavy
                        )
                        myAssignedPv.forEach { pv ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, MaatramOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${pv.caseRefId} • ${pv.applicantCode}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaatramNavy
                                        )
                                        StatusPill(pv.status)
                                    }
                                    Text("${pv.district} • ${pv.areaLabel}")
                                    if (pv.status != PvCaseStatus.VERIFIED.name) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    onUpdatePvCase(pv.id, PvCaseStatus.VISITED, "Visited applicant home", pv.recommendationSummary)
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Mark Visited", maxLines = 1)
                                            }
                                            Button(
                                                onClick = {
                                                    onUpdatePvCase(pv.id, PvCaseStatus.COMPLETED, "Home PV completed", "Recommended")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Complete PV", maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // My Calling Tasks section inside Calling & BGV Page
                if (activeCat == OpportunityCategory.CALLING_BGV) {
                    val myCalls = callingTasks.filter { currentUserId.isNotBlank() && it.assignedToUid == currentUserId }
                    if (myCalls.isNotEmpty()) {
                        HorizontalDivider(color = MaatramOutline)
                        Text(
                            text = "My Calling Verification Tasks (${myCalls.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaatramNavy
                        )
                        myCalls.forEach { task ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, MaatramOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${task.applicantRefId} • ${task.district}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaatramNavy
                                        )
                                        StatusPill(task.callStatus)
                                    }
                                    if (!task.verifiedByOffice) {
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(CallStatus.CALLED, CallStatus.DETAILS_VERIFIED, CallStatus.NOT_REACHABLE).forEach { st ->
                                                FilterChip(
                                                    selected = task.callStatus == st.name,
                                                    onClick = { onUpdateCallingTask(task.id, st, st == CallStatus.NOT_REACHABLE, task.notes) },
                                                    label = { Text(st.label) }
                                                )
                                            }
                                        }
                                    }
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

    requestDialogOpportunity?.let { opp ->
        AlertDialog(
            onDismissRequest = { requestDialogOpportunity = null },
            title = { Text("Send Availability Request") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(opp.title, fontWeight = FontWeight.Bold, color = MaatramNavy)
                    Text("${opp.district} • ${opp.eventDate} (${opp.startTime} - ${opp.endTime})")
                    if (opp.instructions.isNotBlank()) {
                        Text("Instructions: ${opp.instructions}")
                    }
                    OutlinedTextField(
                        value = availabilityNote,
                        onValueChange = { availabilityNote = it },
                        label = { Text("Availability Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRequestJoin(opp, availabilityNote)
                        requestDialogOpportunity = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaatramBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("Send Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { requestDialogOpportunity = null }) { Text("Cancel") }
            }
        )
    }
}

private fun categoryMeta(cat: OpportunityCategory): Pair<ImageVector, String> = when (cat) {
    OpportunityCategory.PHYSICAL_VERIFICATION ->
        Icons.Filled.HomeWork to "Check PV availability, view PV cases, and request district home visits."
    OpportunityCategory.CALLING_BGV ->
        Icons.Filled.Call to "Call applicants, verify details, and log BGV outcomes."
    OpportunityCategory.INTERVIEWS ->
        Icons.Filled.RecordVoiceOver to "Join mock interviews and student admission panels."
    OpportunityCategory.EVENTS ->
        Icons.Filled.Event to "Support Sangamam, school visits, boot camps, and foundation events."
}
