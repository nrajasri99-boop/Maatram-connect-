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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Assignment
import com.example.data.model.AssignmentStatus
import com.example.data.model.CallStatus
import com.example.data.model.CallingTask
import com.example.data.model.PhysicalVerificationCase
import com.example.data.model.PvCaseStatus
import com.example.domain.FairAllocationEngine
import com.example.ui.components.CategoryBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ResponsivePair
import com.example.ui.components.ResponsiveTabBar
import com.example.ui.components.ShareUtils
import com.example.ui.components.StatusPill
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MaatramBlue

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VolunteerWorkScreen(
    assignments: List<Assignment>,
    pvCases: List<PhysicalVerificationCase>,
    callingTasks: List<CallingTask>,
    isBusy: Boolean,
    onUpdateAssignment: (String, AssignmentStatus, String) -> Unit,
    onUpdatePvCase: (String, PvCaseStatus, String, String) -> Unit,
    onUpdateCallingTask: (String, CallStatus, Boolean, String) -> Unit,
    onSubmitHours: (Assignment, String, String, String, Int, Double?, Int, String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Assignments (${assignments.size})", "PV Cases (${pvCases.size})", "Calling (${callingTasks.size})")

    var hourDialogAssignment by remember { mutableStateOf<Assignment?>(null) }
    var editingPvCase by remember { mutableStateOf<PhysicalVerificationCase?>(null) }
    var editingCallTask by remember { mutableStateOf<CallingTask?>(null) }
    var editingAssignment by remember { mutableStateOf<Assignment?>(null) }

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
                        if (assignments.isEmpty()) {
                            EmptyStateCard(
                                title = "No Assigned Activities",
                                message = "Once the Office Team approves your request or allocates cases, your assignments and WhatsApp group invite links appear here."
                            )
                        } else {
                            assignments.forEach { assign ->
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            CategoryBadge(assign.category)
                                            StatusPill(assign.status)
                                        }
                                        Text(assign.opportunityTitle, style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            text = "${assign.district} • Cases: ${assign.assignedCasesCount}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (assign.progressNotes.isNotBlank()) {
                                            Text("Notes: ${assign.progressNotes}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        if (assign.whatsappGroupLink.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = { ShareUtils.openExternalUrl(context, assign.whatsappGroupLink) },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Join WhatsApp Group (Invite Link)")
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (assign.status != AssignmentStatus.VERIFIED.name) {
                                                OutlinedButton(
                                                    onClick = { editingAssignment = assign },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Update Status")
                                                }
                                            }
                                            Button(
                                                onClick = { hourDialogAssignment = assign },
                                                enabled = !isBusy,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaatramBlue,
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Log Hours", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        Text(
                            text = "Physical Verification Workspace",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Only anonymized Applicant Codes and Zone labels assigned to you are visible here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (pvCases.isEmpty()) {
                            EmptyStateCard(
                                title = "No Physical Verification Cases Assigned",
                                message = "When Office Team allocates district PV cases to you via the Fair Allocation Engine, they appear here."
                            )
                        } else {
                            pvCases.forEach { pv ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(pv.caseRefId, style = MaterialTheme.typography.titleMedium)
                                            StatusPill(pv.status)
                                        }
                                        Text("${pv.district} • ${pv.areaLabel} • Applicant: ${pv.applicantCode}")
                                        if (pv.visitNotes.isNotBlank()) {
                                            Text("Visit Notes: ${pv.visitNotes}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        if (pv.status != PvCaseStatus.VERIFIED.name) {
                                            Button(
                                                onClick = { editingPvCase = pv },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaatramBlue,
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Update Visit Status")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        val completedCalls = callingTasks.count {
                            it.callStatus == CallStatus.DETAILS_VERIFIED.name || it.callStatus == CallStatus.COMPLETED.name
                        }
                        val followUps = callingTasks.count { it.followUpRequired }
                        Text(
                            text = "Calling & BGV Sheet (${completedCalls}/${callingTasks.size} Verified • $followUps Follow-ups)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (callingTasks.isEmpty()) {
                            EmptyStateCard(
                                title = "No Calling Tasks Assigned",
                                message = "Assigned applicant verification calls will appear here with privacy-safe Reference IDs."
                            )
                        } else {
                            callingTasks.forEach { task ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(task.applicantRefId, style = MaterialTheme.typography.titleMedium)
                                            StatusPill(task.callStatus)
                                        }
                                        Text("${task.district} • Call Date: ${task.callingDate}")
                                        if (task.followUpRequired) {
                                            Text("Follow-up required", color = MaterialTheme.colorScheme.error)
                                        }
                                        if (task.notes.isNotBlank()) {
                                            Text("Notes: ${task.notes}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        if (!task.verifiedByOffice) {
                                            Button(
                                                onClick = { editingCallTask = task },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaatramBlue,
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Log Call Result")
                                            }
                                        } else {
                                            StatusPill("VERIFIED BY OFFICE")
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

    editingAssignment?.let { assign ->
        var status by remember(assign) {
            mutableStateOf(
                if (assign.status == AssignmentStatus.COMPLETED.name) AssignmentStatus.COMPLETED
                else AssignmentStatus.IN_PROGRESS
            )
        }
        var notes by remember(assign) { mutableStateOf(assign.progressNotes) }
        AlertDialog(
            onDismissRequest = { editingAssignment = null },
            title = { Text("Update Assignment Progress") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(AssignmentStatus.IN_PROGRESS, AssignmentStatus.COMPLETED).forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Progress / Interview Feedback Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateAssignment(assign.id, status, notes)
                        editingAssignment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingAssignment = null }) { Text("Cancel") } }
        )
    }

    editingPvCase?.let { pv ->
        var status by remember(pv) { mutableStateOf(PvCaseStatus.COMPLETED) }
        var visitNotes by remember(pv) { mutableStateOf(pv.visitNotes) }
        var summary by remember(pv) { mutableStateOf(pv.recommendationSummary) }
        AlertDialog(
            onDismissRequest = { editingPvCase = null },
            title = { Text("Update PV Case: ${pv.caseRefId}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(PvCaseStatus.VISITED, PvCaseStatus.COMPLETED).forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = visitNotes,
                        onValueChange = { visitNotes = it },
                        label = { Text("Visit Observations") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        label = { Text("Verification Recommendation Summary") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePvCase(pv.id, status, visitNotes, summary)
                        editingPvCase = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White)
                ) { Text("Submit Update") }
            },
            dismissButton = { TextButton(onClick = { editingPvCase = null }) { Text("Cancel") } }
        )
    }

    editingCallTask?.let { task ->
        var status by remember(task) { mutableStateOf(CallStatus.fromRaw(task.callStatus)) }
        var followUp by remember(task) { mutableStateOf(task.followUpRequired) }
        var notes by remember(task) { mutableStateOf(task.notes) }
        AlertDialog(
            onDismissRequest = { editingCallTask = null },
            title = { Text("Calling Log: ${task.applicantRefId}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CallStatus.entries.forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.label) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Follow-Up Required")
                        Switch(checked = followUp, onCheckedChange = { followUp = it })
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Verification Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateCallingTask(task.id, status, followUp, notes)
                        editingCallTask = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White)
                ) { Text("Save Call Log") }
            },
            dismissButton = { TextButton(onClick = { editingCallTask = null }) { Text("Cancel") } }
        )
    }

    hourDialogAssignment?.let { assign ->
        var activityDate by remember { mutableStateOf("2026-11-26") }
        var startTime by remember { mutableStateOf("09:30") }
        var endTime by remember { mutableStateOf("13:00") }
        var breakMinutes by remember { mutableStateOf("30") }
        var manualOverride by remember { mutableStateOf("") }
        var dayNumber by remember { mutableStateOf("1") }
        var notes by remember { mutableStateOf("") }
        val calculated = FairAllocationEngine.calculateDurationHours(startTime, endTime, breakMinutes.toIntOrNull() ?: 0)

        AlertDialog(
            onDismissRequest = { hourDialogAssignment = null },
            title = { Text("Submit Volunteer Hours") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${assign.opportunityTitle} (Requires Office verification; login duration is never counted)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    ResponsivePair(
                        breakpoint = 320.dp,
                        spacing = 8.dp,
                        first = { mod ->
                            OutlinedTextField(
                                value = activityDate,
                                onValueChange = { activityDate = it },
                                label = { Text("Date (YYYY-MM-DD)") },
                                singleLine = true,
                                modifier = mod
                            )
                        },
                        second = { mod ->
                            OutlinedTextField(
                                value = dayNumber,
                                onValueChange = { dayNumber = it },
                                label = { Text("Day #") },
                                singleLine = true,
                                modifier = mod
                            )
                        }
                    )
                    ResponsivePair(
                        breakpoint = 320.dp,
                        spacing = 8.dp,
                        first = { mod ->
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                label = { Text("Start (HH:mm)") },
                                singleLine = true,
                                modifier = mod
                            )
                        },
                        second = { mod ->
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { endTime = it },
                                label = { Text("End (HH:mm)") },
                                singleLine = true,
                                modifier = mod
                            )
                        }
                    )
                    ResponsivePair(
                        breakpoint = 320.dp,
                        spacing = 8.dp,
                        first = { mod ->
                            OutlinedTextField(
                                value = breakMinutes,
                                onValueChange = { breakMinutes = it },
                                label = { Text("Break (mins)") },
                                singleLine = true,
                                modifier = mod
                            )
                        },
                        second = { mod ->
                            OutlinedTextField(
                                value = manualOverride,
                                onValueChange = { manualOverride = it },
                                label = { Text("Manual Hrs (opt)") },
                                singleLine = true,
                                modifier = mod
                            )
                        }
                    )
                    Text(
                        text = "Calculated Duration: ${String.format("%.2f", calculated)} hrs",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Contribution Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitHours(
                            assign,
                            activityDate,
                            startTime,
                            endTime,
                            breakMinutes.toIntOrNull() ?: 0,
                            manualOverride.toDoubleOrNull(),
                            (dayNumber.toIntOrNull() ?: 1).coerceAtLeast(1),
                            notes
                        )
                        hourDialogAssignment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White)
                ) { Text("Submit Hours") }
            },
            dismissButton = { TextButton(onClick = { hourDialogAssignment = null }) { Text("Cancel") } }
        )
    }
}
