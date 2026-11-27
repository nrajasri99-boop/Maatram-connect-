package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNotification
import com.example.data.model.Assignment
import com.example.data.model.CallStatus
import com.example.data.model.CallingTask
import com.example.data.model.Opportunity
import com.example.data.model.OpportunityStatus
import com.example.data.model.PhysicalVerificationCase
import com.example.data.model.PvCaseStatus
import com.example.data.model.RecognitionTier
import com.example.data.model.UserProfile
import com.example.data.model.VerificationStatus
import com.example.data.model.VolunteerHours
import com.example.data.model.VolunteerRequest
import com.example.data.model.VolunteerStats
import com.example.ui.components.ResponsiveGrid
import com.example.ui.components.StatSummaryCard
import com.example.ui.theme.MaatramBlue
import com.example.ui.theme.MaatramBlueContainer
import com.example.ui.theme.MaatramBlueSoft
import com.example.ui.theme.MaatramGreen
import com.example.ui.theme.MaatramNavy
import com.example.ui.theme.MaatramOutline

@Composable
fun VolunteerDashboardScreen(
    profile: UserProfile?,
    myStats: VolunteerStats?,
    allStats: List<VolunteerStats>,
    opportunities: List<Opportunity>,
    myRequests: List<VolunteerRequest>,
    myAssignments: List<Assignment>,
    myPvCases: List<PhysicalVerificationCase> = emptyList(),
    callingTasks: List<CallingTask> = emptyList(),
    myHours: List<VolunteerHours>,
    notifications: List<AppNotification>,
    onExploreOpportunities: () -> Unit,
    onOpenWorkHub: () -> Unit,
    onOpenImpactHub: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val openWorksCount = opportunities.count { it.status == OpportunityStatus.OPEN.name }
    val myUid = profile?.userId ?: myStats?.userId ?: ""

    val callDoneByUser = remember(callingTasks) {
        callingTasks
            .filter {
                it.verifiedByOffice ||
                    it.callStatus == CallStatus.DETAILS_VERIFIED.name ||
                    it.callStatus == CallStatus.COMPLETED.name ||
                    it.callStatus == CallStatus.CALLED.name
            }
            .groupingBy { it.assignedToUid }
            .eachCount()
    }

    val myPvDone = myStats?.completedPvCases?.toInt()
        ?: myPvCases.count { it.status == PvCaseStatus.COMPLETED.name || it.status == PvCaseStatus.VERIFIED.name }
    val myPvActive = myStats?.activePvCases?.toInt()
        ?: myPvCases.count { it.status == PvCaseStatus.ASSIGNED.name || it.status == PvCaseStatus.VISITED.name }
    val myCallDone = callDoneByUser[myUid] ?: 0
    val myCallPending = callingTasks.count {
        it.assignedToUid == myUid &&
            !it.verifiedByOffice &&
            it.callStatus != CallStatus.DETAILS_VERIFIED.name &&
            it.callStatus != CallStatus.COMPLETED.name &&
            it.callStatus != CallStatus.CALLED.name
    }
    val verifiedHours = myStats?.verifiedHours ?: myHours
        .filter { it.verificationStatus == VerificationStatus.APPROVED.name }
        .sumOf { it.submittedHours }

    val sortedLeaderboard = remember(allStats) {
        allStats.sortedWith(
            compareByDescending<VolunteerStats> { it.points }
                .thenByDescending { it.completedPvCases }
                .thenByDescending { it.verifiedHours }
        )
    }
    val rankIndex = sortedLeaderboard.indexOfFirst { it.userId == myUid }
    val rankLabel = if (rankIndex >= 0) "#${rankIndex + 1} / ${sortedLeaderboard.size}" else "-"
    val tier = RecognitionTier.fromRaw(myStats?.tier)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 1040.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Single Primary Top Button: Volunteer Works
            Button(
                onClick = onExploreOpportunities,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaatramBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("volunteer_works_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.WorkOutline,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Volunteer Works",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "$openWorksCount open opportunities • PV, Calling, Interviews & Events",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaatramBlueSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Volunteer Works",
                        tint = Color.White
                    )
                }
            }

            // 2. Clean Student Identity Strip
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaatramOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Vanakkam, ${profile?.fullName ?: "Student"}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaatramNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${profile?.district ?: "Tamil Nadu"} • ${tier.label} Tier • ${myStats?.points ?: 0} pts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onOpenProfile) {
                        Text("Profile", color = MaatramBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 3. Work Counts Done by Student (PV Done, Call Verifications Done, Hours, Rank)
            Text(
                text = "My Work Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaatramNavy
            )
            val progressCards = listOf(
                Triple(
                    "Physical Verifications",
                    "$myPvDone Done",
                    "$myPvActive active PV"
                ) to (Icons.Filled.HomeWork to MaatramBlue),
                Triple(
                    "Call Verifications",
                    "$myCallDone Done",
                    "$myCallPending pending calls"
                ) to (Icons.Filled.Call to MaatramGreen),
                Triple(
                    "Verified Hours",
                    String.format("%.1fh", verifiedHours),
                    "${myRequests.size} requests sent"
                ) to (Icons.Filled.AccessTimeFilled to MaatramBlue),
                Triple(
                    "Leaderboard Rank",
                    rankLabel,
                    "${myStats?.badges?.size ?: 0} badges earned"
                ) to (Icons.Filled.Leaderboard to MaatramBlue)
            )
            ResponsiveGrid(
                items = progressCards,
                compactColumns = 2,
                mediumColumns = 2,
                expandedColumns = 4
            ) { (info, visual), mod ->
                StatSummaryCard(
                    title = info.first,
                    value = info.second,
                    subtitle = info.third,
                    icon = visual.first,
                    accentColor = visual.second,
                    modifier = mod
                )
            }

            // 4. Student Leaderboard Comparison Card (comparing PV, Calls, Hours & Points)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaatramOutline),
                modifier = Modifier.fillMaxWidth().testTag("student_leaderboard_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = MaatramBlue
                            )
                            Text(
                                text = "Student Leaderboard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaatramNavy
                            )
                        }
                        Surface(color = MaatramBlueSoft, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "PV & Call Comparison",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaatramBlue,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (sortedLeaderboard.isEmpty()) {
                        Text(
                            text = "Leaderboard scores will appear as students complete PV and Calling tasks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        sortedLeaderboard.forEachIndexed { idx, st ->
                            val isMe = st.userId == myUid
                            val callsForVol = callDoneByUser[st.userId] ?: 0
                            Surface(
                                color = if (isMe) MaatramBlueSoft else Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                                border = if (isMe) BorderStroke(1.dp, MaatramBlueContainer) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            color = if (idx == 0 || isMe) MaatramBlue else MaatramBlueContainer,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#${idx + 1}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (idx == 0 || isMe) Color.White else MaatramNavy
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = st.displayName.ifBlank { st.volunteerCode },
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = if (isMe) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = MaatramNavy,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isMe) {
                                                    Surface(color = MaatramBlue, shape = RoundedCornerShape(6.dp)) {
                                                        Text(
                                                            text = "You",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "PV: ${st.completedPvCases}  •  Calls: $callsForVol  •  ${String.format("%.1fh", st.verifiedHours)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${st.points} pts",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaatramBlue
                                    )
                                }
                            }
                            if (idx < sortedLeaderboard.lastIndex) {
                                HorizontalDivider(color = MaatramOutline.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            // 5. Light Secondary Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenWorkHub,
                    border = BorderStroke(1.dp, MaatramBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("My Active Work (${myAssignments.size})", color = MaatramBlue, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onOpenImpactHub,
                    border = BorderStroke(1.dp, MaatramBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    val unread = notifications.count { !it.readFlag }
                    Text(
                        text = if (unread > 0) "Hours & Alerts ($unread)" else "Hours & Badges",
                        color = MaatramBlue,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
