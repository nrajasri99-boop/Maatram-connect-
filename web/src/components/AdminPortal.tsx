import React, { useMemo, useState } from 'react';
import { Building2, CheckCircle2, Clock, Download, Users } from 'lucide-react';
import {
  CATEGORY_META,
  ENGAGEMENT_LABELS,
  ORDERED_CATEGORIES,
  type AppSettings,
  type Assignment,
  type AuditLog,
  type DistrictItem,
  type EmployerOrganization,
  type EngagementStatus,
  type Opportunity,
  type UserProfile,
  type UserRole,
  type VolunteerHours,
  type VolunteerRequest,
  type VolunteerStats,
} from '../types/maatram';
import {
  classifyEngagement,
  downloadCsvFile,
  exportEmployerSummaryCsv,
} from '../domain/fairAllocationEngine';
import { BarChartCard, StatSummaryCard, StatusPill } from './Common';

interface AdminPortalProps {
  settings: AppSettings;
  allStats: VolunteerStats[];
  allProfiles: UserProfile[];
  opportunities: Opportunity[];
  requests: VolunteerRequest[];
  assignments: Assignment[];
  volunteerHours: VolunteerHours[];
  districts: DistrictItem[];
  organizations: EmployerOrganization[];
  auditLogs: AuditLog[];
  isBusy: boolean;
  onAddDistrict: (name: string, code: string) => void;
  onAddOrganization: (name: string, industry: string, contact: string) => void;
  onUpdateSettings: (newSettings: AppSettings) => void;
  onPromoteRole: (profile: UserProfile, role: UserRole) => void;
  onSeedDemoData: () => void;
}

