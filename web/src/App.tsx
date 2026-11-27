import React, { useEffect, useState } from 'react';
import { onAuthStateChanged, type User } from 'firebase/auth';
import { where, type Unsubscribe } from 'firebase/firestore';
import {
  Award,
  Briefcase,
  ClipboardCheck,
  Home,
  LayoutDashboard,
  LogOut,
  ShieldCheck,
  User as UserIcon,
  Vote,
} from 'lucide-react';
import {
  USER_ROLE_LABELS,
  type AppNotification,
  type AppSettings,
  type Assignment,
  type AuditLog,
  type CallingTask,
  type DistrictItem,
  type EmployerOrganization,
  type OfficeTeamMember,
  type Opportunity,
  type PhysicalVerificationCase,
  type UserProfile,
  type UserRole,
  type VolunteerHours,
  type VolunteerRequest,
  type VolunteerStats,
} from './types/maatram';
import {
  firebaseAuth,
  firestoreDb,
  signInWithGoogleWeb,
  signOutFirebaseWeb,
} from './services/firebase';
import {
  addDistrictFs,
  addEmployerOrgFs,
  allocatePvCasesFairlyFs,
  cancelVolunteerRequestFs,
  createOpportunityFs,
  importDummyCallingSheetFs,
  markNotificationReadFs,
  promoteUserRoleFs,
  reviewVolunteerHoursFs,
  reviewVolunteerRequestFs,
  saveUserProfileFs,
  seedDemoWorkspaceFs,
  submitVolunteerHoursFs,
  submitVolunteerRequestFs,
  subscribeAppSettings,
  subscribeCollection,
  subscribeMyProfile,
  subscribeOfficeTeamRecord,
  updateAppSettingsFs,
  updateAssignmentProgressFs,
  updateCallingTaskByVolunteerFs,
  updateOpportunityStatusFs,
  updatePvAvailabilityFs,
  updatePvCaseByVolunteerFs,
  verifyAssignmentByOfficeFs,
  verifyCallingTaskByOfficeFs,
  verifyPvCaseByOfficeFs,
} from './services/firestoreRepository';
import { calculateDurationHours, tierForPoints } from './domain/fairAllocationEngine';
import { createInitialDemoWorkspace, type DemoWorkspaceData } from './data/demoWorkspace';
import { AuthPage } from './components/AuthPage';
import { StudentDashboard } from './components/StudentDashboard';
import { VolunteerWorksPage } from './components/VolunteerWorksPage';
import { VolunteerWorkPage } from './components/VolunteerWorkPage';
import { VolunteerImpactPage } from './components/VolunteerImpactPage';
import { ProfileOnboardingPage } from './components/ProfileOnboardingPage';
import { OfficePortal } from './components/OfficePortal';
import { AdminPortal } from './components/AdminPortal';
import { ToastBanner } from './components/Common';

type NavDestination =
  | 'HOME'
  | 'WORKS'
  | 'MY_WORK'
  | 'IMPACT'
  | 'OFFICE_HOME'
  | 'OFFICE_FAIR_ALLOC'
  | 'ADMIN'
  | 'PROFILE';

const DEFAULT_SETTINGS: AppSettings = {
  id: 'global',
  maxActivePvCases: 3,
  activeThresholdDays: 30,
  lessActiveThresholdDays: 60,
  inactiveThresholdDays: 90,
  emailProviderConfigured: false,
  emailSenderName: 'Maatram Foundation',
  updatedBy: '',
};

const NAV_ITEMS_BY_ROLE: Record<UserRole, { key: NavDestination; label: string; icon: React.ReactNode }[]> = {
  VOLUNTEER: [
    { key: 'HOME', label: 'Home', icon: <Home className="h-4 w-4" /> },
    { key: 'WORKS', label: 'Works', icon: <Briefcase className="h-4 w-4" /> },
    { key: 'MY_WORK', label: 'My Work', icon: <ClipboardCheck className="h-4 w-4" /> },
    { key: 'IMPACT', label: 'Impact', icon: <Award className="h-4 w-4" /> },
    { key: 'PROFILE', label: 'Profile', icon: <UserIcon className="h-4 w-4" /> },
  ],
  OFFICE_TEAM: [
    { key: 'OFFICE_HOME', label: 'Polling', icon: <Vote className="h-4 w-4" /> },
    { key: 'OFFICE_FAIR_ALLOC', label: 'Fair PV & Verify', icon: <ClipboardCheck className="h-4 w-4" /> },
    { key: 'WORKS', label: 'Works', icon: <Briefcase className="h-4 w-4" /> },
    { key: 'ADMIN', label: 'Admin', icon: <ShieldCheck className="h-4 w-4" /> },
    { key: 'PROFILE', label: 'Profile', icon: <UserIcon className="h-4 w-4" /> },
  ],
  ADMIN: [
    { key: 'ADMIN', label: 'Analytics', icon: <LayoutDashboard className="h-4 w-4" /> },
    { key: 'OFFICE_HOME', label: 'Polling', icon: <Vote className="h-4 w-4" /> },
    { key: 'OFFICE_FAIR_ALLOC', label: 'Fair PV & Verify', icon: <ClipboardCheck className="h-4 w-4" /> },
    { key: 'HOME', label: 'Volunteer View', icon: <Home className="h-4 w-4" /> },
    { key: 'PROFILE', label: 'Profile', icon: <UserIcon className="h-4 w-4" /> },
  ],
};

function defaultDestForRole(role: UserRole): NavDestination {
  if (role === 'ADMIN') return 'ADMIN';
  if (role === 'OFFICE_TEAM') return 'OFFICE_HOME';
  return 'HOME';
}

