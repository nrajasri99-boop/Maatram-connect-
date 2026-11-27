package com.example

import com.example.data.model.AppSettings
import com.example.data.model.OpportunityCategory
import com.example.data.model.RecognitionTier
import com.example.data.model.VerificationStatus
import com.example.data.model.VolunteerHours
import com.example.data.model.VolunteerStats
import com.example.domain.FairAllocationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FairAllocationEngineTest {

    @Test
    fun fairAllocation_prioritizesZeroAndLowWorkloadVolunteersOverHighWorkload() {
        val settings = AppSettings(maxActivePvCases = 3L)
        val volA = VolunteerStats(userId = "a", displayName = "Vol A", district = "Tiruchirappalli", completedPvCases = 12, activePvCases = 0, verifiedHours = 36.0)
        val volB = VolunteerStats(userId = "b", displayName = "Vol B", district = "Tiruchirappalli", completedPvCases = 2, activePvCases = 0, verifiedHours = 8.0)
        val volC = VolunteerStats(userId = "c", displayName = "Vol C", district = "Tiruchirappalli", completedPvCases = 5, activePvCases = 0, verifiedHours = 18.0)
        val volD = VolunteerStats(userId = "d", displayName = "Vol D", district = "Tiruchirappalli", completedPvCases = 0, activePvCases = 0, verifiedHours = 0.0)
        val volBlocked = VolunteerStats(userId = "e", displayName = "Vol E", district = "Tiruchirappalli", completedPvCases = 1, activePvCases = 2, verifiedHours = 4.0)

        val ranked = FairAllocationEngine.rankPvCandidates(
            opportunityId = "opp_try",
            district = "Tiruchirappalli",
            allStats = listOf(volA, volB, volC, volD, volBlocked),
            requestsForOpp = emptyList(),
            settings = settings
        )

        assertEquals("d", ranked[0].stats.userId)
        assertEquals("b", ranked[1].stats.userId)
        assertEquals("c", ranked[2].stats.userId)
        assertEquals("a", ranked[3].stats.userId)
        assertEquals("e", ranked[4].stats.userId)
        assertFalse(ranked[0].blockedByActiveBatch)
        assertTrue(ranked[4].blockedByActiveBatch)
    }

    @Test
    fun calculateDurationHours_subtractsBreakProperly() {
        val hrs = FairAllocationEngine.calculateDurationHours("09:30", "13:30", 30)
        assertEquals(3.5, hrs, 0.001)
    }

    @Test
    fun gamification_countsOnlyApprovedHours() {
        val hours = listOf(
            VolunteerHours(
                userId = "u1",
                category = OpportunityCategory.PHYSICAL_VERIFICATION.name,
                activityDate = "2026-10-10",
                submittedHours = 5.0,
                verificationStatus = VerificationStatus.APPROVED.name
            ),
            VolunteerHours(
                userId = "u1",
                category = OpportunityCategory.CALLING_BGV.name,
                activityDate = "2026-11-12",
                submittedHours = 10.0,
                verificationStatus = VerificationStatus.PENDING.name
            )
        )

        val res = FairAllocationEngine.computeGamification(hours, completedPvCases = 1L)
        assertEquals(5.0, res.totalVerifiedHours, 0.001)
        assertEquals(1L, res.completedActivities)
        assertTrue("First Contribution" in res.badges)
        assertEquals(RecognitionTier.BRONZE, res.tier)
    }
}
