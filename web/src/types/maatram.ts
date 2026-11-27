export type UserRole = 'VOLUNTEER' | 'OFFICE_TEAM' | 'ADMIN';
export type VolunteerType = 'STUDENT' | 'ALUMNI' | 'SUPPORTER';
export type OpportunityCategory =
  | 'PHYSICAL_VERIFICATION'
  | 'CALLING_BGV'
  | 'INTERVIEWS'
  | 'EVENTS';
export type OpportunityMode = 'ONLINE' | 'OFFLINE';
export type OpportunityStatus = 'OPEN' | 'CLOSED' | 'COMPLETED';
export type RequestStatus =
  | 'PENDING'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'WAITLISTED'
  | 'CANCELLED';
export type AssignmentStatus = 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'VERIFIED';
export type PvCaseStatus = 'UNASSIGNED' | 'ASSIGNED' | 'VISITED' | 'COMPLETED' | 'VERIFIED';
export type CallStatus =
  | 'NOT_STARTED'
  | 'CALLED'
  | 'NOT_REACHABLE'
  | 'CALL_BACK_REQUIRED'
  | 'DETAILS_VERIFIED'
  | 'DETAILS_NOT_VERIFIED'
  | 'COMPLETED';
export type VerificationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type RecognitionTier = 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM';
export type EngagementStatus =
  | 'ACTIVE'
  | 'LESS_ACTIVE'
  | 'INACTIVE'
  | 'NO_ACTIVITY'
  | 'UNAVAILABLE'
  | 'AVAILABLE_UNASSIGNED';

export const USER_ROLE_LABELS: Record<UserRole, string> = {
  VOLUNTEER: 'Volunteer',
  OFFICE_TEAM: 'Office Team',
  ADMIN: 'Admin',
};

export const VOLUNTEER_TYPE_LABELS: Record<VolunteerType, string> = {
  STUDENT: 'Student',
  ALUMNI: 'Alumni',
  SUPPORTER: 'Supporter',
};

export const CATEGORY_META: Record<
  OpportunityCategory,
  { label: string; shortLabel: string; description: string }
> = {
  PHYSICAL_VERIFICATION: {
    label: 'Physical Verification',
    shortLabel: 'Physical PV',
    description: 'Check PV availability, view assigned PV cases, and request district home visits.',
  },
  CALLING_BGV: {
    label: 'Calling & BGV',
    shortLabel: 'Calling',
    description: 'Call applicants, verify details, and log BGV outcomes.',
  },
  INTERVIEWS: {
    label: 'Mock & Panel Interviews',
    shortLabel: 'Interviews',
    description: 'Join mock interviews and student admission panels.',
  },
  EVENTS: {
    label: 'Events & Coordination',
    shortLabel: 'Events',
    description: 'Support Sangamam, school visits, boot camps, and foundation events.',
  },
};

export const ORDERED_CATEGORIES: OpportunityCategory[] = [
  'PHYSICAL_VERIFICATION',
  'CALLING_BGV',
  'INTERVIEWS',
  'EVENTS',
];

export const CALL_STATUS_LABELS: Record<CallStatus, string> = {
  NOT_STARTED: 'Not Started',
  CALLED: 'Called',
  NOT_REACHABLE: 'Not Reachable',
  CALL_BACK_REQUIRED: 'Call Back Required',
  DETAILS_VERIFIED: 'Details Verified',
  DETAILS_NOT_VERIFIED: 'Details Not Verified',
  COMPLETED: 'Completed',
};

export const PV_STATUS_LABELS: Record<PvCaseStatus, string> = {
  UNASSIGNED: 'Unassigned',
  ASSIGNED: 'Assigned',
  VISITED: 'Visited',
  COMPLETED: 'Completed',
  VERIFIED: 'Verified',
};

export const ASSIGNMENT_STATUS_LABELS: Record<AssignmentStatus, string> = {
  ASSIGNED: 'Assigned',
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed',
  VERIFIED: 'Verified',
};

export const REQUEST_STATUS_LABELS: Record<RequestStatus, string> = {
  PENDING: 'Pending',
  UNDER_REVIEW: 'Under Review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  WAITLISTED: 'Waitlisted',
  CANCELLED: 'Cancelled',
};

export const VERIFICATION_STATUS_LABELS: Record<VerificationStatus, string> = {
  PENDING: 'Pending Review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
};

export const TIER_CONFIG: Record<RecognitionTier, { label: string; minPoints: number }> = {
  BRONZE: { label: 'Bronze', minPoints: 0 },
  SILVER: { label: 'Silver', minPoints: 150 },
  GOLD: { label: 'Gold', minPoints: 400 },
  PLATINUM: { label: 'Platinum', minPoints: 800 },
};

export const ENGAGEMENT_LABELS: Record<EngagementStatus, string> = {
  ACTIVE: 'Active',
  LESS_ACTIVE: 'Less Active',
  INACTIVE: 'Inactive',
  NO_ACTIVITY: 'No Activity Recorded',
  UNAVAILABLE: 'Not Available',
  AVAILABLE_UNASSIGNED: 'Available (No Assignment)',
};

export interface TimestampLike {
  seconds: number;
  nanoseconds?: number;
}

