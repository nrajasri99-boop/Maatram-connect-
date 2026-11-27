package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val label: String) {
    VOLUNTEER("Volunteer"),
    OFFICE_TEAM("Office Team"),
    ADMIN("Admin");

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.name == raw } ?: VOLUNTEER
    }
}

enum class VolunteerType(val label: String) {
    STUDENT("Student"),
    ALUMNI("Alumni"),
    SUPPORTER("Supporter");

    companion object {
        fun fromRaw(raw: String?): VolunteerType =
            entries.firstOrNull { it.name == raw } ?: STUDENT
    }
}

enum class OpportunityCategory(val label: String, val shortLabel: String) {
    CALLING_BGV("Calling & BGV", "Calling"),
    PHYSICAL_VERIFICATION("Physical Verification", "Physical PV"),
    INTERVIEWS("Mock & Panel Interviews", "Interviews"),
    EVENTS("Events & Coordination", "Events");

    companion object {
        fun fromRaw(raw: String?): OpportunityCategory =
            entries.firstOrNull { it.name == raw } ?: EVENTS
    }
}

enum class OpportunityMode(val label: String) {
    ONLINE("Online"),
    OFFLINE("Offline");

    companion object {
        fun fromRaw(raw: String?): OpportunityMode =
            entries.firstOrNull { it.name == raw } ?: OFFLINE
    }
}

enum class OpportunityStatus(val label: String) {
    OPEN("Open"),
    CLOSED("Closed"),
    COMPLETED("Completed");

    companion object {
        fun fromRaw(raw: String?): OpportunityStatus =
            entries.firstOrNull { it.name == raw } ?: OPEN
    }
}

enum class RequestStatus(val label: String) {
    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    WAITLISTED("Waitlisted"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String?): RequestStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class AssignmentStatus(val label: String) {
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    VERIFIED("Verified");

    companion object {
        fun fromRaw(raw: String?): AssignmentStatus =
            entries.firstOrNull { it.name == raw } ?: ASSIGNED
    }
}

enum class PvCaseStatus(val label: String) {
    UNASSIGNED("Unassigned"),
    ASSIGNED("Assigned"),
    VISITED("Visited"),
    COMPLETED("Completed"),
    VERIFIED("Verified");

    companion object {
        fun fromRaw(raw: String?): PvCaseStatus =
            entries.firstOrNull { it.name == raw } ?: UNASSIGNED
    }
}

enum class CallStatus(val label: String) {
    NOT_STARTED("Not Started"),
    CALLED("Called"),
    NOT_REACHABLE("Not Reachable"),
    CALL_BACK_REQUIRED("Call Back Required"),
    DETAILS_VERIFIED("Details Verified"),
    DETAILS_NOT_VERIFIED("Details Not Verified"),
    COMPLETED("Completed");