export const App: React.FC = () => {
  const [firebaseUser, setFirebaseUser] = useState<User | null>(null);
  const [demoWorkspace, setDemoWorkspace] = useState<DemoWorkspaceData | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [isBusy, setIsBusy] = useState(false);

  // Live Firestore state
  const [myProfile, setMyProfile] = useState<UserProfile | null>(null);
  const [myOfficeRecord, setMyOfficeRecord] = useState<OfficeTeamMember | null>(null);
  const [settings, setSettings] = useState<AppSettings>(DEFAULT_SETTINGS);
  const [districts, setDistricts] = useState<DistrictItem[]>([]);
  const [organizations, setOrganizations] = useState<EmployerOrganization[]>([]);
  const [allStats, setAllStats] = useState<VolunteerStats[]>([]);
  const [opportunities, setOpportunities] = useState<Opportunity[]>([]);
  const [requests, setRequests] = useState<VolunteerRequest[]>([]);
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [pvCases, setPvCases] = useState<PhysicalVerificationCase[]>([]);
  const [callingTasks, setCallingTasks] = useState<CallingTask[]>([]);
  const [volunteerHours, setVolunteerHours] = useState<VolunteerHours[]>([]);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [allProfiles, setAllProfiles] = useState<UserProfile[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);

  const [activeRole, setActiveRole] = useState<UserRole>('VOLUNTEER');
  const [currentDest, setCurrentDest] = useState<NavDestination>('HOME');

  // Firebase Auth listener
  useEffect(() => {
    if (!firebaseAuth) return;
    const unsub = onAuthStateChanged(firebaseAuth, (user) => {
      setFirebaseUser(user);
      if (!user) {
        setMyProfile(null);
        setMyOfficeRecord(null);
      }
    });
    return unsub;
  }, []);

  const isBootstrappedAdmin =
    firebaseUser?.email === 'nrajasri99@gmail.com' && firebaseUser?.emailVerified === true;
  const resolvedRole: UserRole = isBootstrappedAdmin
    ? 'ADMIN'
    : myProfile?.role === 'ADMIN'
    ? 'ADMIN'
    : myProfile?.role === 'OFFICE_TEAM' || myOfficeRecord?.active
    ? 'OFFICE_TEAM'
    : 'VOLUNTEER';
  const isStaff = resolvedRole === 'ADMIN' || resolvedRole === 'OFFICE_TEAM';

  useEffect(() => {
    if (firebaseUser) {
      setActiveRole(resolvedRole);
      setCurrentDest(defaultDestForRole(resolvedRole));
    }
  }, [firebaseUser?.uid, resolvedRole]);

  // Core Firestore subscriptions when signed in with Firebase
  useEffect(() => {
    if (!firebaseUser || !firestoreDb) return;
    const db = firestoreDb;
    const uid = firebaseUser.uid;
    const onErr = (err: Error) => setToastMessage(err.message);

    const unsubs: Unsubscribe[] = [
      subscribeMyProfile(db, uid, setMyProfile, onErr),
      subscribeOfficeTeamRecord(db, uid, setMyOfficeRecord, onErr),
      subscribeAppSettings(db, setSettings, onErr),
      subscribeCollection<DistrictItem>(db, 'districts', [where('active', '==', true)], setDistricts, onErr),
      subscribeCollection<EmployerOrganization>(
        db,
        'organizations',
        [where('active', '==', true)],
        setOrganizations,
        onErr
      ),
      subscribeCollection<VolunteerStats>(db, 'volunteer_stats', [where('points', '>=', 0)], setAllStats, onErr),
      subscribeCollection<Opportunity>(
        db,
        'opportunities',
        [where('status', 'in', ['OPEN', 'CLOSED', 'COMPLETED'])],
        setOpportunities,
        onErr
      ),
      subscribeCollection<AppNotification>(
        db,
        'notifications',
        [where('userId', '==', uid)],
        setNotifications,
        onErr
      ),
    ];

    return () => unsubs.forEach((u) => u());
  }, [firebaseUser?.uid]);

  // Role-scoped Firestore subscriptions
  useEffect(() => {
    if (!firebaseUser || !firestoreDb) return;
    const db = firestoreDb;
    const uid = firebaseUser.uid;
    const onErr = (err: Error) => setToastMessage(err.message);

    const reqConstraints = isStaff ? [] : [where('userId', '==', uid)];
    const pvConstraints = isStaff ? [] : [where('assignedToUid', '==', uid)];
    const callConstraints = isStaff ? [] : [where('assignedToUid', '==', uid)];

    const unsubs: Unsubscribe[] = [
      subscribeCollection<VolunteerRequest>(db, 'volunteer_requests', reqConstraints, setRequests, onErr),
      subscribeCollection<Assignment>(db, 'assignments', reqConstraints, setAssignments, onErr),
      subscribeCollection<PhysicalVerificationCase>(db, 'pv_cases', pvConstraints, setPvCases, onErr),
      subscribeCollection<CallingTask>(db, 'calling_tasks', callConstraints, setCallingTasks, onErr),
      subscribeCollection<VolunteerHours>(db, 'volunteer_hours', reqConstraints, setVolunteerHours, onErr),
    ];

    if (isStaff) {
      unsubs.push(
        subscribeCollection<UserProfile>(db, 'profiles', [], setAllProfiles, onErr),
        subscribeCollection<AuditLog>(db, 'audit_logs', [], setAuditLogs, onErr)
      );
    }

    return () => unsubs.forEach((u) => u());
  }, [firebaseUser?.uid, isStaff]);

  const showToast = (msg: string) => setToastMessage(msg);

  const runLiveAction = async (fn: () => Promise<void>, successMsg?: string) => {
    setIsBusy(true);
    try {
      await fn();
      if (successMsg) showToast(successMsg);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Action failed.');
    } finally {
      setIsBusy(false);
    }
  };

  // Unauthenticated -> AuthPage
  if (!firebaseUser && !demoWorkspace) {
    return (
      <>
        <AuthPage
          onEmailPasswordSignIn={(name, email, district, role) => {
            const ws = createInitialDemoWorkspace(name, email, district, role);
            setDemoWorkspace(ws);
            setActiveRole(role);
            setCurrentDest(defaultDestForRole(role));
          }}
          onGoogleSignIn={signInWithGoogleWeb}
        />
        <ToastBanner message={toastMessage} onDismiss={() => setToastMessage(null)} />
      </>
    );
  }

  // Determine unified state source (Demo Workspace vs Live Firebase)
  const isDemo = Boolean(demoWorkspace && !firebaseUser);
  const currentUid = isDemo ? demoWorkspace!.uid : firebaseUser!.uid;
  const currentEmail = isDemo ? demoWorkspace!.myProfile.email : firebaseUser!.email || '';
  const currentProfile = isDemo ? demoWorkspace!.myProfile : myProfile;
  const currentSettings = isDemo ? demoWorkspace!.settings : settings;
  const currentDistricts = isDemo ? demoWorkspace!.districts : districts;
  const currentOrgs = isDemo ? demoWorkspace!.organizations : organizations;
  const currentAllStats = isDemo ? demoWorkspace!.allStats : allStats;
  const currentMyStats = currentAllStats.find((s) => s.userId === currentUid) || null;
  const currentOpps = isDemo ? demoWorkspace!.opportunities : opportunities;
  const currentRequests = isDemo ? demoWorkspace!.requests : requests;
  const currentAssignments = isDemo ? demoWorkspace!.assignments : assignments;
  const currentPvCases = isDemo ? demoWorkspace!.pvCases : pvCases;
  const currentCallingTasks = isDemo ? demoWorkspace!.callingTasks : callingTasks;
  const currentHours = isDemo ? demoWorkspace!.volunteerHours : volunteerHours;
  const currentNotifications = isDemo ? demoWorkspace!.notifications : notifications;
  const currentProfiles = isDemo ? demoWorkspace!.allProfiles : allProfiles;
  const currentAuditLogs = isDemo ? demoWorkspace!.auditLogs : auditLogs;

  const effectiveRole: UserRole = isDemo ? demoWorkspace!.activeRole : activeRole;
  const canSwitchRoles = isDemo
    ? demoWorkspace!.myProfile.role !== 'VOLUNTEER'
    : isStaff;

  const myRequests = currentRequests.filter((r) => r.userId === currentUid);
  const myAssignments = currentAssignments.filter((a) => a.userId === currentUid);
  const myPvCases = currentPvCases.filter((p) => p.assignedToUid === currentUid);
  const myCallingTasks = currentCallingTasks.filter((t) => t.assignedToUid === currentUid);
  const myHours = currentHours.filter((h) => h.userId === currentUid);

  // Onboarding check for live Firebase users without completed profile
  const needsOnboarding = !isDemo && (!myProfile || !myProfile.profileCompleted);

  const handleSignOut = async () => {
    if (isDemo) {
      setDemoWorkspace(null);
    } else {
      await signOutFirebaseWeb();
    }
    setCurrentDest('HOME');
  };

  const handleRoleSwitch = (nextRole: UserRole) => {
    if (isDemo && demoWorkspace) {
      setDemoWorkspace({ ...demoWorkspace, activeRole: nextRole });
    } else {
      setActiveRole(nextRole);
    }
    setCurrentDest(defaultDestForRole(nextRole));
  };

  const navItems = NAV_ITEMS_BY_ROLE[effectiveRole];

  const renderMainBody = () => {
    if (needsOnboarding && firebaseUser && firestoreDb) {
      return (
        <ProfileOnboardingPage
          uid={firebaseUser.uid}
          email={firebaseUser.email || ''}
          defaultName={firebaseUser.displayName || 'Maatram Volunteer'}
          existingProfile={myProfile}
          existingStats={currentMyStats}
          districts={currentDistricts}
          organizations={currentOrgs}
          isBusy={isBusy}
          onSaveProfile={(prof, isNew, availPv) =>
            runLiveAction(
              () =>
                saveUserProfileFs(
                  firestoreDb!,
                  firebaseUser.uid,
                  firebaseUser.email,
                  firebaseUser.emailVerified,
                  prof,
                  isNew,
                  availPv
                ),
              'Profile saved!'
            )
          }
        />
      );
    }

    switch (currentDest) {
      case 'HOME':
        return (
          <StudentDashboard
            profile={currentProfile}
            myStats={currentMyStats}
            allStats={currentAllStats}
            opportunities={currentOpps}
            myRequests={myRequests}
            myAssignments={myAssignments}
            myPvCases={myPvCases}
            callingTasks={currentCallingTasks}
            myHours={myHours}
            notifications={currentNotifications}
            onExploreWorks={() => setCurrentDest('WORKS')}
            onOpenWorkHub={() => setCurrentDest('MY_WORK')}
            onOpenImpactHub={() => setCurrentDest('IMPACT')}
            onOpenProfile={() => setCurrentDest('PROFILE')}
          />
        );

      case 'WORKS':
        return (
          <VolunteerWorksPage
            opportunities={currentOpps}
            myRequests={myRequests}
            districts={currentDistricts}
            userDistrict={currentProfile?.district || 'Tiruchirappalli'}
            currentUserId={currentUid}
            availableForPv={currentMyStats?.availableForPv ?? true}
            myAssignments={myAssignments}
            pvCases={currentPvCases}
            callingTasks={currentCallingTasks}
            isBusy={isBusy}
            onTogglePvAvailability={(avail) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  allStats: demoWorkspace.allStats.map((s) =>
                    s.userId === currentUid ? { ...s, availableForPv: avail } : s
                  ),
                });
                showToast(
                  avail
                    ? 'Marked Available for Physical Verification! Send a request below.'
                    : 'Physical Verification availability turned off.'
                );
              } else if (firestoreDb) {
                runLiveAction(
                  () => updatePvAvailabilityFs(firestoreDb!, currentUid, avail),
                  avail ? 'Marked Available for PV!' : 'PV availability turned off.'
                );
              }
            }}
            onRequestJoin={(opp, note) => {
              if (!currentProfile) return;
              if (isDemo && demoWorkspace) {
                const newReq: VolunteerRequest = {
                  id: `req_${Date.now()}`,
                  userId: currentUid,
                  volunteerName: currentProfile.fullName,
                  volunteerCode: currentProfile.volunteerCode,
                  opportunityId: opp.id,
                  opportunityTitle: opp.title,
                  category: opp.category,
                  district: opp.district,
                  status: 'PENDING',
                  rejectionReason: '',
                  availabilityNote: note,
                  reviewedBy: '',
                };
                setDemoWorkspace({
                  ...demoWorkspace,
                  requests: [
                    newReq,
                    ...demoWorkspace.requests.filter(
                      (r) => !(r.userId === currentUid && r.opportunityId === opp.id)
                    ),
                  ],
                });
                showToast(`Request submitted for ${opp.title}!`);
              } else if (firestoreDb) {
                runLiveAction(
                  () => submitVolunteerRequestFs(firestoreDb!, currentProfile, opp, note),
                  `Request submitted for ${opp.title}!`
                );
              }
            }}
            onCancelRequest={(req) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  requests: demoWorkspace.requests.map((r) =>
                    r.id === req.id ? { ...r, status: 'CANCELLED' } : r
                  ),
                });
                showToast('Request cancelled.');
              } else if (firestoreDb) {
                runLiveAction(
                  () => cancelVolunteerRequestFs(firestoreDb!, req.id),
                  'Request cancelled.'
                );
              }
            }}
            onUpdatePvCase={(caseId, status, notes, summary) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  pvCases: demoWorkspace.pvCases.map((p) =>
                    p.id === caseId
                      ? { ...p, status, visitNotes: notes, recommendationSummary: summary }
                      : p
                  ),
                });
                showToast(`PV case updated to ${status}.`);
              } else if (firestoreDb) {
                runLiveAction(
                  () => updatePvCaseByVolunteerFs(firestoreDb!, caseId, status, notes, summary),
                  'PV case updated!'
                );
              }
            }}
            onUpdateCallingTask={(taskId, status, followUp, notes) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  callingTasks: demoWorkspace.callingTasks.map((t) =>
                    t.id === taskId
                      ? { ...t, callStatus: status, followUpRequired: followUp, notes }
                      : t
                  ),
                });
                showToast('Call log saved.');
              } else if (firestoreDb) {
                runLiveAction(
                  () => updateCallingTaskByVolunteerFs(firestoreDb!, taskId, status, followUp, notes),
                  'Call log saved!'
                );
              }
            }}
          />
        );

      case 'MY_WORK':
        return (
          <VolunteerWorkPage
            assignments={myAssignments}
            pvCases={myPvCases}
            callingTasks={myCallingTasks}
            isBusy={isBusy}
            onUpdateAssignment={(id, status, notes) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  assignments: demoWorkspace.assignments.map((a) =>
                    a.id === id ? { ...a, status, progressNotes: notes } : a
                  ),
                });
                showToast('Assignment status updated.');
              } else if (firestoreDb) {
                runLiveAction(
                  () => updateAssignmentProgressFs(firestoreDb!, id, status, notes),
                  'Assignment status updated.'
                );
              }
            }}
            onUpdatePvCase={(id, status, notes, summary) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  pvCases: demoWorkspace.pvCases.map((p) =>
                    p.id === id
                      ? { ...p, status, visitNotes: notes, recommendationSummary: summary }
                      : p
                  ),
                });
                showToast('PV case updated.');
              } else if (firestoreDb) {
                runLiveAction(
                  () => updatePvCaseByVolunteerFs(firestoreDb!, id, status, notes, summary),
                  'PV case updated.'
                );
              }
            }}
            onUpdateCallingTask={(id, status, followUp, notes) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  callingTasks: demoWorkspace.callingTasks.map((t) =>
                    t.id === id ? { ...t, callStatus: status, followUpRequired: followUp, notes } : t
                  ),
                });
                showToast('Call log saved.');
              } else if (firestoreDb) {
                runLiveAction(
                  () => updateCallingTaskByVolunteerFs(firestoreDb!, id, status, followUp, notes),
                  'Call log saved.'
                );
              }
            }}
            onSubmitHours={(assign, date, start, end, breakMins, manualOverride, dayNum, notes) => {
              if (!currentProfile) return;
              if (isDemo && demoWorkspace) {
                const calc = calculateDurationHours(start, end, breakMins);
                const submitted = Math.max(0.25, Math.min(24, manualOverride ?? calc));
                const newEntry: VolunteerHours = {
                  id: `hr_${Date.now()}`,
                  userId: currentUid,
                  volunteerName: currentProfile.fullName,
                  volunteerCode: currentProfile.volunteerCode,
                  assignmentId: assign.id,
                  opportunityId: assign.opportunityId,
                  opportunityTitle: assign.opportunityTitle,
                  category: assign.category,
                  district: assign.district,
                  employerOrgName: currentProfile.employerOrgName,
                  activityDate: date,
                  startTime: start,
                  endTime: end,
                  breakMinutes: breakMins,
                  calculatedHours: calc,
                  submittedHours: submitted,
                  dayNumber: dayNum,
                  notes,
                  verificationStatus: 'PENDING',
                  reviewerUid: '',
                  reviewerName: '',
                  correctionNote: '',
                };
                setDemoWorkspace({
                  ...demoWorkspace,
                  volunteerHours: [newEntry, ...demoWorkspace.volunteerHours],
                });
                showToast(`Submitted ${submitted} hrs for verification.`);
              } else if (firestoreDb) {
                runLiveAction(
                  () =>
                    submitVolunteerHoursFs(
                      firestoreDb!,
                      currentProfile,
                      assign,
                      date,
                      start,
                      end,
                      breakMins,
                      manualOverride,
                      dayNum,
                      notes
                    ),
                  'Hours submitted for verification!'
                );
              }
            }}
          />
        );

      case 'IMPACT':
        return (
          <VolunteerImpactPage
            profile={currentProfile}
            myStats={currentMyStats}
            allStats={currentAllStats}
            myHours={myHours}
            notifications={currentNotifications}
            onMarkNotificationRead={(id) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  notifications: demoWorkspace.notifications.map((n) =>
                    n.id === id ? { ...n, readFlag: true } : n
                  ),
                });
              } else if (firestoreDb) {
                runLiveAction(() => markNotificationReadFs(firestoreDb!, id));
              }
            }}
            onShowMessage={showToast}
          />
        );

      case 'OFFICE_HOME':
      case 'OFFICE_FAIR_ALLOC':
        return (
          <OfficePortal
            section={currentDest === 'OFFICE_HOME' ? 'overview' : 'fair_alloc'}
            settings={currentSettings}
            allStats={currentAllStats}
            opportunities={currentOpps}
            requests={currentRequests}
            assignments={currentAssignments}
            pvCases={currentPvCases}
            callingTasks={currentCallingTasks}
            volunteerHours={currentHours}
            districts={currentDistricts}
            currentUserId={currentUid}
            isBusy={isBusy}
            onCreateOpportunity={(opp, pvCount) => {
              if (isDemo && demoWorkspace) {
                const id = `opp_${Date.now()}`;
                const created: Opportunity = { ...opp, id, totalCases: pvCount };
                const newCases: PhysicalVerificationCase[] =
                  pvCount > 0
                    ? Array.from({ length: pvCount }, (_, idx) => ({
                        id: `${id}_pv_${idx + 1}`,
                        caseRefId: `PV-${Date.now() % 1000}-${idx + 1}`,
                        opportunityId: id,
                        district: opp.district,
                        areaLabel: `${opp.district} Zone ${idx + 1}`,
                        applicantCode: `APP-${1000 + idx + 1}`,
                        assignedToUid: '',
                        assignedToName: '',
                        status: 'UNASSIGNED',
                        visitNotes: '',
                        recommendationSummary: '',
                        verifiedBy: '',
                      }))
                    : [];
                setDemoWorkspace({
                  ...demoWorkspace,
                  opportunities: [created, ...demoWorkspace.opportunities],
                  pvCases: [...newCases, ...demoWorkspace.pvCases],
                });
                showToast(`Published ${opp.title}.`);
              } else if (firestoreDb) {
                runLiveAction(
                  () =>
                    createOpportunityFs(
                      firestoreDb!,
                      currentUid,
                      opp,
                      pvCount,
                      currentProfile?.fullName || 'Office Coordinator',
                      effectiveRole
                    ),
                  `Published ${opp.title}!`
                );
              }
            }}
            onToggleOppStatus={(opp, st) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  opportunities: demoWorkspace.opportunities.map((o) =>
                    o.id === opp.id ? { ...o, status: st } : o
                  ),
                });
                showToast(`Updated ${opp.title} to ${st}.`);
              } else if (firestoreDb) {
                runLiveAction(
                  () =>
                    updateOpportunityStatusFs(
                      firestoreDb!,
                      currentUid,
                      opp.id,
                      st,
                      currentProfile?.fullName || 'Office',
                      effectiveRole
                    ),
                  'Opportunity updated.'
                );
              }
            }}
            onReviewRequest={(req, opp, decision, reason) => {
              if (isDemo && demoWorkspace) {
                const nextAssignments =
                  decision === 'APPROVED'
                    ? [
                        {
                          id: `assign_${Date.now()}`,
                          userId: req.userId,
                          volunteerName: req.volunteerName,
                          volunteerCode: req.volunteerCode,
                          opportunityId: opp.id,
                          opportunityTitle: opp.title,
                          category: opp.category,
                          district: opp.district,
                          assignedCasesCount: 0,
                          caseIds: [],
                          status: 'ASSIGNED',
                          progressNotes: '',
                          whatsappGroupLink: opp.whatsappGroupLink,
                          overrideReason: '',
                          assignedBy: currentUid,
                          verifiedBy: '',
                        } as Assignment,
                        ...demoWorkspace.assignments,
                      ]
                    : demoWorkspace.assignments;
                setDemoWorkspace({
                  ...demoWorkspace,
                  requests: demoWorkspace.requests.map((r) =>
                    r.id === req.id ? { ...r, status: decision, rejectionReason: reason } : r
                  ),
                  assignments: nextAssignments,
                  opportunities:
                    decision === 'APPROVED'
                      ? demoWorkspace.opportunities.map((o) =>
                          o.id === opp.id
                            ? { ...o, availableSlots: Math.max(0, o.availableSlots - 1) }
                            : o
                        )
                      : demoWorkspace.opportunities,
                });
                showToast(`Request ${decision.toLowerCase()}.`);
              } else if (firestoreDb) {
                runLiveAction(
                  () =>
                    reviewVolunteerRequestFs(
                      firestoreDb!,
                      currentUid,
                      req,
                      opp,
                      decision,
                      reason,
                      currentProfile?.fullName || 'Office',
                      effectiveRole
                    ),
                  `Request ${decision.toLowerCase()}.`
                );
              }
            }}
            onCastPollVote={(opp) => {
              if (!currentProfile) return;
              if (isDemo && demoWorkspace) {
                const newReq: VolunteerRequest = {
                  id: `poll_${opp.id}_${currentUid}`,
                  userId: currentUid,
                  volunteerName: currentProfile.fullName,
                  volunteerCode: currentProfile.volunteerCode,
                  opportunityId: opp.id,
                  opportunityTitle: opp.title,
                  category: opp.category,
                  district: opp.district,
                  status: 'PENDING',
                  rejectionReason: '',
                  availabilityNote: 'Available (Office Team Poll Vote)',
                  reviewedBy: '',
                };
                setDemoWorkspace({
                  ...demoWorkspace,
                  requests: [
                    newReq,
                    ...demoWorkspace.requests.filter(
                      (r) => !(r.userId === currentUid && r.opportunityId === opp.id)
                    ),
                  ],
                });
                showToast(`Voted available in ${opp.title}.`);
              } else if (firestoreDb) {
                runLiveAction(
                  () =>
                    submitVolunteerRequestFs(
                      firestoreDb!,
                      currentProfile,
                      opp,
                      'Available (Office Team Poll Vote)'
                    ),
                  'Poll vote submitted!'
                );
              }
            }}
            onCancelPollVote={(req) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  requests: demoWorkspace.requests.map((r) =>
                    r.id === req.id ? { ...r, status: 'CANCELLED' } : r
                  ),
                });
                showToast('Poll vote withdrawn.');
              } else if (firestoreDb) {
                runLiveAction(
                  () => cancelVolunteerRequestFs(firestoreDb!, req.id),
                  'Poll vote withdrawn.'
                );
              }
            }}
            onOpenFairAllocation={() => setCurrentDest('OFFICE_FAIR_ALLOC')}
            onAllocatePvCases={(opp, volStats, cases, overrideReason) => {
              if (isDemo && demoWorkspace) {
                const ids = new Set(cases.map((c) => c.id));
                setDemoWorkspace({
                  ...demoWorkspace,
                  pvCases: demoWorkspace.pvCases.map((p) =>
                    ids.has(p.id)
                      ? {
                          ...p,
                          assignedToUid: volStats.userId,
                          assignedToName: volStats.displayName,
                          status: 'ASSIGNED',
                        }
                      : p
                  ),
                  allStats: demoWorkspace.allStats.map((s) =>
                    s.userId === volStats.userId
                      ? { ...s, activePvCases: s.activePvCases + cases.length }
                      : s
                  ),
                  assignments: [
                    {
                      id: `assign_${Date.now()}`,
                      userId: volStats.userId,
                      volunteerName: volStats.displayName,
                      volunteerCode: volStats.volunteerCode,
                      opportunityId: opp.id,
                      opportunityTitle: opp.title,
                      category: opp.category,
                      district: opp.district,
                      assignedCasesCount: cases.length,
                      caseIds: cases.map((c) => c.id),
                      overrideReason,
                      status: 'ASSIGNED',
                      progressNotes: '',
                      whatsappGroupLink: opp.whatsappGroupLink,
                      assignedBy: currentUid,
                      verifiedBy: '',
                    },
                    ...demoWorkspace.assignments,
                  ],
                });
                showToast(`Allocated ${cases.length} PV case(s) to ${volStats.displayName}.`);
              } else if (firestoreDb) {
                runLiveAction(
                  () =>
                    allocatePvCasesFairlyFs(
                      firestoreDb!,
                      currentUid,
                      opp,
                      volStats,
                      cases,
                      overrideReason,
                      currentSettings.maxActivePvCases,
                      currentProfile?.fullName || 'Office',
                      effectiveRole
                    ),
                  `Allocated ${cases.length} PV case(s)!`
                );
              }
            }}
            onVerifyPvCase={(pv) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  pvCases: demoWorkspace.pvCases.map((p) =>
                    p.id === pv.id ? { ...p, status: 'VERIFIED' } : p
                  ),
                  allStats: demoWorkspace.allStats.map((s) =>
                    s.userId === pv.assignedToUid
                      ? {
                          ...s,
                          completedPvCases: s.completedPvCases + 1,
                          activePvCases: Math.max(0, s.activePvCases - 1),
                        }
                      : s
                  ),
                });
                showToast(`Verified ${pv.caseRefId}.`);
              } else if (firestoreDb) {
                runLiveAction(
                  () =>
                    verifyPvCaseByOfficeFs(
                      firestoreDb!,
                      currentUid,
                      pv,
                      currentProfile?.fullName || 'Office',
                      effectiveRole
                    ),
                  `Verified ${pv.caseRefId}!`
                );
              }
            }}
            onVerifyAssignment={(assign) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  assignments: demoWorkspace.assignments.map((a) =>
                    a.id === assign.id ? { ...a, status: 'VERIFIED' } : a
                  ),
                });
                showToast('Verified assignment completion.');
              } else if (firestoreDb) {
                runLiveAction(
                  () =>
                    verifyAssignmentByOfficeFs(
                      firestoreDb!,
                      currentUid,
                      assign,
                      currentProfile?.fullName || 'Office',
                      effectiveRole
                    ),
                  'Assignment verified!'
                );
              }
            }}
            onImportCallingSheet={(opp, volStats) => {
              if (isDemo && demoWorkspace) {
                const added: CallingTask[] = Array.from({ length: 5 }, (_, idx) => ({
                  id: `call_${Date.now()}_${idx + 1}`,
                  applicantRefId: `APP-SHEET-${200 + idx + 1}`,
                  opportunityId: opp.id,
                  district: opp.district,
                  callingDate: '2026-12-02',
                  assignedToUid: volStats.userId,
                  assignedToName: volStats.displayName,
                  callStatus: 'NOT_STARTED',
                  followUpRequired: false,
                  notes: '',
                  verifiedByOffice: false,
                }));
                setDemoWorkspace({
                  ...demoWorkspace,
                  callingTasks: [...added, ...demoWorkspace.callingTasks],
                });
                showToast(`Imported 5 calling tasks for ${volStats.displayName}.`);
              } else if (firestoreDb) {
                runLiveAction(
                  () => importDummyCallingSheetFs(firestoreDb!, opp, volStats),
                  'Imported 5 calling tasks!'
                );
              }
            }}
            onVerifyCallingTask={(taskId) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  callingTasks: demoWorkspace.callingTasks.map((t) =>
                    t.id === taskId ? { ...t, verifiedByOffice: true } : t
                  ),
                });
                showToast('Calling task verified.');
              } else if (firestoreDb) {
                runLiveAction(
                  () => verifyCallingTaskByOfficeFs(firestoreDb!, taskId),
                  'Calling task verified.'
                );
              }
            }}
            onReviewHours={(hr, status, finalHours, note) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  volunteerHours: demoWorkspace.volunteerHours.map((h) =>
                    h.id === hr.id
                      ? {
                          ...h,
                          verificationStatus: status,
                          submittedHours: finalHours,
                          correctionNote: note,
                          reviewerName: currentProfile?.fullName || 'Office',
                        }
                      : h
                  ),
                  allStats:
                    status === 'APPROVED'
                      ? demoWorkspace.allStats.map((s) => {
                          if (s.userId !== hr.userId) return s;
                          const pts = s.points + Math.round(finalHours * 10) + 25;
                          return {
                            ...s,
                            verifiedHours: s.verifiedHours + finalHours,
                            completedActivities: s.completedActivities + 1,
                            points: pts,
                            tier: tierForPoints(pts),
                          };
                        })
                      : demoWorkspace.allStats,
                });
                showToast(`Hours ${status.toLowerCase()}.`);
              } else if (firestoreDb) {
                runLiveAction(
                  () =>
                    reviewVolunteerHoursFs(
                      firestoreDb!,
                      currentUid,
                      hr,
                      status,
                      finalHours,
                      note,
                      currentProfile?.fullName || 'Office',
                      effectiveRole,
                      currentSettings.emailProviderConfigured
                    ),
                  `Hours ${status.toLowerCase()}!`
                );
              }
            }}
          />
        );

      case 'ADMIN':
        return (
          <AdminPortal
            settings={currentSettings}
            allStats={currentAllStats}
            allProfiles={currentProfiles}
            opportunities={currentOpps}
            requests={currentRequests}
            assignments={currentAssignments}
            volunteerHours={currentHours}
            districts={currentDistricts}
            organizations={currentOrgs}
            auditLogs={currentAuditLogs}
            isBusy={isBusy}
            onAddDistrict={(name, code) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  districts: [
                    ...demoWorkspace.districts,
                    { id: `dist_${Date.now()}`, name, code: code.toUpperCase(), active: true },
                  ],
                });
                showToast(`Added district ${name}.`);
              } else if (firestoreDb) {
                runLiveAction(
                  () =>
                    addDistrictFs(
                      firestoreDb!,
                      currentUid,
                      name,
                      code,
                      currentProfile?.fullName || 'Admin',
                      effectiveRole
                    ),
                  `Added district ${name}!`
                );
              }
            }}
            onAddOrganization={(name, industry, contact) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  organizations: [
                    ...demoWorkspace.organizations,
                    {
                      id: `org_${Date.now()}`,
                      name,
                      industry,
                      contactPerson: contact,
                      active: true,
                    },
                  ],
                });
                showToast(`Added organization ${name}.`);
              } else if (firestoreDb) {
                runLiveAction(
                  () =>
                    addEmployerOrgFs(
                      firestoreDb!,
                      currentUid,
                      name,
                      industry,
                      contact,
                      currentProfile?.fullName || 'Admin',
                      effectiveRole
                    ),
                  `Added organization ${name}!`
                );
              }
            }}
            onUpdateSettings={(newSet) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({ ...demoWorkspace, settings: newSet });
                showToast('Updated settings.');
              } else if (firestoreDb) {
                runLiveAction(
                  () =>
                    updateAppSettingsFs(
                      firestoreDb!,
                      currentUid,
                      newSet,
                      currentProfile?.fullName || 'Admin',
                      effectiveRole
                    ),
                  'Updated settings!'
                );
              }
            }}
            onPromoteRole={(prof, role) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  allProfiles: demoWorkspace.allProfiles.map((p) =>
                    p.userId === prof.userId ? { ...p, role } : p
                  ),
                });
                showToast(`Updated role to ${USER_ROLE_LABELS[role]}.`);
              } else if (firestoreDb) {
                runLiveAction(
                  () =>
                    promoteUserRoleFs(
                      firestoreDb!,
                      currentUid,
                      prof.userId,
                      prof.fullName,
                      prof.email,
                      role,
                      currentProfile?.fullName || 'Admin'
                    ),
                  `Updated role to ${USER_ROLE_LABELS[role]}!`
                );
              }
            }}
            onSeedDemoData={() => {
              if (isDemo && demoWorkspace) {
                showToast('Demo workspace is already active with sample data!');
              } else if (firestoreDb) {
                runLiveAction(
                  () =>
                    seedDemoWorkspaceFs(
                      firestoreDb!,
                      currentUid,
                      currentProfile?.fullName || 'Admin'
                    ),
                  'Seeded demo workspace in Cloud Firestore!'
                );
              }
            }}
          />
        );

      case 'PROFILE':
        return (
          <ProfileOnboardingPage
            uid={currentUid}
            email={currentEmail}
            defaultName={currentProfile?.fullName || 'Maatram Volunteer'}
            existingProfile={currentProfile}
            existingStats={currentMyStats}
            districts={currentDistricts}
            organizations={currentOrgs}
            isBusy={isBusy}
            onSaveProfile={(prof, isNew, availPv) => {
              if (isDemo && demoWorkspace) {
                setDemoWorkspace({
                  ...demoWorkspace,
                  myProfile: { ...prof, profileCompleted: true },
                  allStats: demoWorkspace.allStats.map((s) =>
                    s.userId === currentUid
                      ? {
                          ...s,
                          displayName: prof.fullName,
                          volunteerType: prof.volunteerType,
                          district: prof.district,
                          employerOrgName: prof.employerOrgName,
                          availability: prof.availability,
                          availableForPv: availPv,
                        }
                      : s
                  ),
                });
                showToast('Profile saved!');
              } else if (firestoreDb && firebaseUser) {
                runLiveAction(
                  () =>
                    saveUserProfileFs(
                      firestoreDb!,
                      firebaseUser.uid,
                      firebaseUser.email,
                      firebaseUser.emailVerified,
                      prof,
                      isNew,
                      availPv
                    ),
                  'Profile saved!'
                );
              }
            }}
          />
        );
    }
  };

  return (
    <div className="min-h-screen bg-maatram-surface text-maatram-navy">
      {/* Top App Header */}
      <header className="sticky top-0 z-30 border-b border-maatram-outline bg-white/95 backdrop-blur-xs">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-3 px-4">
          <div className="flex items-center gap-3 min-w-0">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-maatram-blue text-xs font-bold text-white">
              MC
            </div>
            <div className="min-w-0">
              <h1 className="truncate text-sm font-bold text-maatram-navy sm:text-base">
                {effectiveRole === 'VOLUNTEER'
                  ? 'Student Portal'
                  : effectiveRole === 'OFFICE_TEAM'
                  ? 'Office Team Portal'
                  : 'Admin Portal'}
              </h1>
              <p className="truncate text-xs text-slate-500">
                {currentProfile?.fullName || 'Maatram Connect'} • {USER_ROLE_LABELS[effectiveRole]}
                {isDemo ? ' (Demo)' : ''}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {canSwitchRoles && (
              <div className="hidden items-center gap-1 rounded-xl border border-maatram-outline bg-maatram-surface p-1 md:flex">
                {(['VOLUNTEER', 'OFFICE_TEAM', 'ADMIN'] as UserRole[]).map((role) => (
                  <button
                    key={role}
                    type="button"
                    onClick={() => handleRoleSwitch(role)}
                    className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
                      effectiveRole === role
                        ? 'bg-maatram-blue text-white'
                        : 'text-maatram-navy hover:bg-white'
                    }`}
                  >
                    {USER_ROLE_LABELS[role]}
                  </button>
                ))}
              </div>
            )}
            <button
              type="button"
              data-testid="sign_out_button"
              onClick={handleSignOut}
              aria-label="Sign Out"
              className="flex h-10 items-center gap-1.5 rounded-xl border border-maatram-outline bg-white px-3 text-xs font-semibold text-maatram-navy transition hover:bg-maatram-blueSoft"
            >
              <LogOut className="h-4 w-4 text-maatram-blue" />
              <span className="hidden sm:inline">Sign Out</span>
            </button>
          </div>
        </div>

        {/* Mobile Role Switcher strip for Staff/Admin */}
        {canSwitchRoles && (
          <div className="flex gap-1.5 border-t border-maatram-outline bg-white px-4 py-2 md:hidden">
            {(['VOLUNTEER', 'OFFICE_TEAM', 'ADMIN'] as UserRole[]).map((role) => (
              <button
                key={role}
                type="button"
                onClick={() => handleRoleSwitch(role)}
                className={`flex-1 rounded-lg py-1.5 text-xs font-semibold ${
                  effectiveRole === role
                    ? 'bg-maatram-blue text-white'
                    : 'bg-maatram-blueSoft text-maatram-navy'
                }`}
              >
                {USER_ROLE_LABELS[role]}
              </button>
            ))}
          </div>
        )}
      </header>

      {/* Responsive Desktop Left Sidebar + Main Content */}
      <div className="mx-auto flex max-w-7xl">
        {!needsOnboarding && (
          <aside className="hidden w-60 shrink-0 border-r border-maatram-outline bg-white p-4 md:block md:min-h-[calc(100vh-4rem)]">
            <nav className="space-y-1.5">
              {navItems.map((item) => {
                const active = currentDest === item.key;
                return (
                  <button
                    key={item.key}
                    type="button"
                    data-testid={`nav_${item.key}`}
                    onClick={() => setCurrentDest(item.key)}
                    className={`flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition ${
                      active
                        ? 'bg-maatram-blue text-white shadow-sm'
                        : 'text-maatram-navy hover:bg-maatram-blueSoft'
                    }`}
                  >
                    {item.icon}
                    <span>{item.label}</span>
                  </button>
                );
              })}
            </nav>
          </aside>
        )}

        <main className="min-w-0 flex-1 px-4 py-5 pb-24 md:px-6 md:pb-8">{renderMainBody()}</main>
      </div>

      {/* Responsive Mobile Bottom Navigation */}
      {!needsOnboarding && (
        <nav className="fixed bottom-0 left-0 right-0 z-30 border-t border-maatram-outline bg-white md:hidden">
          <div className="grid grid-cols-5">
            {navItems.map((item) => {
              const active = currentDest === item.key;
              return (
                <button
                  key={item.key}
                  type="button"
                  data-testid={`mobile_nav_${item.key}`}
                  onClick={() => setCurrentDest(item.key)}
                  className={`flex flex-col items-center justify-center gap-1 py-2.5 text-[11px] font-semibold ${
                    active ? 'text-maatram-blue' : 'text-slate-500'
                  }`}
                >
                  {item.icon}
                  <span className="truncate">{item.label}</span>
                </button>
              );
            })}
          </div>
        </nav>
      )}

      <ToastBanner message={toastMessage} onDismiss={() => setToastMessage(null)} />
    </div>
  );
};
