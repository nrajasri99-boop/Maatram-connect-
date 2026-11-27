import React, { useMemo, useState } from 'react';
import {
  CheckCircle2,
  ClipboardCheck,
  Clock,
  Download,
  Home,
  Plus,
  Users,
  Vote,
} from 'lucide-react';
import {
  CATEGORY_META,
  ORDERED_CATEGORIES,
  type AppSettings,
  type Assignment,
  type CallingTask,
  type DistrictItem,
  type FairAllocationCandidate,
  type Opportunity,
  type OpportunityCategory,
  type OpportunityMode,
  type OpportunityStatus,
  type PhysicalVerificationCase,
  type RequestStatus,
  type VerificationStatus,
  type VolunteerHours,
  type VolunteerRequest,
  type VolunteerStats,
} from '../types/maatram';
import {
  downloadCsvFile,
  exportCallingTasksCsv,
  exportHoursCsv,
  rankPvCandidates,
} from '../domain/fairAllocationEngine';
import {
  BarChartCard,
  CategoryBadge,
  EmptyStateCard,
  ModalDialog,
  StatSummaryCard,
  StatusPill,
} from './Common';

interface OfficePortalProps {
  section: 'overview' | 'fair_alloc';
  settings: AppSettings;
  allStats: VolunteerStats[];
  opportunities: Opportunity[];
  requests: VolunteerRequest[];
  assignments: Assignment[];
  pvCases: PhysicalVerificationCase[];
  callingTasks: CallingTask[];
  volunteerHours: VolunteerHours[];
  districts: DistrictItem[];
  currentUserId: string;
  isBusy: boolean;
  onCreateOpportunity: (opp: Omit<Opportunity, 'id'>, pvCount: number) => void;
  onToggleOppStatus: (opp: Opportunity, status: OpportunityStatus) => void;
  onReviewRequest: (
    req: VolunteerRequest,
    opp: Opportunity,
    decision: RequestStatus,
    reason: string
  ) => void;
  onCastPollVote: (opp: Opportunity) => void;
  onCancelPollVote: (req: VolunteerRequest) => void;
  onOpenFairAllocation: () => void;
  onAllocatePvCases: (
    opp: Opportunity,
    volStats: VolunteerStats,
    cases: PhysicalVerificationCase[],
    overrideReason: string
  ) => void;
  onVerifyPvCase: (pv: PhysicalVerificationCase) => void;
  onVerifyAssignment: (assign: Assignment) => void;
  onImportCallingSheet: (opp: Opportunity, volStats: VolunteerStats) => void;
  onVerifyCallingTask: (taskId: string) => void;
  onReviewHours: (
    hr: VolunteerHours,
    status: VerificationStatus,
    finalHours: number,
    note: string
  ) => void;
}

