import React, { useMemo } from 'react';
import {
  ArrowRight,
  Briefcase,
  Clock,
  Home,
  PhoneCall,
  Trophy,
} from 'lucide-react';
import {
  TIER_CONFIG,
  type AppNotification,
  type Assignment,
  type CallingTask,
  type Opportunity,
  type PhysicalVerificationCase,
  type RecognitionTier,
  type UserProfile,
  type VolunteerHours,
  type VolunteerRequest,
  type VolunteerStats,
} from '../types/maatram';
import { StatSummaryCard, VolunteerVisualSummaryChart } from './Common';

interface StudentDashboardProps {
  profile: UserProfile | null;
  myStats: VolunteerStats | null;
  allStats: VolunteerStats[];
  opportunities: Opportunity[];
  myRequests: VolunteerRequest[];
  myAssignments: Assignment[];
  myPvCases: PhysicalVerificationCase[];
  callingTasks: CallingTask[];
  myHours: VolunteerHours[];
  notifications: AppNotification[];
  onExploreWorks: () => void;
  onOpenWorkHub: () => void;
  onOpenImpactHub: () => void;
  onOpenProfile: () => void;
}

export const StudentDashboard: React.FC<StudentDashboardProps> = ({
  profile,
  myStats,
  allStats,
  opportunities,
  myRequests,
  myAssignments,
  myPvCases,
  callingTasks,
  myHours,
  notifications,
  onExploreWorks,
  onOpenWorkHub,
  onOpenImpactHub,
  onOpenProfile,
}) => {
  const openWorksCount = opportunities.filter((o) => o.status === 'OPEN').length;
  const myUid = profile?.userId || myStats?.userId || '';

  const callDoneByUser = useMemo(() => {
    const map: Record<string, number> = {};
    callingTasks.forEach((task) => {
      if (
        task.verifiedByOffice ||
        task.callStatus === 'DETAILS_VERIFIED' ||
        task.callStatus === 'COMPLETED' ||
        task.callStatus === 'CALLED'
      ) {
        map[task.assignedToUid] = (map[task.assignedToUid] || 0) + 1;
      }
    });
    return map;
  }, [callingTasks]);

  const myPvDone =
    myStats?.completedPvCases ??
    myPvCases.filter((p) => p.status === 'COMPLETED' || p.status === 'VERIFIED').length;
  const myPvActive =
    myStats?.activePvCases ??
    myPvCases.filter((p) => p.status === 'ASSIGNED' || p.status === 'VISITED').length;
  const myCallDone = callDoneByUser[myUid] || 0;
  const myCallPending = callingTasks.filter(
    (t) =>
      t.assignedToUid === myUid &&
      !t.verifiedByOffice &&
      t.callStatus !== 'DETAILS_VERIFIED' &&
      t.callStatus !== 'COMPLETED' &&
      t.callStatus !== 'CALLED'
  ).length;

  const verifiedHours =
    myStats?.verifiedHours ??
    myHours
      .filter((h) => h.verificationStatus === 'APPROVED')
      .reduce((sum, h) => sum + h.submittedHours, 0);

  const sortedLeaderboard = useMemo(() => {
    return [...allStats].sort((a, b) => {
      if (b.points !== a.points) return b.points - a.points;
      if (b.completedPvCases !== a.completedPvCases) return b.completedPvCases - a.completedPvCases;
      return b.verifiedHours - a.verifiedHours;
    });
  }, [allStats]);

  const rankIdx = sortedLeaderboard.findIndex((s) => s.userId === myUid);
  const rankLabel = rankIdx >= 0 ? `#${rankIdx + 1} / ${sortedLeaderboard.length}` : '-';
  const tierLabel = TIER_CONFIG[(myStats?.tier as RecognitionTier) || 'BRONZE']?.label || 'Bronze';
  const unreadAlerts = notifications.filter((n) => !n.readFlag).length;
  const otherDoneCount = Math.max(0, (myStats?.completedActivities ?? 0) - myPvDone);

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      {/* 1. Primary Top Button: Volunteer Works */}
      <button
        type="button"
        data-testid="volunteer_works_button"
        onClick={onExploreWorks}
        className="flex w-full items-center justify-between gap-4 rounded-2xl bg-maatram-blue px-5 py-4 text-left text-white shadow-sm transition hover:bg-maatram-blueDark"
      >
        <div className="flex items-center gap-3.5 min-w-0">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white/20">
            <Briefcase className="h-5 w-5 text-white" />
          </div>
          <div className="min-w-0">
            <p className="text-base font-bold">Volunteer Works</p>
            <p className="truncate text-xs text-blue-100">
              {openWorksCount} open opportunities • PV, Calling, Interviews &amp; Events
            </p>
          </div>
        </div>
        <ArrowRight className="h-5 w-5 shrink-0 text-white" />
      </button>

      {/* 2. Student Identity Card */}
      <div className="flex items-center justify-between gap-4 rounded-2xl border border-maatram-outline bg-white px-5 py-4 shadow-sm">
        <div className="min-w-0">
          <h2 className="truncate text-lg font-bold text-maatram-navy">
            Vanakkam, {profile?.fullName || 'Student'}
          </h2>
          <p className="mt-0.5 text-sm text-slate-500">
            {profile?.district || 'Tamil Nadu'} • {tierLabel} Tier • {myStats?.points || 0} pts
          </p>
        </div>
        <button
          type="button"
          onClick={onOpenProfile}
          className="shrink-0 rounded-xl px-3.5 py-2 text-sm font-semibold text-maatram-blue transition hover:bg-maatram-blueSoft"
        >
          Profile
        </button>
      </div>

      {/* 3. Work Progress KPI Grid */}
      <div>
        <h3 className="mb-3 text-base font-bold text-maatram-navy">My Work Progress</h3>
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <StatSummaryCard
            title="Physical Verifications"
            value={`${myPvDone} Done`}
            subtitle={`${myPvActive} active PV`}
            icon={<Home className="h-5 w-5" />}
            accent="blue"
          />
          <StatSummaryCard
            title="Call Verifications"
            value={`${myCallDone} Done`}
            subtitle={`${myCallPending} pending calls`}
            icon={<PhoneCall className="h-5 w-5" />}
            accent="green"
          />
          <StatSummaryCard
            title="Verified Hours"
            value={`${verifiedHours.toFixed(1)}h`}
            subtitle={`${myRequests.length} requests sent`}
            icon={<Clock className="h-5 w-5" />}
            accent="blue"
          />
          <StatSummaryCard
            title="Leaderboard Rank"
            value={rankLabel}
            subtitle={`${myStats?.badges?.length ?? 0} badges earned`}
            icon={<Trophy className="h-5 w-5" />}
            accent="blue"
          />
        </div>
      </div>

      {/* 4. Visual Summary Chart calculating Hours & Completed Tasks */}
      <VolunteerVisualSummaryChart
        myHours={myHours}
        pvDone={myPvDone}
        callsDone={myCallDone}
        otherActivitiesDone={otherDoneCount}
      />

      {/* 5. Student Leaderboard Card */}
      <div
        data-testid="student_leaderboard_card"
        className="rounded-2xl border border-maatram-outline bg-white p-5 shadow-sm"
      >
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Trophy className="h-5 w-5 text-maatram-blue" />
            <h3 className="text-base font-bold text-maatram-navy">Student Leaderboard</h3>
          </div>
          <span className="rounded-lg bg-maatram-blueSoft px-2.5 py-1 text-xs font-semibold text-maatram-blue">
            PV &amp; Call Comparison
          </span>
        </div>

        <div className="mt-4 divide-y divide-maatram-outline/60">
          {sortedLeaderboard.length === 0 ? (
            <p className="py-4 text-sm text-slate-500">
              Leaderboard scores will appear as students complete PV and Calling tasks.
            </p>
          ) : (
            sortedLeaderboard.map((st, idx) => {
              const isMe = st.userId === myUid;
              const callsForVol = callDoneByUser[st.userId] || 0;
              return (
                <div
                  key={st.userId}
                  className={`flex items-center justify-between gap-3 py-2.5 ${
                    isMe ? 'rounded-xl bg-maatram-blueSoft px-3' : 'px-1'
                  }`}
                >
                  <div className="flex min-w-0 items-center gap-3">
                    <span
                      className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-xs font-bold ${
                        idx === 0 || isMe
                          ? 'bg-maatram-blue text-white'
                          : 'bg-maatram-blueContainer text-maatram-navy'
                      }`}
                    >
                      #{idx + 1}
                    </span>
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <p className="truncate text-sm font-bold text-maatram-navy">
                          {st.displayName || st.volunteerCode}
                        </p>
                        {isMe && (
                          <span className="rounded-md bg-maatram-blue px-1.5 py-0.5 text-[10px] font-bold text-white">
                            You
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-slate-500">
                        PV: {st.completedPvCases} • Calls: {callsForVol} • {st.verifiedHours.toFixed(1)}h
                      </p>
                    </div>
                  </div>
                  <span className="shrink-0 text-sm font-bold text-maatram-blue">{st.points} pts</span>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* 6. Secondary Quick Navigation */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <button
          type="button"
          onClick={onOpenWorkHub}
          className="h-12 rounded-xl border border-maatram-blue bg-white px-4 text-sm font-semibold text-maatram-blue transition hover:bg-maatram-blueSoft"
        >
          My Active Work ({myAssignments.length})
        </button>
        <button
          type="button"
          onClick={onOpenImpactHub}
          className="h-12 rounded-xl border border-maatram-blue bg-white px-4 text-sm font-semibold text-maatram-blue transition hover:bg-maatram-blueSoft"
        >
          {unreadAlerts > 0 ? `Hours & Alerts (${unreadAlerts})` : 'Hours & Badges'}
        </button>
      </div>
    </div>
  );
};