    companion object {
        fun fromRaw(raw: String?): CallStatus =
            entries.firstOrNull { it.name == raw } ?: NOT_STARTED
    }
}

enum class VerificationStatus(val label: String) {
    PENDING("Pending Review"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    companion object {
        fun fromRaw(raw: String?): VerificationStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class RecognitionTier(val label: String, val minPoints: Int) {
    BRONZE("Bronze", 0),
    SILVER("Silver", 150),
    GOLD("Gold", 400),
    PLATINUM("Platinum", 800);

    companion object {
        fun fromRaw(raw: String?): RecognitionTier =
            entries.firstOrNull { it.name == raw } ?: BRONZE

        fun forPoints(points: Int): RecognitionTier = when {
            points >= PLATINUM.minPoints -> PLATINUM
            points >= GOLD.minPoints -> GOLD
            points >= SILVER.minPoints -> SILVER
            else -> BRONZE
        }
    }
}

enum class EngagementStatus(val label: String) {
    ACTIVE("Active"),
    LESS_ACTIVE("Less Active"),
    INACTIVE("Inactive"),
    NO_ACTIVITY("No Activity Recorded"),
    UNAVAILABLE("Not Available"),
    AVAILABLE_UNASSIGNED("Available (No Assignment)")
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val volunteerCode: String = "",
    val volunteerType: String = VolunteerType.STUDENT.name,
    val college: String = "",
    val department: String = "",
    val yearOfStudy: String = "",
    val district: String = "Tiruchirappalli",
    val city: String = "",
    val skills: String = "",
    val interests: String = "",
    val availability: String = "Weekends",
    val preferredContact: String = "WhatsApp",
    val employerOrgId: String = "",
    val employerOrgName: String = "",
    val consentDataProcessing: Boolean = false,
    val consentLinkedInPublic: Boolean = false,
    val profileCompleted: Boolean = false,
    val role: String = UserRole.VOLUNTEER.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "fullName" to fullName,
        "email" to email,
        "phone" to phone,
        "volunteerCode" to volunteerCode,
        "volunteerType" to volunteerType,
        "college" to college,
        "department" to department,
        "yearOfStudy" to yearOfStudy,
        "district" to district,
        "city" to city,
        "skills" to skills,
        "interests" to interests,
        "availability" to availability,
        "preferredContact" to preferredContact,
        "employerOrgId" to employerOrgId,
        "employerOrgName" to employerOrgName,
        "consentDataProcessing" to consentDataProcessing,
        "consentLinkedInPublic" to consentLinkedInPublic,
        "profileCompleted" to profileCompleted,
        "role" to role,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class VolunteerStats(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val volunteerCode: String = "",
    val volunteerType: String = VolunteerType.STUDENT.name,
    val district: String = "Tiruchirappalli",
    val employerOrgName: String = "",
    val availability: String = "Weekends",
    val availableForPv: Boolean = true,
    val verifiedHours: Double = 0.0,
    val completedActivities: Long = 0L,
    val completedPvCases: Long = 0L,
    val activePvCases: Long = 0L,
    val points: Long = 0L,
    val consistencyScore: Long = 0L,
    val tier: String = RecognitionTier.BRONZE.name,
    val badges: List<String> = emptyList(),
    @ServerTimestamp val lastVerifiedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OfficeTeamMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val department: String = "",
    val canApproveHours: Boolean = true,
    val canAllocatePv: Boolean = true,
    val canManageEvents: Boolean = true,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "",
    val maxActivePvCases: Long = 3L,
    val activeThresholdDays: Long = 30L,
    val lessActiveThresholdDays: Long = 60L,
    val inactiveThresholdDays: Long = 90L,
    val emailProviderConfigured: Boolean = false,
    val emailSenderName: String = "Maatram Foundation",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DistrictItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val code: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EmployerOrganization(
    @DocumentId val id: String = "",
    val name: String = "",
    val industry: String = "",
    val contactPerson: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Opportunity(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = OpportunityCategory.EVENTS.name,
    val activityType: String = "",
    val description: String = "",
    val district: String = "Tiruchirappalli",
    val location: String = "",
    val mode: String = OpportunityMode.OFFLINE.name,
    val eventDate: String = "",
    val startTime: String = "09:30",
    val endTime: String = "16:30",
    val deadline: String = "",
    val requiredVolunteers: Long = 1L,
    val availableSlots: Long = 1L,
    val totalCases: Long = 0L,
    val skillsRequired: String = "",
    val eligibility: String = "All Active Volunteers",
    val instructions: String = "",
    val whatsappGroupLink: String = "",
    val status: String = OpportunityStatus.OPEN.name,
    val multiDayCount: Long = 1L,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VolunteerRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val volunteerName: String = "",
    val volunteerCode: String = "",
    val opportunityId: String = "",
    val opportunityTitle: String = "",
    val category: String = "",
    val district: String = "",
    val status: String = RequestStatus.PENDING.name,
    val rejectionReason: String = "",
    val availabilityNote: String = "",
    val reviewedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Assignment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val volunteerName: String = "",
    val volunteerCode: String = "",
    val opportunityId: String = "",
    val opportunityTitle: String = "",
    val category: String = "",
    val district: String = "",
    val assignedCasesCount: Long = 0L,
    val caseIds: List<String> = emptyList(),
    val status: String = AssignmentStatus.ASSIGNED.name,
    val progressNotes: String = "",
    val whatsappGroupLink: String = "",
    val overrideReason: String = "",
    val assignedBy: String = "",
    val verifiedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PhysicalVerificationCase(
    @DocumentId val id: String = "",
    val caseRefId: String = "",
    val opportunityId: String = "",
    val district: String = "",
    val areaLabel: String = "",
    val applicantCode: String = "",
    val assignedToUid: String = "",
    val assignedToName: String = "",
    val status: String = PvCaseStatus.UNASSIGNED.name,
    val visitNotes: String = "",
    val recommendationSummary: String = "",
    val verifiedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallingTask(
    @DocumentId val id: String = "",
    val applicantRefId: String = "",
    val opportunityId: String = "",
    val district: String = "",
    val callingDate: String = "",
    val assignedToUid: String = "",
    val assignedToName: String = "",
    val callStatus: String = CallStatus.NOT_STARTED.name,
    val followUpRequired: Boolean = false,
    val notes: String = "",
    val verifiedByOffice: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VolunteerHours(
    @DocumentId val id: String = "",
    val userId: String = "",
    val volunteerName: String = "",
    val volunteerCode: String = "",
    val assignmentId: String = "",
    val opportunityId: String = "",
    val opportunityTitle: String = "",
    val category: String = "",
    val district: String = "",
    val employerOrgName: String = "",
    val activityDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val breakMinutes: Long = 0L,
    val calculatedHours: Double = 0.0,
    val submittedHours: Double = 0.0,
    val dayNumber: Long = 1L,
    val notes: String = "",
    val verificationStatus: String = VerificationStatus.PENDING.name,
    val reviewerUid: String = "",
    val reviewerName: String = "",
    val correctionNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "SYSTEM",
    val emailMode: String = "DEMO_PREVIEW",
    val readFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorName: String = "",
    val actorRole: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val details: String = "",
    val reason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class FairAllocationCandidate(
    val stats: VolunteerStats,
    val hasRequested: Boolean,
    val priorityScore: Int,
    val priorityLabel: String,
    val recommendedAction: String,
    val blockedByActiveBatch: Boolean,
    val atMaxLimit: Boolean
)

data class EngagementRecord(
    val stats: VolunteerStats,
    val status: EngagementStatus,
    val daysSinceLastVerified: Int?,
    val pendingRequestCount: Int,
    val activeAssignmentCount: Int
)
