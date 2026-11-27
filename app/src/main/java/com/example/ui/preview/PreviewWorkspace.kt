package com.example.ui.preview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.model.AppNotification
import com.example.data.model.AppSettings
import com.example.data.model.Assignment
import com.example.data.model.AssignmentStatus
import com.example.data.model.AuditLog
import com.example.data.model.CallStatus
import com.example.data.model.CallingTask
import com.example.data.model.DistrictItem
import com.example.data.model.EmployerOrganization
import com.example.data.model.Opportunity
import com.example.data.model.OpportunityStatus
import com.example.data.model.PhysicalVerificationCase
import com.example.data.model.PvCaseStatus
import com.example.data.model.RecognitionTier
import com.example.data.model.RequestStatus
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.VerificationStatus
import com.example.data.model.VolunteerHours
import com.example.data.model.VolunteerRequest
import com.example.data.model.VolunteerStats
import com.example.domain.FairAllocationEngine
import com.google.firebase.Timestamp

class PreviewWorkspaceState(
    initialName: String,
    initialEmail: String,
    initialDistrict: String,
    initialRole: UserRole = UserRole.VOLUNTEER
) {
    val uid: String = "preview_user"
    val email: String = initialEmail

    var activeRole by mutableStateOf(initialRole)
        private set

    var actionMessage by mutableStateOf<String?>(null)
        private set

    var settings by mutableStateOf(
        AppSettings(
            id = "global",
            maxActivePvCases = 3L,
            activeThresholdDays = 30L,
            lessActiveThresholdDays = 60L,
            inactiveThresholdDays = 90L,
            emailProviderConfigured = false,
            emailSenderName = "Maatram Foundation (Preview)"
        )
    )
        private set

    var districts by mutableStateOf(
        listOf(
            DistrictItem(id = "dist_try", name = "Tiruchirappalli", code = "TRY", active = true),
            DistrictItem(id = "dist_chn", name = "Chennai", code = "CHN", active = true),
            DistrictItem(id = "dist_cbe", name = "Coimbatore", code = "CBE", active = true),
            DistrictItem(id = "dist_mdu", name = "Madurai", code = "MDU", active = true),
            DistrictItem(id = "dist_slm", name = "Salem", code = "SLM", active = true)
        )
    )
        private set

    var organizations by mutableStateOf(
        listOf(
            EmployerOrganization(id = "org_maatram_stu", name = "Maatram Student Chapter", industry = "Higher Education"),
            EmployerOrganization(id = "org_zoho", name = "Zoho Outreach Volunteers", industry = "Software & SaaS"),
            EmployerOrganization(id = "org_tcs", name = "TCS Maitree Alumni Wing", industry = "IT Services")
        )
    )
        private set

    var myProfile by mutableStateOf(
        UserProfile(
            id = uid,
            userId = uid,
            fullName = initialName,
            email = initialEmail,
            phone = "9876543210",
            volunteerCode = "MF-PRV-001",
            volunteerType = "STUDENT",
            college = "Maatram Partner Institute",
            department = "Computer Science",
            yearOfStudy = "3rd Year",
            district = initialDistrict,
            city = initialDistrict,
            skills = "Communication, Mentoring, Tamil",
            interests = "Physical Verification, Calling, Events",
            availability = "Weekends",
            preferredContact = "WhatsApp",
            employerOrgId = "org_maatram_stu",
            employerOrgName = "Maatram Student Chapter",
            consentDataProcessing = true,
            consentLinkedInPublic = true,
            profileCompleted = true,
            role = UserRole.ADMIN.name
        )
    )
        private set

    var allStats by mutableStateOf(
        listOf(
            VolunteerStats(
                id = uid,
                userId = uid,
                displayName = initialName,
                volunteerCode = "MF-PRV-001",
                volunteerType = "STUDENT",
                district = initialDistrict,
                employerOrgName = "Maatram Student Chapter",
                availability = "Weekends",
                availableForPv = true,
                verifiedHours = 12.5,
                completedActivities = 3L,
                completedPvCases = 2L,
                activePvCases = 1L,
                points = 200L,
                consistencyScore = 82L,
                tier = RecognitionTier.SILVER.name,
                badges = listOf("First Contribution", "Active Volunteer"),
                lastVerifiedAt = Timestamp.now()
            ),
            VolunteerStats(
                id = "demo_vol_02",
                userId = "demo_vol_02",
                displayName = "Priyadharshini M",
                volunteerCode = "MF-DEMO-002",
                volunteerType = "STUDENT",
                district = "Tiruchirappalli",
                employerOrgName = "Maatram Student Chapter",
                availableForPv = true,
                verifiedHours = 8.5,
                completedActivities = 2L,
                completedPvCases = 1L,
                activePvCases = 0L,
                points = 135L,
                consistencyScore = 70L,
                tier = RecognitionTier.BRONZE.name,
                badges = listOf("First Contribution")
            ),
            VolunteerStats(
                id = "demo_vol_03",
                userId = "demo_vol_03",
                displayName = "Aravind Krishnan",
                volunteerCode = "MF-DEMO-003",
                volunteerType = "ALUMNI",
                district = "Chennai",
                employerOrgName = "TCS Maitree Alumni Wing",
                availableForPv = true,
                verifiedHours = 36.0,
                completedActivities = 9L,
                completedPvCases = 8L,
                activePvCases = 0L,
                points = 585L,
                consistencyScore = 92L,
                tier = RecognitionTier.GOLD.name,
                badges = listOf("First Contribution", "Active Volunteer", "Physical Verification Expert")
            )
        )
    )
        private set

    var opportunities by mutableStateOf(
        listOf(
            Opportunity(
                id = "opp_pv_trichy",
                title = "Tiruchirappalli Admission Home PV Drive",
                category = "PHYSICAL_VERIFICATION",
                activityType = "Physical Verification",
                description = "Home verification visits for 2026 scholarship applicants in Srirangam & Woraiyur.",
                district = "Tiruchirappalli",
                location = "Srirangam & Woraiyur Zones",
                mode = "OFFLINE",
                eventDate = "2026-12-06",
                startTime = "09:00",
                endTime = "16:30",
                deadline = "2026-12-03",
                requiredVolunteers = 5L,
                availableSlots = 3L,
                totalCases = 6L,
                skillsRequired = "Tamil fluency, local transit",
                eligibility = "Verified volunteers with < 3 active PV cases",
                status = OpportunityStatus.OPEN.name
            ),
            Opportunity(
                id = "opp_call_bgv",
                title = "2026 Batch-A Applicant Calling & BGV",
                category = "CALLING_BGV",
                activityType = "Calling Verification",
                description = "Phone verification of admission forms and guardian contact confirmation.",
                district = "Chennai",
                location = "Remote / Reference Portal",
                mode = "ONLINE",
                eventDate = "2026-12-01",
                startTime = "10:00",
                endTime = "13:00",
                deadline = "2026-11-30",
                requiredVolunteers = 6L,
                availableSlots = 4L,
                totalCases = 10L,
                skillsRequired = "Clear Tamil & English communication",
                eligibility = "All volunteers",
                status = OpportunityStatus.OPEN.name
            ),
            Opportunity(
                id = "opp_mock_int",
                title = "Final-Year Student Mock Interviews",
                category = "INTERVIEWS",
                activityType = "Mock Interviews",
                description = "1-on-1 technical & HR mock interviews for graduating scholars.",
                district = "Coimbatore",
                location = "Google Meet",
                mode = "ONLINE",
                eventDate = "2026-12-07",
                startTime = "10:00",
                endTime = "13:00",
                deadline = "2026-12-04",
                requiredVolunteers = 8L,
                availableSlots = 6L,
                skillsRequired = "Interviewing, mentoring",
                eligibility = "Alumni & Supporters",
                status = OpportunityStatus.OPEN.name
            ),
            Opportunity(
                id = "opp_sangamam",
                title = "Maatram Sangamam Annual Gathering (2-Day)",
                category = "EVENTS",
                activityType = "Sangamam",
                description = "Two-day flagship annual gathering of Maatram students, alumni, and mentors.",
                district = "Chennai",
                location = "Chennai Convention Hall",
                mode = "OFFLINE",
                eventDate = "2026-12-19",
                startTime = "08:30",
                endTime = "17:30",
                deadline = "2026-12-12",
                requiredVolunteers = 15L,
                availableSlots = 12L,
                multiDayCount = 2L,
                skillsRequired = "Stage logistics, hospitality",
                eligibility = "All Maatram Students & Alumni",
                status = OpportunityStatus.OPEN.name
            )
        )
    )
        private set

    var requests by mutableStateOf(
        listOf(
            VolunteerRequest(
                id = "req_preview_1",
                userId = "demo_vol_02",
                volunteerName = "Priyadharshini M",
                volunteerCode = "MF-DEMO-002",
                opportunityId = "opp_pv_trichy",
                opportunityTitle = "Tiruchirappalli Admission Home PV Drive",
                category = "PHYSICAL_VERIFICATION",
                district = "Tiruchirappalli",
                status = RequestStatus.PENDING.name,
                availabilityNote = "Available Sunday morning"
            )
        )
    )
        private set

    var assignments by mutableStateOf(
        listOf(
            Assignment(
                id = "assign_preview_1",
                userId = uid,
                volunteerName = initialName,
                volunteerCode = "MF-PRV-001",
                opportunityId = "opp_pv_trichy",
                opportunityTitle = "Tiruchirappalli Admission Home PV Drive",
                category = "PHYSICAL_VERIFICATION",
                district = initialDistrict,
                assignedCasesCount = 1L,
                caseIds = listOf("pv_prv_1"),
                status = AssignmentStatus.IN_PROGRESS.name,
                progressNotes = "Scheduled home visit for Saturday morning",
                whatsappGroupLink = "https://chat.whatsapp.com/maatram-preview",
                assignedBy = "office_preview"
            )
        )
    )
        private set

    var pvCases by mutableStateOf(
        listOf(
            PhysicalVerificationCase(
                id = "pv_prv_1",
                caseRefId = "PV-2026-01",
                opportunityId = "opp_pv_trichy",
                district = initialDistrict,
                areaLabel = "Srirangam Zone",
                applicantCode = "APP-PRV-101",
                assignedToUid = uid,
                assignedToName = initialName,
                status = PvCaseStatus.ASSIGNED.name
            ),
            PhysicalVerificationCase(
                id = "pv_prv_2",
                caseRefId = "PV-2026-02",
                opportunityId = "opp_pv_trichy",
                district = "Tiruchirappalli",
                areaLabel = "Woraiyur Zone",
                applicantCode = "APP-PRV-102",
                status = PvCaseStatus.UNASSIGNED.name
            ),
            PhysicalVerificationCase(
                id = "pv_prv_3",
                caseRefId = "PV-2026-03",
                opportunityId = "opp_pv_trichy",
                district = "Tiruchirappalli",
                areaLabel = "Thiruverumbur Zone",
                applicantCode = "APP-PRV-103",
                status = PvCaseStatus.UNASSIGNED.name
            )
        )
    )
        private set

    var callingTasks by mutableStateOf(
        listOf(
            CallingTask(
                id = "call_prv_1",
                applicantRefId = "APP-CALL-401",
                opportunityId = "opp_call_bgv",
                district = initialDistrict,
                callingDate = "2026-12-01",
                assignedToUid = uid,
                assignedToName = initialName,
                callStatus = CallStatus.DETAILS_VERIFIED.name,
                verifiedByOffice = true
            ),
            CallingTask(
                id = "call_prv_2",
                applicantRefId = "APP-CALL-402",
                opportunityId = "opp_call_bgv",
                district = initialDistrict,
                callingDate = "2026-12-01",
                assignedToUid = uid,
                assignedToName = initialName,
                callStatus = CallStatus.DETAILS_VERIFIED.name,
                verifiedByOffice = true
            ),
            CallingTask(
                id = "call_prv_3",
                applicantRefId = "APP-CALL-403",
                opportunityId = "opp_call_bgv",
                district = initialDistrict,
                callingDate = "2026-12-02",
                assignedToUid = uid,
                assignedToName = initialName,
                callStatus = CallStatus.NOT_STARTED.name,
                verifiedByOffice = false
            ),
            CallingTask(
                id = "call_demo_2_1",
                applicantRefId = "APP-CALL-501",
                opportunityId = "opp_call_bgv",
                district = "Tiruchirappalli",
                callingDate = "2026-11-29",
                assignedToUid = "demo_vol_02",
                assignedToName = "Priyadharshini M",
                callStatus = CallStatus.DETAILS_VERIFIED.name,
                verifiedByOffice = true
            ),
            CallingTask(
                id = "call_demo_3_1",
                applicantRefId = "APP-CALL-601",
                opportunityId = "opp_call_bgv",
                district = "Chennai",
                callingDate = "2026-11-28",
                assignedToUid = "demo_vol_03",
                assignedToName = "Aravind Krishnan",
                callStatus = CallStatus.DETAILS_VERIFIED.name,
                verifiedByOffice = true
            ),
            CallingTask(
                id = "call_demo_3_2",
                applicantRefId = "APP-CALL-602",
                opportunityId = "opp_call_bgv",
                district = "Chennai",
                callingDate = "2026-11-28",
                assignedToUid = "demo_vol_03",
                assignedToName = "Aravind Krishnan",
                callStatus = CallStatus.DETAILS_VERIFIED.name,
                verifiedByOffice = true
            ),
            CallingTask(
                id = "call_demo_3_3",
                applicantRefId = "APP-CALL-603",
                opportunityId = "opp_call_bgv",
                district = "Chennai",
                callingDate = "2026-11-28",
                assignedToUid = "demo_vol_03",
                assignedToName = "Aravind Krishnan",
                callStatus = CallStatus.DETAILS_VERIFIED.name,
                verifiedByOffice = true
            )
        )
    )
        private set

    var volunteerHours by mutableStateOf(
        listOf(
            VolunteerHours(
                id = "hr_prv_1",
                userId = uid,
                volunteerName = initialName,
                volunteerCode = "MF-PRV-001",
                assignmentId = "assign_preview_1",
                opportunityId = "opp_pv_trichy",
                opportunityTitle = "Tiruchirappalli Admission Home PV Drive",
                category = "PHYSICAL_VERIFICATION",
                district = initialDistrict,
                employerOrgName = "Maatram Student Chapter",
                activityDate = "2026-11-20",
                startTime = "09:30",
                endTime = "13:30",
                breakMinutes = 0L,
                calculatedHours = 4.0,
                submittedHours = 4.0,
                dayNumber = 1L,
                notes = "Initial zone verification",
                verificationStatus = VerificationStatus.APPROVED.name,
                reviewerName = "Office Coordinator"
            ),
            VolunteerHours(
                id = "hr_prv_2",
                userId = "demo_vol_02",
                volunteerName = "Priyadharshini M",
                volunteerCode = "MF-DEMO-002",
                assignmentId = "assign_demo_2",
                opportunityId = "opp_call_bgv",
                opportunityTitle = "2026 Batch-A Applicant Calling & BGV",
                category = "CALLING_BGV",
                district = "Tiruchirappalli",
                activityDate = "2026-11-24",
                startTime = "10:00",
                endTime = "12:30",
                calculatedHours = 2.5,
                submittedHours = 2.5,
                dayNumber = 1L,
                notes = "Completed 10 verification calls",
                verificationStatus = VerificationStatus.PENDING.name
            )
        )
    )
        private set

    var notifications by mutableStateOf(
        listOf(
            AppNotification(
                id = "notif_prv_1",
                userId = uid,
                title = "Welcome to Interactive Preview",
                message = "You can browse all Volunteer, Office Team, and Admin pages and enter forms right here.",
                type = "SYSTEM",
                emailMode = "PREVIEW",
                readFlag = false
            )
        )
    )
        private set

    var auditLogs by mutableStateOf(
        listOf(
            AuditLog(
                id = "audit_prv_1",
                actorUid = uid,
                actorName = initialName,
                actorRole = "ADMIN",
                action = "PREVIEW_STARTED",
                targetType = "workspace",
                targetId = "preview",
                details = "Opened interactive multi-page preview workspace."
            )
        )
    )
        private set

    val allProfiles: List<UserProfile>
        get() = listOf(myProfile)

    fun clearMessage() {
        actionMessage = null
    }

    fun switchRole(role: UserRole) {
        activeRole = role
    }

    fun saveProfile(updated: UserProfile, availableForPv: Boolean) {
        myProfile = updated.copy(profileCompleted = true)
        allStats = allStats.map { st ->
            if (st.userId == uid) {
                st.copy(
                    displayName = updated.fullName,
                    volunteerType = updated.volunteerType,
                    district = updated.district,
                    employerOrgName = updated.employerOrgName,
                    availability = updated.availability,
                    availableForPv = availableForPv
                )
            } else st
        }
        actionMessage = "Profile saved!"
    }

    fun setPvAvailability(available: Boolean) {
        allStats = allStats.map { st ->
            if (st.userId == uid) st.copy(availableForPv = available) else st
        }
        actionMessage = if (available) {
            "Marked Available for Physical Verification! Send a request below."
        } else {
            "Physical Verification availability turned off."
        }
    }

    fun requestToJoin(opp: Opportunity, note: String) {
        val req = VolunteerRequest(
            id = "req_${System.currentTimeMillis()}",
            userId = uid,
            volunteerName = myProfile.fullName,
            volunteerCode = myProfile.volunteerCode,
            opportunityId = opp.id,
            opportunityTitle = opp.title,
            category = opp.category,
            district = opp.district,
            status = RequestStatus.PENDING.name,
            availabilityNote = note
        )
        requests = listOf(req) + requests.filterNot { it.userId == uid && it.opportunityId == opp.id }
        actionMessage = "Request submitted for ${opp.title}!"
    }

    fun cancelRequest(req: VolunteerRequest) {
        requests = requests.map {
            if (it.id == req.id) it.copy(status = RequestStatus.CANCELLED.name) else it
        }
        actionMessage = "Request cancelled."
    }

    fun updateAssignment(assignId: String, status: AssignmentStatus, notes: String) {
        assignments = assignments.map {
            if (it.id == assignId) it.copy(status = status.name, progressNotes = notes) else it
        }
        actionMessage = "Assignment updated to ${status.label}."
    }

    fun updatePvCase(caseId: String, status: PvCaseStatus, notes: String, summary: String) {
        pvCases = pvCases.map {
            if (it.id == caseId) it.copy(status = status.name, visitNotes = notes, recommendationSummary = summary) else it
        }
        actionMessage = "PV case updated to ${status.label}."
    }

    fun updateCallingTask(taskId: String, status: CallStatus, followUp: Boolean, notes: String) {
        callingTasks = callingTasks.map {
            if (it.id == taskId) it.copy(callStatus = status.name, followUpRequired = followUp, notes = notes) else it
        }
        actionMessage = "Call log saved."
    }

    fun submitHours(
        assign: Assignment,
        date: String,
        start: String,
        end: String,
        breakMins: Int,
        manualOverride: Double?,
        dayNumber: Int,
        notes: String
    ) {
        val calc = FairAllocationEngine.calculateDurationHours(start, end, breakMins)
        val finalSubmitted = (manualOverride ?: calc).coerceIn(0.25, 24.0)
        val entry = VolunteerHours(
            id = "hr_${System.currentTimeMillis()}",
            userId = uid,
            volunteerName = myProfile.fullName,
            volunteerCode = myProfile.volunteerCode,
            assignmentId = assign.id,
            opportunityId = assign.opportunityId,
            opportunityTitle = assign.opportunityTitle,
            category = assign.category,
            district = assign.district,
            employerOrgName = myProfile.employerOrgName,
            activityDate = date,
            startTime = start,
            endTime = end,
            breakMinutes = breakMins.toLong(),
            calculatedHours = calc,
            submittedHours = finalSubmitted,
            dayNumber = dayNumber.toLong(),
            notes = notes,
            verificationStatus = VerificationStatus.PENDING.name
        )
        volunteerHours = listOf(entry) + volunteerHours
        actionMessage = "Submitted $finalSubmitted hrs for verification."
    }

    fun markNotificationRead(id: String) {
        notifications = notifications.map { if (it.id == id) it.copy(readFlag = true) else it }
    }

    fun createOpportunity(opp: Opportunity, pvCount: Int) {
        val id = "opp_${System.currentTimeMillis()}"
        val created = opp.copy(id = id, totalCases = pvCount.toLong())
        opportunities = listOf(created) + opportunities
        if (pvCount > 0) {
            val newCases = (1..pvCount).map { idx ->
                PhysicalVerificationCase(
                    id = "${id}_pv_$idx",
                    caseRefId = "PV-${(100..999).random()}-$idx",
                    opportunityId = id,
                    district = opp.district,
                    areaLabel = "${opp.district} Zone $idx",
                    applicantCode = "APP-${(1000..9999).random()}",
                    status = PvCaseStatus.UNASSIGNED.name
                )
            }
            pvCases = newCases + pvCases
        }
        actionMessage = "Published ${opp.title}."
    }

    fun toggleOpportunityStatus(opp: Opportunity, status: OpportunityStatus) {
        opportunities = opportunities.map { if (it.id == opp.id) it.copy(status = status.name) else it }
        actionMessage = "Updated ${opp.title} to ${status.label}."
    }

    fun reviewRequest(req: VolunteerRequest, opp: Opportunity, status: RequestStatus, reason: String) {
        requests = requests.map {
            if (it.id == req.id) it.copy(status = status.name, rejectionReason = reason) else it
        }
        if (status == RequestStatus.APPROVED) {
            val assign = Assignment(
                id = "assign_${System.currentTimeMillis()}",
                userId = req.userId,
                volunteerName = req.volunteerName,
                volunteerCode = req.volunteerCode,
                opportunityId = opp.id,
                opportunityTitle = opp.title,
                category = opp.category,
                district = opp.district,
                status = AssignmentStatus.ASSIGNED.name
            )
            assignments = listOf(assign) + assignments
        }
        actionMessage = "Request ${status.label.lowercase()}."
    }

    fun allocatePvCases(
        opp: Opportunity,
        volStats: VolunteerStats,
        cases: List<PhysicalVerificationCase>,
        overrideReason: String
    ) {
        val ids = cases.map { it.id }.toSet()
        pvCases = pvCases.map {
            if (it.id in ids) it.copy(
                assignedToUid = volStats.userId,
                assignedToName = volStats.displayName,
                status = PvCaseStatus.ASSIGNED.name
            ) else it
        }
        allStats = allStats.map {
            if (it.userId == volStats.userId) it.copy(activePvCases = it.activePvCases + cases.size) else it
        }
        assignments = listOf(
            Assignment(
                id = "assign_${System.currentTimeMillis()}",
                userId = volStats.userId,
                volunteerName = volStats.displayName,
                volunteerCode = volStats.volunteerCode,
                opportunityId = opp.id,
                opportunityTitle = opp.title,
                category = opp.category,
                district = opp.district,
                assignedCasesCount = cases.size.toLong(),
                caseIds = cases.map { it.id },
                overrideReason = overrideReason,
                status = AssignmentStatus.ASSIGNED.name
            )
        ) + assignments
        actionMessage = "Allocated ${cases.size} PV case(s) to ${volStats.displayName}."
    }

    fun verifyPvCase(pv: PhysicalVerificationCase) {
        pvCases = pvCases.map { if (it.id == pv.id) it.copy(status = PvCaseStatus.VERIFIED.name) else it }
        allStats = allStats.map {
            if (it.userId == pv.assignedToUid) {
                it.copy(
                    completedPvCases = it.completedPvCases + 1,
                    activePvCases = (it.activePvCases - 1).coerceAtLeast(0L)
                )
            } else it
        }
        actionMessage = "Verified ${pv.caseRefId}."
    }

    fun verifyAssignment(assign: Assignment) {
        assignments = assignments.map { if (it.id == assign.id) it.copy(status = AssignmentStatus.VERIFIED.name) else it }
        actionMessage = "Verified assignment completion."
    }

    fun importDummyCallingSheet(opp: Opportunity, volStats: VolunteerStats) {
        val added = (1..5).map { idx ->
            CallingTask(
                id = "call_${System.currentTimeMillis()}_$idx",
                applicantRefId = "APP-SHEET-${(200..999).random()}",
                opportunityId = opp.id,
                district = opp.district,
                callingDate = "2026-12-02",
                assignedToUid = volStats.userId,
                assignedToName = volStats.displayName,
                callStatus = CallStatus.NOT_STARTED.name
            )
        }
        callingTasks = added + callingTasks
        actionMessage = "Imported 5 calling tasks for ${volStats.displayName}."
    }

    fun verifyCallingTask(id: String) {
        callingTasks = callingTasks.map { if (it.id == id) it.copy(verifiedByOffice = true) else it }
        actionMessage = "Calling task verified."
    }

    fun reviewHours(hr: VolunteerHours, status: VerificationStatus, finalHours: Double, note: String) {
        volunteerHours = volunteerHours.map {
            if (it.id == hr.id) it.copy(
                verificationStatus = status.name,
                submittedHours = finalHours,
                correctionNote = note,
                reviewerName = myProfile.fullName
            ) else it
        }
        if (status == VerificationStatus.APPROVED) {
            allStats = allStats.map {
                if (it.userId == hr.userId) {
                    val pts = it.points + (finalHours * 10).toLong() + 25L
                    it.copy(
                        verifiedHours = it.verifiedHours + finalHours,
                        completedActivities = it.completedActivities + 1,
                        points = pts,
                        tier = RecognitionTier.forPoints(pts.toInt()).name
                    )
                } else it
            }
        }
        actionMessage = "Hours ${status.label.lowercase()}."
    }

    fun addDistrict(name: String, code: String) {
        districts = districts + DistrictItem(
            id = "dist_${System.currentTimeMillis()}",
            name = name.trim(),
            code = code.trim().uppercase(),
            active = true
        )
        actionMessage = "Added district $name."
    }

    fun addOrganization(name: String, industry: String, contact: String) {
        organizations = organizations + EmployerOrganization(
            id = "org_${System.currentTimeMillis()}",
            name = name.trim(),
            industry = industry.trim(),
            contactPerson = contact.trim()
        )
        actionMessage = "Added organization $name."
    }

    fun updateSettings(newSettings: AppSettings) {
        settings = newSettings
        actionMessage = "Updated settings."
    }

    fun promoteRole(profile: UserProfile, role: UserRole) {
        if (profile.userId == uid) {
            myProfile = myProfile.copy(role = role.name)
        }
        actionMessage = "Updated role to ${role.label}."
    }
}
