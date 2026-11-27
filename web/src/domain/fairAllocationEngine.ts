import {
  AppSettings,
  Assignment,
  CallingTask,
  EngagementRecord,
  EngagementStatus,
  FairAllocationCandidate,
  RecognitionTier,
  TIER_CONFIG,
  VolunteerHours,
  VolunteerRequest,
  VolunteerStats,
} from '../types/maatram';

export interface GamificationSummary {
  totalVerifiedHours: number;
  completedActivities: number;
  points: number;
  consistencyScore: number;
  tier: RecognitionTier;
  badges: string[];
}

export function tierForPoints(points: number): RecognitionTier {
  if (points >= TIER_CONFIG.PLATINUM.minPoints) return 'PLATINUM';
  if (points >= TIER_CONFIG.GOLD.minPoints) return 'GOLD';
  if (points >= TIER_CONFIG.SILVER.minPoints) return 'SILVER';
  return 'BRONZE';
}

export function rankPvCandidates(
  opportunityId: string,
  district: string,
  allStats: VolunteerStats[],
  requestsForOpp: VolunteerRequest[],
  settings: AppSettings,
  nowEpochMillis: number = Date.now()
): FairAllocationCandidate[] {
  const requestedUserIds = new Set(
    requestsForOpp
      .filter((r) => r.opportunityId === opportunityId && r.status !== 'CANCELLED' && r.status !== 'REJECTED')
      .map((r) => r.userId)
  );

  const candidates = allStats
    .filter((s) => s.district.toLowerCase() === district.toLowerCase() || requestedUserIds.has(s.userId))
    .map((stats): FairAllocationCandidate => {
      const hasRequested = requestedUserIds.has(stats.userId);
      const blockedByActiveBatch = stats.activePvCases > 0;
      const atMaxLimit = stats.activePvCases >= settings.maxActivePvCases;

      let score = 100;
      if (stats.completedPvCases === 0) score += 40;
      score -= stats.completedPvCases * 8;
      score -= Math.round(stats.verifiedHours * 0.5);
      if (hasRequested) score += 25;
      if (!stats.availableForPv) score -= 50;
      if (blockedByActiveBatch) score -= 45;
      if (atMaxLimit) score -= 80;

      const lastSeconds = stats.lastVerifiedAt?.seconds;
      if (lastSeconds != null) {
        const daysSince = Math.max(0, Math.floor((nowEpochMillis - lastSeconds * 1000) / 86400000));
        if (daysSince >= 21) score += 15;
      } else {
        score += 20;
      }

      const clampedScore = Math.max(0, Math.min(200, score));

      let priorityLabel: string;
      if (!stats.availableForPv) priorityLabel = 'Unavailable';
      else if (atMaxLimit) priorityLabel = 'Blocked (Max Active)';
      else if (blockedByActiveBatch) priorityLabel = 'Blocked (Active Batch)';
      else if (stats.completedPvCases <= 2) priorityLabel = 'High Priority (Fairness)';
      else if (clampedScore >= 90) priorityLabel = 'Recommended';
      else priorityLabel = 'Standard Pool';

      let recommendedAction: string;
      if (!stats.availableForPv) recommendedAction = 'Volunteer marked unavailable for PV';
      else if (atMaxLimit) recommendedAction = `At max ${settings.maxActivePvCases} active PV cases (override required)`;
      else if (blockedByActiveBatch) recommendedAction = 'Complete active PV batch first (override allowed)';
      else if (stats.completedPvCases === 0) recommendedAction = 'First-time PV priority — assign first';
      else recommendedAction = `Eligible (${stats.completedPvCases} prior PV)`;

      return {
        stats,
        hasRequested,
        priorityScore: clampedScore,
        priorityLabel,
        recommendedAction,
        blockedByActiveBatch,
        atMaxLimit,
      };
    });

  return candidates.sort((a, b) => {
    const aBlocked = a.blockedByActiveBatch || a.atMaxLimit ? 1 : 0;
    const bBlocked = b.blockedByActiveBatch || b.atMaxLimit ? 1 : 0;
    if (aBlocked !== bBlocked) return aBlocked - bBlocked;
    const aAvail = a.stats.availableForPv ? 0 : 1;
    const bAvail = b.stats.availableForPv ? 0 : 1;
    if (aAvail !== bAvail) return aAvail - bAvail;
    if (a.stats.completedPvCases !== b.stats.completedPvCases) {
      return a.stats.completedPvCases - b.stats.completedPvCases;
    }
    if (a.stats.verifiedHours !== b.stats.verifiedHours) {
      return a.stats.verifiedHours - b.stats.verifiedHours;
    }
    if (a.hasRequested !== b.hasRequested) return a.hasRequested ? -1 : 1;
    return b.priorityScore - a.priorityScore;
  });
}

