"use client";

import { type ReactNode, FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { Briefcase, CalendarDays, CheckCircle2, Plus, Send, ShieldCheck, Trash2 } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/erm/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { FixedInputField, FloatingTextareaField, LabeledSelectField } from "@/components/ui/form-fields";
import { Spinner } from "@/components/ui/spinner";
import { loadSession } from "@/lib/auth-storage";
import {
  ApiError,
  getMyTimesheets,
  getSelfDashboard,
  getTimesheetApprovals,
  getTimesheetApproverVisibility,
  submitTimesheet,
  takeTimesheetAction,
  type SelfDashboard,
  type TimesheetActionDecision,
  type TimesheetEntry,
  type TimesheetResponse,
  type TimesheetStatus,
  type TimesheetWorkType,
} from "@/lib/api";

type DraftTimesheetRow = {
  id: string;
  workType: TimesheetWorkType;
  projectAllocationId: string;
  taskName: string;
  mondayHours: string;
  tuesdayHours: string;
  wednesdayHours: string;
  thursdayHours: string;
  fridayHours: string;
  saturdayHours: string;
  sundayHours: string;
};

const DAYS = [
  { key: "mondayHours", label: "Mon" },
  { key: "tuesdayHours", label: "Tue" },
  { key: "wednesdayHours", label: "Wed" },
  { key: "thursdayHours", label: "Thu" },
  { key: "fridayHours", label: "Fri" },
  { key: "saturdayHours", label: "Sat" },
  { key: "sundayHours", label: "Sun" },
] as const;

function localDateString(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function mondayOfWeek(date: Date) {
  const clone = new Date(date);
  const day = clone.getDay() === 0 ? 7 : clone.getDay();
  if (day !== 1) {
    clone.setDate(clone.getDate() - (day - 1));
  }
  return clone;
}

function formatDateOnly(value: string) {
  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

function formatHours(value: number | string) {
  const numeric = typeof value === "string" ? Number(value) : value;
  if (!Number.isFinite(numeric)) {
    return "0";
  }
  return numeric.toLocaleString("en-IN", { maximumFractionDigits: 2 });
}

function formatWorkType(value: TimesheetWorkType) {
  return value === "BILLABLE" ? "Billable" : "Non-billable";
}

function formatTimesheetStatus(value: TimesheetStatus) {
  if (value === "APPROVED") return "Approved";
  if (value === "REJECTED") return "Rejected";
  return "Pending manager approval";
}

function isWeekendDay(key: typeof DAYS[number]["key"]) {
  return key === "saturdayHours" || key === "sundayHours";
}

function statusClass(status: TimesheetStatus | string) {
  const value = status.toString().toLowerCase();
  if (value.includes("approved")) return "border-emerald-200 bg-emerald-50 text-emerald-700";
  if (value.includes("rejected")) return "border-rose-200 bg-rose-50 text-rose-700";
  return "border-amber-200 bg-amber-50 text-amber-700";
}

function defaultRow(workType: TimesheetWorkType = "NON_BILLABLE"): DraftTimesheetRow {
  return {
    id: crypto.randomUUID(),
    workType,
    projectAllocationId: "",
    taskName: "",
    mondayHours: "0",
    tuesdayHours: "0",
    wednesdayHours: "0",
    thursdayHours: "0",
    fridayHours: "0",
    saturdayHours: "0",
    sundayHours: "0",
  };
}

function toNumber(value: string) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function sheetTotals(rows: DraftTimesheetRow[]) {
  const totals = {
    total: 0,
    billable: 0,
    nonBillable: 0,
  };
  rows.forEach((row) => {
    const rowTotal = DAYS.reduce((sum, day) => sum + toNumber(row[day.key]), 0);
    totals.total += rowTotal;
    if (row.workType === "BILLABLE") {
      totals.billable += rowTotal;
    } else {
      totals.nonBillable += rowTotal;
    }
  });
  return totals;
}

function statusSummary(timesheets: TimesheetResponse[]) {
  return timesheets.reduce(
      (acc, sheet) => {
        acc.total += 1;
        acc.hours += Number(sheet.totalHours) || 0;
        if (sheet.status === "PENDING_MANAGER_APPROVAL") acc.pending += 1;
        if (sheet.status === "APPROVED") acc.approved += 1;
        if (sheet.status === "REJECTED") acc.rejected += 1;
        return acc;
      },
      { total: 0, pending: 0, approved: 0, rejected: 0, hours: 0 }
  );
}

function TimesheetEntryTable({ entries }: { entries: TimesheetEntry[] }) {
  return (
      <div className="overflow-x-auto rounded-2xl border border-zinc-200">
        <table className="w-full min-w-[1100px] text-sm">
          <thead className="bg-zinc-50 text-left">
          <tr>
            <th className="px-4 py-3">Task</th>
            <th className="px-4 py-3">Project</th>
            <th className="px-4 py-3">Type</th>
            {DAYS.map((day) => (
                <th className="px-4 py-3" key={day.key}>
                  {day.label}
                </th>
            ))}
            <th className="px-4 py-3">Total</th>
          </tr>
          </thead>
          <tbody>
          {entries.map((entry) => (
              <tr className="border-t border-zinc-200" key={entry.id}>
                <td className="px-4 py-3 font-medium text-zinc-900">{entry.taskName}</td>
                <td className="px-4 py-3 text-zinc-700">
                  {entry.projectName ? (
                      <>
                        {entry.projectName}
                        <span className="block text-xs text-zinc-500">{entry.projectCode}</span>
                      </>
                  ) : (
                      "-"
                  )}
                </td>
                <td className="px-4 py-3">
                  <Badge className={entry.workType === "BILLABLE" ? "border-sky-200 bg-sky-50 text-sky-700" : "border-zinc-200 bg-zinc-50 text-zinc-700"}>
                    {formatWorkType(entry.workType)}
                  </Badge>
                </td>
                <td className="px-4 py-3">{formatHours(entry.mondayHours)}</td>
                <td className="px-4 py-3">{formatHours(entry.tuesdayHours)}</td>
                <td className="px-4 py-3">{formatHours(entry.wednesdayHours)}</td>
                <td className="px-4 py-3">{formatHours(entry.thursdayHours)}</td>
                <td className="px-4 py-3">{formatHours(entry.fridayHours)}</td>
                <td className="px-4 py-3">{formatHours(entry.saturdayHours)}</td>
                <td className="px-4 py-3">{formatHours(entry.sundayHours)}</td>
                <td className="px-4 py-3 font-semibold text-zinc-900">{formatHours(entry.totalHours)}</td>
              </tr>
          ))}
          </tbody>
        </table>
      </div>
  );
}

export default function AttendancePage() {
  const accessToken = useMemo(() => loadSession()?.accessToken ?? null, []);
  const initialWeekStart = useMemo(() => localDateString(mondayOfWeek(new Date())), []);

  const [dashboard, setDashboard] = useState<SelfDashboard | null>(null);
  const [myTimesheets, setMyTimesheets] = useState<TimesheetResponse[]>([]);
  const [approvalQueue, setApprovalQueue] = useState<TimesheetResponse[]>([]);
  const [showApproverRequests, setShowApproverRequests] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [activeTab, setActiveTab] = useState<"submit" | "mine" | "approvals">("submit");
  const [selectedTimesheet, setSelectedTimesheet] = useState<TimesheetResponse | null>(null);
  const [selectedApproval, setSelectedApproval] = useState<TimesheetResponse | null>(null);
  const [approvalAction, setApprovalAction] = useState<TimesheetActionDecision>("APPROVE");
  const [approvalComment, setApprovalComment] = useState("");
  const [weekStartDate, setWeekStartDate] = useState(initialWeekStart);
  const [submissionComment, setSubmissionComment] = useState("");
  const [rows, setRows] = useState<DraftTimesheetRow[]>([defaultRow()]);

  const billableProjects = useMemo(
      () => dashboard?.currentProjects.filter((project) => project.allocationType.toLowerCase() === "billable") ?? [],
      [dashboard]
  );
  const nonBillableProjects = useMemo(
      () => dashboard?.currentProjects.filter((project) => project.allocationType.toLowerCase() !== "billable") ?? [],
      [dashboard]
  );
  const billableProjectIdSet = useMemo(
      () => new Set(billableProjects.map((project) => String(project.allocationId))),
      [billableProjects]
  );
  const nonBillableProjectIdSet = useMemo(
      () => new Set(nonBillableProjects.map((project) => String(project.allocationId))),
      [nonBillableProjects]
  );
  const billableCapacity = useMemo(
      () => billableProjects.reduce((sum, project) => sum + project.allocationPercent, 0),
      [billableProjects]
  );
  const nonBillableCapacity = useMemo(
      () => nonBillableProjects.reduce((sum, project) => sum + project.allocationPercent, 0),
      [nonBillableProjects]
  );
  const currentTotals = useMemo(() => sheetTotals(rows), [rows]);
  const mySummary = useMemo(() => statusSummary(myTimesheets), [myTimesheets]);

  const loadData = useCallback(async () => {
    if (!accessToken) {
      toast.error("Session not found. Please login again.");
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      const [dashboardData, mine, approvals, visibility] = await Promise.all([
        getSelfDashboard(accessToken),
        getMyTimesheets(accessToken),
        getTimesheetApprovals(accessToken),
        getTimesheetApproverVisibility(accessToken),
      ]);

      setDashboard(dashboardData);
      setMyTimesheets(mine);
      setApprovalQueue(approvals);
      setShowApproverRequests(visibility.showApproverRequests);
      setSelectedTimesheet((current) => {
        if (current && mine.some((sheet) => sheet.id === current.id)) {
          return current;
        }
        return mine[0] ?? null;
      });
      setSelectedApproval((current) => {
        if (current && approvals.some((sheet) => sheet.id === current.id)) {
          return current;
        }
        return approvals[0] ?? null;
      });
    } catch {
      toast.error("Unable to load timesheet details.");
    } finally {
      setLoading(false);
    }
  }, [accessToken]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadData();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [loadData]);

  const effectiveTab = !showApproverRequests && activeTab === "approvals" ? "submit" : activeTab;

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accessToken) {
      toast.error("Session not found. Please login again.");
      return;
    }
    if (!weekStartDate) {
      toast.error("Week start date is required.");
      return;
    }
    if (rows.length === 0) {
      toast.error("Add at least one timesheet row.");
      return;
    }

    const payloadRows = rows.map((row) => ({
      workType: row.workType,
      projectAllocationId: row.projectAllocationId ? Number(row.projectAllocationId) : null,
      taskName: row.taskName.trim(),
      mondayHours: toNumber(row.mondayHours),
      tuesdayHours: toNumber(row.tuesdayHours),
      wednesdayHours: toNumber(row.wednesdayHours),
      thursdayHours: toNumber(row.thursdayHours),
      fridayHours: toNumber(row.fridayHours),
      saturdayHours: toNumber(row.saturdayHours),
      sundayHours: toNumber(row.sundayHours),
    }));

    for (const row of payloadRows) {
      if (row.workType === "BILLABLE") {
        if (row.projectAllocationId === null || !billableProjectIdSet.has(String(row.projectAllocationId))) {
          toast.error("Billable rows need a billable project allocation.");
          return;
        }
      } else if (row.projectAllocationId !== null && !nonBillableProjectIdSet.has(String(row.projectAllocationId))) {
        toast.error("Non-billable rows must use a non-billable allocation or no project.");
        return;
      }
    }

    setSubmitting(true);
    try {
      await submitTimesheet(accessToken, {
        weekStartDate,
        submissionComment: submissionComment.trim() || undefined,
        rows: payloadRows,
      });
      toast.success("Timesheet submitted successfully.");
      setRows([defaultRow()]);
      setSubmissionComment("");
      await loadData();
      setActiveTab("mine");
    } catch (error) {
      if (error instanceof ApiError) {
        toast.error(error.message || `Unable to submit timesheet (${error.status})`);
      } else {
        toast.error("Unable to submit timesheet.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  async function submitApprovalAction() {
    if (!selectedApproval) {
      return;
    }
    if (!accessToken) {
      toast.error("Session not found. Please login again.");
      return;
    }
    if (!approvalComment.trim()) {
      toast.error("Comment is required for approval actions.");
      return;
    }

    setSubmitting(true);
    try {
      await takeTimesheetAction(accessToken, selectedApproval.id, {
        decision: approvalAction,
        comment: approvalComment.trim(),
      });
      toast.success(`Timesheet ${approvalAction === "APPROVE" ? "approved" : "rejected"} successfully.`);
      setApprovalComment("");
      await loadData();
      setSelectedApproval(null);
    } catch (error) {
      if (error instanceof ApiError) {
        toast.error(error.message || `Unable to action timesheet (${error.status})`);
      } else {
        toast.error("Unable to action timesheet.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  function updateRow(rowId: string, patch: Partial<DraftTimesheetRow>) {
    setRows((current) =>
        current.map((row) =>
            row.id === rowId
                ? {
                  ...row,
                  ...patch,
                  projectAllocationId:
                      patch.workType === "NON_BILLABLE"
                          ? ""
                          : patch.workType === "BILLABLE" && patch.projectAllocationId === ""
                              ? row.projectAllocationId
                              : patch.projectAllocationId ?? row.projectAllocationId,
                }
                : row
        )
    );
  }

  function addRow() {
    setRows((current) => [...current, defaultRow(billableProjects.length > 0 ? "BILLABLE" : "NON_BILLABLE")]);
  }

  function removeRow(rowId: string) {
    setRows((current) => (current.length === 1 ? current : current.filter((row) => row.id !== rowId)));
  }

  return (
      <>
        <PageHeader description="Fill weekly timesheets and manage approval flow for billable work." title="Attendance" />

        <div className="mb-5 flex flex-wrap gap-2 rounded-2xl border border-zinc-200 bg-white p-2 shadow-sm">
          <Button
              className={effectiveTab === "submit" ? "bg-gradient-to-r from-blue-600 to-indigo-600 text-white" : ""}
              onClick={() => setActiveTab("submit")}
              variant={effectiveTab === "submit" ? "default" : "ghost"}
          >
            Submit Timesheet
          </Button>
          <Button
              className={effectiveTab === "mine" ? "bg-gradient-to-r from-cyan-600 to-blue-600 text-white" : ""}
              onClick={() => setActiveTab("mine")}
              variant={effectiveTab === "mine" ? "default" : "ghost"}
          >
            My Timesheets
          </Button>
          {showApproverRequests ? (
              <Button
                  className={effectiveTab === "approvals" ? "bg-gradient-to-r from-emerald-600 to-teal-600 text-white" : ""}
                  onClick={() => setActiveTab("approvals")}
                  variant={effectiveTab === "approvals" ? "default" : "ghost"}
              >
                Approvals
              </Button>
          ) : null}
        </div>

        {effectiveTab === "submit" ? (
            <Card className="border-blue-100 shadow-md shadow-blue-100/40">
              <CardHeader className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white">
                <CardTitle className="text-white">Weekly timesheet</CardTitle>
                <CardDescription className="text-blue-100">
                  Monday to Sunday. Billable rows go to your reporting manager; non-billable rows auto-approve.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-5 pt-5">
                {loading ? (
                    <div className="flex justify-center py-10">
                      <Spinner />
                    </div>
                ) : (
                    <form className="space-y-5" onSubmit={handleSubmit}>
                      <div className="grid gap-4 lg:grid-cols-4">
                        <FixedInputField
                            label="Week Start (Monday)"
                            onChange={(event) => setWeekStartDate(event.target.value)}
                            type="date"
                            value={weekStartDate}
                        />
                        <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-3 shadow-sm">
                          <p className="text-xs font-semibold uppercase tracking-wide text-zinc-500">Project allocations</p>
                          <p className="mt-1 text-sm text-zinc-700">
                            {billableProjects.length} billable • {nonBillableProjects.length} non-billable
                          </p>
                        </div>
                        <div className="rounded-2xl border border-sky-100 bg-sky-50 p-3 shadow-sm">
                          <p className="text-xs font-semibold uppercase tracking-wide text-sky-700">Billable capacity</p>
                          <p className="mt-1 flex items-end gap-2 text-sm text-sky-900">
                            <span className="text-2xl font-semibold">{formatHours(billableCapacity)}</span>
                            <span className="pb-0.5">%</span>
                          </p>
                          <div className="mt-2 h-2 overflow-hidden rounded-full bg-white">
                            <div className="h-full rounded-full bg-sky-500" style={{ width: `${Math.min(billableCapacity, 100)}%` }} />
                          </div>
                        </div>
                        <div className="rounded-2xl border border-amber-100 bg-amber-50 p-3 shadow-sm">
                          <p className="text-xs font-semibold uppercase tracking-wide text-amber-700">Non-billable capacity</p>
                          <p className="mt-1 flex items-end gap-2 text-sm text-amber-900">
                            <span className="text-2xl font-semibold">{formatHours(nonBillableCapacity)}</span>
                            <span className="pb-0.5">%</span>
                          </p>
                          <div className="mt-2 h-2 overflow-hidden rounded-full bg-white">
                            <div className="h-full rounded-full bg-amber-500" style={{ width: `${Math.min(nonBillableCapacity, 100)}%` }} />
                          </div>
                        </div>
                        <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-3 shadow-sm">
                          <p className="text-xs font-semibold uppercase tracking-wide text-zinc-500">Hours summary</p>
                          <p className="mt-1 text-sm text-zinc-700">
                            Total <strong>{formatHours(currentTotals.total)}</strong>h • Billable <strong>{formatHours(currentTotals.billable)}</strong>h • Non-billable{" "}
                            <strong>{formatHours(currentTotals.nonBillable)}</strong>h
                          </p>
                        </div>
                      </div>
                      <p className="text-xs text-zinc-500">
                        Billable and non-billable capacities are tracked separately; combined allocation can exceed 100%.
                      </p>

                      <FloatingTextareaField
                          className="min-h-24"
                          label="Submission comment"
                          onChange={(event) => setSubmissionComment(event.target.value)}
                          value={submissionComment}
                      />

                      <div className="space-y-4">
                        {rows.map((row, index) => (
                            <div className="rounded-3xl border border-zinc-200 bg-white p-4 shadow-sm" key={row.id}>
                              <div className="flex flex-wrap items-center justify-between gap-2">
                                <div className="flex items-center gap-2">
                                  <div className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">Row {index + 1}</div>
                                  <Badge className={row.workType === "BILLABLE" ? "border-sky-200 bg-sky-50 text-sky-700" : "border-zinc-200 bg-zinc-50 text-zinc-700"}>
                                    {formatWorkType(row.workType)}
                                  </Badge>
                                </div>
                                <Button onClick={() => removeRow(row.id)} size="sm" type="button" variant="outline">
                                  <Trash2 className="mr-2 h-4 w-4" />
                                  Remove
                                </Button>
                              </div>

                              <div className="mt-4 grid gap-4 lg:grid-cols-3">
                                <LabeledSelectField
                                    label="Work type"
                                    onChange={(event) =>
                                        updateRow(row.id, {
                                          workType: event.target.value as TimesheetWorkType,
                                          projectAllocationId:
                                              event.target.value === "BILLABLE"
                                                  ? billableProjectIdSet.has(row.projectAllocationId)
                                                      ? row.projectAllocationId
                                                      : ""
                                                  : nonBillableProjectIdSet.has(row.projectAllocationId)
                                                      ? row.projectAllocationId
                                                      : "",
                                        })
                                    }
                                    value={row.workType}
                                >
                                  <option value="NON_BILLABLE">Non-billable</option>
                                  <option disabled={billableProjects.length === 0} value="BILLABLE">
                                    Billable{billableProjects.length === 0 ? " (No billable project)" : ""}
                                  </option>
                                </LabeledSelectField>

                                <LabeledSelectField
                                    disabled={row.workType === "BILLABLE" ? billableProjects.length === 0 : nonBillableProjects.length === 0}
                                    label="Project allocation"
                                    onChange={(event) => updateRow(row.id, { projectAllocationId: event.target.value })}
                                    value={row.projectAllocationId}
                                >
                                  <option value="">
                                    {row.workType === "BILLABLE"
                                        ? billableProjects.length === 0
                                            ? "No billable projects"
                                            : "Select billable project"
                                        : nonBillableProjects.length === 0
                                            ? "No non-billable projects"
                                            : "Optional project"}
                                  </option>
                                  {(row.workType === "BILLABLE" ? billableProjects : nonBillableProjects).map((project) => (
                                      <option key={project.allocationId} value={project.allocationId}>
                                        {project.projectName} ({project.projectCode})
                                      </option>
                                  ))}
                                </LabeledSelectField>

                                <FixedInputField
                                    label="Task name"
                                    onChange={(event) => updateRow(row.id, { taskName: event.target.value })}
                                    value={row.taskName}
                                />
                              </div>

                              <div className="mt-4 grid gap-3 md:grid-cols-7">
                                {DAYS.map((day) => (
                                    <FixedInputField
                                        key={day.key}
                                        label={day.label}
                                        min="0"
                                        disabled={isWeekendDay(day.key)}
                                        className={isWeekendDay(day.key) ? "bg-zinc-100 text-zinc-400" : ""}
                                        onChange={(event) => updateRow(row.id, { [day.key]: event.target.value } as Partial<DraftTimesheetRow>)}
                                        step="0.5"
                                        type="number"
                                        value={row[day.key]}
                                    />
                                ))}
                              </div>
                            </div>
                        ))}
                      </div>

                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <Button onClick={addRow} type="button" variant="outline">
                          <Plus className="mr-2 h-4 w-4" />
                          Add row
                        </Button>
                        <Button className="min-w-44 gap-2" disabled={submitting} type="submit">
                          <Send className="h-4 w-4" />
                          {submitting ? "Submitting..." : "Submit Timesheet"}
                        </Button>
                      </div>
                    </form>
                )}
              </CardContent>
            </Card>
        ) : null}

        {effectiveTab === "mine" ? (
            <div className="space-y-4">
              <section className="grid gap-4 md:grid-cols-4">
                <InfoCard label="Total sheets" value={mySummary.total} tone="blue" icon={<CalendarDays className="h-5 w-5" />} />
                <InfoCard label="Pending review" value={mySummary.pending} tone="amber" icon={<ShieldCheck className="h-5 w-5" />} />
                <InfoCard label="Approved" value={mySummary.approved} tone="emerald" icon={<CheckCircle2 className="h-5 w-5" />} />
                <InfoCard label="Hours logged" value={formatHours(mySummary.hours)} tone="violet" icon={<Briefcase className="h-5 w-5" />} suffix="h" />
              </section>

              <div className="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
                <Card className="overflow-hidden border-sky-100 shadow-md shadow-sky-100/40">
                  <CardHeader className="bg-gradient-to-r from-sky-500 via-blue-600 to-indigo-600 text-white">
                    <CardTitle className="text-white">My timesheets</CardTitle>
                    <CardDescription className="text-sky-100">Weekly submissions with summary, status, and hours.</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-3 p-5">
                    {loading ? (
                        <div className="flex justify-center py-10">
                          <Spinner />
                        </div>
                    ) : myTimesheets.length === 0 ? (
                        <div className="rounded-2xl border border-dashed border-zinc-200 bg-zinc-50 p-5 text-sm text-zinc-600">
                          No timesheets submitted yet.
                        </div>
                    ) : (
                        myTimesheets.map((sheet) => {
                          const isSelected = selectedTimesheet?.id === sheet.id;
                          return (
                              <button
                                  className={`w-full rounded-3xl border p-4 text-left shadow-sm transition ${
                                      isSelected
                                          ? "border-blue-300 bg-gradient-to-r from-blue-50 to-indigo-50 ring-2 ring-blue-100"
                                          : "border-zinc-200 bg-white hover:border-blue-200 hover:bg-blue-50/30"
                                  }`}
                                  key={sheet.id}
                                  onClick={() => setSelectedTimesheet(sheet)}
                                  type="button"
                              >
                                <div className="flex items-start gap-3">
                                  <div className="rounded-2xl bg-white p-2.5 shadow-sm ring-1 ring-zinc-100">
                                    <CalendarDays className="h-5 w-5 text-sky-600" />
                                  </div>
                                  <div className="min-w-0 flex-1">
                                    <div className="flex flex-wrap items-start justify-between gap-3">
                                      <div>
                                        <p className="font-semibold text-zinc-900">{sheet.timesheetCode}</p>
                                        <p className="text-sm text-zinc-500">
                                          {formatDateOnly(sheet.weekStartDate)} - {formatDateOnly(sheet.weekEndDate)}
                                        </p>
                                      </div>
                                      <Badge className={statusClass(sheet.status)}>{formatTimesheetStatus(sheet.status)}</Badge>
                                    </div>
                                    <div className="mt-3 grid gap-2 sm:grid-cols-3">
                                      <MiniStat label="Total" value={`${formatHours(sheet.totalHours)}h`} />
                                      <MiniStat label="Billable" value={`${formatHours(sheet.billableHours)}h`} />
                                      <MiniStat label="Non-billable" value={`${formatHours(sheet.nonBillableHours)}h`} />
                                    </div>
                                  </div>
                                </div>
                              </button>
                          );
                        })
                    )}
                  </CardContent>
                </Card>

                <Card className="overflow-hidden border-zinc-200">
                  <CardHeader className="bg-gradient-to-r from-zinc-900 to-zinc-700 text-white">
                    <CardTitle className="text-white">Timesheet details</CardTitle>
                    <CardDescription className="text-zinc-200">Selected sheet, approval state, and daily breakdown.</CardDescription>
                  </CardHeader>
                  <CardContent className="p-5">
                    {selectedTimesheet ? (
                        <div className="space-y-4">
                          <div className="rounded-3xl bg-gradient-to-r from-sky-50 via-white to-indigo-50 p-4 ring-1 ring-sky-100">
                            <div className="flex flex-wrap items-start justify-between gap-3">
                              <div>
                                <p className="text-sm font-semibold text-zinc-900">{selectedTimesheet.timesheetCode}</p>
                                <p className="text-sm text-zinc-500">
                                  {formatDateOnly(selectedTimesheet.weekStartDate)} - {formatDateOnly(selectedTimesheet.weekEndDate)}
                                </p>
                              </div>
                              <Badge className={statusClass(selectedTimesheet.status)}>{formatTimesheetStatus(selectedTimesheet.status)}</Badge>
                            </div>
                            <div className="mt-4 grid gap-3 sm:grid-cols-3">
                              <MetricBox label="Total" value={`${formatHours(selectedTimesheet.totalHours)}h`} />
                              <MetricBox label="Billable" value={`${formatHours(selectedTimesheet.billableHours)}h`} />
                              <MetricBox label="Non-billable" value={`${formatHours(selectedTimesheet.nonBillableHours)}h`} />
                            </div>
                          </div>

                          <div className="grid gap-3 md:grid-cols-2">
                            <div className="rounded-2xl bg-zinc-50 p-4 text-sm text-zinc-700">
                              <p className="text-xs font-semibold uppercase tracking-wide text-zinc-500">Submission comment</p>
                              <p className="mt-2 leading-6">{selectedTimesheet.submissionComment || "No comment provided."}</p>
                            </div>
                            <div className="rounded-2xl bg-zinc-50 p-4 text-sm text-zinc-700">
                              <p className="text-xs font-semibold uppercase tracking-wide text-zinc-500">Manager comment</p>
                              <p className="mt-2 leading-6">{selectedTimesheet.managerComment || "Pending review."}</p>
                            </div>
                          </div>

                          <TimesheetEntryTable entries={selectedTimesheet.rows} />
                        </div>
                    ) : (
                        <div className="rounded-2xl border border-dashed border-zinc-200 bg-zinc-50 p-5 text-sm text-zinc-600">
                          Select a timesheet to inspect row details.
                        </div>
                    )}
                  </CardContent>
                </Card>
              </div>
            </div>
        ) : null}

        {showApproverRequests && effectiveTab === "approvals" ? (
            <div className="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
              <Card className="border-emerald-100 shadow-md shadow-emerald-100/40">
                <CardHeader className="bg-gradient-to-r from-emerald-500 to-teal-600 text-white">
                  <CardTitle className="text-white">Approval queue</CardTitle>
                  <CardDescription className="text-emerald-50">Billable weekly sheets waiting for your decision.</CardDescription>
                </CardHeader>
                <CardContent className="space-y-3 p-5">
                  {loading ? (
                      <div className="flex justify-center py-10">
                        <Spinner />
                      </div>
                  ) : approvalQueue.length === 0 ? (
                      <div className="rounded-2xl border border-dashed border-zinc-200 bg-zinc-50 p-5 text-sm text-zinc-600">
                        No pending approvals.
                      </div>
                  ) : (
                      approvalQueue.map((sheet) => (
                          <button
                              className={`w-full rounded-2xl border p-4 text-left shadow-sm transition hover:border-emerald-200 hover:bg-emerald-50/30 ${
                                  selectedApproval?.id === sheet.id ? "border-emerald-300 bg-emerald-50/60" : "border-zinc-200 bg-white"
                              }`}
                              key={sheet.id}
                              onClick={() => {
                                setSelectedApproval(sheet);
                                setApprovalComment("");
                                setApprovalAction("APPROVE");
                              }}
                              type="button"
                          >
                            <div className="flex flex-wrap items-start justify-between gap-3">
                              <div>
                                <p className="font-semibold text-zinc-900">{sheet.employeeFullName}</p>
                                <p className="text-sm text-zinc-500">
                                  {sheet.timesheetCode} • {formatDateOnly(sheet.weekStartDate)} - {formatDateOnly(sheet.weekEndDate)}
                                </p>
                              </div>
                              <Badge className="border-amber-200 bg-amber-50 text-amber-700">{sheet.status}</Badge>
                            </div>
                            <div className="mt-3 grid gap-2 text-sm text-zinc-600 sm:grid-cols-2">
                              <span>Total: {formatHours(sheet.totalHours)}h</span>
                              <span>Billable: {formatHours(sheet.billableHours)}h</span>
                            </div>
                          </button>
                      ))
                  )}
                </CardContent>
              </Card>

              <Card className="border-zinc-200">
                <CardHeader>
                  <CardTitle>Review timesheet</CardTitle>
                  <CardDescription>Approve or reject with a comment.</CardDescription>
                </CardHeader>
                <CardContent>
                  {selectedApproval ? (
                      <div className="space-y-4">
                        <div className="flex flex-wrap items-center justify-between gap-3">
                          <div>
                            <p className="text-sm font-semibold text-zinc-900">{selectedApproval.employeeFullName}</p>
                            <p className="text-sm text-zinc-500">{selectedApproval.employeeUsername}</p>
                          </div>
                          <Badge className={statusClass(selectedApproval.status)}>{selectedApproval.status}</Badge>
                        </div>
                        <div className="grid gap-3 sm:grid-cols-3">
                          <MetricBox label="Total" value={`${formatHours(selectedApproval.totalHours)}h`} />
                          <MetricBox label="Billable" value={`${formatHours(selectedApproval.billableHours)}h`} />
                          <MetricBox label="Non-billable" value={`${formatHours(selectedApproval.nonBillableHours)}h`} />
                        </div>
                        <TimesheetEntryTable entries={selectedApproval.rows} />
                        <FloatingTextareaField
                            className="min-h-24"
                            label="Manager comment"
                            onChange={(event) => setApprovalComment(event.target.value)}
                            value={approvalComment}
                        />
                        <div className="flex gap-2">
                          <Button
                              className={approvalAction === "APPROVE" ? "bg-emerald-600 text-white hover:bg-emerald-500" : ""}
                              onClick={() => setApprovalAction("APPROVE")}
                              type="button"
                              variant={approvalAction === "APPROVE" ? "default" : "outline"}
                          >
                            <CheckCircle2 className="mr-2 h-4 w-4" />
                            Approve
                          </Button>
                          <Button
                              className={approvalAction === "REJECT" ? "bg-rose-600 text-white hover:bg-rose-500" : ""}
                              onClick={() => setApprovalAction("REJECT")}
                              type="button"
                              variant={approvalAction === "REJECT" ? "default" : "outline"}
                          >
                            <ShieldCheck className="mr-2 h-4 w-4" />
                            Reject
                          </Button>
                        </div>
                        <Button className="w-full gap-2" disabled={submitting} onClick={submitApprovalAction} type="button">
                          <Send className="h-4 w-4" />
                          {submitting ? "Submitting..." : "Submit decision"}
                        </Button>
                      </div>
                  ) : (
                      <div className="rounded-2xl border border-dashed border-zinc-200 bg-zinc-50 p-5 text-sm text-zinc-600">
                        Select a pending timesheet to review.
                      </div>
                  )}
                </CardContent>
              </Card>
            </div>
        ) : null}
      </>
  );
}

function MetricBox({ label, value }: { label: string; value: string }) {
  return (
      <div className="rounded-2xl bg-zinc-50 p-3">
        <p className="text-xs font-semibold uppercase tracking-wide text-zinc-500">{label}</p>
        <p className="mt-1 text-sm font-semibold text-zinc-900">{value}</p>
      </div>
  );
}

function MiniStat({ label, value }: { label: string; value: string }) {
  return (
      <div className="rounded-2xl border border-zinc-200 bg-white px-3 py-2 text-sm shadow-sm">
        <p className="text-[11px] font-semibold uppercase tracking-wide text-zinc-500">{label}</p>
        <p className="mt-1 font-semibold text-zinc-900">{value}</p>
      </div>
  );
}

function InfoCard({
                    label,
                    value,
                    tone,
                    icon,
                    suffix,
                  }: {
  label: string;
  value: number | string;
  tone: "blue" | "amber" | "emerald" | "violet";
  icon: ReactNode;
  suffix?: string;
}) {
  const toneClasses = {
    blue: "from-blue-600 to-indigo-600 text-white",
    amber: "from-amber-500 to-orange-500 text-white",
    emerald: "from-emerald-500 to-teal-600 text-white",
    violet: "from-violet-600 to-fuchsia-600 text-white",
  }[tone];

  return (
      <Card className={`border-0 bg-gradient-to-br ${toneClasses} shadow-lg`}>
        <CardContent className="p-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.28em] text-white/70">{label}</p>
              <div className="mt-2 flex items-end gap-1">
                <span className="text-3xl font-semibold leading-none">{value}</span>
                {suffix ? <span className="pb-1 text-sm text-white/75">{suffix}</span> : null}
              </div>
            </div>
            <div className="rounded-2xl bg-white/15 p-2 ring-1 ring-white/20">{icon}</div>
          </div>
        </CardContent>
      </Card>
  );
}