export const AdminPortal: React.FC<AdminPortalProps> = ({
  settings,
  allStats,
  allProfiles,
  opportunities,
  requests,
  assignments,
  volunteerHours,
  districts,
  organizations,
  auditLogs,
  isBusy,
  onAddDistrict,
  onAddOrganization,
  onUpdateSettings,
  onPromoteRole,
  onSeedDemoData,
}) => {
  const [selectedTab, setSelectedTab] = useState<'analytics' | 'engagement' | 'employers' | 'settings'>(
    'analytics'
  );
  const [districtFilter, setDistrictFilter] = useState('All');
  const [engagementFilter, setEngagementFilter] = useState<EngagementStatus | null>(null);

  // Form states
  const [orgName, setOrgName] = useState('');
  const [orgIndustry, setOrgIndustry] = useState('Technology');
  const [distName, setDistName] = useState('');
  const [distCode, setDistCode] = useState('');

  const [maxPv, setMaxPv] = useState(settings.maxActivePvCases.toString());
  const [activeDays, setActiveDays] = useState(settings.activeThresholdDays.toString());
  const [lessActiveDays, setLessActiveDays] = useState(
    settings.lessActiveThresholdDays.toString()
  );
  const [inactiveDays, setInactiveDays] = useState(settings.inactiveThresholdDays.toString());
  const [emailConfigured, setEmailConfigured] = useState(settings.emailProviderConfigured);

  const filteredStats = useMemo(
    () => allStats.filter((s) => districtFilter === 'All' || s.district === districtFilter),
    [allStats, districtFilter]
  );

  const approvedHours = useMemo(
    () =>
      volunteerHours.filter(
        (h) =>
          h.verificationStatus === 'APPROVED' &&
          (districtFilter === 'All' || h.district === districtFilter)
      ),
    [volunteerHours, districtFilter]
  );

  const engagementList = useMemo(
    () => classifyEngagement(allStats, requests, assignments, settings),
    [allStats, requests, assignments, settings]
  );

  const orgNames = useMemo(
    () =>
      Array.from(
        new Set(
          [...organizations.map((o) => o.name), ...allStats.map((s) => s.employerOrgName)].filter(
            Boolean
          )
        )
      ).sort(),
    [organizations, allStats]
  );

  const studentsCount = filteredStats.filter((s) => s.volunteerType === 'STUDENT').length;
  const alumniCount = filteredStats.filter((s) => s.volunteerType === 'ALUMNI').length;
  const statsHrs = filteredStats.reduce((sum, s) => sum + s.verifiedHours, 0);
  const totalHours =
    statsHrs > 0 ? statsHrs : approvedHours.reduce((sum, h) => sum + h.submittedHours, 0);
  const completedAssigns = filteredStats.reduce((sum, s) => sum + s.completedActivities, 0);

  const distChart = useMemo(() => {
    const map: Record<string, number> = {};
    allStats.forEach((s) => {
      map[s.district] = (map[s.district] || 0) + 1;
    });
    return Object.entries(map)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 6)
      .map(([k, v]) => ({ label: k, value: v }));
  }, [allStats]);

  const catChart = ORDERED_CATEGORIES.map((cat) => {
    const hrs = approvedHours
      .filter((h) => h.category === cat)
      .reduce((sum, h) => sum + h.submittedHours, 0);
    const fallback = opportunities.filter((o) => o.category === cat).length;
    return {
      label: CATEGORY_META[cat].shortLabel,
      value: hrs > 0 ? hrs : fallback,
      unit: hrs > 0 ? 'h' : ' drives',
    };
  });

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <div className="flex gap-2 overflow-x-auto rounded-2xl border border-maatram-outline bg-white p-1.5 shadow-sm">
        {[
          { key: 'analytics', label: 'Analytics' },
          { key: 'engagement', label: 'Engagement' },
          { key: 'employers', label: 'Employers' },
          { key: 'settings', label: 'Settings & Audit' },
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

      {selectedTab === 'analytics' && (
        <div className="space-y-5">
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setDistrictFilter('All')}
              className={`rounded-xl border px-3 py-1.5 text-xs font-semibold ${
                districtFilter === 'All'
                  ? 'border-maatram-blue bg-maatram-blue text-white'
                  : 'border-maatram-outline bg-white text-maatram-navy'
              }`}
            >
              All Districts
            </button>
            {districts.map((d) => (
              <button
                key={d.id}
                type="button"
                onClick={() => setDistrictFilter(d.name)}
                className={`rounded-xl border px-3 py-1.5 text-xs font-semibold ${
                  districtFilter === d.name
                    ? 'border-maatram-blue bg-maatram-blue text-white'
                    : 'border-maatram-outline bg-white text-maatram-navy'
                }`}
              >
                {d.name}
              </button>
            ))}
          </div>

          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <StatSummaryCard
              title="Volunteers"
              value={`${filteredStats.length}`}
              subtitle={`${studentsCount} Students • ${alumniCount} Alumni`}
              icon={<Users className="h-5 w-5" />}
              accent="blue"
            />
            <StatSummaryCard
              title="Verified Hours"
              value={`${totalHours.toFixed(1)}h`}
              subtitle={`Across ${opportunities.length} opps`}
              icon={<Clock className="h-5 w-5" />}
              accent="green"
            />
            <StatSummaryCard
              title="Completed Work"
              value={`${completedAssigns}`}
              subtitle={`${requests.length} total requests`}
              icon={<CheckCircle2 className="h-5 w-5" />}
              accent="blue"
            />
            <StatSummaryCard
              title="Employer Orgs"
              value={`${organizations.length}`}
              subtitle="Active CSR partners"
              icon={<Building2 className="h-5 w-5" />}
              accent="orange"
            />
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <BarChartCard
              title="District-Wise Volunteer Distribution"
              subtitle="Registered volunteers per district"
              items={distChart}
            />
            <BarChartCard
              title="Category-Wise Contributions"
              subtitle="Verified hours / active opportunities by category"
              items={catChart}
              barColorClass="bg-maatram-green"
            />
          </div>
        </div>
      )}

      {selectedTab === 'engagement' && (
        <div className="space-y-4">
          <p className="text-xs text-slate-600">
            Thresholds: Active &le; {settings.activeThresholdDays}d | Less Active &le;{' '}
            {settings.lessActiveThresholdDays}d | Inactive &le; {settings.inactiveThresholdDays}d
          </p>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setEngagementFilter(null)}
              className={`rounded-xl border px-3 py-1.5 text-xs font-semibold ${
                engagementFilter === null
                  ? 'border-maatram-blue bg-maatram-blue text-white'
                  : 'border-maatram-outline bg-white text-maatram-navy'
              }`}
            >
              All ({engagementList.length})
            </button>
            {(Object.keys(ENGAGEMENT_LABELS) as EngagementStatus[]).map((st) => {
              const count = engagementList.filter((r) => r.status === st).length;
              return (
                <button
                  key={st}
                  type="button"
                  onClick={() => setEngagementFilter((prev) => (prev === st ? null : st))}
                  className={`rounded-xl border px-3 py-1.5 text-xs font-semibold ${
                    engagementFilter === st
                      ? 'border-maatram-blue bg-maatram-blue text-white'
                      : 'border-maatram-outline bg-white text-maatram-navy'
                  }`}
                >
                  {ENGAGEMENT_LABELS[st]} ({count})
                </button>
              );
            })}
          </div>
          <div className="space-y-2.5">
            {engagementList
              .filter((r) => engagementFilter === null || r.status === engagementFilter)
              .map((rec) => (
                <div
                  key={rec.stats.userId}
                  className="flex items-center justify-between gap-3 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm"
                >
                  <div>
                    <h4 className="font-bold text-maatram-navy">{rec.stats.displayName}</h4>
                    <p className="text-xs text-slate-500">
                      {rec.stats.district} • Last verified:{' '}
                      {rec.daysSinceLastVerified != null
                        ? `${rec.daysSinceLastVerified}d ago`
                        : 'Never'}{' '}
                      • Pending req: {rec.pendingRequestCount}
                    </p>
                  </div>
                  <StatusPill status={ENGAGEMENT_LABELS[rec.status]} />
                </div>
              ))}
          </div>
        </div>
      )}

      {selectedTab === 'employers' && (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h3 className="font-bold text-maatram-navy">Employer Organization Reports</h3>
            <button
              type="button"
              onClick={() =>
                downloadCsvFile(
                  'maatram_employer_report.csv',
                  exportEmployerSummaryCsv(allStats, volunteerHours)
                )
              }
              className="flex items-center gap-1.5 rounded-xl border border-maatram-blue px-3.5 py-2 text-xs font-semibold text-maatram-blue hover:bg-maatram-blueSoft"
            >
              <Download className="h-4 w-4" />
              Export Employer CSV
            </button>
          </div>

          <div className="space-y-3 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm">
            <h4 className="font-bold text-maatram-navy">Add Employer Organization</h4>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <input
                type="text"
                value={orgName}
                onChange={(e) => setOrgName(e.target.value)}
                placeholder="Org Name"
                className="rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
              />
              <input
                type="text"
                value={orgIndustry}
                onChange={(e) => setOrgIndustry(e.target.value)}
                placeholder="Industry"
                className="rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
              />
            </div>
            <button
              type="button"
              onClick={() => {
                if (orgName.trim().length >= 2) {
                  onAddOrganization(orgName.trim(), orgIndustry.trim(), 'HR Outreach');
                  setOrgName('');
                }
              }}
              className="h-10 rounded-xl bg-maatram-blue px-5 text-xs font-bold text-white"
            >
              Save Organization
            </button>
          </div>

          <div className="space-y-2.5">
            {orgNames.map((org) => {
              const linked = allStats.filter((s) => s.employerOrgName === org);
              const hrs = linked.reduce((sum, s) => sum + s.verifiedHours, 0);
              const acts = linked.reduce((sum, s) => sum + s.completedActivities, 0);
              return (
                <div
                  key={org}
                  className="space-y-1 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm"
                >
                  <div className="flex items-center justify-between">
                    <h4 className="font-bold text-maatram-navy">{org}</h4>
                    <StatusPill status={`${linked.length} VOLUNTEERS`} />
                  </div>
                  <p className="text-sm text-slate-600">
                    Verified Hours: {hrs.toFixed(1)}h • Completed Activities: {acts}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {selectedTab === 'settings' && (
        <div className="space-y-4">
          <div className="space-y-2.5 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm">
            <h4 className="font-bold text-maatram-navy">Demo Dataset &amp; Initial Workspace Setup</h4>
            <p className="text-xs text-slate-600">
              Populates sample volunteers, Tamil Nadu districts, opportunities across all 4
              categories, PV cases, and calling tasks.
            </p>
            <button
              type="button"
              disabled={isBusy}
              data-testid="seed_demo_button"
              onClick={onSeedDemoData}
              className="h-11 w-full rounded-xl bg-maatram-blue text-sm font-bold text-white hover:bg-maatram-blueDark disabled:opacity-50"
            >
              Seed Fictional Demo Workspace
            </button>
          </div>

          <div className="space-y-3 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm">
            <h4 className="font-bold text-maatram-navy">Allocation &amp; Engagement Thresholds</h4>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-maatram-navy">
                  Max Active PV
                </label>
                <input
                  type="number"
                  value={maxPv}
                  onChange={(e) => setMaxPv(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-maatram-outline p-2 text-sm text-black"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-maatram-navy">Active (d)</label>
                <input
                  type="number"
                  value={activeDays}
                  onChange={(e) => setActiveDays(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-maatram-outline p-2 text-sm text-black"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-maatram-navy">
                  Less Active (d)
                </label>
                <input
                  type="number"
                  value={lessActiveDays}
                  onChange={(e) => setLessActiveDays(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-maatram-outline p-2 text-sm text-black"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-maatram-navy">
                  Inactive (d)
                </label>
                <input
                  type="number"
                  value={inactiveDays}
                  onChange={(e) => setInactiveDays(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-maatram-outline p-2 text-sm text-black"
                />
              </div>
            </div>
            <label className="flex items-center justify-between text-sm text-maatram-navy">
              <span>External Email API Configured (else Demo Preview)</span>
              <input
                type="checkbox"
                checked={emailConfigured}
                onChange={(e) => setEmailConfigured(e.target.checked)}
                className="h-4 w-4 accent-maatram-blue"
              />
            </label>
            <button
              type="button"
              onClick={() =>
                onUpdateSettings({
                  ...settings,
                  maxActivePvCases: Number.parseInt(maxPv, 10) || 3,
                  activeThresholdDays: Number.parseInt(activeDays, 10) || 30,
                  lessActiveThresholdDays: Number.parseInt(lessActiveDays, 10) || 60,
                  inactiveThresholdDays: Number.parseInt(inactiveDays, 10) || 90,
                  emailProviderConfigured: emailConfigured,
                })
              }
              className="h-10 rounded-xl bg-maatram-blue px-5 text-xs font-bold text-white"
            >
              Save Settings
            </button>
          </div>

          <div className="space-y-3 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm">
            <h4 className="font-bold text-maatram-navy">
              District Management ({districts.length} Active)
            </h4>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <input
                type="text"
                value={distName}
                onChange={(e) => setDistName(e.target.value)}
                placeholder="District Name"
                className="rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
              />
              <input
                type="text"
                value={distCode}
                onChange={(e) => setDistCode(e.target.value)}
                placeholder="Code (e.g., KRR)"
                className="rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
              />
            </div>
            <button
              type="button"
              onClick={() => {
                if (distName.trim().length >= 2 && distCode.trim().length >= 2) {
                  onAddDistrict(distName.trim(), distCode.trim());
                  setDistName('');
                  setDistCode('');
                }
              }}
              className="h-10 rounded-xl bg-maatram-blue px-5 text-xs font-bold text-white"
            >
              Add District
            </button>
          </div>

          {allProfiles.length > 0 && (
            <div className="space-y-2.5">
              <h4 className="font-bold text-maatram-navy">User &amp; Role Management</h4>
              {allProfiles.slice(0, 10).map((prof) => (
                <div
                  key={prof.userId}
                  className="flex items-center justify-between gap-3 rounded-2xl border border-maatram-outline bg-white p-3.5 shadow-sm"
                >
                  <div>
                    <p className="font-bold text-maatram-navy">{prof.fullName}</p>
                    <p className="text-xs text-slate-500">
                      {prof.volunteerCode} • {prof.role}
                    </p>
                  </div>
                  {prof.role === 'VOLUNTEER' ? (
                    <button
                      type="button"
                      onClick={() => onPromoteRole(prof, 'OFFICE_TEAM')}
                      className="rounded-xl border border-maatram-blue px-3 py-1.5 text-xs font-semibold text-maatram-blue hover:bg-maatram-blueSoft"
                    >
                      Make Office
                    </button>
                  ) : (
                    <StatusPill status={prof.role} />
                  )}
                </div>
              ))}
            </div>
          )}

          <div className="space-y-2.5">
            <h4 className="font-bold text-maatram-navy">
              Immutable Audit Logs ({auditLogs.length})
            </h4>
            {[...auditLogs]
              .sort((a, b) => (b.createdAt?.seconds ?? 0) - (a.createdAt?.seconds ?? 0))
              .slice(0, 20)
              .map((log) => (
                <div
                  key={log.id}
                  className="space-y-1 rounded-2xl border border-maatram-outline bg-white p-3.5 shadow-sm"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-maatram-navy">{log.action}</span>
                    <StatusPill status={log.actorRole} />
                  </div>
                  <p className="text-sm text-slate-700">{log.details}</p>
                  {log.reason && <p className="text-xs text-maatram-red">Reason: {log.reason}</p>}
                  <p className="text-xs text-slate-400">By {log.actorName}</p>
                </div>
              ))}
          </div>
        </div>
      )}
    </div>
  );
};
