import React, { useMemo, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  CalendarDays,
  Home,
  MapPin,
  Mic,
  PhoneCall,
  Search,
} from 'lucide-react';
import {
  CALL_STATUS_LABELS,
  CATEGORY_META,
  ORDERED_CATEGORIES,
  type Assignment,
  type CallStatus,
  type CallingTask,
  type DistrictItem,
  type Opportunity,
  type OpportunityCategory,
  type PhysicalVerificationCase,
  type PvCaseStatus,
  type VolunteerRequest,
} from '../types/maatram';
import { CategoryBadge, EmptyStateCard, ModalDialog, StatusPill } from './Common';

interface VolunteerWorksPageProps {
  opportunities: Opportunity[];
  myRequests: VolunteerRequest[];
  districts: DistrictItem[];
  userDistrict: string;
  currentUserId: string;
  availableForPv: boolean;
  myAssignments: Assignment[];
  pvCases: PhysicalVerificationCase[];
  callingTasks: CallingTask[];
  isBusy: boolean;
  onTogglePvAvailability: (available: boolean) => void;
  onRequestJoin: (opp: Opportunity, note: string) => void;
  onCancelRequest: (req: VolunteerRequest) => void;
  onUpdatePvCase: (caseId: string, status: PvCaseStatus, notes: string, summary: string) => void;
  onUpdateCallingTask: (taskId: string, status: CallStatus, followUp: boolean, notes: string) => void;
}

const CAT_ICONS: Record<OpportunityCategory, React.ReactNode> = {
  PHYSICAL_VERIFICATION: <Home className="h-6 w-6 text-maatram-blue" />,
  CALLING_BGV: <PhoneCall className="h-6 w-6 text-maatram-blue" />,
  INTERVIEWS: <Mic className="h-6 w-6 text-maatram-blue" />,
  EVENTS: <CalendarDays className="h-6 w-6 text-maatram-blue" />,
};

