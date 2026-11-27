import React, { useState } from 'react';
import { ExternalLink } from 'lucide-react';
import {
  ASSIGNMENT_STATUS_LABELS,
  CALL_STATUS_LABELS,
  PV_STATUS_LABELS,
  type Assignment,
  type AssignmentStatus,
  type CallStatus,
  type CallingTask,
  type PhysicalVerificationCase,
  type PvCaseStatus,
} from '../types/maatram';
import { calculateDurationHours } from '../domain/fairAllocationEngine';
import { CategoryBadge, EmptyStateCard, ModalDialog, StatusPill } from './Common';

interface VolunteerWorkPageProps {
  assignments: Assignment[];
  pvCases: PhysicalVerificationCase[];
  callingTasks: CallingTask[];
  isBusy: boolean;
  onUpdateAssignment: (id: string, status: AssignmentStatus, notes: string) => void;
  onUpdatePvCase: (id: string, status: PvCaseStatus, notes: string, summary: string) => void;
  onUpdateCallingTask: (id: string, status: CallStatus, followUp: boolean, notes: string) => void;
  onSubmitHours: (
    assign: Assignment,
    activityDate: string,
    startTime: string,
    endTime: string,
    breakMinutes: number,
    manualOverride: number | null,
    dayNumber: number,
    notes: string
  ) => void;
}

