package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AppNotification
import com.example.data.model.AppSettings
import com.example.data.model.Assignment
import com.example.data.model.AssignmentStatus
import com.example.data.model.AuditLog
import com.example.data.model.CallStatus
import com.example.data.model.CallingTask
import com.example.data.model.DistrictItem
import com.example.data.model.EmployerOrganization
import com.example.data.model.OfficeTeamMember
import com.example.data.model.Opportunity
import com.example.data.model.OpportunityCategory
import com.example.data.model.OpportunityMode
import com.example.data.model.OpportunityStatus
import com.example.data.model.PhysicalVerificationCase
import com.example.data.model.PvCaseStatus
import com.example.data.model.RequestStatus
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.VerificationStatus
import com.example.data.model.VolunteerHours
import com.example.data.model.VolunteerRequest
import com.example.data.model.VolunteerStats
import com.example.data.model.VolunteerType
import com.example.data.seed.MaatramSeedData
import com.example.domain.FairAllocationEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MaatramRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> Query.observeTypedList(
        path: String,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        emitAll(
            snapshots()
                .map { it.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMyProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Profile observation mismatch" }
        val docRef = db.collection("profiles").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeMyOfficeTeamRecord(userId: String): Flow<OfficeTeamMember?> = flow {
        val uid = requireUserId()
        val docRef = db.collection("office_team").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(OfficeTeamMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeAppSettings(): Flow<AppSettings> = flow {
        val uid = requireUserId()
        val docRef = db.collection("app_settings").document("global")
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    snap.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: AppSettings()
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeAllStats(): Flow<List<VolunteerStats>> =
        db.collection("volunteer_stats")
            .whereGreaterThanOrEqualTo("points", 0)
            .observeTypedList("volunteer_stats", VolunteerStats::class.java)

    fun observeDistricts(): Flow<List<DistrictItem>> =
        db.collection("districts")
            .whereEqualTo("active", true)
            .observeTypedList("districts", DistrictItem::class.java)

    fun observeOrganizations(): Flow<List<EmployerOrganization>> =
        db.collection("organizations")
            .whereEqualTo("active", true)
            .observeTypedList("organizations", EmployerOrganization::class.java)

    fun observeOpportunities(): Flow<List<Opportunity>> =
        db.collection("opportunities")
            .whereIn("status", listOf("OPEN", "CLOSED", "COMPLETED"))
            .observeTypedList("opportunities", Opportunity::class.java)

    fun observeRequests(userId: String, isStaff: Boolean): Flow<List<VolunteerRequest>> =
        if (isStaff) {
            db.collection("volunteer_requests").observeTypedList("volunteer_requests", VolunteerRequest::class.java)
        } else {
            db.collection("volunteer_requests")
                .whereEqualTo("userId", userId)
                .observeTypedList("volunteer_requests", VolunteerRequest::class.java)
        }

    fun observeAssignments(userId: String, isStaff: Boolean): Flow<List<Assignment>> =
        if (isStaff) {
            db.collection("assignments").observeTypedList("assignments", Assignment::class.java)
        } else {
            db.collection("assignments")
                .whereEqualTo("userId", userId)
                .observeTypedList("assignments", Assignment::class.java)
        }

    fun observePvCases(userId: String, isStaff: Boolean): Flow<List<PhysicalVerificationCase>> =
        if (isStaff) {
            db.collection("pv_cases").observeTypedList("pv_cases", PhysicalVerificationCase::class.java)
        } else {
            db.collection("pv_cases")
                .whereEqualTo("assignedToUid", userId)
                .observeTypedList("pv_cases", PhysicalVerificationCase::class.java)
        }

    fun observeCallingTasks(userId: String, isStaff: Boolean): Flow<List<CallingTask>> =
        if (isStaff) {
            db.collection("calling_tasks").observeTypedList("calling_tasks", CallingTask::class.java)
        } else {
            db.collection("calling_tasks")
                .whereEqualTo("assignedToUid", userId)
                .observeTypedList("calling_tasks", CallingTask::class.java)
        }

    fun observeVolunteerHours(userId: String, isStaff: Boolean): Flow<List<VolunteerHours>> =
        if (isStaff) {
            db.collection("volunteer_hours").observeTypedList("volunteer_hours", VolunteerHours::class.java)
        } else {
            db.collection("volunteer_hours")
                .whereEqualTo("userId", userId)
                .observeTypedList("volunteer_hours", VolunteerHours::class.java)
        }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> =
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .observeTypedList("notifications", AppNotification::class.java)

    fun observeAllProfilesForStaff(): Flow<List<UserProfile>> =
        db.collection("profiles").observeTypedList("profiles", UserProfile::class.java)

    fun observeOfficeTeamForStaff(): Flow<List<OfficeTeamMember>> =
        db.collection("office_team").observeTypedList("office_team", OfficeTeamMember::class.java)

    fun observeAuditLogsForStaff(): Flow<List<AuditLog>> =
        db.collection("audit_logs").observeTypedList("audit_logs", AuditLog::class.java)

    suspend fun saveUserProfile(
        profile: UserProfile,
        isNewProfile: Boolean,
        availableForPv: Boolean
    ): Result<Unit> {
        val path = "profiles/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Refusing cross-user profile write" }
            val batch = db.batch()
            val profileRef = db.collection("profiles").document(uid)
            val statsRef = db.collection("volunteer_stats").document(uid)

            if (isNewProfile) {
                val safeRole = if (auth.currentUser?.email == "nrajasri99@gmail.com" && auth.currentUser?.isEmailVerified == true) {
                    UserRole.ADMIN.name
                } else {
                    UserRole.VOLUNTEER.name
                }
                batch.set(profileRef, profile.copy(role = safeRole, profileCompleted = true).toCreateMap())
                batch.set(
                    statsRef,
                    mapOf(
                        "userId" to uid,
                        "displayName" to profile.fullName,
                        "volunteerCode" to profile.volunteerCode,
                        "volunteerType" to profile.volunteerType,
                        "district" to profile.district,
                        "employerOrgName" to profile.employerOrgName,
                        "availability" to profile.availability,
                        "availableForPv" to availableForPv,
                        "verifiedHours" to 0.0,
                        "completedActivities" to 0L,
                        "completedPvCases" to 0L,
                        "activePvCases" to 0L,
                        "points" to 0L,
                        "consistencyScore" to 0L,
                        "tier" to "BRONZE",
                        "badges" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val notifRef = db.collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "userId" to uid,
                        "title" to "Welcome to Maatram Connect",
                        "message" to "Registration confirmed for ${profile.fullName} (${profile.volunteerCode}). Explore opportunities across Tamil Nadu.",
                        "type" to "REGISTRATION",
                        "emailMode" to "DEMO_PREVIEW",
                        "readFlag" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    profileRef,
                    mapOf(
                        "fullName" to profile.fullName,
                        "phone" to profile.phone,
                        "volunteerType" to profile.volunteerType,
                        "college" to profile.college,
                        "department" to profile.department,
                        "yearOfStudy" to profile.yearOfStudy,
                        "district" to profile.district,
                        "city" to profile.city,
                        "skills" to profile.skills,
                        "interests" to profile.interests,
                        "availability" to profile.availability,
                        "preferredContact" to profile.preferredContact,
                        "employerOrgId" to profile.employerOrgId,
                        "employerOrgName" to profile.employerOrgName,
                        "consentDataProcessing" to profile.consentDataProcessing,
                        "consentLinkedInPublic" to profile.consentLinkedInPublic,
                        "profileCompleted" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    statsRef,
                    mapOf(
                        "displayName" to profile.fullName,
                        "volunteerType" to profile.volunteerType,
                        "district" to profile.district,
                        "employerOrgName" to profile.employerOrgName,
                        "availability" to profile.availability,
                        "availableForPv" to availableForPv,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePvAvailability(availableForPv: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not signed in"))
        val path = "volunteer_stats/$uid"
        return try {
            db.collection("volunteer_stats").document(uid).update(
                mapOf(
                    "availableForPv" to availableForPv,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createOpportunity(
        opportunity: Opportunity,
        pvCaseCountToGenerate: Int,
        actorName: String,
        actorRole: String
    ): Result<Unit> {
        val path = "opportunities"
        return try {
            val uid = requireUserId()
            val oppRef = db.collection("opportunities").document()
            val oppId = oppRef.id
            val batch = db.batch()
            batch.set(
                oppRef,
                mapOf(
                    "title" to opportunity.title.trim(),
                    "category" to opportunity.category,
                    "activityType" to opportunity.activityType.trim(),
                    "description" to opportunity.description.trim(),
                    "district" to opportunity.district,
                    "location" to opportunity.location.trim(),
                    "mode" to opportunity.mode,
                    "eventDate" to opportunity.eventDate.trim(),
                    "startTime" to opportunity.startTime.trim(),
                    "endTime" to opportunity.endTime.trim(),
                    "deadline" to opportunity.deadline.trim(),
                    "requiredVolunteers" to opportunity.requiredVolunteers,
                    "availableSlots" to opportunity.requiredVolunteers,
                    "totalCases" to pvCaseCountToGenerate.toLong(),
                    "skillsRequired" to opportunity.skillsRequired.trim(),
                    "eligibility" to opportunity.eligibility.trim(),
                    "instructions" to opportunity.instructions.trim(),
                    "whatsappGroupLink" to opportunity.whatsappGroupLink.trim(),
                    "status" to OpportunityStatus.OPEN.name,
                    "multiDayCount" to opportunity.multiDayCount.coerceAtLeast(1L),
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (opportunity.category == OpportunityCategory.PHYSICAL_VERIFICATION.name && pvCaseCountToGenerate > 0) {
                val distCode = opportunity.district.take(3).uppercase()
                for (i in 1..pvCaseCountToGenerate) {
                    val caseRef = db.collection("pv_cases").document()
                    batch.set(
                        caseRef,
                        mapOf(
                            "caseRefId" to "PV-$distCode-${System.currentTimeMillis() % 10000}-$i",
                            "opportunityId" to oppId,
                            "district" to opportunity.district,
                            "areaLabel" to "${opportunity.location.ifBlank { opportunity.district }} Zone $i",
                            "applicantCode" to "APP-PV-${100 + i}",
                            "assignedToUid" to "",
                            "assignedToName" to "",
                            "status" to PvCaseStatus.UNASSIGNED.name,
                            "visitNotes" to "",
                            "recommendationSummary" to "",
                            "verifiedBy" to "",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            val logRef = db.collection("audit_logs").document()
            batch.set(
                logRef,
                mapOf(
                    "actorUid" to uid,
                    "actorName" to actorName,
                    "actorRole" to actorRole,
                    "action" to "OPPORTUNITY_CREATED",
                    "targetType" to "opportunities",
                    "targetId" to oppId,
                    "details" to "Created ${opportunity.category} opportunity '${opportunity.title}' in ${opportunity.district}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOpportunityStatus(
        oppId: String,
        newStatus: OpportunityStatus,
        actorName: String,
        actorRole: String
    ): Result<Unit> {
        val path = "opportunities/$oppId"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            batch.update(
                db.collection("opportunities").document(oppId),
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("audit_logs").document(),
                mapOf(
                    "actorUid" to uid,
                    "actorName" to actorName,
                    "actorRole" to actorRole,
                    "action" to "OPPORTUNITY_STATUS_UPDATED",
                    "targetType" to "opportunities",
                    "targetId" to oppId,
                    "details" to "Updated opportunity status to ${newStatus.name}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitVolunteerRequest(
        profile: UserProfile,
        opportunity: Opportunity,
        availabilityNote: String
    ): Result<Unit> {
        val reqId = "${opportunity.id}_${profile.userId}"
        val path = "volunteer_requests/$reqId"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId)
            val existingDoc = db.collection("volunteer_requests").document(reqId).get().await()
            if (existingDoc.exists()) {
                val status = existingDoc.getString("status") ?: ""
                if (status != RequestStatus.CANCELLED.name && status != RequestStatus.REJECTED.name) {
                    return Result.failure(IllegalStateException("You have already submitted a request for this opportunity."))
                }
            }
            val batch = db.batch()
            batch.set(
                db.collection("volunteer_requests").document(reqId),
                mapOf(
                    "userId" to uid,
                    "volunteerName" to profile.fullName,
                    "volunteerCode" to profile.volunteerCode,
                    "opportunityId" to opportunity.id,
                    "opportunityTitle" to opportunity.title,
                    "category" to opportunity.category,
                    "district" to opportunity.district,
                    "status" to RequestStatus.PENDING.name,
                    "rejectionReason" to "",
                    "availabilityNote" to availabilityNote.ifBlank { profile.availability },
                    "reviewedBy" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("notifications").document(),
                mapOf(
                    "userId" to uid,
                    "title" to "Request Submitted",
                    "message" to "Your request to join '${opportunity.title}' is pending Office Team review.",
                    "type" to "REQUEST_STATUS",
                    "emailMode" to "DEMO_PREVIEW",
                    "readFlag" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelVolunteerRequest(reqId: String): Result<Unit> {
        val path = "volunteer_requests/$reqId"
        return try {
            requireUserId()
            db.collection("volunteer_requests").document(reqId)
                .update(
                    mapOf(
                        "status" to RequestStatus.CANCELLED.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun reviewVolunteerRequest(
        request: VolunteerRequest,
        opportunity: Opportunity,
        decision: RequestStatus,
        rejectionReason: String,
        actorName: String,
        actorRole: String
    ): Result<Unit> {
        val path = "volunteer_requests/${request.id}"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            val reqRef = db.collection("volunteer_requests").document(request.id)
            batch.update(
                reqRef,
                mapOf(
                    "status" to decision.name,
                    "rejectionReason" to if (decision == RequestStatus.REJECTED) rejectionReason.trim() else "",
                    "reviewedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (decision == RequestStatus.APPROVED) {
                val assignId = "${opportunity.id}_${request.userId}"
                val assignRef = db.collection("assignments").document(assignId)
                batch.set(
                    assignRef,
                    mapOf(
                        "userId" to request.userId,
                        "volunteerName" to request.volunteerName,
                        "volunteerCode" to request.volunteerCode,
                        "opportunityId" to opportunity.id,
                        "opportunityTitle" to opportunity.title,
                        "category" to opportunity.category,
                        "district" to opportunity.district,
                        "assignedCasesCount" to 0L,
                        "caseIds" to emptyList<String>(),
                        "status" to AssignmentStatus.ASSIGNED.name,
                        "progressNotes" to "",
                        "whatsappGroupLink" to opportunity.whatsappGroupLink,
                        "overrideReason" to "",
                        "assignedBy" to uid,
                        "verifiedBy" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (opportunity.availableSlots > 0) {
                    batch.update(
                        db.collection("opportunities").document(opportunity.id),
                        mapOf(
                            "availableSlots" to FieldValue.increment(-1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            val notifMsg = when (decision) {
                RequestStatus.APPROVED -> "Approved for '${opportunity.title}'. Check My Assignments for details & WhatsApp invite link."
                RequestStatus.REJECTED -> "Request for '${opportunity.title}' was not approved. Reason: ${rejectionReason.ifBlank { "Capacity filled" }}"
                RequestStatus.WAITLISTED -> "You are waitlisted for '${opportunity.title}'."
                else -> "Request status updated to ${decision.label}."
            }
            batch.set(
                db.collection("notifications").document(),
                mapOf(
                    "userId" to request.userId,
                    "title" to "Request ${decision.label}",
                    "message" to notifMsg,
                    "type" to "REQUEST_STATUS",
                    "emailMode" to "DEMO_PREVIEW",
                    "readFlag" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("audit_logs").document(),
                mapOf(
                    "actorUid" to uid,
                    "actorName" to actorName,
                    "actorRole" to actorRole,
                    "action" to "REQUEST_${decision.name}",
                    "targetType" to "volunteer_requests",
                    "targetId" to request.id,
                    "details" to "${decision.name} request for ${request.volunteerName} on ${opportunity.title}",
                    "reason" to rejectionReason,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun allocatePvCasesFairly(
        opportunity: Opportunity,
        candidateStats: VolunteerStats,
        selectedCases: List<PhysicalVerificationCase>,
        overrideReason: String,
        maxActivePvCases: Int,
        actorName: String,
        actorRole: String
    ): Result<Unit> {
        val assignId = "${opportunity.id}_${candidateStats.userId}"
        val path = "assignments/$assignId"
        return try {
            val uid = requireUserId()
            require(selectedCases.isNotEmpty()) { "Select at least one unassigned PV case to allocate." }
            val currentActive = candidateStats.activePvCases.toInt()
            val needsOverride = currentActive > 0 || (currentActive + selectedCases.size > maxActivePvCases)
            if (needsOverride && overrideReason.isBlank()) {
                return Result.failure(
                    IllegalStateException(
                        "Volunteer already has $currentActive active PV cases (limit: $maxActivePvCases). Provide a mandatory override reason to proceed."
                    )
                )
            }

            // Prevent duplicate assignment to same opportunity
            val existingAssign = db.collection("assignments").document(assignId).get().await()
            if (existingAssign.exists()) {
                return Result.failure(IllegalStateException("${candidateStats.displayName} is already assigned to this PV drive."))
            }

            val batch = db.batch()
            val caseIds = selectedCases.map { it.id }

            selectedCases.forEach { pvCase ->
                batch.update(
                    db.collection("pv_cases").document(pvCase.id),
                    mapOf(
                        "assignedToUid" to candidateStats.userId,
                        "assignedToName" to candidateStats.displayName,
                        "status" to PvCaseStatus.ASSIGNED.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.set(
                db.collection("assignments").document(assignId),
                mapOf(
                    "userId" to candidateStats.userId,
                    "volunteerName" to candidateStats.displayName,
                    "volunteerCode" to candidateStats.volunteerCode,
                    "opportunityId" to opportunity.id,
                    "opportunityTitle" to opportunity.title,
                    "category" to OpportunityCategory.PHYSICAL_VERIFICATION.name,
                    "district" to opportunity.district,
                    "assignedCasesCount" to selectedCases.size.toLong(),
                    "caseIds" to caseIds,
                    "status" to AssignmentStatus.ASSIGNED.name,
                    "progressNotes" to "",
                    "whatsappGroupLink" to opportunity.whatsappGroupLink,
                    "overrideReason" to overrideReason.trim(),
                    "assignedBy" to uid,
                    "verifiedBy" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Update request status if one exists
            val reqId = "${opportunity.id}_${candidateStats.userId}"
            val reqSnap = db.collection("volunteer_requests").document(reqId).get().await()
            if (reqSnap.exists()) {
                batch.update(
                    db.collection("volunteer_requests").document(reqId),
                    mapOf(
                        "status" to RequestStatus.APPROVED.name,
                        "reviewedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.update(
                db.collection("volunteer_stats").document(candidateStats.userId),
                mapOf(
                    "activePvCases" to FieldValue.increment(selectedCases.size.toLong()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (opportunity.availableSlots > 0) {
                batch.update(
                    db.collection("opportunities").document(opportunity.id),
                    mapOf(
                        "availableSlots" to FieldValue.increment(-1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.set(
                db.collection("notifications").document(),
                mapOf(
                    "userId" to candidateStats.userId,
                    "title" to "Physical Verification Assigned",
                    "message" to "Allocated ${selectedCases.size} PV case(s) in ${opportunity.district} for '${opportunity.title}'.",
                    "type" to "ASSIGNMENT",
                    "emailMode" to "DEMO_PREVIEW",
                    "readFlag" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.set(
                db.collection("audit_logs").document(),
                mapOf(
                    "actorUid" to uid,
                    "actorName" to actorName,
                    "actorRole" to actorRole,
                    "action" to if (overrideReason.isNotBlank()) "PV_ALLOCATION_OVERRIDE" else "PV_FAIR_ALLOCATION",
                    "targetType" to "assignments",
                    "targetId" to assignId,
                    "details" to "Assigned ${selectedCases.size} PV cases to ${candidateStats.displayName} (prior completed=${candidateStats.completedPvCases}, prior active=${candidateStats.activePvCases})",
                    "reason" to overrideReason.trim(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePvCaseByVolunteer(
        caseId: String,
        newStatus: PvCaseStatus,
        visitNotes: String,
        recommendationSummary: String
    ): Result<Unit> {
        val path = "pv_cases/$caseId"
        return try {
            requireUserId()
            require(newStatus == PvCaseStatus.VISITED || newStatus == PvCaseStatus.COMPLETED)
            db.collection("pv_cases").document(caseId)
                .update(
                    mapOf(
                        "status" to newStatus.name,
                        "visitNotes" to visitNotes.trim(),
                        "recommendationSummary" to recommendationSummary.trim(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun verifyPvCaseByOffice(
        pvCase: PhysicalVerificationCase,
        actorName: String,
        actorRole: String
    ): Result<Unit> {
        val path = "pv_cases/${pvCase.id}"
        return try {
            val uid = requireUserId()
            if (pvCase.status == PvCaseStatus.VERIFIED.name) {
                return Result.failure(IllegalStateException("Case already verified."))
            }
            val batch = db.batch()
            batch.update(
                db.collection("pv_cases").document(pvCase.id),
                mapOf(
                    "status" to PvCaseStatus.VERIFIED.name,
                    "verifiedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (pvCase.assignedToUid.isNotBlank()) {
                val volStatsRef = db.collection("volunteer_stats").document(pvCase.assignedToUid)
                val volSnap = volStatsRef.get().await()
                val currentActive = volSnap.getLong("activePvCases") ?: 0L
                val nextActive = (currentActive - 1L).coerceAtLeast(0L)
                batch.update(
                    volStatsRef,
                    mapOf(
                        "activePvCases" to nextActive,
                        "completedPvCases" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                db.collection("audit_logs").document(),
                mapOf(
                    "actorUid" to uid,
                    "actorName" to actorName,
                    "actorRole" to actorRole,
                    "action" to "PV_CASE_VERIFIED",
                    "targetType" to "pv_cases",
                    "targetId" to pvCase.id,
                    "details" to "Verified PV case ${pvCase.caseRefId} completed by ${pvCase.assignedToName}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAssignmentProgress(
        assignmentId: String,
        newStatus: AssignmentStatus,
        progressNotes: String
    ): Result<Unit> {
        val path = "assignments/$assignmentId"
        return try {
            requireUserId()
            require(newStatus == AssignmentStatus.IN_PROGRESS || newStatus == AssignmentStatus.COMPLETED)
            db.collection("assignments").document(assignmentId)
                .update(
                    mapOf(
                        "status" to newStatus.name,
                        "progressNotes" to progressNotes.trim(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun verifyAssignmentByOffice(
        assignment: Assignment,
        actorName: String,
        actorRole: String
    ): Result<Unit> {
        val path = "assignments/${assignment.id}"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            batch.update(
                db.collection("assignments").document(assignment.id),
                mapOf(
                    "status" to AssignmentStatus.VERIFIED.name,
                    "verifiedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("audit_logs").document(),
                mapOf(
                    "actorUid" to uid,
                    "actorName" to actorName,
                    "actorRole" to actorRole,
                    "action" to "ASSIGNMENT_VERIFIED",
                    "targetType" to "assignments",
                    "targetId" to assignment.id,
                    "details" to "Verified assignment '${assignment.opportunityTitle}' for ${assignment.volunteerName}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createCallingTask(
        applicantRefId: String,
        opportunityId: String,
        district: String,
        callingDate: String,
        assignedToUid: String,
        assignedToName: String
    ): Result<Unit> {
        val path = "calling_tasks"
        return try {
            requireUserId()
            db.collection("calling_tasks").document().set(
                mapOf(
                    "applicantRefId" to applicantRefId.trim(),
                    "opportunityId" to opportunityId,
                    "district" to district,
                    "callingDate" to callingDate.trim(),
                    "assignedToUid" to assignedToUid,
                    "assignedToName" to assignedToName,
                    "callStatus" to CallStatus.NOT_STARTED.name,
                    "followUpRequired" to false,
                    "notes" to "",
                    "verifiedByOffice" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun importDummyCallingSheet(
        opportunity: Opportunity,
        assignedVolunteer: VolunteerStats,
        count: Int = 5
    ): Result<Unit> {
        val path = "calling_tasks"
        return try {
            requireUserId()
            val batch = db.batch()
            val base = (System.currentTimeMillis() % 10000).toInt()
            for (i in 1..count) {
                val ref = db.collection("calling_tasks").document()
                batch.set(
                    ref,
                    mapOf(
                        "applicantRefId" to "APP-CSV-${base + i}",
                        "opportunityId" to opportunity.id,
                        "district" to opportunity.district,
                        "callingDate" to opportunity.eventDate,
                        "assignedToUid" to assignedVolunteer.userId,
                        "assignedToName" to assignedVolunteer.displayName,
                        "callStatus" to CallStatus.NOT_STARTED.name,
                        "followUpRequired" to false,
                        "notes" to "Imported from dummy CSV sheet",
                        "verifiedByOffice" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCallingTaskByVolunteer(
        taskId: String,
        callStatus: CallStatus,
        followUpRequired: Boolean,
        notes: String
    ): Result<Unit> {
        val path = "calling_tasks/$taskId"
        return try {
            requireUserId()
            db.collection("calling_tasks").document(taskId)
                .update(
                    mapOf(
                        "callStatus" to callStatus.name,
                        "followUpRequired" to followUpRequired,
                        "notes" to notes.trim(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun verifyCallingTaskByOffice(taskId: String): Result<Unit> {
        val path = "calling_tasks/$taskId"
        return try {
            requireUserId()
            db.collection("calling_tasks").document(taskId)
                .update(
                    mapOf(
                        "verifiedByOffice" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitVolunteerHours(
        profile: UserProfile,
        assignment: Assignment,
        activityDate: String,
        startTime: String,
        endTime: String,
        breakMinutes: Int,
        manualHoursOverride: Double?,
        dayNumber: Int,
        notes: String
    ): Result<Unit> {
        val hourDocId = "${assignment.id}_day$dayNumber"
        val path = "volunteer_hours/$hourDocId"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId)
            val existing = db.collection("volunteer_hours").document(hourDocId).get().await()
            if (existing.exists() && existing.getString("verificationStatus") != VerificationStatus.REJECTED.name) {
                return Result.failure(IllegalStateException("Hours for Day $dayNumber of this activity have already been submitted."))
            }
            val calculated = FairAllocationEngine.calculateDurationHours(startTime, endTime, breakMinutes)
            val submitted = (manualHoursOverride ?: calculated).coerceIn(0.25, 24.0)
            db.collection("volunteer_hours").document(hourDocId).set(
                mapOf(
                    "userId" to uid,
                    "volunteerName" to profile.fullName,
                    "volunteerCode" to profile.volunteerCode,
                    "assignmentId" to assignment.id,
                    "opportunityId" to assignment.opportunityId,
                    "opportunityTitle" to assignment.opportunityTitle,
                    "category" to assignment.category,
                    "district" to assignment.district,
                    "employerOrgName" to profile.employerOrgName,
                    "activityDate" to activityDate.trim(),
                    "startTime" to startTime.trim(),
                    "endTime" to endTime.trim(),
                    "breakMinutes" to breakMinutes.toLong(),
                    "calculatedHours" to calculated,
                    "submittedHours" to submitted,
                    "dayNumber" to dayNumber.toLong(),
                    "notes" to notes.trim(),
                    "verificationStatus" to VerificationStatus.PENDING.name,
                    "reviewerUid" to "",
                    "reviewerName" to "",
                    "correctionNote" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun reviewVolunteerHours(
        hoursRecord: VolunteerHours,
        decision: VerificationStatus,
        adjustedHours: Double,
        correctionNote: String,
        actorName: String,
        actorRole: String,
        emailConfigured: Boolean
    ): Result<Unit> {
        val path = "volunteer_hours/${hoursRecord.id}"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            val finalHours = adjustedHours.coerceIn(0.25, 24.0)
            batch.update(
                db.collection("volunteer_hours").document(hoursRecord.id),
                mapOf(
                    "verificationStatus" to decision.name,
                    "submittedHours" to finalHours,
                    "correctionNote" to correctionNote.trim(),
                    "reviewerUid" to uid,
                    "reviewerName" to actorName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (decision == VerificationStatus.APPROVED) {
                val volHoursSnap = db.collection("volunteer_hours")
                    .whereEqualTo("userId", hoursRecord.userId)
                    .get()
                    .await()
                val existingList = volHoursSnap.toObjects(VolunteerHours::class.java)
                    .filter { it.id != hoursRecord.id } + hoursRecord.copy(
                    verificationStatus = VerificationStatus.APPROVED.name,
                    submittedHours = finalHours
                )
                val statsSnap = db.collection("volunteer_stats").document(hoursRecord.userId).get().await()
                val currentStats = statsSnap.toObject(VolunteerStats::class.java)
                val completedPv = currentStats?.completedPvCases ?: 0L
                val gamification = FairAllocationEngine.computeGamification(existingList, completedPv, currentStats)

                if (statsSnap.exists()) {
                    batch.update(
                        db.collection("volunteer_stats").document(hoursRecord.userId),
                        mapOf(
                            "verifiedHours" to gamification.totalVerifiedHours,
                            "completedActivities" to gamification.completedActivities,
                            "points" to gamification.points,
                            "consistencyScore" to gamification.consistencyScore,
                            "tier" to gamification.tier.name,
                            "badges" to gamification.badges,
                            "lastVerifiedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // Mark assignment verified too if present
                if (hoursRecord.assignmentId.isNotBlank()) {
                    val assignSnap = db.collection("assignments").document(hoursRecord.assignmentId).get().await()
                    if (assignSnap.exists() && assignSnap.getString("status") != AssignmentStatus.VERIFIED.name) {
                        batch.update(
                            db.collection("assignments").document(hoursRecord.assignmentId),
                            mapOf(
                                "status" to AssignmentStatus.VERIFIED.name,
                                "verifiedBy" to uid,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }

                val emailMode = if (emailConfigured) "CONFIGURED_PROVIDER" else "DEMO_PREVIEW"
                val thankYouMsg = FairAllocationEngine.buildThankYouEmailBody(
                    volunteerName = hoursRecord.volunteerName,
                    activityTitle = hoursRecord.opportunityTitle,
                    verifiedHours = finalHours
                )
                batch.set(
                    db.collection("notifications").document(),
                    mapOf(
                        "userId" to hoursRecord.userId,
                        "title" to "Thank You! ${finalHours}h Verified",
                        "message" to thankYouMsg,
                        "type" to "THANK_YOU",
                        "emailMode" to emailMode,
                        "readFlag" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    db.collection("notifications").document(),
                    mapOf(
                        "userId" to hoursRecord.userId,
                        "title" to "Hours Submission ${decision.label}",
                        "message" to "Hours for '${hoursRecord.opportunityTitle}' were marked ${decision.label}. Note: ${correctionNote.ifBlank { "Please verify timings" }}",
                        "type" to "HOURS_APPROVED",
                        "emailMode" to "DEMO_PREVIEW",
                        "readFlag" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.set(
                db.collection("audit_logs").document(),
                mapOf(
                    "actorUid" to uid,
                    "actorName" to actorName,
                    "actorRole" to actorRole,
                    "action" to "HOURS_${decision.name}",
                    "targetType" to "volunteer_hours",
                    "targetId" to hoursRecord.id,
                    "details" to "${decision.name} ${finalHours}h for ${hoursRecord.volunteerName} (${hoursRecord.opportunityTitle})",
                    "reason" to correctionNote.trim(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addDistrict(name: String, code: String, actorName: String, actorRole: String): Result<Unit> {
        val id = "dist_${code.trim().lowercase().replace(Regex("[^a-z0-9]"), "")}"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            batch.set(
                db.collection("districts").document(id),
                mapOf(
                    "name" to name.trim(),
                    "code" to code.trim().uppercase(),
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("audit_logs").document(),
                mapOf(
                    "actorUid" to uid,
                    "actorName" to actorName,
                    "actorRole" to actorRole,
                    "action" to "DISTRICT_ADDED",
                    "targetType" to "districts",
                    "targetId" to id,
                    "details" to "Added district $name ($code)",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "districts/$id")
            Result.failure(e)
        }
    }

    suspend fun addEmployerOrganization(
        name: String,
        industry: String,
        contactPerson: String,
        actorName: String,
        actorRole: String
    ): Result<Unit> {
        val docRef = db.collection("organizations").document()
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            batch.set(
                docRef,
                mapOf(
                    "name" to name.trim(),
                    "industry" to industry.trim(),
                    "contactPerson" to contactPerson.trim(),
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("audit_logs").document(),
                mapOf(
                    "actorUid" to uid,
                    "actorName" to actorName,
                    "actorRole" to actorRole,
                    "action" to "ORGANIZATION_ADDED",
                    "targetType" to "organizations",
                    "targetId" to docRef.id,
                    "details" to "Added employer organization $name",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "organizations")
            Result.failure(e)
        }
    }

    suspend fun updateAppSettings(
        settings: AppSettings,
        actorName: String,
        actorRole: String
    ): Result<Unit> {
        val path = "app_settings/global"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            batch.set(
                db.collection("app_settings").document("global"),
                mapOf(
                    "maxActivePvCases" to settings.maxActivePvCases.coerceIn(1L, 20L),
                    "activeThresholdDays" to settings.activeThresholdDays.coerceIn(1L, 365L),
                    "lessActiveThresholdDays" to settings.lessActiveThresholdDays.coerceIn(1L, 365L),
                    "inactiveThresholdDays" to settings.inactiveThresholdDays.coerceIn(1L, 730L),
                    "emailProviderConfigured" to settings.emailProviderConfigured,
                    "emailSenderName" to settings.emailSenderName,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("audit_logs").document(),
                mapOf(
                    "actorUid" to uid,
                    "actorName" to actorName,
                    "actorRole" to actorRole,
                    "action" to "SETTINGS_UPDATED",
                    "targetType" to "app_settings",
                    "targetId" to "global",
                    "details" to "Updated maxActivePvCases=${settings.maxActivePvCases}, thresholds=${settings.activeThresholdDays}/${settings.lessActiveThresholdDays}/${settings.inactiveThresholdDays}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("notifications").document(notifId)
                .update(mapOf("readFlag" to true))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$notifId")
            Result.failure(e)
        }
    }

    suspend fun promoteUserRole(
        targetUserId: String,
        targetName: String,
        targetEmail: String,
        newRole: UserRole,
        actorName: String
    ): Result<Unit> {
        val path = "profiles/$targetUserId"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            batch.update(
                db.collection("profiles").document(targetUserId),
                mapOf(
                    "role" to newRole.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (newRole == UserRole.OFFICE_TEAM) {
                batch.set(
                    db.collection("office_team").document(targetUserId),
                    mapOf(
                        "userId" to targetUserId,
                        "displayName" to targetName,
                        "email" to targetEmail.ifBlank { "$targetUserId@maatram.org" },
                        "department" to "Volunteer Coordination",
                        "canApproveHours" to true,
                        "canAllocatePv" to true,
                        "canManageEvents" to true,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else if (newRole == UserRole.ADMIN) {
                batch.set(
                    db.collection("admins").document(targetUserId),
                    mapOf(
                        "userId" to targetUserId,
                        "email" to targetEmail.ifBlank { "$targetUserId@maatram.org" },
                        "grantedBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                db.collection("audit_logs").document(),
                mapOf(
                    "actorUid" to uid,
                    "actorName" to actorName,
                    "actorRole" to "ADMIN",
                    "action" to "ROLE_UPDATED",
                    "targetType" to "profiles",
                    "targetId" to targetUserId,
                    "details" to "Updated role of $targetName to ${newRole.name}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun seedDemoWorkspace(actorName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            MaatramSeedData.seedDemoWorkspace(db, uid, actorName)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seed_demo")
            Result.failure(e)
        }
    }
}