export const VolunteerWorksPage: React.FC<VolunteerWorksPageProps> = ({
  opportunities,
  myRequests,
  districts,
  userDistrict,
  currentUserId,
  availableForPv,
  myAssignments,
  pvCases,
  callingTasks,
  isBusy,
  onTogglePvAvailability,
  onRequestJoin,
  onCancelRequest,
  onUpdatePvCase,
  onUpdateCallingTask,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<OpportunityCategory | null>(null);
  const [selectedDistrict, setSelectedDistrict] = useState('All');
  const [requestOpp, setRequestOpp] = useState<Opportunity | null>(null);
  const [availabilityNote, setAvailabilityNote] = useState('I am available as scheduled');

  const districtOptions = useMemo(() => {
    const names =
      districts.length > 0
        ? districts.map((d) => d.name).sort()
        : ['Tiruchirappalli', 'Chennai', 'Coimbatore', 'Madurai', 'Salem'];
    return ['All', ...names];
  }, [districts]);

  if (!selectedCategory) {
    const visibleCategories = ORDERED_CATEGORIES.filter((cat) => {
      if (!searchQuery.trim()) return true;
      const q = searchQuery.toLowerCase();
      return (
        CATEGORY_META[cat].label.toLowerCase().includes(q) ||
        opportunities.some(
          (o) =>
            o.category === cat &&
            (o.title.toLowerCase().includes(q) || o.district.toLowerCase().includes(q))
        )
      );
    });

    return (
      <div className="mx-auto max-w-5xl space-y-5">
        <div>
          <h2 className="text-xl font-bold text-maatram-navy">Volunteer Works</h2>
          <p className="mt-1 text-sm text-slate-500">
            Works posted by Admin and Office Team. Choose a category to open its dedicated page.
          </p>
        </div>

        <div className="relative">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-maatram-blue" />
          <input
            type="text"
            data-testid="opportunity_search_input"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search works by title or district..."
            className="w-full rounded-xl border border-maatram-outline bg-white py-3 pl-10 pr-4 text-sm text-black outline-none focus:border-maatram-blue focus:ring-2 focus:ring-maatram-blue/20"
          />
        </div>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {visibleCategories.map((cat) => {
            const catOpps = opportunities.filter((o) => o.category === cat);
            const openCount = catOpps.filter((o) => o.status === 'OPEN').length;
            const meta = CATEGORY_META[cat];

            let subtitleExtra = meta.description;
            if (cat === 'PHYSICAL_VERIFICATION') {
              const assignedCount = pvCases.filter(
                (p) => currentUserId && p.assignedToUid === currentUserId
              ).length;
              if (assignedCount > 0) {
                subtitleExtra = `PV Available: ${openCount} open • ${assignedCount} assigned to you`;
              } else if (availableForPv) {
                subtitleExtra = `PV Available • ${openCount} open for request`;
              } else {
                subtitleExtra = 'Mark PV available & send request';
              }
            } else if (cat === 'CALLING_BGV') {
              const assignedCalls = callingTasks.filter(
                (t) => currentUserId && t.assignedToUid === currentUserId
              ).length;
              if (assignedCalls > 0) {
                subtitleExtra = `${assignedCalls} assigned to you • ${openCount} open`;
              }
            }

            return (
              <button
                key={cat}
                type="button"
                data-testid={`category_card_${cat}`}
                onClick={() => setSelectedCategory(cat)}
                className="flex items-center gap-4 rounded-2xl border border-maatram-outline bg-white p-5 text-left shadow-sm transition hover:border-maatram-blue hover:shadow-md"
              >
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-maatram-blueSoft">
                  {CAT_ICONS[cat]}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="text-base font-bold text-maatram-navy">{meta.label}</h3>
                    <span className="rounded-lg bg-maatram-blueContainer px-2 py-0.5 text-xs font-bold text-maatram-blue">
                      {openCount} Open
                    </span>
                  </div>
                  <p className="mt-1 text-sm text-slate-500">{subtitleExtra}</p>
                </div>
                <ArrowRight className="h-5 w-5 shrink-0 text-maatram-blue" />
              </button>
            );
          })}
        </div>
      </div>
    );
  }

  const categoryOpps = opportunities
    .filter(
      (opp) =>
        opp.category === selectedCategory &&
        (selectedDistrict === 'All' ||
          opp.district.toLowerCase() === selectedDistrict.toLowerCase()) &&
        (!searchQuery.trim() ||
          opp.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
          opp.district.toLowerCase().includes(searchQuery.toLowerCase()))
    )
    .sort((a, b) => (a.status === 'OPEN' ? -1 : 1) - (b.status === 'OPEN' ? -1 : 1));

  const myCategoryAssignments = myAssignments.filter((a) => a.category === selectedCategory);
  const myAssignedPv = pvCases.filter(
    (p) => currentUserId && p.assignedToUid === currentUserId
  );
  const myCalls = callingTasks.filter(
    (c) => currentUserId && c.assignedToUid === currentUserId
  );

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      {/* Pinned Category Header */}
      <div className="flex items-center gap-3 border-b border-maatram-outline pb-3">
        <button
          type="button"
          data-testid="category_back_button"
          onClick={() => {
            setSelectedCategory(null);
            setSelectedDistrict('All');
          }}
          aria-label="Back to Volunteer Works"
          className="flex h-10 w-10 items-center justify-center rounded-xl border border-maatram-outline bg-white text-maatram-navy hover:bg-maatram-blueSoft"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <h2 className="text-xl font-bold text-maatram-navy">
            {CATEGORY_META[selectedCategory].label}
          </h2>
          <p className="text-xs text-slate-500">{CATEGORY_META[selectedCategory].description}</p>
        </div>
      </div>

      {/* PV Availability Toggle */}
      {selectedCategory === 'PHYSICAL_VERIFICATION' && (
        <div
          className={`flex items-center justify-between gap-4 rounded-2xl border p-4 ${
            availableForPv
              ? 'border-maatram-green bg-maatram-greenSoft'
              : 'border-maatram-outline bg-white'
          }`}
        >
          <div>
            <p className="text-sm font-bold text-maatram-navy">
              I am Available for Physical Verification
            </p>
            <p className="text-xs text-slate-600">
              {availableForPv
                ? 'Available — Office Team can view your readiness and allocate PV cases.'
                : 'Not Available — Turn this on and send a request on any PV drive below.'}
            </p>
          </div>
          <label className="relative inline-flex cursor-pointer items-center">
            <input
              type="checkbox"
              data-testid="pv_availability_switch"
              checked={availableForPv}
              onChange={(e) => onTogglePvAvailability(e.target.checked)}
              className="peer sr-only"
            />
            <div className="peer h-7 w-12 rounded-full bg-slate-300 after:absolute after:left-1 after:top-1 after:h-5 after:w-5 after:rounded-full after:bg-white after:transition-all peer-checked:bg-maatram-blue peer-checked:after:translate-x-5" />
          </label>
        </div>
      )}

      {/* District Filter Chips */}
      <div className="flex flex-wrap gap-2">
        {districtOptions.map((dist) => {
          const active = selectedDistrict === dist;
          return (
            <button
              key={dist}
              type="button"
              onClick={() => setSelectedDistrict(dist)}
              className={`rounded-xl border px-3 py-1.5 text-xs font-semibold transition ${
                active
                  ? 'border-maatram-blue bg-maatram-blue text-white'
                  : 'border-maatram-outline bg-white text-maatram-navy hover:bg-maatram-blueSoft'
              }`}
            >
              {dist === userDistrict ? `${dist} (My District)` : dist}
            </button>
          );
        })}
      </div>

      {/* Category Opportunities List */}
      {categoryOpps.length === 0 ? (
        <EmptyStateCard
          title={`No ${CATEGORY_META[selectedCategory].label} Works Posted`}
          message="Works posted by Admin or Office Team under this category will appear here."
        />
      ) : (
        <div className="space-y-4">
          {categoryOpps.map((opp) => {
            const existingReq = myRequests.find((r) => r.opportunityId === opp.id);
            const myOppAssignment = myAssignments.find((a) => a.opportunityId === opp.id);
            const canRequest =
              opp.status === 'OPEN' &&
              !myOppAssignment &&
              (!existingReq ||
                existingReq.status === 'CANCELLED' ||
                existingReq.status === 'REJECTED');

            return (
              <div
                key={opp.id}
                className="space-y-3 rounded-2xl border border-maatram-outline bg-white p-5 shadow-sm"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <CategoryBadge category={opp.category} />
                  <div className="flex items-center gap-2">
                    <StatusPill status={opp.mode} />
                    <StatusPill
                      status={
                        myOppAssignment
                          ? 'ASSIGNED'
                          : existingReq
                          ? existingReq.status
                          : opp.status
                      }
                    />
                  </div>
                </div>

                <div>
                  <h3 className="text-lg font-bold text-maatram-navy">{opp.title}</h3>
                  <p className="mt-1 text-sm text-slate-600">
                    {opp.description || opp.activityType}
                  </p>
                </div>

                <div className="flex items-center gap-2 text-sm text-maatram-navy">
                  <MapPin className="h-4 w-4 shrink-0 text-maatram-blue" />
                  <span>
                    {opp.district} • {opp.location || opp.mode} • {opp.eventDate} ({opp.startTime}-
                    {opp.endTime})
                  </span>
                </div>

                <div className="rounded-xl bg-maatram-blueSoft px-3.5 py-2.5 text-xs font-semibold text-maatram-navy">
                  Office Availability Poll: {opp.availableSlots} of {opp.requiredVolunteers} slots
                  available
                </div>

                {existingReq?.status === 'REJECTED' && existingReq.rejectionReason && (
                  <p className="text-sm font-medium text-maatram-red">
                    Reason: {existingReq.rejectionReason}
                  </p>
                )}

                {myOppAssignment ? (
                  <div className="rounded-xl bg-maatram-greenSoft px-3.5 py-2.5 text-xs font-bold text-maatram-green">
                    Assigned to You — See your assigned details below.
                  </div>
                ) : canRequest ? (
                  <button
                    type="button"
                    disabled={isBusy}
                    data-testid={`request_join_${opp.id}`}
                    onClick={() => setRequestOpp(opp)}
                    className="h-12 w-full rounded-xl bg-maatram-blue text-sm font-bold text-white transition hover:bg-maatram-blueDark disabled:opacity-50"
                  >
                    Accept Availability — Send Request
                  </button>
                ) : existingReq &&
                  (existingReq.status === 'PENDING' || existingReq.status === 'UNDER_REVIEW') ? (
                  <div className="flex flex-wrap items-center gap-2">
                    <div className="flex-1 rounded-xl bg-maatram-greenSoft px-3.5 py-2.5 text-xs font-semibold text-maatram-green">
                      Request Sent (Awaiting Assignment)
                    </div>
                    <button
                      type="button"
                      disabled={isBusy}
                      onClick={() => onCancelRequest(existingReq)}
                      className="h-10 rounded-xl border border-maatram-outline px-4 text-xs font-semibold text-maatram-navy hover:bg-slate-50"
                    >
                      Cancel
                    </button>
                  </div>
                ) : null}
              </div>
            );
          })}
        </div>
      )}

      {/* Assigned Details — ONLY shown when assigned */}
      {myCategoryAssignments.length > 0 && (
        <div className="space-y-3 border-t border-maatram-outline pt-4">
          <h3 className="text-base font-bold text-maatram-navy">
            My Assigned Details ({myCategoryAssignments.length})
          </h3>
          {myCategoryAssignments.map((assign) => (
            <div
              key={assign.id}
              className="space-y-1.5 rounded-2xl border border-maatram-green bg-white p-4 shadow-sm"
            >
              <div className="flex items-center justify-between gap-2">
                <h4 className="font-bold text-maatram-navy">{assign.opportunityTitle}</h4>
                <StatusPill status={assign.status} />
              </div>
              <p className="text-sm text-slate-500">
                {assign.district}
                {assign.assignedCasesCount > 0
                  ? ` • ${assign.assignedCasesCount} case(s) assigned`
                  : ''}
              </p>
              {assign.progressNotes && (
                <p className="text-sm text-maatram-navy">Notes: {assign.progressNotes}</p>
              )}
              {assign.whatsappGroupLink && (
                <a
                  href={assign.whatsappGroupLink}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-block text-xs font-semibold text-maatram-blue underline"
                >
                  Coordination Group: {assign.whatsappGroupLink}
                </a>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Assigned PV Cases inside Physical Verification Page */}
      {selectedCategory === 'PHYSICAL_VERIFICATION' && myAssignedPv.length > 0 && (
        <div className="space-y-3 border-t border-maatram-outline pt-4">
          <h3 className="text-base font-bold text-maatram-navy">
            My Assigned PV Cases ({myAssignedPv.length})
          </h3>
          {myAssignedPv.map((pv) => (
            <div
              key={pv.id}
              className="space-y-2 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm"
            >
              <div className="flex items-center justify-between gap-2">
                <span className="font-bold text-maatram-navy">
                  {pv.caseRefId} • {pv.applicantCode}
                </span>
                <StatusPill status={pv.status} />
              </div>
              <p className="text-sm text-slate-600">
                {pv.district} • {pv.areaLabel}
              </p>
              {pv.status !== 'VERIFIED' && (
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() =>
                      onUpdatePvCase(pv.id, 'VISITED', 'Visited applicant home', pv.recommendationSummary)
                    }
                    className="h-10 flex-1 rounded-xl border border-maatram-blue text-xs font-semibold text-maatram-blue hover:bg-maatram-blueSoft"
                  >
                    Mark Visited
                  </button>
                  <button
                    type="button"
                    onClick={() =>
                      onUpdatePvCase(pv.id, 'COMPLETED', 'Home PV completed', 'Recommended')
                    }
                    className="h-10 flex-1 rounded-xl bg-maatram-blue text-xs font-bold text-white hover:bg-maatram-blueDark"
                  >
                    Complete PV
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Assigned Calling Tasks inside Calling & BGV Page */}
      {selectedCategory === 'CALLING_BGV' && myCalls.length > 0 && (
        <div className="space-y-3 border-t border-maatram-outline pt-4">
          <h3 className="text-base font-bold text-maatram-navy">
            My Calling Verification Tasks ({myCalls.length})
          </h3>
          {myCalls.map((task) => (
            <div
              key={task.id}
              className="space-y-2 rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm"
            >
              <div className="flex items-center justify-between gap-2">
                <span className="font-bold text-maatram-navy">
                  {task.applicantRefId} • {task.district}
                </span>
                <StatusPill status={task.callStatus} />
              </div>
              {!task.verifiedByOffice && (
                <div className="flex flex-wrap gap-2">
                  {(['CALLED', 'DETAILS_VERIFIED', 'NOT_REACHABLE'] as CallStatus[]).map((st) => (
                    <button
                      key={st}
                      type="button"
                      onClick={() =>
                        onUpdateCallingTask(task.id, st, st === 'NOT_REACHABLE', task.notes)
                      }
                      className={`rounded-lg border px-3 py-1.5 text-xs font-semibold ${
                        task.callStatus === st
                          ? 'border-maatram-blue bg-maatram-blue text-white'
                          : 'border-maatram-outline bg-white text-maatram-navy hover:bg-maatram-blueSoft'
                      }`}
                    >
                      {CALL_STATUS_LABELS[st]}
                    </button>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Send Request Modal */}
      <ModalDialog
        open={Boolean(requestOpp)}
        title="Send Availability Request"
        onClose={() => setRequestOpp(null)}
      >
        {requestOpp && (
          <div className="space-y-4">
            <div>
              <p className="font-bold text-maatram-navy">{requestOpp.title}</p>
              <p className="text-sm text-slate-500">
                {requestOpp.district} • {requestOpp.eventDate} ({requestOpp.startTime} -{' '}
                {requestOpp.endTime})
              </p>
            </div>
            {requestOpp.instructions && (
              <p className="rounded-xl bg-maatram-blueSoft p-3 text-xs text-maatram-navy">
                Instructions: {requestOpp.instructions}
              </p>
            )}
            <div>
              <label className="block text-xs font-semibold text-maatram-navy">
                Availability Note
              </label>
              <textarea
                rows={3}
                value={availabilityNote}
                onChange={(e) => setAvailabilityNote(e.target.value)}
                className="mt-1 w-full rounded-xl border border-maatram-outline p-3 text-sm text-black outline-none focus:border-maatram-blue"
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setRequestOpp(null)}
                className="h-10 rounded-xl px-4 text-sm font-semibold text-slate-600 hover:bg-slate-100"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  onRequestJoin(requestOpp, availabilityNote);
                  setRequestOpp(null);
                }}
                className="h-10 rounded-xl bg-maatram-blue px-5 text-sm font-bold text-white hover:bg-maatram-blueDark"
              >
                Send Request
              </button>
            </div>
          </div>
        )}
      </ModalDialog>
    </div>
  );
};
