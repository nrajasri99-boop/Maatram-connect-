import {
  collection,
  doc,
  getDoc,
  getDocs,
  increment,
  onSnapshot,
  query,
  serverTimestamp,
  setDoc,
  Timestamp,
  updateDoc,
  where,
  writeBatch,
  type Firestore,
  type Unsubscribe,
} from 'firebase/firestore';
import {
  buildThankYouEmailBody,
  calculateDurationHours,
  computeGamification,
} from '../domain/fairAllocationEngine';
import type {
  AppSettings,
  Assignment,
  AssignmentStatus,
  CallStatus,
  OfficeTeamMember,
  Opportunity,
  OpportunityStatus,
  PhysicalVerificationCase,
  PvCaseStatus,
  RequestStatus,
  UserProfile,
  UserRole,
  VerificationStatus,
  VolunteerHours,
  VolunteerRequest,
  VolunteerStats,
} from '../types/maatram';

function mapDoc<T>(id: string, data: Record<string, unknown>): T {
  return { ...data, id } as T;
}

export function subscribeMyProfile(
  db: Firestore,
  userId: string,
  onData: (profile: UserProfile | null) => void,
  onError: (err: Error) => void
): Unsubscribe {
  return onSnapshot(
    doc(db, 'profiles', userId),
    (snap) => {
      onData(snap.exists() ? mapDoc<UserProfile>(snap.id, snap.data()) : null);
    },
    (err) => onError(err)
  );
}

export function subscribeOfficeTeamRecord(
  db: Firestore,
  userId: string,
  onData: (member: OfficeTeamMember | null) => void,
  onError: (err: Error) => void
): Unsubscribe {
  return onSnapshot(
    doc(db, 'office_team', userId),
    (snap) => {
      onData(snap.exists() ? mapDoc<OfficeTeamMember>(snap.id, snap.data()) : null);
    },
    (err) => onError(err)
  );
}

export function subscribeAppSettings(
  db: Firestore,
  onData: (settings: AppSettings) => void,
  onError: (err: Error) => void
): Unsubscribe {
  const defaults: AppSettings = {
    id: 'global',
    maxActivePvCases: 3,
    activeThresholdDays: 30,
    lessActiveThresholdDays: 60,
    inactiveThresholdDays: 90,
    emailProviderConfigured: false,
    emailSenderName: 'Maatram Foundation',
    updatedBy: '',
  };
  return onSnapshot(
    doc(db, 'app_settings', 'global'),
    (snap) => {
      onData(snap.exists() ? { ...defaults, ...mapDoc<AppSettings>(snap.id, snap.data()) } : defaults);
    },
    (err) => onError(err)
  );
}

export function subscribeCollection<T>(
  db: Firestore,
  path: string,
  constraints: ReturnType<typeof where>[],
  onData: (items: T[]) => void,
  onError: (err: Error) => void
): Unsubscribe {
  const ref = collection(db, path);
  const q = constraints.length > 0 ? query(ref, ...constraints) : ref;
  return onSnapshot(
    q,
    (snap) => {
      onData(snap.docs.map((d) => mapDoc<T>(d.id, d.data())));
    },
    (err) => onError(err)
  );
}

