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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.Assignment
import com.example.data.model.AssignmentStatus
import com.example.data.model.CallingTask
import com.example.data.model.FairAllocationCandidate
import com.example.data.model.Opportunity
import com.example.data.model.OpportunityCategory
import com.example.data.model.PhysicalVerificationCase
import com.example.data.model.PvCaseStatus
import com.example.data.model.VerificationStatus
import com.example.data.model.VolunteerHours
import com.example.data.model.VolunteerRequest
import com.example.data.model.VolunteerStats
import com.example.domain.FairAllocationEngine
import com.example.ui.components.CategoryBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ResponsivePair
import com.example.ui.components.ResponsiveTabBar
import com.example.ui.components.ShareUtils
import com.example.ui.components.StatusPill
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MaatramBlue
import com.example.ui.theme.MaatramBlueSoft
import com.example.ui.theme.MaatramGreen

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OfficeFairAllocationAndVerificationScreen(
    settings: AppSettings,
    allStats: List<VolunteerStats>,
    opportunities: List<Opportunity>,
    requests: List<VolunteerRequest>,
    assignments: List<Assignment>,
    pvCases: List<PhysicalVerificationCase>,
    callingTasks: List<CallingTask>,
    volunteerHours: List<VolunteerHours>,
    isBusy: Boolean,
    onAllocatePvCases: (Opportunity, VolunteerStats, List<PhysicalVerificationCase>, String) -> Unit,
    onVerifyPvCase: (PhysicalVerificationCase) -> Unit,
    onVerifyAssignment: (Assignment) -> Unit,
    onImportCallingSheet: (Opportunity, VolunteerStats) -> Unit,
    onVerifyCallingTask: (String) -> Unit,
    onReviewHours: (VolunteerHours, VerificationStatus, Double, String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Fair PV Alloc",
        "Verify Work",
        "Hours (${volunteerHours.count { it.verificationStatus == VerificationStatus.PENDING.name }})"
    )

    val pvOpps = remember(opportunities) {
        opportunities.filter { it.category == OpportunityCategory.PHYSICAL_VERIFICATION.name }
    }
    var selectedPvOppId by remember(pvOpps) { mutableStateOf(pvOpps.firstOrNull()?.id ?: "") }
    val selectedPvOpp = pvOpps.firstOrNull { it.id == selectedPvOppId } ?: pvOpps.firstOrNull()

    var allocateTarget by remember { mutableStateOf<FairAllocationCandidate?>(null) }
    var reviewHourTarget by remember { mutableStateOf<VolunteerHours?>(null) }

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
                        Surface(color = MaatramBlueSoft, shape = RoundedCornerShape(14.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Fair Volunteer Allocation Engine", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Prioritizes zero/low completed PV cases & lower verified hours. Blocks volunteers with incomplete active batches or >= ${settings.maxActivePvCases} active cases unless overridden with a mandatory reason.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        if (pvOpps.isEmpty()) {
                            EmptyStateCard(
                                title = "No Physical Verification Opportunities",
                                message = "Create a Physical Verification opportunity in the Office tab to start fair district allocation."
                            )
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pvOpps.forEach { opp ->
                                    FilterChip(
                                        selected = selectedPvOpp?.id == opp.id,
                                        onClick = { selectedPvOppId = opp.id },
                                        label = { Text("${opp.district}: ${opp.title.take(22)}") }
                                    )
                                }
                            }

                            if (selectedPvOpp != null) {
                                val unassignedCases = pvCases.filter {
                                    it.opportunityId == selectedPvOpp.id && it.status == PvCaseStatus.UNASSIGNED.name
                                }
                                Text(
                                    text = "Unassigned PV Cases in ${selectedPvOpp.district}: ${unassignedCases.size}",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                val candidates = remember(selectedPvOpp, allStats, requests, settings) {
                                    FairAllocationEngine.rankPvCandidates(
                                        opportunityId = selectedPvOpp.id,
                                        district = selectedPvOpp.district,
                                        allStats = allStats,
                                        requestsForOpp = requests,
                                        settings = settings
                                    )
                                }

                                if (candidates.isEmpty()) {
                                    EmptyStateCard(
                                        title = "No Volunteers Found for ${selectedPvOpp.district}",
                                        message = "Volunteers registered in ${selectedPvOpp.district} or requesting this drive appear here."
                                    )
                                } else {
                                    candidates.forEach { cand ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = cand.stats.displayName,
                                                            style = MaterialTheme.typography.titleMedium
                                                        )
                                                        Text(
                                                            text = "${cand.stats.district} • ${cand.stats.volunteerCode}${if (cand.hasRequested) " • REQUESTED" else ""}",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    StatusPill(cand.priorityLabel)
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Completed PV: ${cand.stats.completedPvCases}", style = MaterialTheme.typography.bodyMedium)
                                                    Text("Active PV: ${cand.stats.activePvCases}", style = MaterialTheme.typography.bodyMedium)
                                                    Text("Verified: ${String.format("%.1f", cand.stats.verifiedHours)}h", style = MaterialTheme.typography.bodyMedium)
                                                }
                                                Text(
                                                    text = "Recommendation: ${cand.recommendedAction} (Score: ${cand.priorityScore})",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Button(
                                                    onClick = { allocateTarget = cand },
                                                    enabled = !isBusy && unassignedCases.isNotEmpty(),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (cand.blockedByActiveBatch || cand.atMaxLimit) MaterialTheme.colorScheme.surfaceVariant else MaatramBlue,
                                                        contentColor = if (cand.blockedByActiveBatch || cand.atMaxLimit) MaterialTheme.colorScheme.onSurface else Color.White
                                                    ),
                                                    modifier = Modifier.fillMaxWidth().testTag("alloc_btn_${cand.stats.userId}")
                                                ) {
                                                    Text(
                                                        text = if (cand.blockedByActiveBatch || cand.atMaxLimit) "Assign With Override Reason" else "Assign PV Cases Fairly",
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Calling Sheet Import + Verification
                        val callingOpps = opportunities.filter { it.category == OpportunityCategory.CALLING_BGV.name }
                        if (callingOpps.isNotEmpty() && allStats.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Calling Sheet Management", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Total Calls: ${callingTasks.size} • Verified: ${callingTasks.count { it.verifiedByOffice }}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    ResponsivePair(
                                        spacing = 8.dp,
                                        first = { mod ->
                                            Button(
                                                onClick = { onImportCallingSheet(callingOpps.first(), allStats.first()) },
                                                enabled = !isBusy,
                                                colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White),
                                                modifier = mod
                                            ) { Text("Upload Dummy CSV (5)", maxLines = 1) }
                                        },
                                        second = { mod ->
                                            OutlinedButton(
                                                onClick = {
                                                    val csv = FairAllocationEngine.exportCallingTasksCsv(callingTasks)
                                                    ShareUtils.shareText(context, "Maatram Calling Sheet CSV", csv)
                                                },
                                                modifier = mod
                                            ) { Text("Export Calling CSV", maxLines = 1) }
                                        }
                                    )
                                }
                            }
                        }

                        Text("PV Cases & Assignments Pending Verification", style = MaterialTheme.typography.titleMedium)
                        val verifyPv = pvCases.filter { it.status == PvCaseStatus.COMPLETED.name || it.status == PvCaseStatus.VISITED.name }
                        verifyPv.forEach { pv ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${pv.caseRefId} • ${pv.assignedToName}", style = MaterialTheme.typography.titleMedium)
                                        StatusPill(pv.status)
                                    }
                                    if (pv.visitNotes.isNotBlank()) Text("Notes: ${pv.visitNotes}")
                                    Button(
                                        onClick = { onVerifyPvCase(pv) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaatramGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Verify PV Case & Release Active Batch") }
                                }
                            }
                        }

                        val verifyCalls = callingTasks.filter { !it.verifiedByOffice && it.callStatus != "NOT_STARTED" }
                        verifyCalls.forEach { task ->
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
                                        Text("${task.applicantRefId} (${task.assignedToName})", style = MaterialTheme.typography.titleMedium)
                                        Text("${task.callStatus} • ${task.notes}")
                                    }
                                    Button(
                                        onClick = { onVerifyCallingTask(task.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaatramGreen)
                                    ) { Text("Verify") }
                                }
                            }
                        }

                        val verifyAssigns = assignments.filter { it.status == AssignmentStatus.COMPLETED.name }
                        verifyAssigns.forEach { assign ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("${assign.opportunityTitle} • ${assign.volunteerName}", style = MaterialTheme.typography.titleMedium)
                                    Button(
                                        onClick = { onVerifyAssignment(assign) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaatramGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Verify Assignment Completion") }
                                }
                            }
                        }
                    }

                    2 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hour Approvals & Reports", style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(onClick = {
                                val csv = FairAllocationEngine.exportHoursCsv(volunteerHours)
                                ShareUtils.shareText(context, "Maatram Verified Hours Report CSV", csv)
                            }) {
                                Text("Export Hours CSV")
                            }
                        }
                        if (volunteerHours.isEmpty()) {
                            EmptyStateCard("No Hour Submissions Yet", "Volunteer hour submissions appear here for verification.")
                        } else {
                            volunteerHours.sortedBy { it.verificationStatus != VerificationStatus.PENDING.name }.forEach { hr ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            CategoryBadge(hr.category)
                                            StatusPill(hr.verificationStatus)
                                        }
                                        Text("${hr.volunteerName} • ${hr.submittedHours} hrs", style = MaterialTheme.typography.titleMedium)
                                        Text("${hr.opportunityTitle} (${hr.activityDate}, Day ${hr.dayNumber})")
                                        if (hr.notes.isNotBlank()) Text("Notes: ${hr.notes}", style = MaterialTheme.typography.bodyMedium)
                                        if (hr.verificationStatus == VerificationStatus.PENDING.name) {
                                            Button(
                                                onClick = { reviewHourTarget = hr },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White),
                                                modifier = Modifier.fillMaxWidth()
                                            ) { Text("Review & Verify Hours", fontWeight = FontWeight.Bold) }
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

    allocateTarget?.let { cand ->
        val opp = selectedPvOpp
        if (opp != null) {
            val unassignedCases = pvCases.filter {
                it.opportunityId == opp.id && it.status == PvCaseStatus.UNASSIGNED.name
            }
            var selectedCaseIds by remember(opp.id) {
                mutableStateOf(unassignedCases.take(2).map { it.id }.toSet())
            }
            var overrideReason by remember { mutableStateOf("") }
            val needsOverride = cand.blockedByActiveBatch ||
                (cand.stats.activePvCases + selectedCaseIds.size > settings.maxActivePvCases)

            AlertDialog(
                onDismissRequest = { allocateTarget = null },
                title = { Text("Allocate PV Cases to ${cand.stats.displayName}") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Completed: ${cand.stats.completedPvCases} • Active: ${cand.stats.activePvCases}/${settings.maxActivePvCases}")
                        Text("Select Unassigned Cases:", style = MaterialTheme.typography.labelLarge)
                        unassignedCases.forEach { pv ->
                            val checked = pv.id in selectedCaseIds
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        selectedCaseIds = if (isChecked) selectedCaseIds + pv.id else selectedCaseIds - pv.id
                                    }
                                )
                                Text("${pv.caseRefId} (${pv.areaLabel})")
                            }
                        }
                        if (needsOverride) {
                            Text(
                                text = "Active-Batch / Limit Protection Triggered: Mandatory override reason required.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = overrideReason,
                                onValueChange = { overrideReason = it },
                                label = { Text("Mandatory Override Reason *") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val chosen = unassignedCases.filter { it.id in selectedCaseIds }
                            onAllocatePvCases(opp, cand.stats, chosen, overrideReason)
                            allocateTarget = null
                        },
                        enabled = selectedCaseIds.isNotEmpty() && (!needsOverride || overrideReason.isNotBlank()),
                        colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White)
                    ) { Text("Confirm Allocation") }
                },
                dismissButton = { TextButton(onClick = { allocateTarget = null }) { Text("Cancel") } }
            )
        }
    }

    reviewHourTarget?.let { hr ->
        var adjustedHours by remember(hr) { mutableStateOf(hr.submittedHours.toString()) }
        var correctionNote by remember(hr) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reviewHourTarget = null },
            title = { Text("Verify Hours: ${hr.volunteerName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${hr.opportunityTitle} (${hr.startTime}-${hr.endTime}, Break ${hr.breakMinutes}m)")
                    OutlinedTextField(
                        value = adjustedHours,
                        onValueChange = { adjustedHours = it },
                        label = { Text("Approved Hours (edit if correcting)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = correctionNote,
                        onValueChange = { correctionNote = it },
                        label = { Text("Correction / Reviewer Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onReviewHours(hr, VerificationStatus.REJECTED, hr.submittedHours, correctionNote.ifBlank { "Please re-check timings" })
                            reviewHourTarget = null
                        }
                    ) { Text("Reject", maxLines = 1) }
                    Button(
                        onClick = {
                            val finalHrs = adjustedHours.toDoubleOrNull() ?: hr.submittedHours
                            onReviewHours(hr, VerificationStatus.APPROVED, finalHrs, correctionNote)
                            reviewHourTarget = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaatramGreen)
                    ) { Text("Approve & Thank", maxLines = 1) }
                }
            },
            dismissButton = { TextButton(onClick = { reviewHourTarget = null }) { Text("Cancel") } }
        )
    }
}