export interface UserProfile {
  id: string;
  userId: string;
  fullName: string;
  email: string;
  phone: string;
  volunteerCode: string;
  volunteerType: VolunteerType | string;
  college: string;
  department: string;
  yearOfStudy: string;
  district: string;
  city: string;
  skills: string;
  interests: string;
  availability: string;
  preferredContact: string;
  employerOrgId: string;
  employerOrgName: string;
  consentDataProcessing: boolean;
  consentLinkedInPublic: boolean;
  profileCompleted: boolean;
  role: UserRole | string;
  createdAt?: TimestampLike | null;
  updatedAt?: TimestampLike | null;
}

export interface VolunteerStats {
  id: string;
  userId: string;
  displayName: string;
  volunteerCode: string;
  volunteerType: VolunteerType | string;
  district: string;
  employerOrgName: string;
  availability: string;
  availableForPv: boolean;
  verifiedHours: number;
  completedActivities: number;
  completedPvCases: number;
  activePvCases: number;
  points: number;
  consistencyScore: number;
  tier: RecognitionTier | string;
  badges: string[];
  lastVerifiedAt?: TimestampLike | null;
  createdAt?: TimestampLike | null;
  updatedAt?: TimestampLike | null;
}

export interface OfficeTeamMember {
  id: string;
  userId: string;
  displayName: string;
  email: string;
  department: string;
  canApproveHours: boolean;
  canAllocatePv: boolean;
  canManageEvents: boolean;
  active: boolean;
}

export interface AppSettings {
  id: string;
  maxActivePvCases: number;
  activeThresholdDays: number;
  lessActiveThresholdDays: number;
  inactiveThresholdDays: number;
  emailProviderConfigured: boolean;
  emailSenderName: string;
  updatedBy: string;
}

export interface DistrictItem {
  id: string;
  name: string;
  code: string;
  active: boolean;
}

export interface EmployerOrganization {
  id: string;
  name: string;
  industry: string;
  contactPerson: string;
  active: boolean;
}

export interface Opportunity {
  id: string;
  title: string;
  category: OpportunityCategory | string;
  activityType: string;
  description: string;
  district: string;
  location: string;
  mode: OpportunityMode | string;
  eventDate: string;
  startTime: string;
  endTime: string;
  deadline: string;
  requiredVolunteers: number;
  availableSlots: number;
  totalCases: number;
  skillsRequired: string;
  eligibility: string;
  instructions: string;
  whatsappGroupLink: string;
  status: OpportunityStatus | string;
  multiDayCount: number;
  createdBy: string;
}

export interface VolunteerRequest {
  id: string;
  userId: string;
  volunteerName: string;
  volunteerCode: string;
  opportunityId: string;
  opportunityTitle: string;
  category: string;
  district: string;
  status: RequestStatus | string;
  rejectionReason: string;
  availabilityNote: string;
  reviewedBy: string;
}

export interface Assignment {
  id: string;
  userId: string;
  volunteerName: string;
  volunteerCode: string;
  opportunityId: string;
  opportunityTitle: string;
  category: string;
  district: string;
  assignedCasesCount: number;
  caseIds: string[];
  status: AssignmentStatus | string;
  progressNotes: string;
  whatsappGroupLink: string;
  overrideReason: string;
  assignedBy: string;
  verifiedBy: string;
}

export interface PhysicalVerificationCase {
  id: string;
  caseRefId: string;
  opportunityId: string;
  district: string;
  areaLabel: string;
  applicantCode: string;
  assignedToUid: string;
  assignedToName: string;
  status: PvCaseStatus | string;
  visitNotes: string;
  recommendationSummary: string;
  verifiedBy: string;
}

export interface CallingTask {
  id: string;
  applicantRefId: string;
  opportunityId: string;
  district: string;
  callingDate: string;
  assignedToUid: string;
  assignedToName: string;
  callStatus: CallStatus | string;
  followUpRequired: boolean;
  notes: string;
  verifiedByOffice: boolean;
}

export interface VolunteerHours {
  id: string;
  userId: string;
  volunteerName: string;
  volunteerCode: string;
  assignmentId: string;
  opportunityId: string;
  opportunityTitle: string;
  category: string;
  district: string;
  employerOrgName: string;
  activityDate: string;
  startTime: string;
  endTime: string;
  breakMinutes: number;
  calculatedHours: number;
  submittedHours: number;
  dayNumber: number;
  notes: string;
  verificationStatus: VerificationStatus | string;
  reviewerUid: string;
  reviewerName: string;
  correctionNote: string;
}

export interface AppNotification {
  id: string;
  userId: string;
  title: string;
  message: string;
  type: string;
  emailMode: string;
  readFlag: boolean;
  createdAt?: TimestampLike | null;
}

export interface AuditLog {
  id: string;
  actorUid: string;
  actorName: string;
  actorRole: string;
  action: string;
  targetType: string;
  targetId: string;
  details: string;
  reason: string;
  createdAt?: TimestampLike | null;
}

export interface FairAllocationCandidate {
  stats: VolunteerStats;
  hasRequested: boolean;
  priorityScore: number;
  priorityLabel: string;
  recommendedAction: string;
  blockedByActiveBatch: boolean;
  atMaxLimit: boolean;
}

export interface EngagementRecord {
  stats: VolunteerStats;
  status: EngagementStatus;
  daysSinceLastVerified: number | null;
  pendingRequestCount: number;
  activeAssignmentCount: number;
}
