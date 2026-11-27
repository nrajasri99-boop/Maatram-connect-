package com.example.domain

import com.example.data.model.AppSettings
import com.example.data.model.Assignment
import com.example.data.model.AssignmentStatus
import com.example.data.model.CallingTask
import com.example.data.model.EngagementRecord
import com.example.data.model.EngagementStatus
import com.example.data.model.FairAllocationCandidate
import com.example.data.model.OpportunityCategory
import com.example.data.model.RecognitionTier
import com.example.data.model.RequestStatus
import com.example.data.model.VerificationStatus
import com.example.data.model.VolunteerHours
import com.example.data.model.VolunteerRequest
import com.example.data.model.VolunteerStats
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

object FairAllocationEngine {

    fun rankPvCandidates(
        opportunityId: String,
        district: String,
        allStats: List<VolunteerStats>,
        requestsForOpp: List<VolunteerRequest>,
        settings: AppSettings,
        nowMillis: Long = System.currentTimeMillis()
    ): List<FairAllocationCandidate> {
        val requesterIds = requestsForOpp
            .filter {
                it.opportunityId == opportunityId &&
                    (it.status == RequestStatus.PENDING.name ||
                        it.status == RequestStatus.UNDER_REVIEW.name ||
                        it.status == RequestStatus.WAITLISTED.name)
            }
            .map { it.userId }
            .toSet()

        return allStats
            .filter { it.district.equals(district, ignoreCase = true) || it.userId in requesterIds }
            .map { stats ->
                val hasRequested = stats.userId in requesterIds
                val activeCount = stats.activePvCases.toInt()
                val completedCount = stats.completedPvCases.toInt()
                val atMaxLimit = activeCount >= settings.maxActivePvCases.toInt()
                val blockedByActiveBatch = activeCount > 0

                val daysSinceLast = stats.lastVerifiedAt?.toDate()?.time?.let { lastMs ->
                    TimeUnit.MILLISECONDS.toDays(max(0L, nowMillis - lastMs)).toInt()
                } ?: 60

                var score = 1000
                score -= completedCount * 120
                score -= (stats.verifiedHours.toInt() * 4)
                score -= activeCount * 450
                score += daysSinceLast.coerceAtMost(90) * 3
                if (hasRequested) score += 150
                if (!stats.availableForPv) score -= 500

                val priorityLabel = when {
                    atMaxLimit -> "MAX LIMIT"
                    blockedByActiveBatch -> "HOLD (Active)"
                    !stats.availableForPv -> "UNAVAILABLE"
                    completedCount <= 1 -> "HIGHEST"
                    completedCount <= 4 -> "HIGH"
                    else -> "MEDIUM"
                }

                val recommendedAction = when {
                    atMaxLimit -> "Blocked (${activeCount}/${settings.maxActivePvCases}) - Override Only"
                    blockedByActiveBatch -> "Hold Until Active Batch Verified"
                    !stats.availableForPv -> "Volunteer Unavailable"
                    completedCount == 0 -> "Recommend Assign (Zero Prior Cases)"
                    completedCount <= 3 -> "Recommend Assign (Low Workload)"
                    else -> "Assign if No Lower-Load Volunteer"
                }

                FairAllocationCandidate(
                    stats = stats,
                    hasRequested = hasRequested,
                    priorityScore = score,
                    priorityLabel = priorityLabel,
                    recommendedAction = recommendedAction,
                    blockedByActiveBatch = blockedByActiveBatch,
                    atMaxLimit = atMaxLimit
                )
            }
            .sortedWith(
                compareBy<FairAllocationCandidate> { it.atMaxLimit }
                    .thenBy { it.blockedByActiveBatch }
                    .thenBy { !it.stats.availableForPv }
                    .thenByDescending { it.priorityScore }
                    .thenBy { it.stats.completedPvCases }
                    .thenBy { it.stats.verifiedHours }
            )
    }

    fun calculateDurationHours(startTime: String, endTime: String, breakMinutes: Int): Double {
        val startMins = parseTimeToMinutes(startTime) ?: return 0.0
        val endMins = parseTimeToMinutes(endTime) ?: return 0.0
        val diff = endMins - startMins - breakMinutes.coerceAtLeast(0)
        if (diff <= 0) return 0.0
        return ((diff / 60.0) * 100.0).roundToInt() / 100.0
    }

