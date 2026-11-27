package com.example.data.seed

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit

object MaatramSeedData {

    suspend fun seedDemoWorkspace(
        db: FirebaseFirestore,
        actorUid: String,
        actorName: String
    ) {
        val now = System.currentTimeMillis()
        fun daysAgoTs(days: Long): Timestamp = Timestamp(Date(now - TimeUnit.DAYS.toMillis(days)))

        // 1. Global Settings
        db.collection("app_settings").document("global").set(
            mapOf(
                "maxActivePvCases" to 3L,
                "activeThresholdDays" to 30L,
                "lessActiveThresholdDays" to 60L,
                "inactiveThresholdDays" to 90L,
                "emailProviderConfigured" to false,
                "emailSenderName" to "Maatram Foundation (Demo Preview)",
                "updatedBy" to actorUid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // 2. Districts (8 Tamil Nadu districts)
        val districts = listOf(
            "try" to ("Tiruchirappalli" to "TRY"),
            "chn" to ("Chennai" to "CHN"),
            "cbe" to ("Coimbatore" to "CBE"),
            "mdu" to ("Madurai" to "MDU"),
            "slm" to ("Salem" to "SLM"),
            "tnj" to ("Thanjavur" to "TNJ"),
            "erd" to ("Erode" to "ERD"),
            "vlr" to ("Vellore" to "VLR")
        )
        val batch1 = db.batch()
        districts.forEach { (id, pair) ->
            batch1.set(
                db.collection("districts").document("dist_$id"),
                mapOf(
                    "name" to pair.first,
                    "code" to pair.second,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // 3. Employer Organizations (5 orgs)
        val orgs = listOf(
            Triple("org_maatram_stu", "Maatram Student Chapter", "Higher Education"),
            Triple("org_zoho", "Zoho Outreach Volunteers", "Software & SaaS"),
            Triple("org_tcs", "TCS Maitree Alumni Wing", "IT Services"),
            Triple("org_tvs", "TVS Motor CSR Network", "Automotive & Manufacturing"),
            Triple("org_cognizant", "Cognizant Outreach Chapter", "Technology Consulting")
        )
        orgs.forEach { (id, name, industry) ->
            batch1.set(
                db.collection("organizations").document(id),
                mapOf(
                    "name" to name,
                    "industry" to industry,
                    "contactPerson" to "Demo CSR Coordinator",
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // 4. 5 Sample Office Team Coordinators (Fictional Demo)
        val officeCoords = listOf(
            Triple("demo_off_1", "Karthik Subramanian", "Admissions & PV Lead"),
            Triple("demo_off_2", "Meenakshi Sundaram", "Calling & BGV Coordinator"),
            Triple("demo_off_3", "Praveen Raj", "Interview Panel Lead"),
            Triple("demo_off_4", "Divya Bharathi", "Events & Sangamam Lead"),
            Triple("demo_off_5", "Senthil Kumar", "District Operations Coordinator")
        )
        officeCoords.forEach { (uid, name, dept) ->
            batch1.set(
                db.collection("office_team").document(uid),
                mapOf(
                    "userId" to uid,
                    "displayName" to "$name (Demo)",
                    "email" to "$uid@demo.maatram.org",
                    "department" to dept,
                    "canApproveHours" to true,
                    "canAllocatePv" to true,
                    "canManageEvents" to true,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch1.commit().await()

        // 5. 20 Sample Fictional Volunteers (mirroring prompt's 12 vs 2 vs 5 vs 0 PV cases!)
        data class SeedVol(
            val uid: String, val name: String, val code: String, val type: String,
            val district: String, val org: String, val hours: Double, val acts: Long,
            val completedPv: Long, val activePv: Long, val lastDaysAgo: Long?, val avail: Boolean
        )
        val sampleVols = listOf(
            SeedVol("demo_vol_01", "Aravind Krishnan (Demo)", "MF-DEMO-001", "ALUMNI", "Tiruchirappalli", "TCS Maitree Alumni Wing", 36.0, 9, 12, 0, 5, true),
            SeedVol("demo_vol_02", "Priyadharshini M (Demo)", "MF-DEMO-002", "STUDENT", "Tiruchirappalli", "Maatram Student Chapter", 8.5, 2, 2, 0, 18, true),
            SeedVol("demo_vol_03", "Vigneshwaran S (Demo)", "MF-DEMO-003", "ALUMNI", "Tiruchirappalli", "Zoho Outreach Volunteers", 18.0, 5, 5, 0, 24, true),
            SeedVol("demo_vol_04", "Harini Natarajan (Demo)", "MF-DEMO-004", "STUDENT", "Tiruchirappalli", "Maatram Student Chapter", 0.0, 0, 0, 0, null, true),
            SeedVol("demo_vol_05", "Santhosh Kumar (Demo)", "MF-DEMO-005", "STUDENT", "Tiruchirappalli", "Maatram Student Chapter", 12.0, 3, 1, 2, 10, true),
            SeedVol("demo_vol_06", "Nandhini Devi (Demo)", "MF-DEMO-006", "ALUMNI", "Chennai", "Zoho Outreach Volunteers", 44.0, 11, 8, 0, 7, true),
            SeedVol("demo_vol_07", "Deepak Rajendran (Demo)", "MF-DEMO-007", "STUDENT", "Chennai", "Maatram Student Chapter", 4.0, 1, 0, 0, 42, true),
            SeedVol("demo_vol_08", "Keerthana V (Demo)", "MF-DEMO-008", "SUPPORTER", "Chennai", "Cognizant Outreach Chapter", 22.5, 6, 3, 1, 14, true),
            SeedVol("demo_vol_09", "Surya Prakash (Demo)", "MF-DEMO-009", "STUDENT", "Coimbatore", "Maatram Student Chapter", 0.0, 0, 0, 0, null, true),
            SeedVol("demo_vol_10", "Ananya Balaji (Demo)", "MF-DEMO-010", "ALUMNI", "Coimbatore", "TVS Motor CSR Network", 29.0, 7, 6, 0, 19, true),
            SeedVol("demo_vol_11", "Gowtham R (Demo)", "MF-DEMO-011", "STUDENT", "Coimbatore", "Maatram Student Chapter", 10.0, 3, 2, 0, 52, true),
            SeedVol("demo_vol_12", "Lavanya Selvam (Demo)", "MF-DEMO-012", "ALUMNI", "Madurai", "TCS Maitree Alumni Wing", 31.5, 8, 7, 0, 12, true),
            SeedVol("demo_vol_13", "Muthuvel P (Demo)", "MF-DEMO-013", "STUDENT", "Madurai", "Maatram Student Chapter", 3.5, 1, 1, 0, 74, true),
            SeedVol("demo_vol_14", "Swathi Ganesan (Demo)", "MF-DEMO-014", "STUDENT", "Madurai", "Maatram Student Chapter", 0.0, 0, 0, 0, null, false),
            SeedVol("demo_vol_15", "Raghul Saravanan (Demo)", "MF-DEMO-015", "ALUMNI", "Salem", "Zoho Outreach Volunteers", 16.0, 4, 3, 0, 29, true),
            SeedVol("demo_vol_16", "Janani Ramesh (Demo)", "MF-DEMO-016", "STUDENT", "Salem", "Maatram Student Chapter", 6.0, 2, 1, 0, 38, true),
            SeedVol("demo_vol_17", "Bala Murugan (Demo)", "MF-DEMO-017", "SUPPORTER", "Thanjavur", "TVS Motor CSR Network", 14.0, 4, 2, 0, 65, true),
            SeedVol("demo_vol_18", "Pavithra K (Demo)", "MF-DEMO-018", "STUDENT", "Thanjavur", "Maatram Student Chapter", 0.0, 0, 0, 0, null, true),
            SeedVol("demo_vol_19", "Kavin Kumar (Demo)", "MF-DEMO-019", "ALUMNI", "Erode", "Cognizant Outreach Chapter", 26.0, 6, 4, 0, 15, true),
            SeedVol("demo_vol_20", "Shruthi Mohan (Demo)", "MF-DEMO-020", "STUDENT", "Vellore", "Maatram Student Chapter", 9.0, 2, 1, 0, 82, true)
        )

        val batch2 = db.batch()
        sampleVols.forEach { v ->
            val points = (v.hours * 10).toLong() + (v.acts * 25)
            val consistency = (v.acts * 10).coerceAtMost(95)
            val tier = when {
                points >= 800 -> "PLATINUM"
                points >= 400 -> "GOLD"
                points >= 150 -> "SILVER"
                else -> "BRONZE"
            }
            val badges = buildList {
                if (v.acts >= 1) add("First Contribution")
                if (v.hours >= 10.0) add("Active Volunteer")
                if (v.completedPv >= 3) add("Physical Verification Expert")
                if (points >= 400) add("Outstanding Contributor")
            }
            val statsMap = mutableMapOf<String, Any>(
                "userId" to v.uid,
                "displayName" to v.name,
                "volunteerCode" to v.code,
                "volunteerType" to v.type,
                "district" to v.district,
                "employerOrgName" to v.org,
                "availability" to if (v.avail) "Weekends & Evenings" else "Unavailable (Exams)",
                "availableForPv" to v.avail,
                "verifiedHours" to v.hours,
                "completedActivities" to v.acts,
                "completedPvCases" to v.completedPv,
                "activePvCases" to v.activePv,
                "points" to points,
                "consistencyScore" to consistency,
                "tier" to tier,
                "badges" to badges,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (v.lastDaysAgo != null) {
                statsMap["lastVerifiedAt"] = daysAgoTs(v.lastDaysAgo)
            }
            batch2.set(db.collection("volunteer_stats").document(v.uid), statsMap)

            batch2.set(
                db.collection("profiles").document(v.uid),
                mapOf(
                    "userId" to v.uid,
                    "fullName" to v.name,
                    "email" to "${v.uid}@demo.maatram.org",
                    "phone" to "90000${v.code.takeLast(5)}",
                    "volunteerCode" to v.code,
                    "volunteerType" to v.type,
                    "college" to "Demo Partner Institution",
                    "department" to "Engineering / Arts",
                    "yearOfStudy" to if (v.type == "STUDENT") "3rd Year" else "Alumni",
                    "district" to v.district,
                    "city" to v.district,
                    "skills" to "Mentoring, Tamil, Communication",
                    "interests" to "Physical Verification, Calling, Events",
                    "availability" to if (v.avail) "Weekends" else "Unavailable",
                    "preferredContact" to "WhatsApp",
                    "employerOrgId" to "org_maatram_stu",
                    "employerOrgName" to v.org,
                    "consentDataProcessing" to true,
                    "consentLinkedInPublic" to true,
                    "profileCompleted" to true,
                    "role" to "VOLUNTEER",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch2.commit().await()

        // 6. 10 Sample Opportunities across all 4 categories
        val opps = listOf(
            mapOf(
                "id" to "opp_pv_trichy_demo",
                "title" to "Tiruchirappalli Admission Home PV Drive",
                "category" to "PHYSICAL_VERIFICATION",
                "activityType" to "Physical Verification",
                "description" to "Fair-allocation home visits for 2026 scholarship applicants across Srirangam, Woraiyur, and Thiruverumbur.",
                "district" to "Tiruchirappalli",
                "location" to "Srirangam & Woraiyur Zones",
                "mode" to "OFFLINE",
                "eventDate" to "2026-12-06",
                "startTime" to "09:00",
                "endTime" to "16:30",
                "deadline" to "2026-12-03",
                "requiredVolunteers" to 4L,
                "availableSlots" to 4L,
                "totalCases" to 6L,
                "skillsRequired" to "Tamil fluency, local transit, empathetic listening",
                "eligibility" to "Verified volunteers with < 3 active PV cases",
                "instructions" to "Submit visit notes via Maatram Connect; never share applicant info publicly.",
                "whatsappGroupLink" to "https://chat.whatsapp.com/maatram-trichy-pv-demo",
                "status" to "OPEN",
                "multiDayCount" to 1L
            ),
            mapOf(
                "id" to "opp_pv_chennai_demo",
                "title" to "Chennai North Physical Verification Batch",
                "category" to "PHYSICAL_VERIFICATION",
                "activityType" to "Physical Verification",
                "description" to "District verification of applicant family socio-economic background in North Chennai.",
                "district" to "Chennai",
                "location" to "Perambur & Royapuram",
                "mode" to "OFFLINE",
                "eventDate" to "2026-12-08",
                "startTime" to "09:30",
                "endTime" to "15:30",
                "deadline" to "2026-12-05",
                "requiredVolunteers" to 3L,
                "availableSlots" to 3L,
                "totalCases" to 4L,
                "skillsRequired" to "Local commute, documentation",
                "eligibility" to "Chennai district volunteers",
                "instructions" to "Verify details respectfully with guardian present.",
                "whatsappGroupLink" to "https://chat.whatsapp.com/maatram-chennai-pv-demo",
                "status" to "OPEN",
                "multiDayCount" to 1L
            ),
            mapOf(
                "id" to "opp_pv_cbe_demo",
                "title" to "Coimbatore Rural Household Verification",
                "category" to "PHYSICAL_VERIFICATION",
                "activityType" to "Physical Verification",
                "description" to "Home visits for rural admission candidates around Pollachi and Sulur.",
                "district" to "Coimbatore",
                "location" to "Pollachi & Sulur Blocks",
                "mode" to "OFFLINE",
                "eventDate" to "2026-12-10",
                "startTime" to "09:00",
                "endTime" to "16:00",
                "deadline" to "2026-12-07",
                "requiredVolunteers" to 3L,
                "availableSlots" to 3L,
                "totalCases" to 4L,
                "skillsRequired" to "Two-wheeler preferred, field verification",
                "eligibility" to "Coimbatore volunteers",
                "instructions" to "Update case status immediately after visit.",
                "whatsappGroupLink" to "https://chat.whatsapp.com/maatram-cbe-pv-demo",
                "status" to "OPEN",
                "multiDayCount" to 1L
            ),
            mapOf(
                "id" to "opp_call_bgv_1",
                "title" to "2026 Batch-A Applicant Calling & BGV",
                "category" to "CALLING_BGV",
                "activityType" to "Calling Verification",
                "description" to "Phone verification of admission forms, academic marks, and family contact confirmation.",
                "district" to "Chennai",
                "location" to "Remote / Reference Portal",
                "mode" to "ONLINE",
                "eventDate" to "2026-12-01",
                "startTime" to "10:00",
                "endTime" to "13:00",
                "deadline" to "2026-11-30",
                "requiredVolunteers" to 6L,
                "availableSlots" to 5L,
                "totalCases" to 10L,
                "skillsRequired" to "Clear Tamil & English phone communication",
                "eligibility" to "All trained volunteers",
                "instructions" to "Use Applicant Ref IDs only; log follow-ups accurately.",
                "whatsappGroupLink" to "https://chat.whatsapp.com/maatram-calling-demo",
                "status" to "OPEN",
                "multiDayCount" to 1L
            ),
            mapOf(
                "id" to "opp_call_bgv_2",
                "title" to "Madurai & Salem Document BGV Follow-Up",
                "category" to "CALLING_BGV",
                "activityType" to "Background Verification",
                "description" to "Secondary BGV calls for pending scholarship document clarifications.",
                "district" to "Madurai",
                "location" to "Remote",
                "mode" to "ONLINE",
                "eventDate" to "2026-12-04",
                "startTime" to "17:00",
                "endTime" to "19:30",
                "deadline" to "2026-12-02",
                "requiredVolunteers" to 4L,
                "availableSlots" to 4L,
                "totalCases" to 6L,
                "skillsRequired" to "Attention to detail",
                "eligibility" to "All volunteers",
                "instructions" to "Mark Call Back Required if unreachable twice.",
                "whatsappGroupLink" to "https://chat.whatsapp.com/maatram-bgv-demo",
                "status" to "OPEN",
                "multiDayCount" to 1L
            ),
            mapOf(
                "id" to "opp_int_mock",
                "title" to "Final-Year Student Mock Interviews",
                "category" to "INTERVIEWS",
                "activityType" to "Mock Interviews",
                "description" to "1-on-1 technical and HR mock interviews for Maatram scholars preparing for placements.",
                "district" to "Chennai",
                "location" to "Google Meet / Zoom",
                "mode" to "ONLINE",
                "eventDate" to "2026-12-07",
                "startTime" to "10:00",
                "endTime" to "13:00",
                "deadline" to "2026-12-04",
                "requiredVolunteers" to 8L,
                "availableSlots" to 7L,
                "totalCases" to 0L,
                "skillsRequired" to "Industry interviewing experience",
                "eligibility" to "Alumni & Corporate Supporters",
                "instructions" to "Submit structured strengths and growth feedback.",
                "whatsappGroupLink" to "https://chat.whatsapp.com/maatram-mock-demo",
                "status" to "OPEN",
                "multiDayCount" to 1L
            ),
            mapOf(
                "id" to "opp_int_online_panel",
                "title" to "Online Admission Panel Interviews",
                "category" to "INTERVIEWS",
                "activityType" to "Online Admission Panel",
                "description" to "Evaluate incoming scholarship applicants via virtual panel interviews.",
                "district" to "Coimbatore",
                "location" to "Online Panel Room",
                "mode" to "ONLINE",
                "eventDate" to "2026-12-12",
                "startTime" to "09:30",
                "endTime" to "13:30",
                "deadline" to "2026-12-09",
                "requiredVolunteers" to 6L,
                "availableSlots" to 6L,
                "totalCases" to 0L,
                "skillsRequired" to "Student evaluation, empathy",
                "eligibility" to "Alumni & Senior Volunteers",
                "instructions" to "Keep applicant responses confidential.",
                "whatsappGroupLink" to "https://chat.whatsapp.com/maatram-panel-online",
                "status" to "OPEN",
                "multiDayCount" to 1L
            ),
            mapOf(
                "id" to "opp_int_offline_panel",
                "title" to "Offline Admission Panel - Trichy Hub",
                "category" to "INTERVIEWS",
                "activityType" to "Offline Admission Panel",
                "description" to "In-person admission panel interviews at Tiruchirappalli partner campus.",
                "district" to "Tiruchirappalli",
                "location" to "Trichy Partner College Auditorium",
                "mode" to "OFFLINE",
                "eventDate" to "2026-12-14",
                "startTime" to "09:00",
                "endTime" to "16:00",
                "deadline" to "2026-12-10",
                "requiredVolunteers" to 5L,
                "availableSlots" to 5L,
                "totalCases" to 0L,
                "skillsRequired" to "Panel coordination",
                "eligibility" to "Alumni & Office-approved seniors",
                "instructions" to "Arrive 30 minutes before candidate check-in.",
                "whatsappGroupLink" to "https://chat.whatsapp.com/maatram-panel-trichy",
                "status" to "OPEN",
                "multiDayCount" to 1L
            ),
            mapOf(
                "id" to "opp_evt_sangamam",
                "title" to "Maatram Sangamam Annual Gathering (2-Day)",
                "category" to "EVENTS",
                "activityType" to "Sangamam",
                "description" to "Two-day flagship annual gathering of Maatram students, alumni, and mentors.",
                "district" to "Chennai",
                "location" to "Chennai Main Convention Hall",
                "mode" to "OFFLINE",
                "eventDate" to "2026-12-19",
                "startTime" to "08:30",
                "endTime" to "17:30",
                "deadline" to "2026-12-12",
                "requiredVolunteers" to 15L,
                "availableSlots" to 14L,
                "totalCases" to 0L,
                "skillsRequired" to "Stage logistics, hospitality, registration desk",
                "eligibility" to "All Maatram Students & Alumni",
                "instructions" to "Log attendance & hours separately for Day 1 and Day 2.",
                "whatsappGroupLink" to "https://chat.whatsapp.com/maatram-sangamam-demo",
                "status" to "OPEN",
                "multiDayCount" to 2L
            ),
            mapOf(
                "id" to "opp_evt_bootcamp",
                "title" to "School Visit & Career Boot Camp - Madurai",
                "category" to "EVENTS",
                "activityType" to "Boot Camp",
                "description" to "Career guidance workshop and STEM boot camp for government high school students.",
                "district" to "Madurai",
                "location" to "Govt Higher Secondary School, Madurai",
                "mode" to "OFFLINE",
                "eventDate" to "2026-12-21",
                "startTime" to "09:30",
                "endTime" to "15:30",
                "deadline" to "2026-12-16",
                "requiredVolunteers" to 6L,
                "availableSlots" to 6L,
                "totalCases" to 0L,
                "skillsRequired" to "Public speaking, mentoring",
                "eligibility" to "All Volunteers",
                "instructions" to "Bring session slides and activity kits.",
                "whatsappGroupLink" to "https://chat.whatsapp.com/maatram-bootcamp-demo",
                "status" to "OPEN",
                "multiDayCount" to 1L
            )
        )

        val batch3 = db.batch()
        opps.forEach { opp ->
            val id = opp["id"] as String
            val data: MutableMap<String, Any> = opp.filterKeys { it != "id" }.mapValuesTo(mutableMapOf()) { it.value as Any }
            data["createdBy"] = actorUid
            data["createdAt"] = FieldValue.serverTimestamp()
            data["updatedAt"] = FieldValue.serverTimestamp()
            batch3.set(db.collection("opportunities").document(id), data)
        }

        // 7. Sample PV Cases across districts
        val pvCases = listOf(
            Triple("pv_try_1", "opp_pv_trichy_demo", "Tiruchirappalli" to "Srirangam Zone (Demo)"),
            Triple("pv_try_2", "opp_pv_trichy_demo", "Tiruchirappalli" to "Woraiyur Zone (Demo)"),
            Triple("pv_try_3", "opp_pv_trichy_demo", "Tiruchirappalli" to "Thiruverumbur Zone (Demo)"),
            Triple("pv_try_4", "opp_pv_trichy_demo", "Tiruchirappalli" to "KK Nagar Zone (Demo)"),
            Triple("pv_chn_1", "opp_pv_chennai_demo", "Chennai" to "Perambur Zone (Demo)"),
            Triple("pv_chn_2", "opp_pv_chennai_demo", "Chennai" to "Royapuram Zone (Demo)"),
            Triple("pv_cbe_1", "opp_pv_cbe_demo", "Coimbatore" to "Pollachi Block (Demo)"),
            Triple("pv_cbe_2", "opp_pv_cbe_demo", "Coimbatore" to "Sulur Block (Demo)")
        )
        pvCases.forEachIndexed { idx, (docId, oppId, loc) ->
            batch3.set(
                db.collection("pv_cases").document(docId),
                mapOf(
                    "caseRefId" to "PV-DEMO-2026-0${idx + 1}",
                    "opportunityId" to oppId,
                    "district" to loc.first,
                    "areaLabel" to loc.second,
                    "applicantCode" to "APP-FICTIONAL-${101 + idx}",
                    "assignedToUid" to if (idx == 4) "demo_vol_08" else "",
                    "assignedToName" to if (idx == 4) "Keerthana V (Demo)" else "",
                    "status" to if (idx == 4) "ASSIGNED" else "UNASSIGNED",
                    "visitNotes" to "",
                    "recommendationSummary" to "",
                    "verifiedBy" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // 8. Sample Calling Tasks (Fictional Applicant Ref IDs)
        val callTasks = listOf(
            Triple("call_demo_1", "APP-DEMO-401", "Tiruchirappalli"),
            Triple("call_demo_2", "APP-DEMO-402", "Chennai"),
            Triple("call_demo_3", "APP-DEMO-403", "Coimbatore"),
            Triple("call_demo_4", "APP-DEMO-404", "Madurai")
        )
        callTasks.forEachIndexed { idx, (id, ref, dist) ->
            batch3.set(
                db.collection("calling_tasks").document(id),
                mapOf(
                    "applicantRefId" to ref,
                    "opportunityId" to "opp_call_bgv_1",
                    "district" to dist,
                    "callingDate" to "2026-12-01",
                    "assignedToUid" to if (idx < 2) "demo_vol_02" else actorUid,
                    "assignedToName" to if (idx < 2) "Priyadharshini M (Demo)" else actorName,
                    "callStatus" to if (idx == 0) "DETAILS_VERIFIED" else "NOT_STARTED",
                    "followUpRequired" to (idx == 1),
                    "notes" to if (idx == 0) "Marks & parent contact verified (Demo)" else "",
                    "verifiedByOffice" to (idx == 0),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // 9. Sample Pending Requests for Fair Allocation Demo (Trichy PV Drive!)
        val trichyRequesters = listOf(
            Triple("demo_vol_01", "Aravind Krishnan (Demo)", "MF-DEMO-001"),
            Triple("demo_vol_02", "Priyadharshini M (Demo)", "MF-DEMO-002"),
            Triple("demo_vol_03", "Vigneshwaran S (Demo)", "MF-DEMO-003"),
            Triple("demo_vol_04", "Harini Natarajan (Demo)", "MF-DEMO-004"),
            Triple("demo_vol_05", "Santhosh Kumar (Demo)", "MF-DEMO-005")
        )
        trichyRequesters.forEach { (uid, name, code) ->
            batch3.set(
                db.collection("volunteer_requests").document("opp_pv_trichy_demo_$uid"),
                mapOf(
                    "userId" to uid,
                    "volunteerName" to name,
                    "volunteerCode" to code,
                    "opportunityId" to "opp_pv_trichy_demo",
                    "opportunityTitle" to "Tiruchirappalli Admission Home PV Drive",
                    "category" to "PHYSICAL_VERIFICATION",
                    "district" to "Tiruchirappalli",
                    "status" to "PENDING",
                    "availabilityNote" to "Available Sunday 9 AM - 4 PM",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // 10. Sample Approved & Pending Hours + Audit Log
        batch3.set(
            db.collection("volunteer_hours").document("hr_demo_1"),
            mapOf(
                "userId" to "demo_vol_01",
                "volunteerName" to "Aravind Krishnan (Demo)",
                "volunteerCode" to "MF-DEMO-001",
                "assignmentId" to "demo_assign_01",
                "opportunityId" to "opp_pv_trichy_demo",
                "opportunityTitle" to "Tiruchirappalli Admission Home PV Drive",
                "category" to "PHYSICAL_VERIFICATION",
                "district" to "Tiruchirappalli",
                "employerOrgName" to "TCS Maitree Alumni Wing",
                "activityDate" to "2026-11-18",
                "startTime" to "09:30",
                "endTime" to "13:30",
                "breakMinutes" to 0L,
                "calculatedHours" to 4.0,
                "submittedHours" to 4.0,
                "dayNumber" to 1L,
                "notes" to "Verified 2 applicant households in Trichy",
                "verificationStatus" to "APPROVED",
                "reviewerUid" to actorUid,
                "reviewerName" to actorName,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch3.set(
            db.collection("volunteer_hours").document("hr_demo_pending"),
            mapOf(
                "userId" to "demo_vol_02",
                "volunteerName" to "Priyadharshini M (Demo)",
                "volunteerCode" to "MF-DEMO-002",
                "assignmentId" to "demo_assign_02",
                "opportunityId" to "opp_call_bgv_1",
                "opportunityTitle" to "2026 Batch-A Applicant Calling & BGV",
                "category" to "CALLING_BGV",
                "district" to "Tiruchirappalli",
                "employerOrgName" to "Maatram Student Chapter",
                "activityDate" to "2026-11-24",
                "startTime" to "10:00",
                "endTime" to "12:30",
                "breakMinutes" to 0L,
                "calculatedHours" to 2.5,
                "submittedHours" to 2.5,
                "dayNumber" to 1L,
                "notes" to "Completed 12 verification calls",
                "verificationStatus" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch3.set(
            db.collection("audit_logs").document("log_seed_${now}"),
            mapOf(
                "actorUid" to actorUid,
                "actorName" to actorName,
                "actorRole" to "ADMIN",
                "action" to "DEMO_WORKSPACE_SEEDED",
                "targetType" to "system",
                "targetId" to "global",
                "details" to "Seeded 20 fictional demo volunteers, 5 office coordinators, 8 districts, 10 opportunities, PV cases, and calling tasks.",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch3.commit().await()
    }
}