export const VolunteerWorkPage: React.FC<VolunteerWorkPageProps> = ({
  assignments,
  pvCases,
  callingTasks,
  isBusy,
  onUpdateAssignment,
  onUpdatePvCase,
  onUpdateCallingTask,
  onSubmitHours,
}) => {
  const [selectedTab, setSelectedTab] = useState<'assignments' | 'pv' | 'calling'>('assignments');
  const [editingAssignment, setEditingAssignment] = useState<Assignment | null>(null);
  const [editingPvCase, setEditingPvCase] = useState<PhysicalVerificationCase | null>(null);
  const [editingCallTask, setEditingCallTask] = useState<CallingTask | null>(null);
  const [hourAssignment, setHourAssignment] = useState<Assignment | null>(null);

  // Assignment edit form state
  const [assignStatus, setAssignStatus] = useState<AssignmentStatus>('IN_PROGRESS');
  const [assignNotes, setAssignNotes] = useState('');

  // PV case edit form state
  const [pvStatus, setPvStatus] = useState<PvCaseStatus>('COMPLETED');
  const [pvNotes, setPvNotes] = useState('');
  const [pvSummary, setPvSummary] = useState('');

  // Call log form state
  const [callStatus, setCallStatus] = useState<CallStatus>('DETAILS_VERIFIED');
  const [callFollowUp, setCallFollowUp] = useState(false);
  const [callNotes, setCallNotes] = useState('');

  // Hours submission form state
  const [activityDate, setActivityDate] = useState('2026-11-26');
  const [startTime, setStartTime] = useState('09:30');
  const [endTime, setEndTime] = useState('13:00');
  const [breakMinutes, setBreakMinutes] = useState('30');
  const [manualOverride, setManualOverride] = useState('');
  const [dayNumber, setDayNumber] = useState('1');
  const [hourNotes, setHourNotes] = useState('');

  const calculatedHrs = calculateDurationHours(
    startTime,
    endTime,
    Number.parseInt(breakMinutes, 10) || 0
  );

  const completedCalls = callingTasks.filter(
    (t) => t.callStatus === 'DETAILS_VERIFIED' || t.callStatus === 'COMPLETED'
  ).length;
  const followUps = callingTasks.filter((t) => t.followUpRequired).length;

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      {/* Sub-tabs */}
      <div className="flex gap-2 overflow-x-auto rounded-2xl border border-maatram-outline bg-white p-1.5 shadow-sm">
        <button
          type="button"
          onClick={() => setSelectedTab('assignments')}
          className={`flex-1 whitespace-nowrap rounded-xl px-4 py-2.5 text-sm font-semibold transition ${
            selectedTab === 'assignments'
              ? 'bg-maatram-blue text-white'
              : 'text-maatram-navy hover:bg-maatram-blueSoft'
          }`}
        >
          Assignments ({assignments.length})
        </button>
        <button
          type="button"
          onClick={() => setSelectedTab('pv')}
          className={`flex-1 whitespace-nowrap rounded-xl px-4 py-2.5 text-sm font-semibold transition ${
            selectedTab === 'pv'
              ? 'bg-maatram-blue text-white'
              : 'text-maatram-navy hover:bg-maatram-blueSoft'
          }`}
        >
          PV Cases ({pvCases.length})
        </button>
        <button
          type="button"
          onClick={() => setSelectedTab('calling')}
          className={`flex-1 whitespace-nowrap rounded-xl px-4 py-2.5 text-sm font-semibold transition ${
            selectedTab === 'calling'
              ? 'bg-maatram-blue text-white'
              : 'text-maatram-navy hover:bg-maatram-blueSoft'
          }`}
        >
          Calling ({callingTasks.length})
        </button>
      </div>

      {selectedTab === 'assignments' && (
        <div className="space-y-4">
          {assignments.length === 0 ? (
            <EmptyStateCard
              title="No Assigned Activities"
              message="Once the Office Team approves your request or allocates cases, your assignments and WhatsApp group invite links appear here."
            />
          ) : (
            assignments.map((assign) => (
              <div
                key={assign.id}
                className="space-y-3 rounded-2xl border border-maatram-outline bg-white p-5 shadow-sm"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <CategoryBadge category={assign.category} />
                  <StatusPill status={assign.status} />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-maatram-navy">{assign.opportunityTitle}</h3>
                  <p className="text-sm text-slate-500">
                    {assign.district} • Cases: {assign.assignedCasesCount}
                  </p>
                </div>
                {assign.progressNotes && (
                  <p className="text-sm text-maatram-navy">Notes: {assign.progressNotes}</p>
                )}
                {assign.whatsappGroupLink && (
                  <a
                    href={assign.whatsappGroupLink}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex w-full items-center justify-center gap-2 rounded-xl border border-maatram-blue px-4 py-2.5 text-sm font-semibold text-maatram-blue hover:bg-maatram-blueSoft"
                  >
                    <ExternalLink className="h-4 w-4" />
                    Join WhatsApp Group (Invite Link)
                  </a>
                )}
                <div className="flex flex-wrap gap-2">
                  {assign.status !== 'VERIFIED' && (
                    <button
                      type="button"
                      onClick={() => {
                        setEditingAssignment(assign);
                        setAssignStatus(
                          assign.status === 'COMPLETED' ? 'COMPLETED' : 'IN_PROGRESS'
                        );
                        setAssignNotes(assign.progressNotes);
                      }}
                      className="h-11 flex-1 rounded-xl border border-maatram-outline px-4 text-sm font-semibold text-maatram-navy hover:bg-slate-50"
                    >
                      Update Status
                    </button>
                  )}
                  <button
                    type="button"
                    disabled={isBusy}
                    onClick={() => {
                      setHourAssignment(assign);
                      setHourNotes('');
                      setManualOverride('');
                    }}
                    className="h-11 flex-1 rounded-xl bg-maatram-blue px-4 text-sm font-bold text-white hover:bg-maatram-blueDark disabled:opacity-50"
                  >
                    Log Hours
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {selectedTab === 'pv' && (
        <div className="space-y-4">
          <div>
            <h3 className="text-base font-bold text-maatram-navy">Physical Verification Workspace</h3>
            <p className="text-xs text-slate-500">
              Only anonymized Applicant Codes and Zone labels assigned to you are visible here.
            </p>
          </div>
          {pvCases.length === 0 ? (
            <EmptyStateCard
              title="No Physical Verification Cases Assigned"
              message="When Office Team allocates district PV cases to you via the Fair Allocation Engine, they appear here."
            />
          ) : (
            pvCases.map((pv) => (
              <div
                key={pv.id}
                className="space-y-2.5 rounded-2xl border border-maatram-outline bg-white p-5 shadow-sm"
              >
                <div className="flex items-center justify-between gap-2">
                  <h4 className="font-bold text-maatram-navy">{pv.caseRefId}</h4>
                  <StatusPill status={pv.status} />
                </div>
                <p className="text-sm text-slate-600">
                  {pv.district} • {pv.areaLabel} • Applicant: {pv.applicantCode}
                </p>
                {pv.visitNotes && (
                  <p className="text-sm text-maatram-navy">Visit Notes: {pv.visitNotes}</p>
                )}
                {pv.status !== 'VERIFIED' && (
                  <button
                    type="button"
                    onClick={() => {
                      setEditingPvCase(pv);
                      setPvStatus('COMPLETED');
                      setPvNotes(pv.visitNotes);
                      setPvSummary(pv.recommendationSummary);
                    }}
                    className="h-11 w-full rounded-xl bg-maatram-blue text-sm font-bold text-white hover:bg-maatram-blueDark"
                  >
                    Update Visit Status
                  </button>
                )}
              </div>
            ))
          )}
        </div>
      )}

      {selectedTab === 'calling' && (
        <div className="space-y-4">
          <h3 className="text-base font-bold text-maatram-navy">
            Calling &amp; BGV Sheet ({completedCalls}/{callingTasks.length} Verified • {followUps}{' '}
            Follow-ups)
          </h3>
          {callingTasks.length === 0 ? (
            <EmptyStateCard
              title="No Calling Tasks Assigned"
              message="Assigned applicant verification calls will appear here with privacy-safe Reference IDs."
            />
          ) : (
            callingTasks.map((task) => (
              <div
                key={task.id}
                className="space-y-2.5 rounded-2xl border border-maatram-outline bg-white p-5 shadow-sm"
              >
                <div className="flex items-center justify-between gap-2">
                  <h4 className="font-bold text-maatram-navy">{task.applicantRefId}</h4>
                  <StatusPill status={task.callStatus} />
                </div>
                <p className="text-sm text-slate-600">
                  {task.district} • Call Date: {task.callingDate}
                </p>
                {task.followUpRequired && (
                  <p className="text-xs font-semibold text-maatram-red">Follow-up required</p>
                )}
                {task.notes && <p className="text-sm text-maatram-navy">Notes: {task.notes}</p>}
                {!task.verifiedByOffice ? (
                  <button
                    type="button"
                    onClick={() => {
                      setEditingCallTask(task);
                      setCallStatus((task.callStatus as CallStatus) || 'NOT_STARTED');
                      setCallFollowUp(task.followUpRequired);
                      setCallNotes(task.notes);
                    }}
                    className="h-11 w-full rounded-xl bg-maatram-blue text-sm font-bold text-white hover:bg-maatram-blueDark"
                  >
                    Log Call Result
                  </button>
                ) : (
                  <StatusPill status="VERIFIED BY OFFICE" />
                )}
              </div>
            ))
          )}
        </div>
      )}

      {/* Update Assignment Modal */}
      <ModalDialog
        open={Boolean(editingAssignment)}
        title="Update Assignment Progress"
        onClose={() => setEditingAssignment(null)}
      >
        {editingAssignment && (
          <div className="space-y-4">
            <div className="flex gap-2">
              {(['IN_PROGRESS', 'COMPLETED'] as AssignmentStatus[]).map((st) => (
                <button
                  key={st}
                  type="button"
                  onClick={() => setAssignStatus(st)}
                  className={`rounded-xl border px-3.5 py-2 text-xs font-semibold ${
                    assignStatus === st
                      ? 'border-maatram-blue bg-maatram-blue text-white'
                      : 'border-maatram-outline bg-white text-maatram-navy'
                  }`}
                >
                  {ASSIGNMENT_STATUS_LABELS[st]}
                </button>
              ))}
            </div>
            <div>
              <label className="block text-xs font-semibold text-maatram-navy">
                Progress / Interview Feedback Notes
              </label>
              <textarea
                rows={3}
                value={assignNotes}
                onChange={(e) => setAssignNotes(e.target.value)}
                className="mt-1 w-full rounded-xl border border-maatram-outline p-3 text-sm text-black outline-none focus:border-maatram-blue"
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setEditingAssignment(null)}
                className="h-10 rounded-xl px-4 text-sm font-semibold text-slate-600"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  onUpdateAssignment(editingAssignment.id, assignStatus, assignNotes);
                  setEditingAssignment(null);
                }}
                className="h-10 rounded-xl bg-maatram-blue px-5 text-sm font-bold text-white"
              >
                Save
              </button>
            </div>
          </div>
        )}
      </ModalDialog>

      {/* Update PV Case Modal */}
      <ModalDialog
        open={Boolean(editingPvCase)}
        title={`Update PV Case: ${editingPvCase?.caseRefId ?? ''}`}
        onClose={() => setEditingPvCase(null)}
      >
        {editingPvCase && (
          <div className="space-y-4">
            <div className="flex gap-2">
              {(['VISITED', 'COMPLETED'] as PvCaseStatus[]).map((st) => (
                <button
                  key={st}
                  type="button"
                  onClick={() => setPvStatus(st)}
                  className={`rounded-xl border px-3.5 py-2 text-xs font-semibold ${
                    pvStatus === st
                      ? 'border-maatram-blue bg-maatram-blue text-white'
                      : 'border-maatram-outline bg-white text-maatram-navy'
                  }`}
                >
                  {PV_STATUS_LABELS[st]}
                </button>
              ))}
            </div>
            <div>
              <label className="block text-xs font-semibold text-maatram-navy">
                Visit Observations
              </label>
              <textarea
                rows={2}
                value={pvNotes}
                onChange={(e) => setPvNotes(e.target.value)}
                className="mt-1 w-full rounded-xl border border-maatram-outline p-3 text-sm text-black outline-none focus:border-maatram-blue"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-maatram-navy">
                Verification Recommendation Summary
              </label>
              <input
                type="text"
                value={pvSummary}
                onChange={(e) => setPvSummary(e.target.value)}
                className="mt-1 w-full rounded-xl border border-maatram-outline p-3 text-sm text-black outline-none focus:border-maatram-blue"
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setEditingPvCase(null)}
                className="h-10 rounded-xl px-4 text-sm font-semibold text-slate-600"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  onUpdatePvCase(editingPvCase.id, pvStatus, pvNotes, pvSummary);
                  setEditingPvCase(null);
                }}
                className="h-10 rounded-xl bg-maatram-blue px-5 text-sm font-bold text-white"
              >
                Submit Update
              </button>
            </div>
          </div>
        )}
      </ModalDialog>

      {/* Update Calling Task Modal */}
      <ModalDialog
        open={Boolean(editingCallTask)}
        title={`Calling Log: ${editingCallTask?.applicantRefId ?? ''}`}
        onClose={() => setEditingCallTask(null)}
      >
        {editingCallTask && (
          <div className="space-y-4">
            <div className="flex flex-wrap gap-2">
              {(Object.keys(CALL_STATUS_LABELS) as CallStatus[]).map((st) => (
                <button
                  key={st}
                  type="button"
                  onClick={() => setCallStatus(st)}
                  className={`rounded-xl border px-3 py-1.5 text-xs font-semibold ${
                    callStatus === st
                      ? 'border-maatram-blue bg-maatram-blue text-white'
                      : 'border-maatram-outline bg-white text-maatram-navy'
                  }`}
                >
                  {CALL_STATUS_LABELS[st]}
                </button>
              ))}
            </div>
            <label className="flex items-center justify-between rounded-xl border border-maatram-outline p-3 text-sm font-medium text-maatram-navy">
              <span>Follow-Up Required</span>
              <input
                type="checkbox"
                checked={callFollowUp}
                onChange={(e) => setCallFollowUp(e.target.checked)}
                className="h-4 w-4 accent-maatram-blue"
              />
            </label>
            <div>
              <label className="block text-xs font-semibold text-maatram-navy">
                Verification Notes
              </label>
              <textarea
                rows={3}
                value={callNotes}
                onChange={(e) => setCallNotes(e.target.value)}
                className="mt-1 w-full rounded-xl border border-maatram-outline p-3 text-sm text-black outline-none focus:border-maatram-blue"
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setEditingCallTask(null)}
                className="h-10 rounded-xl px-4 text-sm font-semibold text-slate-600"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  onUpdateCallingTask(editingCallTask.id, callStatus, callFollowUp, callNotes);
                  setEditingCallTask(null);
                }}
                className="h-10 rounded-xl bg-maatram-blue px-5 text-sm font-bold text-white"
              >
                Save Call Log
              </button>
            </div>
          </div>
        )}
      </ModalDialog>

      {/* Submit Volunteer Hours Modal */}
      <ModalDialog
        open={Boolean(hourAssignment)}
        title="Submit Volunteer Hours"
        onClose={() => setHourAssignment(null)}
      >
        {hourAssignment && (
          <div className="space-y-3">
            <p className="text-xs text-slate-500">
              {hourAssignment.opportunityTitle} (Requires Office verification; login duration is
              never counted)
            </p>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-maatram-navy">
                  Date (YYYY-MM-DD)
                </label>
                <input
                  type="date"
                  value={activityDate}
                  onChange={(e) => setActivityDate(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-maatram-navy">Day #</label>
                <input
                  type="number"
                  min={1}
                  value={dayNumber}
                  onChange={(e) => setDayNumber(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-maatram-navy">
                  Start (HH:mm)
                </label>
                <input
                  type="time"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-maatram-navy">End (HH:mm)</label>
                <input
                  type="time"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-maatram-navy">
                  Break (mins)
                </label>
                <input
                  type="number"
                  min={0}
                  value={breakMinutes}
                  onChange={(e) => setBreakMinutes(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-maatram-navy">
                  Manual Hrs (opt)
                </label>
                <input
                  type="number"
                  step="0.25"
                  value={manualOverride}
                  onChange={(e) => setManualOverride(e.target.value)}
                  placeholder="Auto"
                  className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
                />
              </div>
            </div>
            <p className="rounded-xl bg-maatram-blueSoft px-3 py-2 text-sm font-bold text-maatram-navy">
              Calculated Duration: {calculatedHrs.toFixed(2)} hrs
            </p>
            <div>
              <label className="block text-xs font-semibold text-maatram-navy">
                Contribution Notes
              </label>
              <textarea
                rows={2}
                value={hourNotes}
                onChange={(e) => setHourNotes(e.target.value)}
                className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setHourAssignment(null)}
                className="h-10 rounded-xl px-4 text-sm font-semibold text-slate-600"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  const parsedOverride = Number.parseFloat(manualOverride);
                  onSubmitHours(
                    hourAssignment,
                    activityDate,
                    startTime,
                    endTime,
                    Number.parseInt(breakMinutes, 10) || 0,
                    Number.isFinite(parsedOverride) ? parsedOverride : null,
                    Math.max(1, Number.parseInt(dayNumber, 10) || 1),
                    hourNotes
                  );
                  setHourAssignment(null);
                }}
                className="h-10 rounded-xl bg-maatram-blue px-5 text-sm font-bold text-white"
              >
                Submit Hours
              </button>
            </div>
          </div>
        )}
      </ModalDialog>
    </div>
  );
};
