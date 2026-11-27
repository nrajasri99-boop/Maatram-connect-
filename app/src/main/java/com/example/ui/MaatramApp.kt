package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.firebase.authStateFlow
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
import com.example.data.model.RequestStatus
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.VerificationStatus
import com.example.data.model.VolunteerHours
import com.example.data.model.VolunteerRequest
import com.example.data.model.VolunteerStats
import com.example.data.repository.MaatramRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.ensureFirebaseAuth
import com.example.ui.auth.signOut
import com.example.ui.preview.PreviewWorkspaceState
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.OfficeDashboardAndRequestsScreen
import com.example.ui.screens.OfficeFairAllocationAndVerificationScreen
import com.example.ui.screens.ProfileOnboardingScreen
import com.example.ui.screens.VolunteerDashboardScreen
import com.example.ui.screens.VolunteerImpactScreen
import com.example.ui.screens.VolunteerOpportunitiesScreen
import com.example.ui.screens.VolunteerWorkScreen
import com.example.ui.theme.MaatramBlue
import com.example.ui.theme.MaatramBlueContainer
import com.example.ui.theme.MaatramNavy
import com.example.ui.viewmodel.MaatramViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private enum class AppTab(val label: String, val icon: ImageVector) {
    VOL_DASHBOARD("Home", Icons.Filled.Dashboard),
    VOL_OPPORTUNITIES("Works", Icons.Filled.Explore),
    VOL_WORK("My Work", Icons.AutoMirrored.Filled.Assignment),
    VOL_IMPACT("Impact", Icons.Filled.EmojiEvents),
    VOL_PROFILE("Profile", Icons.Filled.Person),
    OFFICE_HUB("Office", Icons.Filled.BusinessCenter),
    OFFICE_FAIR_ALLOC("Allocate", Icons.Filled.Balance),
    ADMIN_HUB("Admin", Icons.Filled.AdminPanelSettings)
}

private data class PreviewConfig(
    val name: String,
    val email: String,
    val district: String,
    val role: UserRole
)

@Composable
fun MaatramAppContent() {
    val context = LocalContext.current
    val auth = remember(context) { ensureFirebaseAuth(context) }

    var previewConfig by remember { mutableStateOf<PreviewConfig?>(null) }

    if (auth == null) {
        val activePreview = previewConfig
        if (activePreview != null) {
            MaatramPreviewShell(
                config = activePreview,
                onSignOutToLogin = { previewConfig = null }
            )
        } else {
            AuthScreen(
                onAuthSuccess = {},
                onEmailPasswordSignIn = { name, email, district, role ->
                    previewConfig = PreviewConfig(name, email, district, role)
                }
            )
        }
        return
    }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        val activePreview = previewConfig
        if (activePreview != null) {
            MaatramPreviewShell(
                config = activePreview,
                onSignOutToLogin = { previewConfig = null }
            )
        } else {
            AuthScreen(
                onAuthSuccess = {},
                onEmailPasswordSignIn = { name, email, district, role ->
                    previewConfig = PreviewConfig(name, email, district, role)
                }
            )
        }
    } else {
        key(user.uid) {
            MaatramSignedInShell(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: "Maatram Member",
                isEmailVerified = user.isEmailVerified
            )
        }
    }
}

