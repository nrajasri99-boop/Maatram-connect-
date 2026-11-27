import React, { useMemo, useState } from 'react';
import { Award, BellRing, Copy, Share2 } from 'lucide-react';
import {
  CATEGORY_META,
  ORDERED_CATEGORIES,
  TIER_CONFIG,
  type AppNotification,
  type OpportunityCategory,
  type RecognitionTier,
  type UserProfile,
  type VolunteerHours,
  type VolunteerStats,
} from '../types/maatram';
import { buildLinkedInPost } from '../domain/fairAllocationEngine';
import {
  BarChartCard,
  CategoryBadge,
  EmptyStateCard,
  StatusPill,
  VolunteerVisualSummaryChart,
} from './Common';

interface VolunteerImpactPageProps {
  profile: UserProfile | null;
  myStats: VolunteerStats | null;
  allStats: VolunteerStats[];
  myHours: VolunteerHours[];
  notifications: AppNotification[];
  onMarkNotificationRead: (id: string) => void;
  onShowMessage: (msg: string) => void;
}

const ALL_BADGES = [
  'First Contribution',
  'Active Volunteer',
  'Consistent Volunteer',
  'Community Champion',
  'Physical Verification Expert',
  'Calling Support Champion',
  'Event Coordinator',
  'Outstanding Contributor',
];