export function calculateDurationHours(startTime: string, endTime: string, breakMinutes: number): number {
  const parseTime = (t: string): number | null => {
    const parts = t.trim().split(':');
    if (parts.length !== 2) return null;
    const h = Number.parseInt(parts[0], 10);
    const m = Number.parseInt(parts[1], 10);
    if (Number.isNaN(h) || Number.isNaN(m) || h < 0 || h > 23 || m < 0 || m > 59) return null;
    return h * 60 + m;
  };
  const s = parseTime(startTime);
  const e = parseTime(endTime);
  if (s == null || e == null) return 1.0;
  let diff = e - s;
  if (diff <= 0) diff += 24 * 60;
  const net = Math.max(15, diff - Math.max(0, breakMinutes));
  return Math.round((net / 60) * 100) / 100;
}

export function computeGamification(
  approvedHours: VolunteerHours[],
  completedPvCases: number,
  previousStats?: VolunteerStats | null
): GamificationSummary {
  const approved = approvedHours.filter((h) => h.verificationStatus === 'APPROVED');
  const totalHours = Math.round(approved.reduce((sum, h) => sum + h.submittedHours, 0) * 100) / 100;
  const completedCount = approved.length;
  const distinctMonths = new Set(approved.map((h) => h.activityDate.slice(0, 7)).filter((m) => m.length === 7)).size;

  const points =
    Math.round(totalHours * 10) + completedCount * 25 + completedPvCases * 20 + distinctMonths * 30;
  const consistency = Math.min(100, distinctMonths * 22 + completedCount * 8);
  const tier = tierForPoints(points);

  const badges = new Set<string>(previousStats?.badges ?? []);
  if (completedCount >= 1) badges.add('First Contribution');
  if (totalHours >= 10) badges.add('Active Volunteer');
  if (distinctMonths >= 2 || consistency >= 60) badges.add('Consistent Volunteer');
  if (totalHours >= 25) badges.add('Community Champion');
  if (completedPvCases >= 3) badges.add('Physical Verification Expert');
  if (approved.filter((h) => h.category === 'CALLING_BGV').length >= 2) badges.add('Calling Support Champion');
  if (approved.filter((h) => h.category === 'EVENTS').length >= 2) badges.add('Event Coordinator');
  if (points >= 400) badges.add('Outstanding Contributor');

  return {
    totalVerifiedHours: totalHours,
    completedActivities: completedCount,
    points,
    consistencyScore: consistency,
    tier,
    badges: Array.from(badges),
  };
}

export function classifyEngagement(
  allStats: VolunteerStats[],
  requests: VolunteerRequest[],
  assignments: Assignment[],
  settings: AppSettings,
  nowEpochMillis: number = Date.now()
): EngagementRecord[] {
  return allStats.map((stats) => {
    const reqCount = requests.filter(
      (r) => r.userId === stats.userId && (r.status === 'PENDING' || r.status === 'UNDER_REVIEW')
    ).length;
    const assignCount = assignments.filter(
      (a) => a.userId === stats.userId && (a.status === 'ASSIGNED' || a.status === 'IN_PROGRESS')
    ).length;

    const lastSec = stats.lastVerifiedAt?.seconds;
    const daysSince =
      lastSec != null ? Math.max(0, Math.floor((nowEpochMillis - lastSec * 1000) / 86400000)) : null;

    let status: EngagementStatus;
    if (!stats.availableForPv && stats.availability.toLowerCase().includes('unavailable')) {
      status = 'UNAVAILABLE';
    } else if (stats.verifiedHours <= 0 && stats.completedActivities === 0) {
      status = stats.availableForPv && assignCount === 0 ? 'AVAILABLE_UNASSIGNED' : 'NO_ACTIVITY';
    } else if (daysSince == null || daysSince <= settings.activeThresholdDays) {
      status = 'ACTIVE';
    } else if (daysSince <= settings.lessActiveThresholdDays) {
      status = 'LESS_ACTIVE';
    } else {
      status = 'INACTIVE';
    }

    return {
      stats,
      status,
      daysSinceLastVerified: daysSince,
      pendingRequestCount: reqCount,
      activeAssignmentCount: assignCount,
    };
  });
}