    private fun parseTimeToMinutes(hhmm: String): Int? {
        val parts = hhmm.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    fun computeGamification(
        verifiedHoursList: List<VolunteerHours>,
        completedPvCases: Long,
        currentStats: VolunteerStats? = null
    ): GamificationResult {
        val approved = verifiedHoursList.filter { it.verificationStatus == VerificationStatus.APPROVED.name }
        val totalHours = approved.sumOf { it.submittedHours }
        val completedActivities = approved.size.toLong().coerceAtLeast(currentStats?.completedActivities ?: 0L)
        val pvHours = approved.filter { it.category == OpportunityCategory.PHYSICAL_VERIFICATION.name }.sumOf { it.submittedHours }
        val callingHours = approved.filter { it.category == OpportunityCategory.CALLING_BGV.name }.sumOf { it.submittedHours }
        val eventHours = approved.filter { it.category == OpportunityCategory.EVENTS.name }.sumOf { it.submittedHours }
        val distinctCategories = approved.map { it.category }.distinct().size
        val distinctMonths = approved.map { it.activityDate.take(7) }.distinct().size

        val consistencyScore = ((distinctMonths * 18) + (completedActivities.toInt() * 7) + (distinctCategories * 12))
            .coerceIn(0, 100)
        val points = (totalHours * 10).roundToInt() + (completedActivities.toInt() * 25) + (consistencyScore * 2)
        val tier = RecognitionTier.forPoints(points)

        val badges = buildList {
            if (completedActivities >= 1) add("First Contribution")
            if (totalHours >= 10.0) add("Active Volunteer")
            if (consistencyScore >= 50 || distinctMonths >= 2) add("Consistent Volunteer")
            if (distinctCategories >= 3) add("Community Champion")
            if (completedPvCases >= 3 || pvHours >= 10.0) add("Physical Verification Expert")
            if (callingHours >= 6.0) add("Calling Support Champion")
            if (eventHours >= 8.0) add("Event Coordinator")
            if (points >= 400) add("Outstanding Contributor")
        }

        return GamificationResult(
            totalVerifiedHours = (totalHours * 100.0).roundToInt() / 100.0,
            completedActivities = completedActivities,
            points = points.toLong(),
            consistencyScore = consistencyScore.toLong(),
            tier = tier,
            badges = badges
        )
    }

    fun classifyEngagement(
        allStats: List<VolunteerStats>,
        requests: List<VolunteerRequest>,
        assignments: List<Assignment>,
        settings: AppSettings,
        nowMillis: Long = System.currentTimeMillis()
    ): List<EngagementRecord> {
        return allStats.map { stats ->
            val userRequests = requests.count {
                it.userId == stats.userId &&
                    (it.status == RequestStatus.PENDING.name || it.status == RequestStatus.UNDER_REVIEW.name)
            }
            val activeAssignments = assignments.count {
                it.userId == stats.userId &&
                    (it.status == AssignmentStatus.ASSIGNED.name || it.status == AssignmentStatus.IN_PROGRESS.name)
            }
            val daysSinceLast = stats.lastVerifiedAt?.toDate()?.time?.let {
                TimeUnit.MILLISECONDS.toDays(max(0L, nowMillis - it)).toInt()
            }

            val status = when {
                !stats.availableForPv && stats.availability.contains("Unavailable", ignoreCase = true) ->
                    EngagementStatus.UNAVAILABLE
                stats.availableForPv && activeAssignments == 0 && stats.completedActivities == 0L && userRequests == 0 ->
                    EngagementStatus.AVAILABLE_UNASSIGNED
                daysSinceLast == null -> EngagementStatus.NO_ACTIVITY
                daysSinceLast <= settings.activeThresholdDays -> EngagementStatus.ACTIVE
                daysSinceLast <= settings.lessActiveThresholdDays -> EngagementStatus.LESS_ACTIVE
                daysSinceLast <= settings.inactiveThresholdDays -> EngagementStatus.INACTIVE
                else -> EngagementStatus.INACTIVE
            }

            EngagementRecord(
                stats = stats,
                status = status,
                daysSinceLastVerified = daysSinceLast,
                pendingRequestCount = userRequests,
                activeAssignmentCount = activeAssignments
            )
        }
    }

    fun buildThankYouEmailBody(
        volunteerName: String,
        activityTitle: String,
        verifiedHours: Double
    ): String = """
        Subject: Thank You for Your Valuable Contribution - Maatram Foundation

        Dear $volunteerName,

        Thank you for dedicating your valuable time (${String.format("%.1f", verifiedHours)} verified hrs) to "$activityTitle" with Maatram Foundation.
        Your contribution towards our community activities makes a meaningful difference.
        We sincerely appreciate your commitment and support.

        Every Volunteer Matters. Every Contribution Counts.

        Regards,
        Maatram Foundation
    """.trimIndent()

    fun buildLinkedInPost(
        volunteerName: String,
        activityTitle: String,
        categoryLabel: String,
        verifiedHours: Double,
        includeHours: Boolean,
        includeName: Boolean
    ): String {
        val hoursLine = if (includeHours) {
            "Contributing ${String.format("%.1f", verifiedHours)} verified volunteer hours toward "
        } else {
            "Supporting "
        }
        val signOffName = if (includeName) "\n\n- $volunteerName" else ""
        return "Grateful to volunteer with Maatram Foundation on \"$activityTitle\" ($categoryLabel). " +
            "${hoursLine}empowering deserving students through fair, transparent community action. " +
            "Every Volunteer Matters. Every Contribution Counts.$signOffName\n\n#MaatramFoundation #MaatramConnect #Volunteering #StudentEmpowerment"
    }

    fun exportHoursCsv(hours: List<VolunteerHours>): String {
        val header = "VolunteerCode,VolunteerName,Category,District,Organization,Opportunity,ActivityDate,Hours,Status,Reviewer"
        val rows = hours.map { h ->
            listOf(
                h.volunteerCode, h.volunteerName, h.category, h.district, h.employerOrgName,
                h.opportunityTitle, h.activityDate, h.submittedHours.toString(), h.verificationStatus, h.reviewerName
            ).joinToString(",") { escapeCsv(it) }
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    fun exportEmployerSummaryCsv(stats: List<VolunteerStats>, hours: List<VolunteerHours>): String {
        val approved = hours.filter { it.verificationStatus == VerificationStatus.APPROVED.name }
        val orgNames = (stats.map { it.employerOrgName } + approved.map { it.employerOrgName })
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val header = "Organization,LinkedVolunteers,CompletedActivities,VerifiedHours,Categories"
        val rows = orgNames.map { org ->
            val orgStats = stats.filter { it.employerOrgName == org }
            val orgHours = approved.filter { it.employerOrgName == org }
            val totalHrs = orgHours.sumOf { it.submittedHours }.takeIf { it > 0 } ?: orgStats.sumOf { it.verifiedHours }
            val totalActs = orgHours.size.takeIf { it > 0 } ?: orgStats.sumOf { it.completedActivities.toInt() }
            val cats = orgHours.map { OpportunityCategory.fromRaw(it.category).shortLabel }.distinct().joinToString(" | ")
            listOf(org, orgStats.size.toString(), totalActs.toString(), String.format("%.1f", totalHrs), cats)
                .joinToString(",") { escapeCsv(it) }
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    fun exportCallingTasksCsv(tasks: List<CallingTask>): String {
        val header = "ApplicantRefId,District,CallingDate,AssignedVolunteer,CallStatus,FollowUpRequired,VerifiedByOffice,Notes"
        val rows = tasks.map { t ->
            listOf(
                t.applicantRefId, t.district, t.callingDate, t.assignedToName,
                t.callStatus, t.followUpRequired.toString(), t.verifiedByOffice.toString(), t.notes
            ).joinToString(",") { escapeCsv(it) }
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    private fun escapeCsv(value: String): String {
        val cleaned = value.replace("\"", "\"\"")
        return "\"$cleaned\""
    }
}

data class GamificationResult(
    val totalVerifiedHours: Double,
    val completedActivities: Long,
    val points: Long,
    val consistencyScore: Long,
    val tier: RecognitionTier,
    val badges: List<String>
)
