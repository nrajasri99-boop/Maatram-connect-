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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNotification
import com.example.data.model.OpportunityCategory
import com.example.data.model.RecognitionTier
import com.example.data.model.UserProfile
import com.example.data.model.VerificationStatus
import com.example.data.model.VolunteerHours
import com.example.data.model.VolunteerStats
import com.example.domain.FairAllocationEngine
import com.example.ui.components.BarChartCard
import com.example.ui.components.CategoryBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ResponsiveGrid
import com.example.ui.components.ResponsiveTabBar
import com.example.ui.components.ShareUtils
import com.example.ui.components.StatusPill
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MaatramBlue
import com.example.ui.theme.MaatramBlueContainer
import com.example.ui.theme.MaatramBlueSoft
import com.example.ui.theme.MaatramNavy

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VolunteerImpactScreen(
    profile: UserProfile?,
    myStats: VolunteerStats?,
    allStats: List<VolunteerStats>,
    myHours: List<VolunteerHours>,
    notifications: List<AppNotification>,
    onMarkNotificationRead: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Hours (${myHours.size})", "Badges", "Leaderboard", "LinkedIn & Alerts")

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
                        val approved = myHours.filter { it.verificationStatus == VerificationStatus.APPROVED.name }
                        val catBreakdown = OpportunityCategory.entries.map { cat ->
                            cat.shortLabel to approved.filter { it.category == cat.name }.sumOf { it.submittedHours }.toFloat()
                        }
                        BarChartCard(
                            title = "Category-Wise Verified Hours",
                            subtitle = "Total Approved: ${String.format("%.1f", approved.sumOf { it.submittedHours })}h",
                            items = catBreakdown
                        )
                        if (myHours.isEmpty()) {
                            EmptyStateCard(
                                title = "No Hours Logged Yet",
                                message = "Log hours from My Work after completing an assignment."
                            )
                        } else {
                            myHours.forEach { hr ->
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
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            CategoryBadge(hr.category)
                                            StatusPill(hr.verificationStatus)
                                        }
                                        Text(hr.opportunityTitle, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "Date: ${hr.activityDate} (Day ${hr.dayNumber}) • ${hr.submittedHours} hrs",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (hr.reviewerName.isNotBlank()) {
                                            Text(
                                                text = "Reviewed by: ${hr.reviewerName}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (hr.correctionNote.isNotBlank()) {
                                            Text(
                                                text = "Reviewer Note: ${hr.correctionNote}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        val points = (myStats?.points ?: 0L).toInt()
                        val tier = RecognitionTier.fromRaw(myStats?.tier)
                        val nextTarget = when (tier) {
                            RecognitionTier.BRONZE -> RecognitionTier.SILVER.minPoints
                            RecognitionTier.SILVER -> RecognitionTier.GOLD.minPoints
                            RecognitionTier.GOLD -> RecognitionTier.PLATINUM.minPoints
                            RecognitionTier.PLATINUM -> RecognitionTier.PLATINUM.minPoints
                        }
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaatramBlueSoft)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("${tier.label} Volunteer • $points Points", style = MaterialTheme.typography.titleLarge)
                                Text("Consistency Score: ${myStats?.consistencyScore ?: 0}/100")
                                LinearProgressIndicator(
                                    progress = { (points.toFloat() / nextTarget.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                    color = MaatramBlue,
                                    modifier = Modifier.fillMaxWidth().height(8.dp)
                                )
                                Text(
                                    text = "Points and badges update strictly after Office Team verification.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        val allBadgeNames = listOf(
                            "First Contribution", "Active Volunteer", "Consistent Volunteer", "Community Champion",
                            "Physical Verification Expert", "Calling Support Champion", "Event Coordinator", "Outstanding Contributor"
                        )
                        val earned = myStats?.badges ?: emptyList()
                        allBadgeNames.forEach { badge ->
                            val unlocked = badge in earned
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (unlocked) MaatramBlueContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.EmojiEvents, contentDescription = null)
                                        Text(badge, style = MaterialTheme.typography.titleMedium)
                                    }
                                    StatusPill(if (unlocked) "VERIFIED" else "LOCKED")
                                }
                            }
                        }
                    }

                    2 -> {
                        var districtFilter by remember { mutableStateOf("All") }
                        val districts = remember(allStats) {
                            listOf("All") + allStats.map { it.district }.distinct().sorted()
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            districts.forEach { d ->
                                FilterChip(
                                    selected = districtFilter == d,
                                    onClick = { districtFilter = d },
                                    label = { Text(d) }
                                )
                            }
                        }
                        val ranked = remember(allStats, districtFilter) {
                            allStats
                                .filter { districtFilter == "All" || it.district == districtFilter }
                                .sortedWith(compareByDescending<VolunteerStats> { it.points }.thenByDescending { it.verifiedHours })
                        }
                        ranked.forEachIndexed { idx, st ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (st.userId == profile?.userId) MaatramBlueSoft else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "#${idx + 1}  ${st.displayName}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "${st.district} • ${st.completedActivities} activities • ${String.format("%.1f", st.verifiedHours)}h",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(color = MaatramBlueContainer, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "${st.points} pts",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaatramNavy,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        val latestApproved = myHours.firstOrNull { it.verificationStatus == VerificationStatus.APPROVED.name }
                        var consentShare by remember(profile) { mutableStateOf(profile?.consentLinkedInPublic ?: false) }
                        var includeHours by remember { mutableStateOf(true) }
                        var postText by remember(latestApproved, consentShare, includeHours) {
                            mutableStateOf(
                                FairAllocationEngine.buildLinkedInPost(
                                    volunteerName = profile?.fullName ?: "Maatram Volunteer",
                                    activityTitle = latestApproved?.opportunityTitle ?: "Maatram Foundation Community Drive",
                                    categoryLabel = OpportunityCategory.fromRaw(latestApproved?.category).label,
                                    verifiedHours = latestApproved?.submittedHours ?: (myStats?.verifiedHours ?: 0.0),
                                    includeHours = includeHours,
                                    includeName = consentShare
                                )
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("LinkedIn Appreciation Post Generator", style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = consentShare, onCheckedChange = { consentShare = it })
                                    Text("I give explicit consent to publish my name in this appreciation post.")
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = includeHours, onCheckedChange = { includeHours = it })
                                    Text("Include verified volunteer hours")
                                }
                                OutlinedTextField(
                                    value = postText,
                                    onValueChange = { postText = it },
                                    label = { Text("Editable Appreciation Post") },
                                    minLines = 4,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            if (!consentShare) {
                                                onShowMessage("Please grant explicit consent above before copying or sharing.")
                                            } else {
                                                ShareUtils.copyToClipboard(context, "LinkedIn Post", postText)
                                                onShowMessage("Appreciation post copied to clipboard!")
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Copy Post")
                                    }
                                    Button(
                                        onClick = {
                                            if (!consentShare) {
                                                onShowMessage("Please grant explicit consent above before sharing.")
                                            } else {
                                                ShareUtils.shareText(context, "Maatram Foundation Appreciation", postText)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaatramBlue, contentColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null)
                                        Text(" Share")
                                    }
                                }
                            }
                        }

                        Text("In-App Notifications & Email Dispatch Log", style = MaterialTheme.typography.titleMedium)
                        if (notifications.isEmpty()) {
                            EmptyStateCard(
                                title = "No Notifications Yet",
                                message = "Approvals, assignments, and automated thank-you emails appear here."
                            )
                        } else {
                            notifications.sortedByDescending { it.createdAt?.seconds ?: 0L }.forEach { notif ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!notif.readFlag) MaatramBlueSoft else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                                                Text(notif.title, style = MaterialTheme.typography.titleMedium)
                                            }
                                            StatusPill(notif.emailMode)
                                        }
                                        Text(notif.message, style = MaterialTheme.typography.bodyMedium)
                                        if (!notif.readFlag) {
                                            TextButton(onClick = { onMarkNotificationRead(notif.id) }) {
                                                Text("Mark as Read")
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