export const VolunteerImpactPage: React.FC<VolunteerImpactPageProps> = ({
  profile,
  myStats,
  allStats,
  myHours,
  notifications,
  onMarkNotificationRead,
  onShowMessage,
}) => {
  const [selectedTab, setSelectedTab] = useState<'hours' | 'badges' | 'leaderboard' | 'linkedin'>(
    'hours'
  );
  const [districtFilter, setDistrictFilter] = useState('All');
  const [consentShare, setConsentShare] = useState(profile?.consentLinkedInPublic ?? false);
  const [includeHours, setIncludeHours] = useState(true);

  const approvedHours = useMemo(
    () => myHours.filter((h) => h.verificationStatus === 'APPROVED'),
    [myHours]
  );

  const latestApproved = approvedHours[0];
  const defaultPostText = useMemo(
    () =>
      buildLinkedInPost(
        profile?.fullName || 'Maatram Volunteer',
        latestApproved?.opportunityTitle || 'Maatram Foundation Community Drive',
        CATEGORY_META[(latestApproved?.category as OpportunityCategory) || 'EVENTS']?.label ||
          'Volunteering',
        latestApproved?.submittedHours ?? myStats?.verifiedHours ?? 0,
        includeHours,
        consentShare
      ),
    [profile?.fullName, latestApproved, myStats?.verifiedHours, includeHours, consentShare]
  );

  const [customPostText, setCustomPostText] = useState<string | null>(null);
  const displayedPostText = customPostText ?? defaultPostText;

  const catBreakdown = ORDERED_CATEGORIES.map((cat) => ({
    label: CATEGORY_META[cat].shortLabel,
    value: approvedHours
      .filter((h) => h.category === cat)
      .reduce((sum, h) => sum + h.submittedHours, 0),
    unit: 'h',
  }));

  const tierKey = (myStats?.tier as RecognitionTier) || 'BRONZE';
  const tier = TIER_CONFIG[tierKey] || TIER_CONFIG.BRONZE;
  const points = myStats?.points ?? 0;
  const nextTarget =
    tierKey === 'BRONZE'
      ? TIER_CONFIG.SILVER.minPoints
      : tierKey === 'SILVER'
      ? TIER_CONFIG.GOLD.minPoints
      : TIER_CONFIG.PLATINUM.minPoints;
  const progressPct = Math.min(100, Math.round((points / Math.max(1, nextTarget)) * 100));

  const leaderboardDistricts = useMemo(
    () => ['All', ...Array.from(new Set(allStats.map((s) => s.district))).sort()],
    [allStats]
  );

  const rankedLeaderboard = useMemo(
    () =>
      allStats
        .filter((s) => districtFilter === 'All' || s.district === districtFilter)
        .sort((a, b) =>
          b.points !== a.points ? b.points - a.points : b.verifiedHours - a.verifiedHours
        ),
    [allStats, districtFilter]
  );

  const handleCopyPost = async () => {
    if (!consentShare) {
      onShowMessage('Please grant explicit consent above before copying or sharing.');
      return;
    }
    try {
      await navigator.clipboard.writeText(displayedPostText);
      onShowMessage('Appreciation post copied to clipboard!');
    } catch {
      onShowMessage('Copy failed — please select and copy the text manually.');
    }
  };

  const handleSharePost = async () => {
    if (!consentShare) {
      onShowMessage('Please grant explicit consent above before sharing.');
      return;
    }
    if (navigator.share) {
      try {
        await navigator.share({
          title: 'Maatram Foundation Appreciation',
          text: displayedPostText,
        });
        return;
      } catch {
        // Fallback to LinkedIn share URL
      }
    }
    const url = `https://www.linkedin.com/feed/?shareActive=true&text=${encodeURIComponent(
      displayedPostText
    )}`;
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      {/* Sub-tabs */}
      <div className="flex gap-2 overflow-x-auto rounded-2xl border border-maatram-outline bg-white p-1.5 shadow-sm">
        {[
          { key: 'hours', label: `Hours (${myHours.length})` },
          { key: 'badges', label: 'Badges' },
          { key: 'leaderboard', label: 'Leaderboard' },
          { key: 'linkedin', label: 'LinkedIn & Alerts' },
        ].map((t) => (
          <button
            key={t.key}
            type="button"
            onClick={() => setSelectedTab(t.key as typeof selectedTab)}
            className={`flex-1 whitespace-nowrap rounded-xl px-4 py-2.5 text-sm font-semibold transition ${
              selectedTab === t.key
                ? 'bg-maatram-blue text-white'
                : 'text-maatram-navy hover:bg-maatram-blueSoft'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {selectedTab === 'hours' && (
        <div className="space-y-4">
          <VolunteerVisualSummaryChart
            myHours={myHours}
            pvDone={myStats?.completedPvCases ?? 0}
            callsDone={approvedHours.filter((h) => h.category === 'CALLING_BGV').length}
            otherActivitiesDone={Math.max(
              0,
              (myStats?.completedActivities ?? 0) - (myStats?.completedPvCases ?? 0)
            )}
          />
          <BarChartCard
            title="Category-Wise Verified Hours"
            subtitle={`Total Approved: ${approvedHours
              .reduce((sum, h) => sum + h.submittedHours, 0)
              .toFixed(1)}h`}
            items={catBreakdown}
          />
          {myHours.length === 0 ? (
            <EmptyStateCard
              title="No Hours Logged Yet"
              message="Log hours from My Work after completing an assignment."
            />
          ) : (
            myHours.map((hr) => (
              <div
                key={hr.id}
                className="space-y-2 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm"
              >
                <div className="flex items-center justify-between gap-2">
                  <CategoryBadge category={hr.category} />
                  <StatusPill status={hr.verificationStatus} />
                </div>
                <h4 className="font-bold text-maatram-navy">{hr.opportunityTitle}</h4>
                <p className="text-sm text-slate-600">
                  Date: {hr.activityDate} (Day {hr.dayNumber}) • {hr.submittedHours} hrs
                </p>
                {hr.reviewerName && (
                  <p className="text-xs text-slate-500">Reviewed by: {hr.reviewerName}</p>
                )}
                {hr.correctionNote && (
                  <p className="text-xs font-medium text-maatram-blue">
                    Reviewer Note: {hr.correctionNote}
                  </p>
                )}
              </div>
            ))
          )}
        </div>
      )}

      {selectedTab === 'badges' && (
        <div className="space-y-4">
          <div className="space-y-2 rounded-2xl border border-maatram-blueContainer bg-maatram-blueSoft p-5">
            <h3 className="text-lg font-bold text-maatram-navy">
              {tier.label} Volunteer • {points} Points
            </h3>
            <p className="text-sm text-maatram-navy">
              Consistency Score: {myStats?.consistencyScore ?? 0}/100
            </p>
            <div className="h-2.5 w-full overflow-hidden rounded-full bg-white">
              <div
                className="h-full rounded-full bg-maatram-blue transition-all"
                style={{ width: `${progressPct}%` }}
              />
            </div>
            <p className="text-xs text-slate-600">
              Points and badges update strictly after Office Team verification.
            </p>
          </div>

          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            {ALL_BADGES.map((badge) => {
              const unlocked = myStats?.badges?.includes(badge) ?? false;
              return (
                <div
                  key={badge}
                  className={`flex items-center justify-between rounded-2xl border p-4 ${
                    unlocked
                      ? 'border-maatram-blue bg-maatram-blueContainer'
                      : 'border-maatram-outline bg-white'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <Award
                      className={`h-5 w-5 ${unlocked ? 'text-maatram-blue' : 'text-slate-400'}`}
                    />
                    <span className="text-sm font-bold text-maatram-navy">{badge}</span>
                  </div>
                  <StatusPill status={unlocked ? 'VERIFIED' : 'LOCKED'} />
                </div>
              );
            })}
          </div>
        </div>
      )}

      {selectedTab === 'leaderboard' && (
        <div className="space-y-4">
          <div className="flex flex-wrap gap-2">
            {leaderboardDistricts.map((d) => (
              <button
                key={d}
                type="button"
                onClick={() => setDistrictFilter(d)}
                className={`rounded-xl border px-3 py-1.5 text-xs font-semibold ${
                  districtFilter === d
                    ? 'border-maatram-blue bg-maatram-blue text-white'
                    : 'border-maatram-outline bg-white text-maatram-navy hover:bg-maatram-blueSoft'
                }`}
              >
                {d}
              </button>
            ))}
          </div>
          {rankedLeaderboard.map((st, idx) => {
            const isMe = st.userId === profile?.userId;
            return (
              <div
                key={st.userId}
                className={`flex items-center justify-between rounded-2xl border p-4 ${
                  isMe
                    ? 'border-maatram-blue bg-maatram-blueSoft'
                    : 'border-maatram-outline bg-white'
                }`}
              >
                <div>
                  <p className="font-bold text-maatram-navy">
                    #{idx + 1} {st.displayName}
                  </p>
                  <p className="text-xs text-slate-500">
                    {st.district} • {st.completedActivities} activities •{' '}
                    {st.verifiedHours.toFixed(1)}h
                  </p>
                </div>
                <span className="rounded-lg bg-maatram-blueContainer px-3 py-1 text-xs font-bold text-maatram-navy">
                  {st.points} pts
                </span>
              </div>
            );
          })}
        </div>
      )}

      {selectedTab === 'linkedin' && (
        <div className="space-y-5">
          <div className="space-y-3 rounded-2xl border border-maatram-outline bg-white p-5 shadow-sm">
            <h3 className="text-base font-bold text-maatram-navy">
              LinkedIn Appreciation Post Generator
            </h3>
            <label className="flex items-center gap-2.5 text-sm text-maatram-navy">
              <input
                type="checkbox"
                checked={consentShare}
                onChange={(e) => {
                  setConsentShare(e.target.checked);
                  setCustomPostText(null);
                }}
                className="h-4 w-4 accent-maatram-blue"
              />
              <span>I give explicit consent to publish my name in this appreciation post.</span>
            </label>
            <label className="flex items-center gap-2.5 text-sm text-maatram-navy">
              <input
                type="checkbox"
                checked={includeHours}
                onChange={(e) => {
                  setIncludeHours(e.target.checked);
                  setCustomPostText(null);
                }}
                className="h-4 w-4 accent-maatram-blue"
              />
              <span>Include verified volunteer hours</span>
            </label>
            <textarea
              rows={4}
              value={displayedPostText}
              onChange={(e) => setCustomPostText(e.target.value)}
              className="w-full rounded-xl border border-maatram-outline p-3 text-sm text-black outline-none focus:border-maatram-blue"
            />
            <div className="flex gap-3">
              <button
                type="button"
                onClick={handleCopyPost}
                className="flex h-11 flex-1 items-center justify-center gap-2 rounded-xl border border-maatram-blue text-sm font-semibold text-maatram-blue hover:bg-maatram-blueSoft"
              >
                <Copy className="h-4 w-4" />
                Copy Post
              </button>
              <button
                type="button"
                onClick={handleSharePost}
                className="flex h-11 flex-1 items-center justify-center gap-2 rounded-xl bg-maatram-blue text-sm font-bold text-white hover:bg-maatram-blueDark"
              >
                <Share2 className="h-4 w-4" />
                Share
              </button>
            </div>
          </div>

          <div className="space-y-3">
            <h3 className="text-base font-bold text-maatram-navy">
              In-App Notifications &amp; Email Dispatch Log
            </h3>
            {notifications.length === 0 ? (
              <EmptyStateCard
                title="No Notifications Yet"
                message="Approvals, assignments, and automated thank-you emails appear here."
              />
            ) : (
              [...notifications]
                .sort((a, b) => (b.createdAt?.seconds ?? 0) - (a.createdAt?.seconds ?? 0))
                .map((notif) => (
                  <div
                    key={notif.id}
                    className={`space-y-2 rounded-2xl border p-4 ${
                      notif.readFlag
                        ? 'border-maatram-outline bg-white'
                        : 'border-maatram-blueContainer bg-maatram-blueSoft'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <BellRing className="h-4 w-4 text-maatram-blue" />
                        <h4 className="text-sm font-bold text-maatram-navy">{notif.title}</h4>
                      </div>
                      <StatusPill status={notif.emailMode} />
                    </div>
                    <p className="text-sm text-slate-600">{notif.message}</p>
                    {!notif.readFlag && (
                      <button
                        type="button"
                        onClick={() => onMarkNotificationRead(notif.id)}
                        className="text-xs font-semibold text-maatram-blue hover:underline"
                      >
                        Mark as Read
                      </button>
                    )}
                  </div>
                ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};