export const OfficePortal: React.FC<OfficePortalProps> = ({
  section,
  settings,
  allStats,
  opportunities,
  requests,
  assignments,
  pvCases,
  callingTasks,
  volunteerHours,
  districts,
  currentUserId,
  isBusy,
  onCreateOpportunity,
  onToggleOppStatus,
  onReviewRequest,
  onCastPollVote,
  onCancelPollVote,
  onOpenFairAllocation,
  onAllocatePvCases,
  onVerifyPvCase,
  onVerifyAssignment,
  onImportCallingSheet,
  onVerifyCallingTask,
  onReviewHours,
}) => {
  const [overviewTab, setOverviewTab] = useState<'overview' | 'categories' | 'requests'>(
    'overview'
  );
  const [allocTab, setAllocTab] = useState<'fair_pv' | 'verify' | 'hours'>('fair_pv');
  const [selectedWorkCat, setSelectedWorkCat] =
    useState<OpportunityCategory>('PHYSICAL_VERIFICATION');
  const [reqFilter, setReqFilter] = useState<string>('PENDING');

  // Modals
  const [showCreateOpp, setShowCreateOpp] = useState(false);
  const [rejectTarget, setRejectTarget] = useState<{
    req: VolunteerRequest;
    opp: Opportunity;
  } | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  // Create opportunity state
  const districtNames =
    districts.length > 0
      ? districts.map((d) => d.name).sort()
      : ['Tiruchirappalli', 'Chennai', 'Coimbatore', 'Madurai', 'Salem'];

  const [oppTitle, setOppTitle] = useState('');
  const [oppCategory, setOppCategory] = useState<OpportunityCategory>('PHYSICAL_VERIFICATION');
  const [oppDistrict, setOppDistrict] = useState(districtNames[0]);
  const [oppLocation, setOppLocation] = useState('');
  const [oppMode, setOppMode] = useState<OpportunityMode>('OFFLINE');
  const [oppDate, setOppDate] = useState('2026-12-15');
  const [oppStart, setOppStart] = useState('09:00');
  const [oppEnd, setOppEnd] = useState('16:00');
  const [oppSlots, setOppSlots] = useState('4');
  const [oppPvCount, setOppPvCount] = useState('6');
  const [oppWhatsapp, setOppWhatsapp] = useState('https://chat.whatsapp.com/maatram-invite');
  const [oppInstructions, setOppInstructions] = useState('');

  // Fair PV Allocation state
  const pvOpps = useMemo(
    () => opportunities.filter((o) => o.category === 'PHYSICAL_VERIFICATION'),
    [opportunities]
  );
  const [selectedPvOppId, setSelectedPvOppId] = useState('');
  const activePvOpp = pvOpps.find((o) => o.id === selectedPvOppId) || pvOpps[0] || null;

  const [allocateTarget, setAllocateTarget] = useState<FairAllocationCandidate | null>(null);
  const [selectedCaseIds, setSelectedCaseIds] = useState<Set<string>>(new Set());
  const [overrideReason, setOverrideReason] = useState('');

  // Hours verification state
  const [reviewHourTarget, setReviewHourTarget] = useState<VolunteerHours | null>(null);
  const [adjustedHours, setAdjustedHours] = useState('');
  const [correctionNote, setCorrectionNote] = useState('');

  const pendingReqsCount = requests.filter(
    (r) => r.status === 'PENDING' || r.status === 'UNDER_REVIEW'
  ).length;
  const pendingHoursCount = volunteerHours.filter((h) => h.verificationStatus === 'PENDING').length;

  const openCreateModal = (cat: OpportunityCategory) => {
    setOppCategory(cat);
    setOppMode(cat === 'CALLING_BGV' ? 'ONLINE' : 'OFFLINE');
    setOppTitle('');
    setShowCreateOpp(true);
  };

  const renderRequestCard = (req: VolunteerRequest, opp: Opportunity | undefined) => {
    const volStats = allStats.find((s) => s.userId === req.userId);
    const canAct =
      opp &&
      (req.status === 'PENDING' || req.status === 'UNDER_REVIEW' || req.status === 'WAITLISTED');
    return (
      <div
        key={req.id}
        className="space-y-2.5 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm"
      >
        <div className="flex items-center justify-between gap-2">
          <h4 className="font-bold text-maatram-navy">
            {req.volunteerName} ({req.volunteerCode})
          </h4>
          <StatusPill status={req.status} />
        </div>
        <p className="text-sm text-slate-600">
          {req.opportunityTitle} • {req.district}
        </p>
        {volStats && (
          <p className="text-xs font-semibold text-maatram-blue">
            History: {volStats.verifiedHours.toFixed(1)}h • {volStats.completedPvCases} PV done •{' '}
            {volStats.activePvCases} active PV
          </p>
        )}
        {req.availabilityNote && (
          <p className="text-xs text-slate-600">Availability: {req.availabilityNote}</p>
        )}
        {canAct && opp && (
          <div className="flex flex-wrap gap-2 pt-1">
            <button
              type="button"
              disabled={isBusy}
              onClick={() => onReviewRequest(req, opp, 'APPROVED', '')}
              className="rounded-xl bg-maatram-green px-4 py-2 text-xs font-bold text-white hover:opacity-90"
            >
              Approve
            </button>
            <button
              type="button"
              disabled={isBusy}
              onClick={() => onReviewRequest(req, opp, 'WAITLISTED', '')}
              className="rounded-xl border border-maatram-outline px-4 py-2 text-xs font-semibold text-maatram-navy hover:bg-slate-50"
            >
              Waitlist
            </button>
            <button
              type="button"
              disabled={isBusy}
              onClick={() => {
                setRejectTarget({ req, opp });
                setRejectReason('');
              }}
              className="rounded-xl border border-red-200 px-4 py-2 text-xs font-semibold text-maatram-red hover:bg-maatram-redSoft"
            >
              Reject
            </button>
          </div>
        )}
      </div>
    );
  };

  if (section === 'overview') {
    const unassignedPv = pvCases.filter((p) => p.status === 'UNASSIGNED').length;
    const availableVols = allStats.filter((s) => s.availableForPv).length;
    const openPolls = opportunities.filter((o) => o.status === 'OPEN');

    const distMap: Record<string, number> = {};
    allStats
      .filter((s) => s.availableForPv)
      .forEach((s) => {
        distMap[s.district] = (distMap[s.district] || 0) + 1;
      });
    const distChart = Object.entries(distMap)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 6)
      .map(([k, v]) => ({ label: k, value: v }));

    return (
      <div className="mx-auto max-w-5xl space-y-5">
        <div className="flex gap-2 overflow-x-auto rounded-2xl border border-maatram-outline bg-white p-1.5 shadow-sm">
          {[
            { key: 'overview', label: 'Overview' },
            { key: 'categories', label: 'Works by Category' },
            { key: 'requests', label: `Requests (${pendingReqsCount})` },
          ].map((t) => (
            <button
              key={t.key}
              type="button"
              onClick={() => setOverviewTab(t.key as typeof overviewTab)}
              className={`flex-1 whitespace-nowrap rounded-xl px-4 py-2.5 text-sm font-semibold transition ${
                overviewTab === t.key
                  ? 'bg-maatram-blue text-white'
                  : 'text-maatram-navy hover:bg-maatram-blueSoft'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {overviewTab === 'overview' && (
          <div className="space-y-5">
            <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
              <StatSummaryCard
                title="Pending Requests"
                value={`${pendingReqsCount}`}
                subtitle="Need Office review"
                icon={<ClipboardCheck className="h-5 w-5" />}
                accent="orange"
              />
              <StatSummaryCard
                title="Available Volunteers"
                value={`${availableVols}`}
                subtitle="Ready for PV & works"
                icon={<Users className="h-5 w-5" />}
                accent="green"
              />
              <StatSummaryCard
                title="Unassigned PV Cases"
                value={`${unassignedPv}`}
                subtitle="Ready for fair allocation"
                icon={<Home className="h-5 w-5" />}
                accent="blue"
              />
              <StatSummaryCard
                title="Pending Hours"
                value={`${pendingHoursCount}`}
                subtitle="Awaiting verification"
                icon={<Clock className="h-5 w-5" />}
                accent="blue"
              />
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <button
                type="button"
                data-testid="office_create_opp_btn"
                onClick={() => openCreateModal('PHYSICAL_VERIFICATION')}
                className="flex h-12 items-center justify-center gap-2 rounded-xl bg-maatram-blue text-sm font-bold text-white shadow-sm hover:bg-maatram-blueDark"
              >
                <Plus className="h-4 w-4" />
                Post Availability Poll
              </button>
              <button
                type="button"
                data-testid="office_open_fair_alloc_btn"
                onClick={onOpenFairAllocation}
                className="h-12 rounded-xl border border-maatram-blue bg-white text-sm font-semibold text-maatram-blue hover:bg-maatram-blueSoft"
              >
                Fair PV Allocation
              </button>
            </div>

            <div className="space-y-3">
              <h3 className="text-base font-bold text-maatram-navy">
                Availability Polling (Office Vote &amp; Student Slots)
              </h3>
              {openPolls.length === 0 ? (
                <EmptyStateCard
                  title="No Open Availability Polls"
                  message="Click Post Availability Poll above to publish available slots for students."
                />
              ) : (
                openPolls.map((opp) => {
                  const activeVotes = requests.filter(
                    (r) =>
                      r.opportunityId === opp.id &&
                      r.status !== 'CANCELLED' &&
                      r.status !== 'REJECTED'
                  );
                  const myVote = activeVotes.find((r) => r.userId === currentUserId);
                  return (
                    <div
                      key={opp.id}
                      className="space-y-2.5 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm"
                    >
                      <div className="flex items-center justify-between gap-2">
                        <CategoryBadge category={opp.category} />
                        <span className="text-xs font-bold text-maatram-blue">
                          {activeVotes.length} Voted Available
                        </span>
                      </div>
                      <h4 className="font-bold text-maatram-navy">{opp.title}</h4>
                      <p className="text-sm text-slate-600">
                        Posted Available for Students: {opp.availableSlots} /{' '}
                        {opp.requiredVolunteers} slots • {opp.district} ({opp.eventDate})
                      </p>
                      {myVote ? (
                        <button
                          type="button"
                          disabled={isBusy}
                          data-testid={`office_poll_vote_${opp.id}`}
                          onClick={() => onCancelPollVote(myVote)}
                          className="flex h-11 w-full items-center justify-center gap-2 rounded-xl border border-maatram-green bg-maatram-greenSoft text-sm font-semibold text-maatram-green hover:opacity-90"
                        >
                          <CheckCircle2 className="h-4 w-4" />
                          Voted Available (Click to Withdraw)
                        </button>
                      ) : (
                        <button
                          type="button"
                          disabled={isBusy}
                          data-testid={`office_poll_vote_${opp.id}`}
                          onClick={() => onCastPollVote(opp)}
                          className="flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-maatram-blue text-sm font-bold text-white hover:bg-maatram-blueDark"
                        >
                          <Vote className="h-4 w-4" />
                          Vote Available in Poll
                        </button>
                      )}
                    </div>
                  );
                })
              )}
            </div>

            <BarChartCard
              title="District-Wise Available Volunteers"
              subtitle="Active volunteers available for district deployment"
              items={distChart}
              barColorClass="bg-maatram-green"
            />
          </div>
        )}

        {overviewTab === 'categories' && (
          <div className="space-y-4">
            <h3 className="text-base font-bold text-maatram-navy">Select Work Category Page</h3>
            <div className="flex flex-wrap gap-2">
              {ORDERED_CATEGORIES.map((cat) => {
                const count = opportunities.filter((o) => o.category === cat).length;
                const active = selectedWorkCat === cat;
                return (
                  <button
                    key={cat}
                    type="button"
                    onClick={() => setSelectedWorkCat(cat)}
                    className={`rounded-xl border px-3.5 py-2 text-xs font-semibold ${
                      active
                        ? 'border-maatram-blue bg-maatram-blue text-white'
                        : 'border-maatram-outline bg-white text-maatram-navy hover:bg-maatram-blueSoft'
                    }`}
                  >
                    {CATEGORY_META[cat].shortLabel} ({count})
                  </button>
                );
              })}
            </div>

            <button
              type="button"
              onClick={() => openCreateModal(selectedWorkCat)}
              className="flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-maatram-blue text-sm font-bold text-white hover:bg-maatram-blueDark"
            >
              <Plus className="h-4 w-4" />
              Create {CATEGORY_META[selectedWorkCat].shortLabel} Work
            </button>

            {opportunities.filter((o) => o.category === selectedWorkCat).length === 0 ? (
              <EmptyStateCard
                title={`No ${CATEGORY_META[selectedWorkCat].label} Works Yet`}
                message="Click Create Work above to publish one for students."
              />
            ) : (
              opportunities
                .filter((o) => o.category === selectedWorkCat)
                .map((opp) => {
                  const casesCount = pvCases.filter((p) => p.opportunityId === opp.id).length;
                  return (
                    <div
                      key={opp.id}
                      className="space-y-2.5 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm"
                    >
                      <div className="flex items-center justify-between gap-2">
                        <CategoryBadge category={opp.category} />
                        <StatusPill status={opp.status} />
                      </div>
                      <h4 className="font-bold text-maatram-navy">{opp.title}</h4>
                      <p className="text-sm text-slate-600">
                        {opp.district} • {opp.eventDate} • Slots: {opp.availableSlots}/
                        {opp.requiredVolunteers}
                        {casesCount > 0 ? ` • ${casesCount} PV Cases` : ''}
                      </p>
                      <div className="flex gap-2">
                        <button
                          type="button"
                          onClick={() =>
                            onToggleOppStatus(opp, opp.status === 'OPEN' ? 'CLOSED' : 'OPEN')
                          }
                          className="h-10 flex-1 rounded-xl border border-maatram-outline text-xs font-semibold text-maatram-navy hover:bg-slate-50"
                        >
                          {opp.status === 'OPEN' ? 'Close' : 'Reopen'}
                        </button>
                        {opp.category === 'PHYSICAL_VERIFICATION' && (
                          <button
                            type="button"
                            onClick={onOpenFairAllocation}
                            className="h-10 flex-1 rounded-xl bg-maatram-blue text-xs font-bold text-white hover:bg-maatram-blueDark"
                          >
                            Allocate PV
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })
            )}

            {requests.filter(
              (r) =>
                r.category === selectedWorkCat &&
                (r.status === 'PENDING' || r.status === 'UNDER_REVIEW')
            ).length > 0 && (
              <div className="space-y-3 pt-2">
                <h4 className="font-bold text-maatram-navy">
                  Pending {CATEGORY_META[selectedWorkCat].shortLabel} Requests
                </h4>
                {requests
                  .filter(
                    (r) =>
                      r.category === selectedWorkCat &&
                      (r.status === 'PENDING' || r.status === 'UNDER_REVIEW')
                  )
                  .map((r) =>
                    renderRequestCard(
                      r,
                      opportunities.find((o) => o.id === r.opportunityId)
                    )
                  )}
              </div>
            )}
          </div>
        )}

        {overviewTab === 'requests' && (
          <div className="space-y-4">
            <div className="flex flex-wrap gap-2">
              {['ALL', 'PENDING', 'APPROVED', 'REJECTED', 'WAITLISTED'].map((st) => (
                <button
                  key={st}
                  type="button"
                  onClick={() => setReqFilter(st)}
                  className={`rounded-xl border px-3 py-1.5 text-xs font-semibold ${
                    reqFilter === st
                      ? 'border-maatram-blue bg-maatram-blue text-white'
                      : 'border-maatram-outline bg-white text-maatram-navy'
                  }`}
                >
                  {st}
                </button>
              ))}
            </div>
            {requests.filter((r) => reqFilter === 'ALL' || r.status === reqFilter).length === 0 ? (
              <EmptyStateCard
                title="No Requests in This Filter"
                message="Student availability requests appear here in real time."
              />
            ) : (
              requests
                .filter((r) => reqFilter === 'ALL' || r.status === reqFilter)
                .map((r) =>
                  renderRequestCard(
                    r,
                    opportunities.find((o) => o.id === r.opportunityId)
                  )
                )
            )}
          </div>
        )}

        {/* Create Opportunity / Availability Poll Modal */}
        <ModalDialog
          open={showCreateOpp}
          title="Post Availability Poll / Work"
          onClose={() => setShowCreateOpp(false)}
        >
          <div className="space-y-3">
            <div>
              <label className="block text-xs font-semibold text-maatram-navy">Work Title *</label>
              <input
                type="text"
                value={oppTitle}
                onChange={(e) => setOppTitle(e.target.value)}
                placeholder="e.g., Madurai Home PV Drive"
                className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-maatram-navy">Category</label>
              <div className="mt-1 flex flex-wrap gap-1.5">
                {ORDERED_CATEGORIES.map((cat) => (
                  <button
                    key={cat}
                    type="button"
                    onClick={() => {
                      setOppCategory(cat);
                      if (cat === 'CALLING_BGV') setOppMode('ONLINE');
                    }}
                    className={`rounded-lg border px-2.5 py-1 text-xs font-semibold ${
                      oppCategory === cat
                        ? 'border-maatram-blue bg-maatram-blue text-white'
                        : 'border-maatram-outline bg-white text-maatram-navy'
                    }`}
                  >
                    {CATEGORY_META[cat].shortLabel}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label className="block text-xs font-semibold text-maatram-navy">District</label>
              <div className="mt-1 flex flex-wrap gap-1.5">
                {districtNames.map((d) => (
                  <button
                    key={d}
                    type="button"
                    onClick={() => setOppDistrict(d)}
                    className={`rounded-lg border px-2.5 py-1 text-xs font-semibold ${
                      oppDistrict === d
                        ? 'border-maatram-blue bg-maatram-blue text-white'
                        : 'border-maatram-outline bg-white text-maatram-navy'
                    }`}
                  >
                    {d}
                  </button>
                ))}
              </div>
            </div>
            <div className="flex gap-2">
              {(['OFFLINE', 'ONLINE'] as OpportunityMode[]).map((m) => (
                <button
                  key={m}
                  type="button"
                  onClick={() => setOppMode(m)}
                  className={`rounded-lg border px-3 py-1 text-xs font-semibold ${
                    oppMode === m
                      ? 'border-maatram-blue bg-maatram-blue text-white'
                      : 'border-maatram-outline bg-white text-maatram-navy'
                  }`}
                >
                  {m}
                </button>
              ))}
            </div>
            <input
              type="text"
              value={oppLocation}
              onChange={(e) => setOppLocation(e.target.value)}
              placeholder="Location / Zones"
              className="w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
            />
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-maatram-navy">Date</label>
                <input
                  type="date"
                  value={oppDate}
                  onChange={(e) => setOppDate(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-maatram-outline p-2 text-sm text-black"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-maatram-navy">
                  Available Slots
                </label>
                <input
                  type="number"
                  min={1}
                  value={oppSlots}
                  onChange={(e) => setOppSlots(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-maatram-outline p-2 text-sm text-black"
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <input
                type="time"
                value={oppStart}
                onChange={(e) => setOppStart(e.target.value)}
                className="w-full rounded-xl border border-maatram-outline p-2 text-sm text-black"
              />
              <input
                type="time"
                value={oppEnd}
                onChange={(e) => setOppEnd(e.target.value)}
                className="w-full rounded-xl border border-maatram-outline p-2 text-sm text-black"
              />
            </div>
            {oppCategory === 'PHYSICAL_VERIFICATION' && (
              <div>
                <label className="block text-xs font-semibold text-maatram-navy">
                  PV Cases to Generate
                </label>
                <input
                  type="number"
                  min={0}
                  max={50}
                  value={oppPvCount}
                  onChange={(e) => setOppPvCount(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-maatram-outline p-2 text-sm text-black"
                />
              </div>
            )}
            <input
              type="text"
              value={oppWhatsapp}
              onChange={(e) => setOppWhatsapp(e.target.value)}
              placeholder="WhatsApp Group Link (optional)"
              className="w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
            />
            <textarea
              rows={2}
              value={oppInstructions}
              onChange={(e) => setOppInstructions(e.target.value)}
              placeholder="Instructions for students"
              className="w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
            />
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowCreateOpp(false)}
                className="h-10 rounded-xl px-4 text-sm font-semibold text-slate-600"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  if (oppTitle.trim().length < 3) return;
                  const slots = Math.max(1, Number.parseInt(oppSlots, 10) || 4);
                  const pvN =
                    oppCategory === 'PHYSICAL_VERIFICATION'
                      ? Math.max(0, Math.min(50, Number.parseInt(oppPvCount, 10) || 4))
                      : 0;
                  onCreateOpportunity(
                    {
                      title: oppTitle.trim(),
                      category: oppCategory,
                      activityType: CATEGORY_META[oppCategory].label,
                      description:
                        oppInstructions.trim() ||
                        `${CATEGORY_META[oppCategory].label} in ${oppDistrict}`,
                      district: oppDistrict,
                      location: oppLocation.trim() || oppDistrict,
                      mode: oppMode,
                      eventDate: oppDate,
                      startTime: oppStart,
                      endTime: oppEnd,
                      deadline: oppDate,
                      requiredVolunteers: slots,
                      availableSlots: slots,
                      totalCases: pvN,
                      skillsRequired: '',
                      eligibility: 'All Registered Volunteers',
                      instructions: oppInstructions.trim(),
                      whatsappGroupLink: oppWhatsapp.trim(),
                      status: 'OPEN',
                      multiDayCount: 1,
                      createdBy: currentUserId,
                    },
                    pvN
                  );
                  setShowCreateOpp(false);
                }}
                className="h-10 rounded-xl bg-maatram-blue px-5 text-sm font-bold text-white"
              >
                Publish
              </button>
            </div>
          </div>
        </ModalDialog>

        {/* Reject Request Modal */}
        <ModalDialog
          open={Boolean(rejectTarget)}
          title="Reject Request"
          onClose={() => setRejectTarget(null)}
        >
          {rejectTarget && (
            <div className="space-y-4">
              <textarea
                rows={3}
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="Reason (visible to student)"
                className="w-full rounded-xl border border-maatram-outline p-3 text-sm text-black"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setRejectTarget(null)}
                  className="h-10 rounded-xl px-4 text-sm font-semibold text-slate-600"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={() => {
                    onReviewRequest(
                      rejectTarget.req,
                      rejectTarget.opp,
                      'REJECTED',
                      rejectReason || 'Slots filled via fair priority'
                    );
                    setRejectTarget(null);
                  }}
                  className="h-10 rounded-xl bg-maatram-red px-5 text-sm font-bold text-white"
                >
                  Confirm Reject
                </button>
              </div>
            </div>
          )}
        </ModalDialog>
      </div>
    );
  }

  // Section 2: Fair PV Allocation & Verification
  const unassignedCases = activePvOpp
    ? pvCases.filter((p) => p.opportunityId === activePvOpp.id && p.status === 'UNASSIGNED')
    : [];
  const rankedCandidates = activePvOpp
    ? rankPvCandidates(activePvOpp.id, activePvOpp.district, allStats, requests, settings)
    : [];

  const callingOpps = opportunities.filter((o) => o.category === 'CALLING_BGV');
  const verifyPv = pvCases.filter((p) => p.status === 'COMPLETED' || p.status === 'VISITED');
  const verifyCalls = callingTasks.filter((t) => !t.verifiedByOffice && t.callStatus !== 'NOT_STARTED');
  const verifyAssigns = assignments.filter((a) => a.status === 'COMPLETED');

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <div className="flex gap-2 overflow-x-auto rounded-2xl border border-maatram-outline bg-white p-1.5 shadow-sm">
        {[
          { key: 'fair_pv', label: 'Fair PV Alloc' },
          { key: 'verify', label: 'Verify Work' },
          { key: 'hours', label: `Hours (${pendingHoursCount})` },
        ].map((t) => (
          <button
            key={t.key}
            type="button"
            onClick={() => setAllocTab(t.key as typeof allocTab)}
            className={`flex-1 whitespace-nowrap rounded-xl px-4 py-2.5 text-sm font-semibold transition ${
              allocTab === t.key
                ? 'bg-maatram-blue text-white'
                : 'text-maatram-navy hover:bg-maatram-blueSoft'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {allocTab === 'fair_pv' && (
        <div className="space-y-4">
          <div className="rounded-2xl border border-maatram-blueContainer bg-maatram-blueSoft p-4">
            <h3 className="text-sm font-bold text-maatram-navy">
              Fair Volunteer Allocation Engine
            </h3>
            <p className="mt-1 text-xs text-slate-700">
              Prioritizes zero/low completed PV cases &amp; lower verified hours. Blocks volunteers
              with incomplete active batches or &ge; {settings.maxActivePvCases} active cases unless
              overridden with a mandatory reason.
            </p>
          </div>

          {pvOpps.length === 0 ? (
            <EmptyStateCard
              title="No Physical Verification Opportunities"
              message="Create a Physical Verification opportunity in the Office tab to start fair district allocation."
            />
          ) : (
            <>
              <div className="flex flex-wrap gap-2">
                {pvOpps.map((opp) => (
                  <button
                    key={opp.id}
                    type="button"
                    onClick={() => setSelectedPvOppId(opp.id)}
                    className={`rounded-xl border px-3 py-1.5 text-xs font-semibold ${
                      activePvOpp?.id === opp.id
                        ? 'border-maatram-blue bg-maatram-blue text-white'
                        : 'border-maatram-outline bg-white text-maatram-navy'
                    }`}
                  >
                    {opp.district}: {opp.title.slice(0, 24)}
                  </button>
                ))}
              </div>

              {activePvOpp && (
                <>
                  <p className="text-sm font-bold text-maatram-navy">
                    Unassigned PV Cases in {activePvOpp.district}: {unassignedCases.length}
                  </p>
                  {rankedCandidates.length === 0 ? (
                    <EmptyStateCard
                      title={`No Volunteers Found for ${activePvOpp.district}`}
                      message="Volunteers registered in this district or requesting this drive appear here."
                    />
                  ) : (
                    <div className="space-y-3">
                      {rankedCandidates.map((cand) => (
                        <div
                          key={cand.stats.userId}
                          className="space-y-2.5 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm"
                        >
                          <div className="flex items-start justify-between gap-2">
                            <div>
                              <h4 className="font-bold text-maatram-navy">
                                {cand.stats.displayName}
                              </h4>
                              <p className="text-xs text-slate-500">
                                {cand.stats.district} • {cand.stats.volunteerCode}
                                {cand.hasRequested ? ' • REQUESTED' : ''}
                              </p>
                            </div>
                            <StatusPill status={cand.priorityLabel} />
                          </div>
                          <div className="flex flex-wrap gap-4 text-xs font-medium text-slate-700">
                            <span>Completed PV: {cand.stats.completedPvCases}</span>
                            <span>Active PV: {cand.stats.activePvCases}</span>
                            <span>Verified: {cand.stats.verifiedHours.toFixed(1)}h</span>
                          </div>
                          <p className="text-xs font-semibold text-maatram-blue">
                            Recommendation: {cand.recommendedAction} (Score: {cand.priorityScore})
                          </p>
                          <button
                            type="button"
                            disabled={isBusy || unassignedCases.length === 0}
                            onClick={() => {
                              setAllocateTarget(cand);
                              setSelectedCaseIds(
                                new Set(unassignedCases.slice(0, 2).map((c) => c.id))
                              );
                              setOverrideReason('');
                            }}
                            className={`h-10 w-full rounded-xl text-xs font-bold transition disabled:opacity-50 ${
                              cand.blockedByActiveBatch || cand.atMaxLimit
                                ? 'bg-slate-200 text-maatram-navy hover:bg-slate-300'
                                : 'bg-maatram-blue text-white hover:bg-maatram-blueDark'
                            }`}
                          >
                            {cand.blockedByActiveBatch || cand.atMaxLimit
                              ? 'Assign With Override Reason'
                              : 'Assign PV Cases Fairly'}
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </>
              )}
            </>
          )}
        </div>
      )}

      {allocTab === 'verify' && (
        <div className="space-y-4">
          {callingOpps.length > 0 && allStats.length > 0 && (
            <div className="space-y-3 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm">
              <h3 className="font-bold text-maatram-navy">Calling Sheet Management</h3>
              <p className="text-sm text-slate-600">
                Total Calls: {callingTasks.length} • Verified:{' '}
                {callingTasks.filter((t) => t.verifiedByOffice).length}
              </p>
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                <button
                  type="button"
                  disabled={isBusy}
                  onClick={() => onImportCallingSheet(callingOpps[0], allStats[0])}
                  className="h-10 rounded-xl bg-maatram-blue text-xs font-bold text-white hover:bg-maatram-blueDark"
                >
                  Upload Sample Calling Sheet (5)
                </button>
                <button
                  type="button"
                  onClick={() =>
                    downloadCsvFile('maatram_calling_sheet.csv', exportCallingTasksCsv(callingTasks))
                  }
                  className="flex h-10 items-center justify-center gap-1.5 rounded-xl border border-maatram-blue text-xs font-semibold text-maatram-blue hover:bg-maatram-blueSoft"
                >
                  <Download className="h-4 w-4" />
                  Export Calling CSV
                </button>
              </div>
            </div>
          )}

          <h3 className="font-bold text-maatram-navy">
            PV Cases &amp; Assignments Pending Verification
          </h3>

          {verifyPv.length === 0 && verifyCalls.length === 0 && verifyAssigns.length === 0 ? (
            <EmptyStateCard
              title="All Submitted Work Verified"
              message="Completed PV visits, call logs, and assignments waiting for Office approval appear here."
            />
          ) : (
            <div className="space-y-3">
              {verifyPv.map((pv) => (
                <div
                  key={pv.id}
                  className="space-y-2 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm"
                >
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-maatram-navy">
                      {pv.caseRefId} • {pv.assignedToName}
                    </span>
                    <StatusPill status={pv.status} />
                  </div>
                  {pv.visitNotes && <p className="text-sm text-slate-600">Notes: {pv.visitNotes}</p>}
                  <button
                    type="button"
                    onClick={() => onVerifyPvCase(pv)}
                    className="h-10 w-full rounded-xl bg-maatram-green text-xs font-bold text-white"
                  >
                    Verify PV Case &amp; Release Active Batch
                  </button>
                </div>
              ))}

              {verifyCalls.map((task) => (
                <div
                  key={task.id}
                  className="flex items-center justify-between gap-3 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm"
                >
                  <div>
                    <p className="font-bold text-maatram-navy">
                      {task.applicantRefId} ({task.assignedToName})
                    </p>
                    <p className="text-xs text-slate-500">
                      {task.callStatus} • {task.notes}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => onVerifyCallingTask(task.id)}
                    className="rounded-xl bg-maatram-green px-4 py-2 text-xs font-bold text-white"
                  >
                    Verify
                  </button>
                </div>
              ))}

              {verifyAssigns.map((assign) => (
                <div
                  key={assign.id}
                  className="space-y-2 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm"
                >
                  <p className="font-bold text-maatram-navy">
                    {assign.opportunityTitle} • {assign.volunteerName}
                  </p>
                  <button
                    type="button"
                    onClick={() => onVerifyAssignment(assign)}
                    className="h-10 w-full rounded-xl bg-maatram-green text-xs font-bold text-white"
                  >
                    Verify Assignment Completion
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {allocTab === 'hours' && (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h3 className="font-bold text-maatram-navy">Hour Approvals &amp; Reports</h3>
            <button
              type="button"
              onClick={() =>
                downloadCsvFile('maatram_verified_hours.csv', exportHoursCsv(volunteerHours))
              }
              className="flex items-center gap-1.5 rounded-xl border border-maatram-blue px-3.5 py-2 text-xs font-semibold text-maatram-blue hover:bg-maatram-blueSoft"
            >
              <Download className="h-4 w-4" />
              Export Hours CSV
            </button>
          </div>
          {volunteerHours.length === 0 ? (
            <EmptyStateCard
              title="No Hour Submissions Yet"
              message="Volunteer hour submissions appear here for verification."
            />
          ) : (
            [...volunteerHours]
              .sort((a) => (a.verificationStatus === 'PENDING' ? -1 : 1))
              .map((hr) => (
                <div
                  key={hr.id}
                  className="space-y-2 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm"
                >
                  <div className="flex items-center justify-between">
                    <CategoryBadge category={hr.category} />
                    <StatusPill status={hr.verificationStatus} />
                  </div>
                  <h4 className="font-bold text-maatram-navy">
                    {hr.volunteerName} • {hr.submittedHours} hrs
                  </h4>
                  <p className="text-sm text-slate-600">
                    {hr.opportunityTitle} ({hr.activityDate}, Day {hr.dayNumber})
                  </p>
                  {hr.notes && <p className="text-xs text-slate-500">Notes: {hr.notes}</p>}
                  {hr.verificationStatus === 'PENDING' && (
                    <button
                      type="button"
                      onClick={() => {
                        setReviewHourTarget(hr);
                        setAdjustedHours(hr.submittedHours.toString());
                        setCorrectionNote('');
                      }}
                      className="h-10 w-full rounded-xl bg-maatram-blue text-xs font-bold text-white hover:bg-maatram-blueDark"
                    >
                      Review &amp; Verify Hours
                    </button>
                  )}
                </div>
              ))
          )}
        </div>
      )}

      {/* Allocate PV Modal */}
      <ModalDialog
        open={Boolean(allocateTarget && activePvOpp)}
        title={`Allocate PV Cases to ${allocateTarget?.stats.displayName ?? ''}`}
        onClose={() => setAllocateTarget(null)}
      >
        {allocateTarget && activePvOpp && (
          <div className="space-y-3">
            <p className="text-xs text-slate-600">
              Completed: {allocateTarget.stats.completedPvCases} • Active:{' '}
              {allocateTarget.stats.activePvCases}/{settings.maxActivePvCases}
            </p>
            <p className="text-xs font-bold text-maatram-navy">Select Unassigned Cases:</p>
            <div className="space-y-1.5">
              {unassignedCases.map((pv) => (
                <label
                  key={pv.id}
                  className="flex items-center gap-2 rounded-xl border border-maatram-outline p-2.5 text-sm text-maatram-navy"
                >
                  <input
                    type="checkbox"
                    checked={selectedCaseIds.has(pv.id)}
                    onChange={(e) => {
                      const next = new Set(selectedCaseIds);
                      if (e.target.checked) next.add(pv.id);
                      else next.delete(pv.id);
                      setSelectedCaseIds(next);
                    }}
                    className="h-4 w-4 accent-maatram-blue"
                  />
                  <span>
                    {pv.caseRefId} ({pv.areaLabel})
                  </span>
                </label>
              ))}
            </div>
            {(allocateTarget.blockedByActiveBatch ||
              allocateTarget.stats.activePvCases + selectedCaseIds.size >
                settings.maxActivePvCases) && (
              <div className="space-y-1.5">
                <p className="text-xs font-semibold text-maatram-red">
                  Active-Batch / Limit Protection Triggered: Mandatory override reason required.
                </p>
                <input
                  type="text"
                  value={overrideReason}
                  onChange={(e) => setOverrideReason(e.target.value)}
                  placeholder="Mandatory Override Reason *"
                  className="w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
                />
              </div>
            )}
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setAllocateTarget(null)}
                className="h-10 rounded-xl px-4 text-sm font-semibold text-slate-600"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={
                  selectedCaseIds.size === 0 ||
                  ((allocateTarget.blockedByActiveBatch ||
                    allocateTarget.stats.activePvCases + selectedCaseIds.size >
                      settings.maxActivePvCases) &&
                    !overrideReason.trim())
                }
                onClick={() => {
                  const chosen = unassignedCases.filter((c) => selectedCaseIds.has(c.id));
                  onAllocatePvCases(activePvOpp, allocateTarget.stats, chosen, overrideReason);
                  setAllocateTarget(null);
                }}
                className="h-10 rounded-xl bg-maatram-blue px-5 text-sm font-bold text-white disabled:opacity-50"
              >
                Confirm Allocation
              </button>
            </div>
          </div>
        )}
      </ModalDialog>

      {/* Review Hours Modal */}
      <ModalDialog
        open={Boolean(reviewHourTarget)}
        title={`Verify Hours: ${reviewHourTarget?.volunteerName ?? ''}`}
        onClose={() => setReviewHourTarget(null)}
      >
        {reviewHourTarget && (
          <div className="space-y-3">
            <p className="text-xs text-slate-600">
              {reviewHourTarget.opportunityTitle} ({reviewHourTarget.startTime}-
              {reviewHourTarget.endTime}, Break {reviewHourTarget.breakMinutes}m)
            </p>
            <div>
              <label className="block text-xs font-semibold text-maatram-navy">
                Approved Hours (edit if correcting)
              </label>
              <input
                type="number"
                step="0.25"
                value={adjustedHours}
                onChange={(e) => setAdjustedHours(e.target.value)}
                className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-maatram-navy">
                Correction / Reviewer Note
              </label>
              <textarea
                rows={2}
                value={correctionNote}
                onChange={(e) => setCorrectionNote(e.target.value)}
                className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => {
                  onReviewHours(
                    reviewHourTarget,
                    'REJECTED',
                    reviewHourTarget.submittedHours,
                    correctionNote || 'Please re-check timings'
                  );
                  setReviewHourTarget(null);
                }}
                className="h-10 rounded-xl border border-red-200 px-4 text-xs font-semibold text-maatram-red"
              >
                Reject
              </button>
              <button
                type="button"
                onClick={() => {
                  const finalHrs = Number.parseFloat(adjustedHours) || reviewHourTarget.submittedHours;
                  onReviewHours(reviewHourTarget, 'APPROVED', finalHrs, correctionNote);
                  setReviewHourTarget(null);
                }}
                className="h-10 rounded-xl bg-maatram-green px-5 text-xs font-bold text-white"
              >
                Approve &amp; Thank
              </button>
            </div>
          </div>
        )}
      </ModalDialog>
    </div>
  );
};