export function buildThankYouEmailBody(
  volunteerName: string,
  activityTitle: string,
  verifiedHours: number
): string {
  return (
    `Dear ${volunteerName},\n\n` +
    `Thank you for your verified contribution of ${verifiedHours.toFixed(2)} hours toward ` +
    `"${activityTitle}" with Maatram Foundation. Your dedication directly empowers deserving students ` +
    `across Tamil Nadu.\n\nWith gratitude,\nMaatram Foundation Office Team`
  );
}

export function buildLinkedInPost(
  volunteerName: string,
  activityTitle: string,
  categoryLabel: string,
  verifiedHours: number,
  includeHours: boolean,
  includeName: boolean
): string {
  const who = includeName ? volunteerName : 'A Maatram Foundation Volunteer';
  const hrsText = includeHours ? ` (${verifiedHours.toFixed(1)} verified hours)` : '';
  return (
    `Grateful to contribute with Maatram Foundation!\n` +
    `${who} recently completed "${activityTitle}" under ${categoryLabel}${hrsText}, ` +
    `supporting student admissions and mentorship.\n` +
    `#MaatramFoundation #MaatramConnect #StudentEmpowerment #Volunteering`
  );
}

function escapeCsv(value: string): string {
  return `"${value.replace(/"/g, '""').replace(/\n/g, ' ')}"`;
}

export function exportHoursCsv(hours: VolunteerHours[]): string {
  const header = 'Volunteer,Code,Employer,District,Activity,Category,Date,Hours,Status,Reviewer';
  const rows = hours.map((h) =>
    [
      escapeCsv(h.volunteerName),
      escapeCsv(h.volunteerCode),
      escapeCsv(h.employerOrgName),
      escapeCsv(h.district),
      escapeCsv(h.opportunityTitle),
      escapeCsv(h.category),
      escapeCsv(h.activityDate),
      h.submittedHours.toString(),
      escapeCsv(h.verificationStatus),
      escapeCsv(h.reviewerName),
    ].join(',')
  );
  return [header, ...rows].join('\n');
}

export function exportEmployerSummaryCsv(allStats: VolunteerStats[], hours: VolunteerHours[]): string {
  const header = 'Employer Organization,Volunteers,Total Verified Hours,Completed Activities';
  const orgNames = Array.from(
    new Set([...allStats.map((s) => s.employerOrgName), ...hours.map((h) => h.employerOrgName)].filter(Boolean))
  ).sort();

  const rows = orgNames.map((org) => {
    const members = allStats.filter((s) => s.employerOrgName === org);
    const memberHours = members.reduce((sum, s) => sum + s.verifiedHours, 0);
    const verifiedHrs = hours
      .filter((h) => h.employerOrgName === org && h.verificationStatus === 'APPROVED')
      .reduce((sum, h) => sum + h.submittedHours, 0);
    const activities = members.reduce((sum, s) => sum + s.completedActivities, 0);
    return [
      escapeCsv(org),
      members.length.toString(),
      Math.max(memberHours, verifiedHrs).toFixed(2),
      activities.toString(),
    ].join(',');
  });
  return [header, ...rows].join('\n');
}

export function exportCallingTasksCsv(tasks: CallingTask[]): string {
  const header = 'Applicant Ref,District,Date,Assigned To,Call Status,Follow Up,Verified,Notes';
  const rows = tasks.map((t) =>
    [
      escapeCsv(t.applicantRefId),
      escapeCsv(t.district),
      escapeCsv(t.callingDate),
      escapeCsv(t.assignedToName),
      escapeCsv(t.callStatus),
      t.followUpRequired ? 'Yes' : 'No',
      t.verifiedByOffice ? 'Verified' : 'Pending',
      escapeCsv(t.notes),
    ].join(',')
  );
  return [header, ...rows].join('\n');
}

export function downloadCsvFile(filename: string, content: string): void {
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
