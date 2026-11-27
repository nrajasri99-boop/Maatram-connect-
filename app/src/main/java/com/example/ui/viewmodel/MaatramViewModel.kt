package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.model.OpportunityStatus
import com.example.data.model.PhysicalVerificationCase
import com.example.data.model.PvCaseStatus
import com.example.data.model.RequestStatus
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.VerificationStatus
import com.example.data.model.VolunteerHours
import com.example.data.model.VolunteerRequest
import com.example.data.model.VolunteerStats
import com.example.data.repository.MaatramRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MaatramViewModel(
    private val repository: MaatramRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String,
    val isEmailVerified: Boolean
) : ViewModel() {

    private val _previewRole = MutableStateFlow<UserRole?>(null)
    val previewRole: StateFlow<UserRole?> = _previewRole.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private fun <T> Flow<T>.toUiState(tag: String, fallbackError: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w("MaatramVM-$tag", fallbackError, rootError)
                emit(UiState.Error(rootError.message ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private fun <T> Flow<T>.safeStateIn(initial: T, tag: String): StateFlow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w("MaatramVM-$tag", "Stream error", firestoreError ?: error)
            emit(initial)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = initial
        )

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeMyProfile(currentUserId)
            .toUiState("Profile", "Failed to load user profile")

    val officeTeamRecord: StateFlow<OfficeTeamMember?> =
        repository.observeMyOfficeTeamRecord(currentUserId)
            .safeStateIn(null, "OfficeRecord")

    private val initialBootstrappedAdmin =
        currentUserEmail.equals("nrajasri99@gmail.com", ignoreCase = true) && isEmailVerified

    private val initialRole = if (initialBootstrappedAdmin) UserRole.ADMIN else UserRole.VOLUNTEER

    val isActualAdmin: StateFlow<Boolean> = combine(profileState, officeTeamRecord) { pState, _ ->
        val profileRole = (pState as? UiState.Success)?.data?.role
        initialBootstrappedAdmin || profileRole == UserRole.ADMIN.name
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initialBootstrappedAdmin)

    val actualRole: StateFlow<UserRole> = combine(profileState, officeTeamRecord, isActualAdmin) { pState, offRec, admin ->
        when {
            admin -> UserRole.ADMIN
            offRec != null && offRec.active -> UserRole.OFFICE_TEAM
            (pState as? UiState.Success)?.data?.role == UserRole.OFFICE_TEAM.name -> UserRole.OFFICE_TEAM
            else -> UserRole.VOLUNTEER
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initialRole)

    val activeRole: StateFlow<UserRole> = combine(actualRole, previewRole) { actual, preview ->
        if (actual == UserRole.ADMIN && preview != null) preview
        else if (actual == UserRole.OFFICE_TEAM && preview == UserRole.VOLUNTEER) UserRole.VOLUNTEER
        else actual
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initialRole)

    private val isStaffBackend: StateFlow<Boolean> = actualRole
        .map { it == UserRole.ADMIN || it == UserRole.OFFICE_TEAM }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initialBootstrappedAdmin)

    val appSettings: StateFlow<AppSettings> =
        repository.observeAppSettings().safeStateIn(AppSettings(), "Settings")

    val allStats: StateFlow<List<VolunteerStats>> =
        repository.observeAllStats().safeStateIn(emptyList(), "Stats")

    val districts: StateFlow<List<DistrictItem>> =
        repository.observeDistricts().safeStateIn(emptyList(), "Districts")

    val organizations: StateFlow<List<EmployerOrganization>> =
        repository.observeOrganizations().safeStateIn(emptyList(), "Organizations")

    val opportunitiesState: StateFlow<UiState<List<Opportunity>>> =
        repository.observeOpportunities().toUiState("Opportunities", "Failed to load opportunities")

    val requests: StateFlow<List<VolunteerRequest>> = isStaffBackend
        .flatMapLatest { staff -> repository.observeRequests(currentUserId, staff) }
        .safeStateIn(emptyList(), "Requests")

    val assignments: StateFlow<List<Assignment>> = isStaffBackend
        .flatMapLatest { staff -> repository.observeAssignments(currentUserId, staff) }
        .safeStateIn(emptyList(), "Assignments")

    val pvCases: StateFlow<List<PhysicalVerificationCase>> = isStaffBackend
        .flatMapLatest { staff -> repository.observePvCases(currentUserId, staff) }
        .safeStateIn(emptyList(), "PvCases")

    val callingTasks: StateFlow<List<CallingTask>> = isStaffBackend
        .flatMapLatest { staff -> repository.observeCallingTasks(currentUserId, staff) }
        .safeStateIn(emptyList(), "CallingTasks")

    val volunteerHours: StateFlow<List<VolunteerHours>> = isStaffBackend
        .flatMapLatest { staff -> repository.observeVolunteerHours(currentUserId, staff) }
        .safeStateIn(emptyList(), "Hours")

    val notifications: StateFlow<List<AppNotification>> =
        repository.observeNotifications(currentUserId).safeStateIn(emptyList(), "Notifications")

    val allProfilesForStaff: StateFlow<List<UserProfile>> = isStaffBackend
        .flatMapLatest { staff -> if (staff) repository.observeAllProfilesForStaff() else flowOf(emptyList()) }
        .safeStateIn(emptyList(), "AllProfiles")

    val officeTeamMembers: StateFlow<List<OfficeTeamMember>> = isStaffBackend
        .flatMapLatest { staff -> if (staff) repository.observeOfficeTeamForStaff() else flowOf(emptyList()) }
        .safeStateIn(emptyList(), "OfficeMembers")

    val auditLogs: StateFlow<List<AuditLog>> = isStaffBackend
        .flatMapLatest { staff -> if (staff) repository.observeAuditLogsForStaff() else flowOf(emptyList()) }
        .safeStateIn(emptyList(), "AuditLogs")

    fun switchPreviewRole(role: UserRole) {
        _previewRole.value = role
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    private fun runAction(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = block()
            _isBusy.value = false
            _actionMessage.value = result.fold(
                onSuccess = { successMessage },
                onFailure = { it.localizedMessage ?: "Operation failed" }
            )
        }
    }

    fun saveProfile(profile: UserProfile, isNew: Boolean, availableForPv: Boolean) {
        runAction(if (isNew) "Welcome to Maatram Connect!" else "Profile updated successfully") {
            repository.saveUserProfile(profile, isNew, availableForPv)
        }
    }

    fun setPvAvailability(availableForPv: Boolean) {
        runAction(if (availableForPv) "Marked Available for Physical Verification" else "PV Availability turned off") {
            repository.updatePvAvailability(availableForPv)
        }
    }

    fun createOpportunity(opportunity: Opportunity, pvCasesCount: Int) {
        val name = (profileState.value as? UiState.Success)?.data?.fullName ?: currentUserDisplayName
        runAction("Opportunity published") {
            repository.createOpportunity(opportunity, pvCasesCount, name, actualRole.value.name)
        }
    }

    fun toggleOpportunityStatus(opp: Opportunity, newStatus: OpportunityStatus) {
        val name = (profileState.value as? UiState.Success)?.data?.fullName ?: currentUserDisplayName
        runAction("Opportunity marked ${newStatus.label}") {
            repository.updateOpportunityStatus(opp.id, newStatus, name, actualRole.value.name)
        }
    }

    fun requestToJoin(opportunity: Opportunity, availabilityNote: String) {
        val profile = (profileState.value as? UiState.Success)?.data
        if (profile == null) {
            _actionMessage.value = "Please complete your profile before requesting opportunities."
            return
        }
        runAction("Request submitted for ${opportunity.title}") {
            repository.submitVolunteerRequest(profile, opportunity, availabilityNote)
        }
    }

    fun cancelRequest(request: VolunteerRequest) {
        runAction("Request cancelled") {
            repository.cancelVolunteerRequest(request.id)
        }
    }

    fun reviewRequest(request: VolunteerRequest, opp: Opportunity, decision: RequestStatus, reason: String) {
        val name = (profileState.value as? UiState.Success)?.data?.fullName ?: currentUserDisplayName
        runAction("Request ${decision.label}") {
            repository.reviewVolunteerRequest(request, opp, decision, reason, name, actualRole.value.name)
        }
    }

    fun allocatePvCases(
        opportunity: Opportunity,
        candidate: VolunteerStats,
        cases: List<PhysicalVerificationCase>,
        overrideReason: String
    ) {
        val name = (profileState.value as? UiState.Success)?.data?.fullName ?: currentUserDisplayName
        val maxActive = appSettings.value.maxActivePvCases.toInt()
        runAction("Allocated ${cases.size} PV case(s) to ${candidate.displayName}") {
            repository.allocatePvCasesFairly(
                opportunity = opportunity,
                candidateStats = candidate,
                selectedCases = cases,
                overrideReason = overrideReason,
                maxActivePvCases = maxActive,
                actorName = name,
                actorRole = actualRole.value.name
            )
        }
    }

    fun updatePvCase(caseId: String, status: PvCaseStatus, visitNotes: String, summary: String) {
        runAction("PV Case updated to ${status.label}") {
            repository.updatePvCaseByVolunteer(caseId, status, visitNotes, summary)
        }
    }

    fun verifyPvCase(pvCase: PhysicalVerificationCase) {
        val name = (profileState.value as? UiState.Success)?.data?.fullName ?: currentUserDisplayName
        runAction("PV Case ${pvCase.caseRefId} verified") {
            repository.verifyPvCaseByOffice(pvCase, name, actualRole.value.name)
        }
    }

    fun updateAssignment(assignmentId: String, status: AssignmentStatus, notes: String) {
        runAction("Assignment marked ${status.label}") {
            repository.updateAssignmentProgress(assignmentId, status, notes)
        }
    }

    fun verifyAssignment(assignment: Assignment) {
        val name = (profileState.value as? UiState.Success)?.data?.fullName ?: currentUserDisplayName
        runAction("Assignment verified") {
            repository.verifyAssignmentByOffice(assignment, name, actualRole.value.name)
        }
    }

    fun createCallingTask(refId: String, oppId: String, district: String, date: String, vol: VolunteerStats) {
        runAction("Calling task $refId assigned to ${vol.displayName}") {
            repository.createCallingTask(refId, oppId, district, date, vol.userId, vol.displayName)
        }
    }

    fun importDummyCallingSheet(opp: Opportunity, vol: VolunteerStats) {
        runAction("Imported 5 demo calling tasks for ${vol.displayName}") {
            repository.importDummyCallingSheet(opp, vol, 5)
        }
    }

    fun updateCallingTask(taskId: String, status: CallStatus, followUp: Boolean, notes: String) {
        runAction("Call log saved (${status.label})") {
            repository.updateCallingTaskByVolunteer(taskId, status, followUp, notes)
        }
    }

    fun verifyCallingTask(taskId: String) {
        runAction("Calling record verified") {
            repository.verifyCallingTaskByOffice(taskId)
        }
    }

    fun submitHours(
        assignment: Assignment,
        date: String,
        startTime: String,
        endTime: String,
        breakMins: Int,
        manualHours: Double?,
        dayNumber: Int,
        notes: String
    ) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        runAction("Volunteer hours submitted for verification") {
            repository.submitVolunteerHours(profile, assignment, date, startTime, endTime, breakMins, manualHours, dayNumber, notes)
        }
    }

    fun reviewHours(
        record: VolunteerHours,
        decision: VerificationStatus,
        adjustedHours: Double,
        correctionNote: String
    ) {
        val name = (profileState.value as? UiState.Success)?.data?.fullName ?: currentUserDisplayName
        runAction(if (decision == VerificationStatus.APPROVED) "Hours verified & Thank-You sent" else "Hours marked ${decision.label}") {
            repository.reviewVolunteerHours(
                hoursRecord = record,
                decision = decision,
                adjustedHours = adjustedHours,
                correctionNote = correctionNote,
                actorName = name,
                actorRole = actualRole.value.name,
                emailConfigured = appSettings.value.emailProviderConfigured
            )
        }
    }

    fun addDistrict(name: String, code: String) {
        val actorName = (profileState.value as? UiState.Success)?.data?.fullName ?: currentUserDisplayName
        runAction("District $name added") {
            repository.addDistrict(name, code, actorName, actualRole.value.name)
        }
    }

    fun addOrganization(name: String, industry: String, contact: String) {
        val actorName = (profileState.value as? UiState.Success)?.data?.fullName ?: currentUserDisplayName
        runAction("Organization $name added") {
            repository.addEmployerOrganization(name, industry, contact, actorName, actualRole.value.name)
        }
    }

    fun updateSettings(settings: AppSettings) {
        val actorName = (profileState.value as? UiState.Success)?.data?.fullName ?: currentUserDisplayName
        runAction("System settings saved") {
            repository.updateAppSettings(settings, actorName, actualRole.value.name)
        }
    }

    fun promoteRole(targetProfile: UserProfile, newRole: UserRole) {
        val actorName = (profileState.value as? UiState.Success)?.data?.fullName ?: currentUserDisplayName
        runAction("Updated ${targetProfile.fullName} to ${newRole.label}") {
            repository.promoteUserRole(targetProfile.userId, targetProfile.fullName, targetProfile.email, newRole, actorName)
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notifId)
        }
    }

    fun seedDemoData() {
        val actorName = (profileState.value as? UiState.Success)?.data?.fullName ?: currentUserDisplayName
        runAction("Demo dataset seeded with 20 volunteers, 5 office coordinators, and 10 opportunities") {
            repository.seedDemoWorkspace(actorName)
        }
    }
}
