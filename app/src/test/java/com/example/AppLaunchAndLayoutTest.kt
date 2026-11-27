package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.model.AppSettings
import com.example.data.model.Opportunity
import com.example.data.model.PhysicalVerificationCase
import com.example.data.model.UserProfile
import com.example.data.model.VolunteerStats
import com.example.ui.MaatramAppContent
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.OfficeDashboardAndRequestsScreen
import com.example.ui.screens.OfficeFairAllocationAndVerificationScreen
import com.example.ui.screens.ProfileOnboardingScreen
import com.example.ui.screens.VolunteerDashboardScreen
import com.example.ui.screens.VolunteerImpactScreen
import com.example.ui.screens.VolunteerOpportunitiesScreen
import com.example.ui.screens.VolunteerWorkScreen
import com.example.ui.theme.MaatramConnectTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppLaunchAndLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleProfile = UserProfile(
        id = "u1",
        userId = "u1",
        fullName = "Karthik Subramanian",
        volunteerCode = "MF-DEMO-001",
        district = "Tiruchirappalli",
        profileCompleted = true
    )

    private val sampleStats = VolunteerStats(
        id = "u1",
        userId = "u1",
        displayName = "Karthik Subramanian",
        volunteerCode = "MF-DEMO-001",
        district = "Tiruchirappalli",
        verifiedHours = 12.5,
        completedPvCases = 2L,
        points = 200L,
        tier = "SILVER"
    )

    private val sampleOpp = Opportunity(
        id = "opp1",
        title = "PV Drive - Tiruchirappalli",
        category = "PHYSICAL_VERIFICATION",
        activityType = "Physical Verification",
        district = "Tiruchirappalli",
        mode = "OFFLINE",
        eventDate = "2026-12-10",
        status = "OPEN",
        requiredVolunteers = 5,
        availableSlots = 3
    )

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun maatramAppContent_studentLoginNavigatesThroughWorksAndDedicatedPvPage() {
        composeTestRule.setContent {
            MaatramConnectTheme {
                MaatramAppContent()
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MAATRAM CONNECT").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_email_input").performTextInput("raja.student@maatram.org")
        composeTestRule.onNodeWithTag("login_password_input").performTextInput("pass1234")
        composeTestRule.onNodeWithTag("login_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Student Portal Home: Top Volunteer Works button, PV & Call counts, Leaderboard
        composeTestRule.onNodeWithTag("volunteer_works_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Physical Verifications").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Call Verifications").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_leaderboard_card").performScrollTo().assertIsDisplayed()

        // Click Volunteer Works -> 4 category cards -> click Physical Verification -> PV page
        composeTestRule.onNodeWithTag("volunteer_works_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("category_card_PHYSICAL_VERIFICATION").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_card_CALLING_BGV").assertIsDisplayed()

        composeTestRule.onNodeWithTag("category_card_PHYSICAL_VERIFICATION").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pv_availability_switch").assertIsDisplayed()
        composeTestRule.onNodeWithText("PV-2026-01 • Srirangam Zone").performScrollTo().assertIsDisplayed()

        // Return back to Category Hub
        composeTestRule.onNodeWithTag("category_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("category_card_EVENTS").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun maatramAppContent_officeTeamSharedLoginOpensOfficePortal() {
        composeTestRule.setContent {
            MaatramConnectTheme {
                MaatramAppContent()
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("login_role_office_team").performClick()
        composeTestRule.onNodeWithTag("login_email_input").performTextInput("office@maatram.org")
        composeTestRule.onNodeWithTag("login_password_input").performTextInput("office123")
        composeTestRule.onNodeWithTag("login_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Office Team Portal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Works by Category").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("office_create_category_work_btn").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun maatramAppContent_adminLoginOpensAdminPortal() {
        composeTestRule.setContent {
            MaatramConnectTheme {
                MaatramAppContent()
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("login_role_admin").performClick()
        composeTestRule.onNodeWithTag("login_email_input").performTextInput("nrajasri99@gmail.com")
        composeTestRule.onNodeWithTag("login_password_input").performTextInput("admin123")
        composeTestRule.onNodeWithTag("login_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Admin Portal").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun authScreen_rendersCleanlyOnCompactPhone() {
        composeTestRule.setContent {
            MaatramConnectTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("login_email_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_password_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w900dp-h720dp")
    fun authScreen_rendersCleanlyOnTablet() {
        composeTestRule.setContent {
            MaatramConnectTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("login_email_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun profileOnboarding_rendersOnCompactPhone() {
        composeTestRule.setContent {
            MaatramConnectTheme {
                ProfileOnboardingScreen(
                    uid = "u1",
                    email = "karthik@example.org",
                    defaultName = "Karthik Subramanian",
                    existingProfile = null,
                    existingStats = null,
                    districts = emptyList(),
                    organizations = emptyList(),
                    isBusy = false,
                    onSaveProfile = { _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("save_profile_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun volunteerDashboard_rendersOnCompactPhone() {
        composeTestRule.setContent {
            MaatramConnectTheme {
                VolunteerDashboardScreen(
                    profile = sampleProfile,
                    myStats = sampleStats,
                    allStats = listOf(sampleStats),
                    opportunities = listOf(sampleOpp),
                    myRequests = emptyList(),
                    myAssignments = emptyList(),
                    myHours = emptyList(),
                    notifications = emptyList(),
                    onExploreOpportunities = {},
                    onOpenWorkHub = {},
                    onOpenImpactHub = {},
                    onOpenProfile = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("volunteer_works_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_leaderboard_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun volunteerOpportunities_rendersCategoriesAndPvPageOnCompactPhone() {
        val samplePvCase = PhysicalVerificationCase(
            id = "pv1",
            caseRefId = "PV-2026-99",
            opportunityId = "opp1",
            district = "Tiruchirappalli",
            areaLabel = "Srirangam",
            applicantCode = "APP-99"
        )
        composeTestRule.setContent {
            MaatramConnectTheme {
                VolunteerOpportunitiesScreen(
                    opportunities = listOf(sampleOpp),
                    myRequests = emptyList(),
                    districts = emptyList(),
                    userDistrict = "Tiruchirappalli",
                    pvCases = listOf(samplePvCase),
                    isBusy = false,
                    onRequestJoin = { _, _ -> },
                    onCancelRequest = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("category_card_PHYSICAL_VERIFICATION").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pv_availability_switch").assertIsDisplayed()
        composeTestRule.onNodeWithText("PV Drive - Tiruchirappalli").assertIsDisplayed()
        composeTestRule.onNodeWithText("PV-2026-99 • Srirangam").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun volunteerWork_rendersOnCompactPhone() {
        composeTestRule.setContent {
            MaatramConnectTheme {
                VolunteerWorkScreen(
                    assignments = emptyList(),
                    pvCases = emptyList(),
                    callingTasks = emptyList(),
                    isBusy = false,
                    onUpdateAssignment = { _, _, _ -> },
                    onUpdatePvCase = { _, _, _, _ -> },
                    onUpdateCallingTask = { _, _, _, _ -> },
                    onSubmitHours = { _, _, _, _, _, _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Assignments (0)").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w900dp-h720dp")
    fun volunteerImpact_rendersOnTablet() {
        composeTestRule.setContent {
            MaatramConnectTheme {
                VolunteerImpactScreen(
                    profile = sampleProfile,
                    myStats = sampleStats,
                    allStats = listOf(sampleStats),
                    myHours = emptyList(),
                    notifications = emptyList(),
                    onMarkNotificationRead = {},
                    onShowMessage = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hours (0)").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun officeHub_rendersOnCompactPhone() {
        composeTestRule.setContent {
            MaatramConnectTheme {
                OfficeDashboardAndRequestsScreen(
                    allStats = listOf(sampleStats),
                    opportunities = listOf(sampleOpp),
                    requests = emptyList(),
                    pvCases = emptyList(),
                    volunteerHours = emptyList(),
                    districts = emptyList(),
                    isBusy = false,
                    onCreateOpportunity = { _, _ -> },
                    onToggleOppStatus = { _, _ -> },
                    onReviewRequest = { _, _, _, _ -> },
                    onOpenFairAllocation = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Overview").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w900dp-h720dp")
    fun officeFairAllocation_rendersOnTablet() {
        composeTestRule.setContent {
            MaatramConnectTheme {
                OfficeFairAllocationAndVerificationScreen(
                    settings = AppSettings(),
                    allStats = listOf(sampleStats),
                    opportunities = listOf(sampleOpp),
                    requests = emptyList(),
                    assignments = emptyList(),
                    pvCases = emptyList(),
                    callingTasks = emptyList(),
                    volunteerHours = emptyList(),
                    isBusy = false,
                    onAllocatePvCases = { _, _, _, _ -> },
                    onVerifyPvCase = {},
                    onVerifyAssignment = {},
                    onImportCallingSheet = { _, _ -> },
                    onVerifyCallingTask = {},
                    onReviewHours = { _, _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Fair PV Alloc").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w900dp-h720dp")
    fun adminDashboard_rendersOnTablet() {
        composeTestRule.setContent {
            MaatramConnectTheme {
                AdminDashboardScreen(
                    settings = AppSettings(),
                    allStats = listOf(sampleStats),
                    allProfiles = listOf(sampleProfile),
                    opportunities = listOf(sampleOpp),
                    requests = emptyList(),
                    assignments = emptyList(),
                    volunteerHours = emptyList(),
                    districts = emptyList(),
                    organizations = emptyList(),
                    auditLogs = emptyList(),
                    isBusy = false,
                    onAddDistrict = { _, _ -> },
                    onAddOrganization = { _, _, _ -> },
                    onUpdateSettings = {},
                    onPromoteRole = { _, _ -> },
                    onSeedDemoData = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Analytics").assertIsDisplayed()
    }
}