@Composable
private fun MaatramPreviewShell(
    config: PreviewConfig,
    onSignOutToLogin: () -> Unit
) {
    val state = remember(config) {
        PreviewWorkspaceState(
            initialName = config.name,
            initialEmail = config.email,
            initialDistrict = config.district,
            initialRole = config.role
        )
    }
    BackHandler(onBack = onSignOutToLogin)

    MaatramWorkspaceShell(
        uid = state.uid,
        email = state.email,
        displayName = state.myProfile.fullName,
        profileState = UiState.Success(state.myProfile),
        actualRole = config.role,
        activeRole = state.activeRole,
        settings = state.settings,
        allStats = state.allStats,
        districts = state.districts,
        organizations = state.organizations,
        opportunities = state.opportunities,
        requests = state.requests,
        assignments = state.assignments,
        pvCases = state.pvCases,
        callingTasks = state.callingTasks,
        volunteerHours = state.volunteerHours,
        notifications = state.notifications,
        allProfiles = state.allProfiles,
        auditLogs = state.auditLogs,
        isBusy = false,
        actionMessage = state.actionMessage,
        isPreviewMode = false,
        onClearMessage = state::clearMessage,
        onSwitchRole = state::switchRole,
        onSaveProfile = { prof, _, availPv -> state.saveProfile(prof, availPv) },
        onTogglePvAvailability = state::setPvAvailability,
        onRequestJoin = state::requestToJoin,
        onCancelRequest = state::cancelRequest,
        onUpdateAssignment = state::updateAssignment,
        onUpdatePvCase = state::updatePvCase,
        onUpdateCallingTask = state::updateCallingTask,
        onSubmitHours = state::submitHours,
        onMarkNotificationRead = state::markNotificationRead,
        onCreateOpportunity = state::createOpportunity,
        onToggleOppStatus = state::toggleOpportunityStatus,
        onReviewRequest = state::reviewRequest,
        onAllocatePvCases = state::allocatePvCases,
        onVerifyPvCase = state::verifyPvCase,
        onVerifyAssignment = state::verifyAssignment,
        onImportCallingSheet = state::importDummyCallingSheet,
        onVerifyCallingTask = state::verifyCallingTask,
        onReviewHours = state::reviewHours,
        onAddDistrict = state::addDistrict,
        onAddOrganization = state::addOrganization,
        onUpdateSettings = state::updateSettings,
        onPromoteRole = state::promoteRole,
        onSeedDemoData = {},
        onSignOutOrSignIn = onSignOutToLogin
    )
}