export async function saveUserProfileFs(
  db: Firestore,
  uid: string,
  email: string | null,
  emailVerified: boolean,
  profile: UserProfile,
  isNewProfile: boolean,
  availableForPv: boolean
): Promise<void> {
  const batch = writeBatch(db);
  const profileRef = doc(db, 'profiles', uid);
  const statsRef = doc(db, 'volunteer_stats', uid);

  if (isNewProfile) {
    const safeRole: UserRole =
      email === 'nrajasri99@gmail.com' && emailVerified ? 'ADMIN' : 'VOLUNTEER';
    batch.set(profileRef, {
      userId: uid,
      fullName: profile.fullName.trim(),
      email: profile.email,
      phone: profile.phone.trim(),
      volunteerCode: profile.volunteerCode.trim(),
      volunteerType: profile.volunteerType,
      college: profile.college.trim(),
      department: profile.department.trim(),
      yearOfStudy: profile.yearOfStudy.trim(),
      district: profile.district,
      city: profile.city.trim(),
      skills: profile.skills.trim(),
      interests: profile.interests.trim(),
      availability: profile.availability.trim(),
      preferredContact: profile.preferredContact.trim(),
      employerOrgId: profile.employerOrgId,
      employerOrgName: profile.employerOrgName,
      consentDataProcessing: profile.consentDataProcessing,
      consentLinkedInPublic: profile.consentLinkedInPublic,
      profileCompleted: true,
      role: safeRole,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    batch.set(statsRef, {
      userId: uid,
      displayName: profile.fullName.trim(),
      volunteerCode: profile.volunteerCode.trim(),
      volunteerType: profile.volunteerType,
      district: profile.district,
      employerOrgName: profile.employerOrgName,
      availability: profile.availability.trim(),
      availableForPv,
      verifiedHours: 0,
      completedActivities: 0,
      completedPvCases: 0,
      activePvCases: 0,
      points: 0,
      consistencyScore: 0,
      tier: 'BRONZE',
      badges: [],
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    batch.set(doc(collection(db, 'notifications')), {
      userId: uid,
      title: 'Welcome to Maatram Connect',
      message: `Registration confirmed for ${profile.fullName} (${profile.volunteerCode}). Explore opportunities across Tamil Nadu.`,
      type: 'REGISTRATION',
      emailMode: 'DEMO_PREVIEW',
      readFlag: false,
      createdAt: serverTimestamp(),
    });
  } else {
    batch.update(profileRef, {
      fullName: profile.fullName.trim(),
      phone: profile.phone.trim(),
      volunteerType: profile.volunteerType,
      college: profile.college.trim(),
      department: profile.department.trim(),
      yearOfStudy: profile.yearOfStudy.trim(),
      district: profile.district,
      city: profile.city.trim(),
      skills: profile.skills.trim(),
      interests: profile.interests.trim(),
      availability: profile.availability.trim(),
      preferredContact: profile.preferredContact.trim(),
      employerOrgId: profile.employerOrgId,
      employerOrgName: profile.employerOrgName,
      consentDataProcessing: profile.consentDataProcessing,
      consentLinkedInPublic: profile.consentLinkedInPublic,
      profileCompleted: true,
      updatedAt: serverTimestamp(),
    });
    batch.update(statsRef, {
      displayName: profile.fullName.trim(),
      volunteerType: profile.volunteerType,
      district: profile.district,
      employerOrgName: profile.employerOrgName,
      availability: profile.availability.trim(),
      availableForPv,
      updatedAt: serverTimestamp(),
    });
  }
  await batch.commit();
}

export async function updatePvAvailabilityFs(
  db: Firestore,
  uid: string,
  availableForPv: boolean
): Promise<void> {
  await updateDoc(doc(db, 'volunteer_stats', uid), {
    availableForPv,
    updatedAt: serverTimestamp(),
  });
}

export async function createOpportunityFs(
  db: Firestore,
  uid: string,
  opportunity: Omit<Opportunity, 'id'>,
  pvCaseCountToGenerate: number,
  actorName: string,
  actorRole: string
): Promise<void> {
  const oppRef = doc(collection(db, 'opportunities'));
  const oppId = oppRef.id;
  const batch = writeBatch(db);

  batch.set(oppRef, {
    title: opportunity.title.trim(),
    category: opportunity.category,
    activityType: opportunity.activityType.trim(),
    description: opportunity.description.trim(),
    district: opportunity.district,
    location: opportunity.location.trim(),
    mode: opportunity.mode,
    eventDate: opportunity.eventDate.trim(),
    startTime: opportunity.startTime.trim(),
    endTime: opportunity.endTime.trim(),
    deadline: opportunity.deadline.trim(),
    requiredVolunteers: opportunity.requiredVolunteers,
    availableSlots: opportunity.requiredVolunteers,
    totalCases: pvCaseCountToGenerate,
    skillsRequired: opportunity.skillsRequired.trim(),
    eligibility: opportunity.eligibility.trim(),
    instructions: opportunity.instructions.trim(),
    whatsappGroupLink: opportunity.whatsappGroupLink.trim(),
    status: 'OPEN',
    multiDayCount: Math.max(1, opportunity.multiDayCount || 1),
    createdBy: uid,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });

  if (opportunity.category === 'PHYSICAL_VERIFICATION' && pvCaseCountToGenerate > 0) {
    const distCode = opportunity.district.slice(0, 3).toUpperCase();
    for (let i = 1; i <= pvCaseCountToGenerate; i++) {
      batch.set(doc(collection(db, 'pv_cases')), {
        caseRefId: `PV-${distCode}-${Date.now() % 10000}-${i}`,
        opportunityId: oppId,
        district: opportunity.district,
        areaLabel: `${opportunity.location.trim() || opportunity.district} Zone ${i}`,
        applicantCode: `APP-PV-${100 + i}`,
        assignedToUid: '',
        assignedToName: '',
        status: 'UNASSIGNED',
        visitNotes: '',
        recommendationSummary: '',
        verifiedBy: '',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    }
  }

  batch.set(doc(collection(db, 'audit_logs')), {
    actorUid: uid,
    actorName,
    actorRole,
    action: 'OPPORTUNITY_CREATED',
    targetType: 'opportunities',
    targetId: oppId,
    details: `Created ${opportunity.category} opportunity '${opportunity.title}' in ${opportunity.district}`,
    reason: '',
    createdAt: serverTimestamp(),
  });

  await batch.commit();
}

export async function updateOpportunityStatusFs(
  db: Firestore,
  uid: string,
  oppId: string,
  newStatus: OpportunityStatus,
  actorName: string,
  actorRole: string
): Promise<void> {
  const batch = writeBatch(db);
  batch.update(doc(db, 'opportunities', oppId), {
    status: newStatus,
    updatedAt: serverTimestamp(),
  });
  batch.set(doc(collection(db, 'audit_logs')), {
    actorUid: uid,
    actorName,
    actorRole,
    action: 'OPPORTUNITY_STATUS_UPDATED',
    targetType: 'opportunities',
    targetId: oppId,
    details: `Updated opportunity status to ${newStatus}`,
    reason: '',
    createdAt: serverTimestamp(),
  });
  await batch.commit();
}

export async function submitVolunteerRequestFs(
  db: Firestore,
  profile: UserProfile,
  opportunity: Opportunity,
  availabilityNote: string
): Promise<void> {
  const reqId = `${opportunity.id}_${profile.userId}`;
  const existing = await getDoc(doc(db, 'volunteer_requests', reqId));
  if (existing.exists()) {
    const status = existing.data().status as string;
    if (status !== 'CANCELLED' && status !== 'REJECTED') {
      throw new Error('You have already submitted a request for this opportunity.');
    }
  }
  const batch = writeBatch(db);
  batch.set(doc(db, 'volunteer_requests', reqId), {
    userId: profile.userId,
    volunteerName: profile.fullName,
    volunteerCode: profile.volunteerCode,
    opportunityId: opportunity.id,
    opportunityTitle: opportunity.title,
    category: opportunity.category,
    district: opportunity.district,
    status: 'PENDING',
    rejectionReason: '',
    availabilityNote: availabilityNote.trim() || profile.availability,
    reviewedBy: '',
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  batch.set(doc(collection(db, 'notifications')), {
    userId: profile.userId,
    title: 'Request Submitted',
    message: `Your request to join '${opportunity.title}' is pending Office Team review.`,
    type: 'REQUEST_STATUS',
    emailMode: 'DEMO_PREVIEW',
    readFlag: false,
    createdAt: serverTimestamp(),
  });
  await batch.commit();
}

export async function cancelVolunteerRequestFs(db: Firestore, reqId: string): Promise<void> {
  await updateDoc(doc(db, 'volunteer_requests', reqId), {
    status: 'CANCELLED',
    updatedAt: serverTimestamp(),
  });
}

export async function reviewVolunteerRequestFs(
  db: Firestore,
  uid: string,
  request: VolunteerRequest,
  opportunity: Opportunity,
  decision: RequestStatus,
  rejectionReason: string,
  actorName: string,
  actorRole: string
): Promise<void> {
  const batch = writeBatch(db);
  batch.update(doc(db, 'volunteer_requests', request.id), {
    status: decision,
    rejectionReason: decision === 'REJECTED' ? rejectionReason.trim() : '',
    reviewedBy: uid,
    updatedAt: serverTimestamp(),
  });

  if (decision === 'APPROVED') {
    const assignId = `${opportunity.id}_${request.userId}`;
    batch.set(doc(db, 'assignments', assignId), {
      userId: request.userId,
      volunteerName: request.volunteerName,
      volunteerCode: request.volunteerCode,
      opportunityId: opportunity.id,
      opportunityTitle: opportunity.title,
      category: opportunity.category,
      district: opportunity.district,
      assignedCasesCount: 0,
      caseIds: [],
      status: 'ASSIGNED',
      progressNotes: '',
      whatsappGroupLink: opportunity.whatsappGroupLink,
      overrideReason: '',
      assignedBy: uid,
      verifiedBy: '',
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    if (opportunity.availableSlots > 0) {
      batch.update(doc(db, 'opportunities', opportunity.id), {
        availableSlots: increment(-1),
        updatedAt: serverTimestamp(),
      });
    }
  }

  const notifMsg =
    decision === 'APPROVED'
      ? `Approved for '${opportunity.title}'. Check My Work for assigned details & WhatsApp invite link.`
      : decision === 'REJECTED'
      ? `Request for '${opportunity.title}' was not approved. Reason: ${rejectionReason || 'Capacity filled'}`
      : `You are waitlisted for '${opportunity.title}'.`;

  batch.set(doc(collection(db, 'notifications')), {
    userId: request.userId,
    title: `Request ${decision}`,
    message: notifMsg,
    type: 'REQUEST_STATUS',
    emailMode: 'DEMO_PREVIEW',
    readFlag: false,
    createdAt: serverTimestamp(),
  });

  batch.set(doc(collection(db, 'audit_logs')), {
    actorUid: uid,
    actorName,
    actorRole,
    action: `REQUEST_${decision}`,
    targetType: 'volunteer_requests',
    targetId: request.id,
    details: `${decision} request for ${request.volunteerName} on ${opportunity.title}`,
    reason: rejectionReason.trim(),
    createdAt: serverTimestamp(),
  });

  await batch.commit();
}

export async function allocatePvCasesFairlyFs(
  db: Firestore,
  uid: string,
  opportunity: Opportunity,
  candidateStats: VolunteerStats,
  selectedCases: PhysicalVerificationCase[],
  overrideReason: string,
  maxActivePvCases: number,
  actorName: string,
  actorRole: string
): Promise<void> {
  if (selectedCases.length === 0) {
    throw new Error('Select at least one unassigned PV case to allocate.');
  }
  const currentActive = candidateStats.activePvCases;
  const needsOverride = currentActive > 0 || currentActive + selectedCases.length > maxActivePvCases;
  if (needsOverride && !overrideReason.trim()) {
    throw new Error(
      `Volunteer already has ${currentActive} active PV cases (limit: ${maxActivePvCases}). Provide a mandatory override reason.`
    );
  }
  const assignId = `${opportunity.id}_${candidateStats.userId}`;
  const existingAssign = await getDoc(doc(db, 'assignments', assignId));
  if (existingAssign.exists()) {
    throw new Error(`${candidateStats.displayName} is already assigned to this PV drive.`);
  }

  const batch = writeBatch(db);
  selectedCases.forEach((pvCase) => {
    batch.update(doc(db, 'pv_cases', pvCase.id), {
      assignedToUid: candidateStats.userId,
      assignedToName: candidateStats.displayName,
      status: 'ASSIGNED',
      updatedAt: serverTimestamp(),
    });
  });

  batch.set(doc(db, 'assignments', assignId), {
    userId: candidateStats.userId,
    volunteerName: candidateStats.displayName,
    volunteerCode: candidateStats.volunteerCode,
    opportunityId: opportunity.id,
    opportunityTitle: opportunity.title,
    category: 'PHYSICAL_VERIFICATION',
    district: opportunity.district,
    assignedCasesCount: selectedCases.length,
    caseIds: selectedCases.map((c) => c.id),
    status: 'ASSIGNED',
    progressNotes: '',
    whatsappGroupLink: opportunity.whatsappGroupLink,
    overrideReason: overrideReason.trim(),
    assignedBy: uid,
    verifiedBy: '',
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });

  const reqId = `${opportunity.id}_${candidateStats.userId}`;
  const reqSnap = await getDoc(doc(db, 'volunteer_requests', reqId));
  if (reqSnap.exists()) {
    batch.update(doc(db, 'volunteer_requests', reqId), {
      status: 'APPROVED',
      reviewedBy: uid,
      updatedAt: serverTimestamp(),
    });
  }

  batch.update(doc(db, 'volunteer_stats', candidateStats.userId), {
    activePvCases: increment(selectedCases.length),
    updatedAt: serverTimestamp(),
  });

  if (opportunity.availableSlots > 0) {
    batch.update(doc(db, 'opportunities', opportunity.id), {
      availableSlots: increment(-1),
      updatedAt: serverTimestamp(),
    });
  }

  batch.set(doc(collection(db, 'notifications')), {
    userId: candidateStats.userId,
    title: 'Physical Verification Assigned',
    message: `Allocated ${selectedCases.length} PV case(s) in ${opportunity.district} for '${opportunity.title}'.`,
    type: 'ASSIGNMENT',
    emailMode: 'DEMO_PREVIEW',
    readFlag: false,
    createdAt: serverTimestamp(),
  });

  batch.set(doc(collection(db, 'audit_logs')), {
    actorUid: uid,
    actorName,
    actorRole,
    action: overrideReason.trim() ? 'PV_ALLOCATION_OVERRIDE' : 'PV_FAIR_ALLOCATION',
    targetType: 'assignments',
    targetId: assignId,
    details: `Assigned ${selectedCases.length} PV cases to ${candidateStats.displayName} (prior completed=${candidateStats.completedPvCases}, prior active=${candidateStats.activePvCases})`,
    reason: overrideReason.trim(),
    createdAt: serverTimestamp(),
  });

  await batch.commit();
}

export async function updatePvCaseByVolunteerFs(
  db: Firestore,
  caseId: string,
  newStatus: PvCaseStatus,
  visitNotes: string,
  recommendationSummary: string
): Promise<void> {
  await updateDoc(doc(db, 'pv_cases', caseId), {
    status: newStatus,
    visitNotes: visitNotes.trim(),
    recommendationSummary: recommendationSummary.trim(),
    updatedAt: serverTimestamp(),
  });
}

export async function verifyPvCaseByOfficeFs(
  db: Firestore,
  uid: string,
  pvCase: PhysicalVerificationCase,
  actorName: string,
  actorRole: string
): Promise<void> {
  if (pvCase.status === 'VERIFIED') {
    throw new Error('Case already verified.');
  }
  const batch = writeBatch(db);
  batch.update(doc(db, 'pv_cases', pvCase.id), {
    status: 'VERIFIED',
    verifiedBy: uid,
    updatedAt: serverTimestamp(),
  });
  if (pvCase.assignedToUid) {
    const volRef = doc(db, 'volunteer_stats', pvCase.assignedToUid);
    const volSnap = await getDoc(volRef);
    const currentActive = (volSnap.data()?.activePvCases as number) || 0;
    batch.update(volRef, {
      activePvCases: Math.max(0, currentActive - 1),
      completedPvCases: increment(1),
      updatedAt: serverTimestamp(),
    });
  }
  batch.set(doc(collection(db, 'audit_logs')), {
    actorUid: uid,
    actorName,
    actorRole,
    action: 'PV_CASE_VERIFIED',
    targetType: 'pv_cases',
    targetId: pvCase.id,
    details: `Verified PV case ${pvCase.caseRefId} completed by ${pvCase.assignedToName}`,
    reason: '',
    createdAt: serverTimestamp(),
  });
  await batch.commit();
}

export async function updateAssignmentProgressFs(
  db: Firestore,
  assignmentId: string,
  newStatus: AssignmentStatus,
  progressNotes: string
): Promise<void> {
  await updateDoc(doc(db, 'assignments', assignmentId), {
    status: newStatus,
    progressNotes: progressNotes.trim(),
    updatedAt: serverTimestamp(),
  });
}

export async function verifyAssignmentByOfficeFs(
  db: Firestore,
  uid: string,
  assignment: Assignment,
  actorName: string,
  actorRole: string
): Promise<void> {
  const batch = writeBatch(db);
  batch.update(doc(db, 'assignments', assignment.id), {
    status: 'VERIFIED',
    verifiedBy: uid,
    updatedAt: serverTimestamp(),
  });
  batch.set(doc(collection(db, 'audit_logs')), {
    actorUid: uid,
    actorName,
    actorRole,
    action: 'ASSIGNMENT_VERIFIED',
    targetType: 'assignments',
    targetId: assignment.id,
    details: `Verified assignment '${assignment.opportunityTitle}' for ${assignment.volunteerName}`,
    reason: '',
    createdAt: serverTimestamp(),
  });
  await batch.commit();
}

export async function importDummyCallingSheetFs(
  db: Firestore,
  opportunity: Opportunity,
  assignedVolunteer: VolunteerStats,
  count = 5
): Promise<void> {
  const batch = writeBatch(db);
  const base = Date.now() % 10000;
  for (let i = 1; i <= count; i++) {
    batch.set(doc(collection(db, 'calling_tasks')), {
      applicantRefId: `APP-CSV-${base + i}`,
      opportunityId: opportunity.id,
      district: opportunity.district,
      callingDate: opportunity.eventDate,
      assignedToUid: assignedVolunteer.userId,
      assignedToName: assignedVolunteer.displayName,
      callStatus: 'NOT_STARTED',
      followUpRequired: false,
      notes: 'Imported from calling sheet',
      verifiedByOffice: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }
  await batch.commit();
}

export async function updateCallingTaskByVolunteerFs(
  db: Firestore,
  taskId: string,
  callStatus: CallStatus,
  followUpRequired: boolean,
  notes: string
): Promise<void> {
  await updateDoc(doc(db, 'calling_tasks', taskId), {
    callStatus,
    followUpRequired,
    notes: notes.trim(),
    updatedAt: serverTimestamp(),
  });
}

export async function verifyCallingTaskByOfficeFs(db: Firestore, taskId: string): Promise<void> {
  await updateDoc(doc(db, 'calling_tasks', taskId), {
    verifiedByOffice: true,
    updatedAt: serverTimestamp(),
  });
}

export async function submitVolunteerHoursFs(
  db: Firestore,
  profile: UserProfile,
  assignment: Assignment,
  activityDate: string,
  startTime: string,
  endTime: string,
  breakMinutes: number,
  manualHoursOverride: number | null,
  dayNumber: number,
  notes: string
): Promise<void> {
  const hourDocId = `${assignment.id}_day${dayNumber}`;
  const existing = await getDoc(doc(db, 'volunteer_hours', hourDocId));
  if (existing.exists() && existing.data().verificationStatus !== 'REJECTED') {
    throw new Error(`Hours for Day ${dayNumber} of this activity have already been submitted.`);
  }
  const calculated = calculateDurationHours(startTime, endTime, breakMinutes);
  const raw = manualHoursOverride ?? calculated;
  const submitted = Math.max(0.25, Math.min(24, raw));

  await setDoc(doc(db, 'volunteer_hours', hourDocId), {
    userId: profile.userId,
    volunteerName: profile.fullName,
    volunteerCode: profile.volunteerCode,
    assignmentId: assignment.id,
    opportunityId: assignment.opportunityId,
    opportunityTitle: assignment.opportunityTitle,
    category: assignment.category,
    district: assignment.district,
    employerOrgName: profile.employerOrgName,
    activityDate: activityDate.trim(),
    startTime: startTime.trim(),
    endTime: endTime.trim(),
    breakMinutes,
    calculatedHours: calculated,
    submittedHours: submitted,
    dayNumber,
    notes: notes.trim(),
    verificationStatus: 'PENDING',
    reviewerUid: '',
    reviewerName: '',
    correctionNote: '',
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
}

export async function reviewVolunteerHoursFs(
  db: Firestore,
  uid: string,
  hoursRecord: VolunteerHours,
  decision: VerificationStatus,
  adjustedHours: number,
  correctionNote: string,
  actorName: string,
  actorRole: string,
  emailConfigured: boolean
): Promise<void> {
  const batch = writeBatch(db);
  const finalHours = Math.max(0.25, Math.min(24, adjustedHours));

  batch.update(doc(db, 'volunteer_hours', hoursRecord.id), {
    verificationStatus: decision,
    submittedHours: finalHours,
    correctionNote: correctionNote.trim(),
    reviewerUid: uid,
    reviewerName: actorName,
    updatedAt: serverTimestamp(),
  });

  if (decision === 'APPROVED') {
    const volHoursSnap = await getDocs(
      query(collection(db, 'volunteer_hours'), where('userId', '==', hoursRecord.userId))
    );
    const existingList = volHoursSnap.docs
      .map((d) => mapDoc<VolunteerHours>(d.id, d.data()))
      .filter((h) => h.id !== hoursRecord.id)
      .concat({
        ...hoursRecord,
        verificationStatus: 'APPROVED',
        submittedHours: finalHours,
      });

    const statsSnap = await getDoc(doc(db, 'volunteer_stats', hoursRecord.userId));
    const currentStats = statsSnap.exists()
      ? mapDoc<VolunteerStats>(statsSnap.id, statsSnap.data())
      : null;
    const completedPv = currentStats?.completedPvCases ?? 0;
    const gamification = computeGamification(existingList, completedPv, currentStats);

    if (statsSnap.exists()) {
      batch.update(doc(db, 'volunteer_stats', hoursRecord.userId), {
        verifiedHours: gamification.totalVerifiedHours,
        completedActivities: gamification.completedActivities,
        points: gamification.points,
        consistencyScore: gamification.consistencyScore,
        tier: gamification.tier,
        badges: gamification.badges,
        lastVerifiedAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    }

    if (hoursRecord.assignmentId) {
      const assignSnap = await getDoc(doc(db, 'assignments', hoursRecord.assignmentId));
      if (assignSnap.exists() && assignSnap.data().status !== 'VERIFIED') {
        batch.update(doc(db, 'assignments', hoursRecord.assignmentId), {
          status: 'VERIFIED',
          verifiedBy: uid,
          updatedAt: serverTimestamp(),
        });
      }
    }

    const emailMode = emailConfigured ? 'CONFIGURED_PROVIDER' : 'DEMO_PREVIEW';
    batch.set(doc(collection(db, 'notifications')), {
      userId: hoursRecord.userId,
      title: `Thank You! ${finalHours}h Verified`,
      message: buildThankYouEmailBody(
        hoursRecord.volunteerName,
        hoursRecord.opportunityTitle,
        finalHours
      ),
      type: 'THANK_YOU',
      emailMode,
      readFlag: false,
      createdAt: serverTimestamp(),
    });
  } else {
    batch.set(doc(collection(db, 'notifications')), {
      userId: hoursRecord.userId,
      title: `Hours Submission ${decision}`,
      message: `Hours for '${hoursRecord.opportunityTitle}' were marked ${decision}. Note: ${correctionNote || 'Please verify timings'}`,
      type: 'HOURS_APPROVED',
      emailMode: 'DEMO_PREVIEW',
      readFlag: false,
      createdAt: serverTimestamp(),
    });
  }

  batch.set(doc(collection(db, 'audit_logs')), {
    actorUid: uid,
    actorName,
    actorRole,
    action: `HOURS_${decision}`,
    targetType: 'volunteer_hours',
    targetId: hoursRecord.id,
    details: `${decision} ${finalHours}h for ${hoursRecord.volunteerName} (${hoursRecord.opportunityTitle})`,
    reason: correctionNote.trim(),
    createdAt: serverTimestamp(),
  });

  await batch.commit();
}

export async function addDistrictFs(
  db: Firestore,
  uid: string,
  name: string,
  code: string,
  actorName: string,
  actorRole: string
): Promise<void> {
  const id = `dist_${code.trim().toLowerCase().replace(/[^a-z0-9]/g, '')}`;
  const batch = writeBatch(db);
  batch.set(doc(db, 'districts', id), {
    name: name.trim(),
    code: code.trim().toUpperCase(),
    active: true,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  batch.set(doc(collection(db, 'audit_logs')), {
    actorUid: uid,
    actorName,
    actorRole,
    action: 'DISTRICT_ADDED',
    targetType: 'districts',
    targetId: id,
    details: `Added district ${name} (${code})`,
    reason: '',
    createdAt: serverTimestamp(),
  });
  await batch.commit();
}

export async function addEmployerOrgFs(
  db: Firestore,
  uid: string,
  name: string,
  industry: string,
  contactPerson: string,
  actorName: string,
  actorRole: string
): Promise<void> {
  const orgRef = doc(collection(db, 'organizations'));
  const batch = writeBatch(db);
  batch.set(orgRef, {
    name: name.trim(),
    industry: industry.trim(),
    contactPerson: contactPerson.trim(),
    active: true,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  batch.set(doc(collection(db, 'audit_logs')), {
    actorUid: uid,
    actorName,
    actorRole,
    action: 'ORGANIZATION_ADDED',
    targetType: 'organizations',
    targetId: orgRef.id,
    details: `Added employer organization ${name}`,
    reason: '',
    createdAt: serverTimestamp(),
  });
  await batch.commit();
}

export async function updateAppSettingsFs(
  db: Firestore,
  uid: string,
  settings: AppSettings,
  actorName: string,
  actorRole: string
): Promise<void> {
  const batch = writeBatch(db);
  batch.set(doc(db, 'app_settings', 'global'), {
    maxActivePvCases: Math.max(1, Math.min(20, settings.maxActivePvCases)),
    activeThresholdDays: Math.max(1, Math.min(365, settings.activeThresholdDays)),
    lessActiveThresholdDays: Math.max(1, Math.min(365, settings.lessActiveThresholdDays)),
    inactiveThresholdDays: Math.max(1, Math.min(730, settings.inactiveThresholdDays)),
    emailProviderConfigured: settings.emailProviderConfigured,
    emailSenderName: settings.emailSenderName,
    updatedBy: uid,
    updatedAt: serverTimestamp(),
  });
  batch.set(doc(collection(db, 'audit_logs')), {
    actorUid: uid,
    actorName,
    actorRole,
    action: 'SETTINGS_UPDATED',
    targetType: 'app_settings',
    targetId: 'global',
    details: `Updated maxActivePvCases=${settings.maxActivePvCases}, thresholds=${settings.activeThresholdDays}/${settings.lessActiveThresholdDays}/${settings.inactiveThresholdDays}`,
    reason: '',
    createdAt: serverTimestamp(),
  });
  await batch.commit();
}

export async function markNotificationReadFs(db: Firestore, notifId: string): Promise<void> {
  await updateDoc(doc(db, 'notifications', notifId), { readFlag: true });
}

export async function promoteUserRoleFs(
  db: Firestore,
  uid: string,
  targetUserId: string,
  targetName: string,
  targetEmail: string,
  newRole: UserRole,
  actorName: string
): Promise<void> {
  const batch = writeBatch(db);
  batch.update(doc(db, 'profiles', targetUserId), {
    role: newRole,
    updatedAt: serverTimestamp(),
  });
  if (newRole === 'OFFICE_TEAM') {
    batch.set(doc(db, 'office_team', targetUserId), {
      userId: targetUserId,
      displayName: targetName,
      email: targetEmail || `${targetUserId}@maatram.org`,
      department: 'Volunteer Coordination',
      canApproveHours: true,
      canAllocatePv: true,
      canManageEvents: true,
      active: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  } else if (newRole === 'ADMIN') {
    batch.set(doc(db, 'admins', targetUserId), {
      userId: targetUserId,
      email: targetEmail || `${targetUserId}@maatram.org`,
      grantedBy: uid,
      createdAt: serverTimestamp(),
    });
  }
  batch.set(doc(collection(db, 'audit_logs')), {
    actorUid: uid,
    actorName,
    actorRole: 'ADMIN',
    action: 'ROLE_UPDATED',
    targetType: 'profiles',
    targetId: targetUserId,
    details: `Updated role of ${targetName} to ${newRole}`,
    reason: '',
    createdAt: serverTimestamp(),
  });
  await batch.commit();
}

export async function seedDemoWorkspaceFs(
  db: Firestore,
  actorUid: string,
  actorName: string
): Promise<void> {
  const nowMs = Date.now();
  const daysAgo = (days: number) => Timestamp.fromMillis(nowMs - days * 86400000);

  await setDoc(doc(db, 'app_settings', 'global'), {
    maxActivePvCases: 3,
    activeThresholdDays: 30,
    lessActiveThresholdDays: 60,
    inactiveThresholdDays: 90,
    emailProviderConfigured: false,
    emailSenderName: 'Maatram Foundation (Demo Preview)',
    updatedBy: actorUid,
    updatedAt: serverTimestamp(),
  });

  const batch = writeBatch(db);
  const districts = [
    ['try', 'Tiruchirappalli', 'TRY'],
    ['chn', 'Chennai', 'CHN'],
    ['cbe', 'Coimbatore', 'CBE'],
    ['mdu', 'Madurai', 'MDU'],
    ['slm', 'Salem', 'SLM'],
    ['tnj', 'Thanjavur', 'TNJ'],
    ['erd', 'Erode', 'ERD'],
    ['vlr', 'Vellore', 'VLR'],
  ];
  districts.forEach(([id, name, code]) => {
    batch.set(doc(db, 'districts', `dist_${id}`), {
      name,
      code,
      active: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  });

  const orgs = [
    ['org_maatram_stu', 'Maatram Student Chapter', 'Higher Education'],
    ['org_zoho', 'Zoho Outreach Volunteers', 'Software & SaaS'],
    ['org_tcs', 'TCS Maitree Alumni Wing', 'IT Services'],
    ['org_tvs', 'TVS Motor CSR Network', 'Automotive & Manufacturing'],
    ['org_cognizant', 'Cognizant Outreach Chapter', 'Technology Consulting'],
  ];
  orgs.forEach(([id, name, industry]) => {
    batch.set(doc(db, 'organizations', id), {
      name,
      industry,
      contactPerson: 'Demo CSR Coordinator',
      active: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  });

  const sampleVols = [
    ['demo_vol_01', 'Aravind Krishnan (Demo)', 'MF-DEMO-001', 'ALUMNI', 'Tiruchirappalli', 'TCS Maitree Alumni Wing', 36, 9, 12, 0, 5, true],
    ['demo_vol_02', 'Priyadharshini M (Demo)', 'MF-DEMO-002', 'STUDENT', 'Tiruchirappalli', 'Maatram Student Chapter', 8.5, 2, 2, 0, 18, true],
    ['demo_vol_03', 'Vigneshwaran S (Demo)', 'MF-DEMO-003', 'ALUMNI', 'Tiruchirappalli', 'Zoho Outreach Volunteers', 18, 5, 5, 0, 24, true],
    ['demo_vol_04', 'Harini Natarajan (Demo)', 'MF-DEMO-004', 'STUDENT', 'Tiruchirappalli', 'Maatram Student Chapter', 0, 0, 0, 0, null, true],
    ['demo_vol_05', 'Santhosh Kumar (Demo)', 'MF-DEMO-005', 'STUDENT', 'Tiruchirappalli', 'Maatram Student Chapter', 12, 3, 1, 2, 10, true],
    ['demo_vol_06', 'Nandhini Devi (Demo)', 'MF-DEMO-006', 'ALUMNI', 'Chennai', 'Zoho Outreach Volunteers', 44, 11, 8, 0, 7, true],
    ['demo_vol_07', 'Deepak Rajendran (Demo)', 'MF-DEMO-007', 'STUDENT', 'Chennai', 'Maatram Student Chapter', 4, 1, 0, 0, 42, true],
    ['demo_vol_08', 'Keerthana V (Demo)', 'MF-DEMO-008', 'SUPPORTER', 'Chennai', 'Cognizant Outreach Chapter', 22.5, 6, 3, 1, 14, true],
  ] as const;

  sampleVols.forEach(([uid, name, code, type, district, org, hours, acts, completedPv, activePv, lastDaysAgo, avail]) => {
    const pts = Math.round(hours * 10) + acts * 25;
    const tier = pts >= 800 ? 'PLATINUM' : pts >= 400 ? 'GOLD' : pts >= 150 ? 'SILVER' : 'BRONZE';
    const badges: string[] = [];
    if (acts >= 1) badges.push('First Contribution');
    if (hours >= 10) badges.push('Active Volunteer');
    if (completedPv >= 3) badges.push('Physical Verification Expert');
    if (pts >= 400) badges.push('Outstanding Contributor');

    const statsData: Record<string, unknown> = {
      userId: uid,
      displayName: name,
      volunteerCode: code,
      volunteerType: type,
      district,
      employerOrgName: org,
      availability: avail ? 'Weekends & Evenings' : 'Unavailable',
      availableForPv: avail,
      verifiedHours: hours,
      completedActivities: acts,
      completedPvCases: completedPv,
      activePvCases: activePv,
      points: pts,
      consistencyScore: Math.min(95, acts * 10),
      tier,
      badges,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    };
    if (lastDaysAgo !== null) statsData.lastVerifiedAt = daysAgo(lastDaysAgo);
    batch.set(doc(db, 'volunteer_stats', uid), statsData);
  });

  const sampleOpps = [
    ['demo_opp_pv_try', 'Tiruchirappalli Admission Home PV Drive', 'PHYSICAL_VERIFICATION', 'Physical Verification', 'Tiruchirappalli', 'Srirangam & Woraiyur Zones', 'OFFLINE', '2026-12-06', '09:00', '16:30', 5, 6],
    ['demo_opp_call_bgv', '2026 Batch-A Applicant Calling & BGV', 'CALLING_BGV', 'Calling Verification', 'Chennai', 'Remote Portal', 'ONLINE', '2026-12-01', '10:00', '13:00', 6, 10],
    ['demo_opp_mock_int', 'Final-Year Student Mock Interviews', 'INTERVIEWS', 'Mock Interviews', 'Coimbatore', 'Google Meet', 'ONLINE', '2026-12-07', '10:00', '13:00', 8, 0],
    ['demo_opp_sangamam', 'Maatram Sangamam Annual Gathering (2-Day)', 'EVENTS', 'Sangamam', 'Chennai', 'Chennai Convention Hall', 'OFFLINE', '2026-12-19', '08:30', '17:30', 15, 0],
  ] as const;

  sampleOpps.forEach(([id, title, cat, actType, dist, loc, mode, dt, st, et, reqV, cases]) => {
    batch.set(doc(db, 'opportunities', id), {
      title,
      category: cat,
      activityType: actType,
      description: `${actType} coordinated by Maatram Office Team in ${dist}.`,
      district: dist,
      location: loc,
      mode,
      eventDate: dt,
      startTime: st,
      endTime: et,
      deadline: dt,
      requiredVolunteers: reqV,
      availableSlots: reqV,
      totalCases: cases,
      skillsRequired: 'Communication & Tamil fluency',
      eligibility: 'All Registered Volunteers',
      instructions: 'Coordinate with assigned Office Coordinator.',
      whatsappGroupLink: 'https://chat.whatsapp.com/maatram-demo',
      status: 'OPEN',
      multiDayCount: cat === 'EVENTS' ? 2 : 1,
      createdBy: actorUid,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  });

  batch.set(doc(collection(db, 'audit_logs')), {
    actorUid,
    actorName,
    actorRole: 'ADMIN',
    action: 'DEMO_WORKSPACE_SEEDED',
    targetType: 'workspace',
    targetId: 'demo',
    details: 'Seeded demo districts, organizations, volunteers, and opportunities.',
    reason: '',
    createdAt: serverTimestamp(),
  });

  await batch.commit();
}
