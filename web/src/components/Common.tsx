import React, { useState } from 'react';
import {
  BarChart3,
  CheckCircle2,
  Clock,
  Inbox,
  X,
} from 'lucide-react';
import {
  CATEGORY_META,
  ORDERED_CATEGORIES,
  type OpportunityCategory,
  type VolunteerHours,
} from '../types/maatram';

interface StatSummaryCardProps {
  title: string;
  value: string;
  subtitle: string;
  icon: React.ReactNode;
  accent?: 'blue' | 'green' | 'orange';
}

export const StatSummaryCard: React.FC<StatSummaryCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  accent = 'blue',
}) => {
  const accentStyles =
    accent === 'green'
      ? 'bg-maatram-greenSoft text-maatram-green'
      : accent === 'orange'
      ? 'bg-maatram-orangeSoft text-maatram-orange'
      : 'bg-maatram-blueSoft text-maatram-blue';

  return (
    <div className="rounded-2xl border border-maatram-outline bg-white p-4 shadow-sm transition hover:border-maatram-blue/40">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{title}</p>
          <p className="mt-1.5 text-2xl font-bold text-maatram-navy">{value}</p>
          <p className="mt-0.5 truncate text-xs text-slate-500">{subtitle}</p>
        </div>
        <div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${accentStyles}`}>
          {icon}
        </div>
      </div>
    </div>
  );
};

export const StatusPill: React.FC<{ status: string }> = ({ status }) => {
  const s = status.toUpperCase();
  let cls = 'bg-maatram-blueSoft text-maatram-blue border-maatram-blueContainer';
  if (
    s.includes('APPROVED') ||
    s.includes('VERIFIED') ||
    s === 'OPEN' ||
    s === 'ACTIVE' ||
    s.includes('RECOMMENDED') ||
    s.includes('HIGH PRIORITY')
  ) {
    cls = 'bg-maatram-greenSoft text-maatram-green border-emerald-200';
  } else if (
    s.includes('PENDING') ||
    s.includes('WAITLIST') ||
    s.includes('VISITED') ||
    s.includes('IN_PROGRESS') ||
    s.includes('LESS ACTIVE') ||
    s.includes('UNDER_REVIEW')
  ) {
    cls = 'bg-maatram-orangeSoft text-maatram-orange border-orange-200';
  } else if (
    s.includes('REJECTED') ||
    s.includes('BLOCKED') ||
    s.includes('CLOSED') ||
    s.includes('CANCELLED') ||
    s === 'INACTIVE' ||
    s === 'LOCKED'
  ) {
    cls = 'bg-maatram-redSoft text-maatram-red border-red-200';
  }
  return (
    <span className={`inline-flex items-center rounded-lg border px-2.5 py-0.5 text-xs font-semibold ${cls}`}>
      {status.replace(/_/g, ' ')}
    </span>
  );
};

export const CategoryBadge: React.FC<{ category: string }> = ({ category }) => {
  const meta = CATEGORY_META[category as OpportunityCategory];
  return (
    <span className="inline-flex items-center rounded-lg bg-maatram-blueContainer px-2.5 py-0.5 text-xs font-bold text-maatram-navy">
      {meta ? meta.shortLabel : category}
    </span>
  );
};

export interface BarChartItem {
  label: string;
  value: number;
  unit?: string;
}

export const BarChartCard: React.FC<{
  title: string;
  subtitle: string;
  items: BarChartItem[];
  barColorClass?: string;
}> = ({ title, subtitle, items, barColorClass = 'bg-maatram-blue' }) => {
  const maxVal = Math.max(1, ...items.map((i) => i.value));
  return (
    <div className="rounded-2xl border border-maatram-outline bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between gap-2">
        <div>
          <h3 className="text-base font-bold text-maatram-navy">{title}</h3>
          <p className="text-xs text-slate-500">{subtitle}</p>
        </div>
        <BarChart3 className="h-5 w-5 text-maatram-blue" />
      </div>
      {items.length === 0 ? (
        <p className="mt-4 text-sm text-slate-500">No chart data recorded yet.</p>
      ) : (
        <div className="mt-4 space-y-3">
          {items.map((item) => {
            const pct = Math.max(4, Math.round((item.value / maxVal) * 100));
            return (
              <div key={item.label} className="space-y-1">
                <div className="flex items-center justify-between text-xs font-semibold">
                  <span className="text-maatram-navy">{item.label}</span>
                  <span className="text-maatram-blue">
                    {Number.isInteger(item.value) ? item.value : item.value.toFixed(1)}
                    {item.unit ?? ''}
                  </span>
                </div>
                <div className="h-3 w-full overflow-hidden rounded-full bg-maatram-blueSoft">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${barColorClass}`}
                    style={{ width: `${item.value <= 0 ? 2 : pct}%` }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export const VolunteerVisualSummaryChart: React.FC<{
  myHours: VolunteerHours[];
  pvDone: number;
  callsDone: number;
  otherActivitiesDone: number;
}> = ({ myHours, pvDone, callsDone, otherActivitiesDone }) => {
  const [mode, setMode] = useState<'hours' | 'tasks'>('hours');

  const approvedHours = myHours.filter((h) => h.verificationStatus === 'APPROVED');
  const pendingHours = myHours.filter((h) => h.verificationStatus === 'PENDING');
  const totalApprovedHrs = approvedHours.reduce((sum, h) => sum + h.submittedHours, 0);
  const totalPendingHrs = pendingHours.reduce((sum, h) => sum + h.submittedHours, 0);
  const totalTasks = pvDone + callsDone + otherActivitiesDone;

  const hoursByCat: BarChartItem[] = ORDERED_CATEGORIES.map((cat) => ({
    label: CATEGORY_META[cat].shortLabel,
    value: approvedHours
      .filter((h) => h.category === cat)
      .reduce((sum, h) => sum + h.submittedHours, 0),
    unit: 'h',
  }));

  const tasksByType: BarChartItem[] = [
    { label: 'Physical PV', value: pvDone, unit: ' done' },
    { label: 'Call Verif.', value: callsDone, unit: ' done' },
    { label: 'Other Drives', value: otherActivitiesDone, unit: ' done' },
  ];

  const activeItems = mode === 'hours' ? hoursByCat : tasksByType;
  const maxVal = Math.max(1, ...activeItems.map((i) => i.value));

  return (
    <div
      data-testid="volunteer_visual_summary_chart"
      className="rounded-2xl border border-maatram-outline bg-white p-5 shadow-sm"
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <BarChart3 className="h-5 w-5 text-maatram-blue" />
            <h3 className="text-base font-bold text-maatram-navy">Volunteer Contribution Summary</h3>
          </div>
          <p className="mt-0.5 text-xs text-slate-500">
            Total Verified: <strong className="text-maatram-navy">{totalApprovedHrs.toFixed(1)} hrs</strong>
            {totalPendingHrs > 0 ? ` (${totalPendingHrs.toFixed(1)}h pending)` : ''} • Completed Tasks:{' '}
            <strong className="text-maatram-navy">{totalTasks}</strong>
          </p>
        </div>

        <div className="inline-flex rounded-xl border border-maatram-outline bg-maatram-surface p-1">
          <button
            type="button"
            onClick={() => setMode('hours')}
            className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
              mode === 'hours'
                ? 'bg-maatram-blue text-white shadow-sm'
                : 'text-maatram-navy hover:bg-white'
            }`}
          >
            <Clock className="h-3.5 w-3.5" />
            Hours ({totalApprovedHrs.toFixed(1)}h)
          </button>
          <button
            type="button"
            onClick={() => setMode('tasks')}
            className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
              mode === 'tasks'
                ? 'bg-maatram-blue text-white shadow-sm'
                : 'text-maatram-navy hover:bg-white'
            }`}
          >
            <CheckCircle2 className="h-3.5 w-3.5" />
            Tasks ({totalTasks})
          </button>
        </div>
      </div>

      {/* Vertical column bar chart */}
      <div className="mt-5 grid grid-cols-3 gap-3 sm:grid-cols-4">
        {activeItems.map((item) => {
          const heightPct = item.value <= 0 ? 6 : Math.max(14, Math.round((item.value / maxVal) * 100));
          return (
            <div
              key={item.label}
              className="flex flex-col items-center justify-end rounded-xl bg-maatram-surface p-3"
            >
              <span className="mb-2 text-xs font-bold text-maatram-blue">
                {mode === 'hours' ? `${item.value.toFixed(1)}h` : `${item.value}`}
              </span>
              <div className="flex h-28 w-full max-w-[44px] items-end justify-center rounded-lg bg-maatram-blueSoft p-1">
                <div
                  className={`w-full rounded-md transition-all duration-500 ${
                    mode === 'hours' ? 'bg-maatram-blue' : 'bg-maatram-green'
                  }`}
                  style={{ height: `${heightPct}%` }}
                />
              </div>
              <span className="mt-2 text-center text-xs font-semibold text-maatram-navy">
                {item.label}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export const EmptyStateCard: React.FC<{ title: string; message: string }> = ({ title, message }) => (
  <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-maatram-outline bg-white p-8 text-center">
    <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-maatram-blueSoft text-maatram-blue">
      <Inbox className="h-6 w-6" />
    </div>
    <h4 className="mt-3 text-base font-bold text-maatram-navy">{title}</h4>
    <p className="mt-1 max-w-md text-sm text-slate-500">{message}</p>
  </div>
);

export const ModalDialog: React.FC<{
  open: boolean;
  title: string;
  onClose: () => void;
  children: React.ReactNode;
}> = ({ open, title, onClose, children }) => {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs">
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-maatram-outline bg-white p-6 shadow-xl">
        <div className="flex items-center justify-between gap-2 border-b border-maatram-outline pb-3">
          <h3 className="text-lg font-bold text-maatram-navy">{title}</h3>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close modal"
            className="flex h-9 w-9 items-center justify-center rounded-xl text-slate-500 hover:bg-maatram-blueSoft hover:text-maatram-navy"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="mt-4">{children}</div>
      </div>
    </div>
  );
};

export const ToastBanner: React.FC<{
  message: string | null;
  onDismiss: () => void;
}> = ({ message, onDismiss }) => {
  if (!message) return null;
  return (
    <div className="fixed bottom-20 right-4 z-50 flex max-w-md items-center gap-3 rounded-2xl border border-maatram-blue bg-maatram-navy px-4 py-3 text-sm font-medium text-white shadow-lg md:bottom-6">
      <CheckCircle2 className="h-5 w-5 shrink-0 text-sky-300" />
      <span className="flex-1">{message}</span>
      <button
        type="button"
        onClick={onDismiss}
        aria-label="Dismiss notification"
        className="rounded-lg p-1 text-slate-300 hover:bg-white/10 hover:text-white"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
};