@Composable
private fun MaatramSignedInShell(
    uid: String,
    email: String,
    displayName: String,
    isEmailVerified: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: MaatramViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MaatramViewModel(
                    repository = MaatramRepository(db),
                    currentUserId = uid,
                    currentUserEmail = email,
                    currentUserDisplayName = displayName,
                    isEmailVerified = isEmailVerified
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val actualRole by viewModel.actualRole.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val allStats by viewModel.allStats.collectAsStateWithLifecycle()
    val districts by viewModel.districts.collectAsStateWithLifecycle()
    val organizations by viewModel.organizations.collectAsStateWithLifecycle()
    val opportunitiesState by viewModel.opportunitiesState.collectAsStateWithLifecycle()
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val pvCases by viewModel.pvCases.collectAsStateWithLifecycle()
    val callingTasks by viewModel.callingTasks.collectAsStateWithLifecycle()
    val volunteerHours by viewModel.volunteerHours.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allProfilesForStaff.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val opportunities: List<Opportunity> = (opportunitiesState as? UiState.Success)?.data ?: emptyList()

    MaatramWorkspaceShell(
        uid = uid,
        email = email,
        displayName = displayName,
        profileState = profileState,
        actualRole = actualRole,
        activeRole = activeRole,
        settings = settings,
        allStats = allStats,
        districts = districts,
        organizations = organizations,
        opportunities = opportunities,
        requests = requests,
        assignments = assignments,
        pvCases = pvCases,
        callingTasks = callingTasks,
        volunteerHours = volunteerHours,
        notifications = notifications,
        allProfiles = allProfiles,
        auditLogs = auditLogs,
        isBusy = isBusy,
        actionMessage = actionMessage,
        isPreviewMode = false,
        onClearMessage = viewModel::clearMessage,
        onSwitchRole = viewModel::switchPreviewRole,
        onSaveProfile = viewModel::saveProfile,
        onTogglePvAvailability = viewModel::setPvAvailability,
        onRequestJoin = viewModel::requestToJoin,
        onCancelRequest = viewModel::cancelRequest,
        onUpdateAssignment = viewModel::updateAssignment,
        onUpdatePvCase = viewModel::updatePvCase,
        onUpdateCallingTask = viewModel::updateCallingTask,
        onSubmitHours = viewModel::submitHours,
        onMarkNotificationRead = viewModel::markNotificationRead,
        onCreateOpportunity = viewModel::createOpportunity,
        onToggleOppStatus = viewModel::toggleOpportunityStatus,
        onReviewRequest = viewModel::reviewRequest,
        onAllocatePvCases = viewModel::allocatePvCases,
        onVerifyPvCase = viewModel::verifyPvCase,
        onVerifyAssignment = viewModel::verifyAssignment,
        onImportCallingSheet = viewModel::importDummyCallingSheet,
        onVerifyCallingTask = viewModel::verifyCallingTask,
        onReviewHours = viewModel::reviewHours,
        onAddDistrict = viewModel::addDistrict,
        onAddOrganization = viewModel::addOrganization,
        onUpdateSettings = viewModel::updateSettings,
        onPromoteRole = viewModel::promoteRole,
        onSeedDemoData = viewModel::seedDemoData,
        onSignOutOrSignIn = { signOut(credentialManager, scope) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaatramWorkspaceShell(
    uid: String,
    email: String,
    displayName: String,
    profileState: UiState<UserProfile?>,
    actualRole: UserRole,
    activeRole: UserRole,
    settings: AppSettings,
    allStats: List<VolunteerStats>,
    districts: List<DistrictItem>,
    organizations: List<EmployerOrganization>,
    opportunities: List<Opportunity>,
    requests: List<VolunteerRequest>,
    assignments: List<Assignment>,
    pvCases: List<PhysicalVerificationCase>,
    callingTasks: List<CallingTask>,
    volunteerHours: List<VolunteerHours>,
    notifications: List<AppNotification>,
    allProfiles: List<UserProfile>,
    auditLogs: List<AuditLog>,
    isBusy: Boolean,
    actionMessage: String?,
    isPreviewMode: Boolean,
    onClearMessage: () -> Unit,
    onSwitchRole: (UserRole) -> Unit,
    onSaveProfile: (UserProfile, Boolean, Boolean) -> Unit,
    onTogglePvAvailability: (Boolean) -> Unit,
    onRequestJoin: (Opportunity, String) -> Unit,
    onCancelRequest: (VolunteerRequest) -> Unit,
    onUpdateAssignment: (String, AssignmentStatus, String) -> Unit,
    onUpdatePvCase: (String, PvCaseStatus, String, String) -> Unit,
    onUpdateCallingTask: (String, CallStatus, Boolean, String) -> Unit,
    onSubmitHours: (Assignment, String, String, String, Int, Double?, Int, String) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onCreateOpportunity: (Opportunity, Int) -> Unit,
    onToggleOppStatus: (Opportunity, OpportunityStatus) -> Unit,
    onReviewRequest: (VolunteerRequest, Opportunity, RequestStatus, String) -> Unit,
    onAllocatePvCases: (Opportunity, VolunteerStats, List<PhysicalVerificationCase>, String) -> Unit,
    onVerifyPvCase: (PhysicalVerificationCase) -> Unit,
    onVerifyAssignment: (Assignment) -> Unit,
    onImportCallingSheet: (Opportunity, VolunteerStats) -> Unit,
    onVerifyCallingTask: (String) -> Unit,
    onReviewHours: (VolunteerHours, VerificationStatus, Double, String) -> Unit,
    onAddDistrict: (String, String) -> Unit,
    onAddOrganization: (String, String, String) -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onPromoteRole: (UserProfile, UserRole) -> Unit,
    onSeedDemoData: () -> Unit,
    onSignOutOrSignIn: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onClearMessage()
        }
    }

    val myProfile = (profileState as? UiState.Success)?.data
    val myStats = allStats.firstOrNull { it.userId == uid }
    val myRequests = requests.filter { it.userId == uid }
    val myAssignments = assignments.filter { it.userId == uid }
    val myPvCases = pvCases.filter { it.assignedToUid == uid }
    val myCallingTasks = callingTasks.filter { it.assignedToUid == uid }
    val myHours = volunteerHours.filter { it.userId == uid }
    val unreadCount = notifications.count { !it.readFlag }

    val availableTabs = remember(activeRole) {
        when (activeRole) {
            UserRole.VOLUNTEER -> listOf(
                AppTab.VOL_DASHBOARD, AppTab.VOL_OPPORTUNITIES, AppTab.VOL_WORK, AppTab.VOL_IMPACT, AppTab.VOL_PROFILE
            )
            UserRole.OFFICE_TEAM -> listOf(
                AppTab.OFFICE_HUB, AppTab.OFFICE_FAIR_ALLOC, AppTab.VOL_OPPORTUNITIES, AppTab.VOL_PROFILE
            )
            UserRole.ADMIN -> listOf(
                AppTab.ADMIN_HUB, AppTab.OFFICE_HUB, AppTab.OFFICE_FAIR_ALLOC, AppTab.VOL_DASHBOARD, AppTab.VOL_PROFILE
            )
        }
    }

    var currentTab by remember(activeRole) { mutableStateOf(availableTabs.first()) }
    var roleMenuExpanded by remember { mutableStateOf(false) }

    if (currentTab != availableTabs.first()) {
        BackHandler { currentTab = availableTabs.first() }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaatramBlue,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("MC", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Column {
                            Text(
                                text = "Maatram Connect",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (activeRole == UserRole.VOLUNTEER) "Student Portal" else "${activeRole.label} Portal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (actualRole == UserRole.ADMIN || actualRole == UserRole.OFFICE_TEAM) {
                        Box {
                            TextButton(
                                onClick = { roleMenuExpanded = true },
                                modifier = Modifier.testTag("role_switcher_btn")
                            ) {
                                Surface(color = MaatramBlueContainer, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = activeRole.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaatramNavy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = roleMenuExpanded,
                                onDismissRequest = { roleMenuExpanded = false }
                            ) {
                                val roles = if (actualRole == UserRole.ADMIN) UserRole.entries
                                else listOf(UserRole.OFFICE_TEAM, UserRole.VOLUNTEER)
                                roles.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text("${r.label} View") },
                                        onClick = {
                                            onSwitchRole(r)
                                            roleMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = onSignOutOrSignIn,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (myProfile != null) {
                BoxWithConstraints {
                    if (maxWidth < 600.dp) {
                        NavigationBar(windowInsets = WindowInsets.navigationBars) {
                            availableTabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        if (tab == AppTab.VOL_IMPACT && unreadCount > 0) {
                                            BadgedBox(badge = { Badge { Text("$unreadCount") } }) {
                                                Icon(tab.icon, contentDescription = tab.label)
                                            }
                                        } else {
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            val useRail = maxWidth >= 600.dp && myProfile != null
            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail) {
                    NavigationRail {
                        availableTabs.forEach { tab ->
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = {
                                    if (tab == AppTab.VOL_IMPACT && unreadCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$unreadCount") } }) {
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (profileState) {
                        is UiState.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(color = MaatramBlue)
                                    Text("Loading your Maatram Connect workspace...")
                                }
                            }
                        }
                        is UiState.Error -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(profileState.message, color = MaterialTheme.colorScheme.error)
                                TextButton(onClick = onSignOutOrSignIn) {
                                    Text("Sign Out & Retry")
                                }
                            }
                        }
                        is UiState.Success -> {
                            val profile = profileState.data
                            if (profile == null) {
                                ProfileOnboardingScreen(
                                    uid = uid,
                                    email = email,
                                    defaultName = displayName,
                                    existingProfile = null,
                                    existingStats = null,
                                    districts = districts,
                                    organizations = organizations,
                                    isBusy = isBusy,
                                    onSaveProfile = onSaveProfile
                                )
                            } else {
                                when (currentTab) {
                                    AppTab.VOL_DASHBOARD -> VolunteerDashboardScreen(
                                        profile = profile,
                                        myStats = myStats,
                                        allStats = allStats,
                                        opportunities = opportunities,
                                        myRequests = myRequests,
                                        myAssignments = myAssignments,
                                        myPvCases = myPvCases,
                                        callingTasks = callingTasks,
                                        myHours = myHours,
                                        notifications = notifications,
                                        onExploreOpportunities = { currentTab = AppTab.VOL_OPPORTUNITIES },
                                        onOpenWorkHub = { currentTab = AppTab.VOL_WORK },
                                        onOpenImpactHub = { currentTab = AppTab.VOL_IMPACT },
                                        onOpenProfile = { currentTab = AppTab.VOL_PROFILE }
                                    )
                                    AppTab.VOL_OPPORTUNITIES -> VolunteerOpportunitiesScreen(
                                        opportunities = opportunities,
                                        myRequests = myRequests,
                                        districts = districts,
                                        userDistrict = profile.district,
                                        currentUserId = uid,
                                        availableForPv = myStats?.availableForPv ?: true,
                                        myAssignments = myAssignments,
                                        pvCases = myPvCases,
                                        callingTasks = myCallingTasks,
                                        isBusy = isBusy,
                                        onTogglePvAvailability = onTogglePvAvailability,
                                        onRequestJoin = onRequestJoin,
                                        onCancelRequest = onCancelRequest,
                                        onUpdatePvCase = onUpdatePvCase,
                                        onUpdateCallingTask = onUpdateCallingTask
                                    )
                                    AppTab.VOL_WORK -> VolunteerWorkScreen(
                                        assignments = myAssignments,
                                        pvCases = myPvCases,
                                        callingTasks = myCallingTasks,
                                        isBusy = isBusy,
                                        onUpdateAssignment = onUpdateAssignment,
                                        onUpdatePvCase = onUpdatePvCase,
                                        onUpdateCallingTask = onUpdateCallingTask,
                                        onSubmitHours = onSubmitHours
                                    )
                                    AppTab.VOL_IMPACT -> VolunteerImpactScreen(
                                        profile = profile,
                                        myStats = myStats,
                                        allStats = allStats,
                                        myHours = myHours,
                                        notifications = notifications,
                                        onMarkNotificationRead = onMarkNotificationRead,
                                        onShowMessage = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )
                                    AppTab.VOL_PROFILE -> ProfileOnboardingScreen(
                                        uid = uid,
                                        email = email,
                                        defaultName = displayName,
                                        existingProfile = profile,
                                        existingStats = myStats,
                                        districts = districts,
                                        organizations = organizations,
                                        isBusy = isBusy,
                                        onSaveProfile = onSaveProfile
                                    )
                                    AppTab.OFFICE_HUB -> OfficeDashboardAndRequestsScreen(
                                        allStats = allStats,
                                        opportunities = opportunities,
                                        requests = requests,
                                        pvCases = pvCases,
                                        volunteerHours = volunteerHours,
                                        districts = districts,
                                        currentUserId = uid,
                                        isBusy = isBusy,
                                        onCreateOpportunity = onCreateOpportunity,
                                        onToggleOppStatus = onToggleOppStatus,
                                        onReviewRequest = onReviewRequest,
                                        onCastPollVote = { opp ->
                                            onRequestJoin(opp, "Office Team poll vote — Available")
                                        },
                                        onCancelPollVote = onCancelRequest,
                                        onOpenFairAllocation = { currentTab = AppTab.OFFICE_FAIR_ALLOC }
                                    )
                                    AppTab.OFFICE_FAIR_ALLOC -> OfficeFairAllocationAndVerificationScreen(
                                        settings = settings,
                                        allStats = allStats,
                                        opportunities = opportunities,
                                        requests = requests,
                                        assignments = assignments,
                                        pvCases = pvCases,
                                        callingTasks = callingTasks,
                                        volunteerHours = volunteerHours,
                                        isBusy = isBusy,
                                        onAllocatePvCases = onAllocatePvCases,
                                        onVerifyPvCase = onVerifyPvCase,
                                        onVerifyAssignment = onVerifyAssignment,
                                        onImportCallingSheet = onImportCallingSheet,
                                        onVerifyCallingTask = onVerifyCallingTask,
                                        onReviewHours = onReviewHours
                                    )
                                    AppTab.ADMIN_HUB -> AdminDashboardScreen(
                                        settings = settings,
                                        allStats = allStats,
                                        allProfiles = allProfiles,
                                        opportunities = opportunities,
                                        requests = requests,
                                        assignments = assignments,
                                        volunteerHours = volunteerHours,
                                        districts = districts,
                                        organizations = organizations,
                                        auditLogs = auditLogs,
                                        isBusy = isBusy,
                                        onAddDistrict = onAddDistrict,
                                        onAddOrganization = onAddOrganization,
                                        onUpdateSettings = onUpdateSettings,
                                        onPromoteRole = onPromoteRole,
                                        onSeedDemoData = onSeedDemoData
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
