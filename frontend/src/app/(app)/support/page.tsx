"use client";

import { type FormEvent, useEffect, useMemo, useState } from "react";
import { AlertCircle, CheckCircle2, Loader2, RefreshCw, ShieldAlert, Ticket, Wrench } from "lucide-react";
import { toast } from "sonner";

import {
  addSupportTicketComment,
  ApiError,
  assignSupportTicket,
  createSupportTicket,
  getSupportCatalog,
  getSupportTicketById,
  getSupportTickets,
  getSupportWorkbenchQueues,
  getSupportWorkbenchTickets,
  updateSupportTicketStatus,
  type SupportCatalog,
  type SupportQueueSummary,
  type SupportTicket,
} from "@/lib/api";
import { loadSession } from "@/lib/auth-storage";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Spinner } from "@/components/ui/spinner";

const initialForm = {
  ticketType: "SUPPORT_TICKET",
  categoryCode: "",
  subcategoryCode: "",
  impactLevel: "Medium",
  urgencyLevel: "Medium",
  shortDescription: "",
  description: "",
  source: "PORTAL",
};

function statusTone(status: SupportTicket["status"]) {
  if (["RESOLVED", "CLOSED"].includes(status)) return "border-emerald-200 bg-emerald-50 text-emerald-700";
  if (["REOPENED", "SECURITY_ESCALATED"].includes(status)) return "border-violet-200 bg-violet-50 text-violet-700";
  if (["CANCELLED"].includes(status)) return "border-rose-200 bg-rose-50 text-rose-700";
  return "border-amber-200 bg-amber-50 text-amber-700";
}

export default function SupportPage() {
  const [catalog, setCatalog] = useState<SupportCatalog | null>(null);
  const [myTickets, setMyTickets] = useState<SupportTicket[]>([]);
  const [selectedTicket, setSelectedTicket] = useState<SupportTicket | null>(null);
  const [workbenchQueues, setWorkbenchQueues] = useState<SupportQueueSummary[]>([]);
  const [workbenchQueueCode, setWorkbenchQueueCode] = useState("");
  const [workbenchTickets, setWorkbenchTickets] = useState<SupportTicket[]>([]);
  const [activeView, setActiveView] = useState<"create" | "mine" | "workbench">("create");
  const [form, setForm] = useState(initialForm);
  const [comment, setComment] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isTicketLoading, setIsTicketLoading] = useState(false);
  const [isWorkbenchLoading, setIsWorkbenchLoading] = useState(false);

  const session = useMemo(() => loadSession(), []);
  const token = session?.accessToken ?? null;
  const ticketCategories = useMemo(
    () => catalog?.categories.filter((item) => item.ticketType === form.ticketType) ?? [],
    [catalog, form.ticketType]
  );

  const loadAll = async () => {
    if (!token) {
      return;
    }
    setIsLoading(true);
    try {
      const [catalogResult, mineResult, queuesResult] = await Promise.allSettled([
        getSupportCatalog(token),
        getSupportTickets(token, { scope: "mine" }),
        getSupportWorkbenchQueues(token),
      ]);

      if (catalogResult.status === "fulfilled") {
        setCatalog(catalogResult.value);
        if (!form.categoryCode && catalogResult.value.categories.length > 0) {
          setForm((current) => ({
            ...current,
            categoryCode: catalogResult.value.categories[0].categoryCode,
          }));
        }
      }

      if (mineResult.status === "fulfilled") {
        setMyTickets(mineResult.value);
      }

      if (queuesResult.status === "fulfilled") {
        setWorkbenchQueues(queuesResult.value);
        if (!workbenchQueueCode && queuesResult.value.length > 0) {
          setWorkbenchQueueCode(queuesResult.value[0].queueCode);
        }
      }
      setActiveView("create");
    } catch (error) {
      if (error instanceof ApiError && error.status === 403) {
        toast.error("You are not authorized to access support.");
      } else {
        toast.error("Unable to load support portal.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!token || !workbenchQueueCode || workbenchQueues.length === 0) {
      return;
    }
    setIsWorkbenchLoading(true);
    getSupportWorkbenchTickets(token, workbenchQueueCode)
      .then(setWorkbenchTickets)
      .catch((error) => {
        if (error instanceof ApiError && error.status === 403) {
          setWorkbenchTickets([]);
        } else {
          toast.error("Unable to load workbench tickets.");
        }
      })
      .finally(() => setIsWorkbenchLoading(false));
  }, [token, workbenchQueueCode, workbenchQueues.length]);

  const refreshMine = async () => {
    if (!token) return;
    const tickets = await getSupportTickets(token, { scope: "mine" });
    setMyTickets(tickets);
  };

  const openTicket = async (ticketId: number) => {
    if (!token) return;
    setIsTicketLoading(true);
    try {
      const ticket = await getSupportTicketById(token, ticketId);
      setSelectedTicket(ticket);
      setComment("");
    } catch {
      toast.error("Unable to load ticket details.");
    } finally {
      setIsTicketLoading(false);
    }
  };

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!token) return;
    setIsSubmitting(true);
    try {
      const ticket = await createSupportTicket(token, form);
      toast.success("Support ticket created.");
      setMyTickets((current) => [ticket, ...current]);
      setSelectedTicket(ticket);
      setActiveView("mine");
      setForm((current) => ({
        ...initialForm,
        ticketType: current.ticketType,
        categoryCode: catalog?.categories.find((item) => item.ticketType === current.ticketType)?.categoryCode ?? "",
      }));
    } catch (error) {
      if (error instanceof ApiError) {
        toast.error(error.message);
      } else {
        toast.error("Unable to create support ticket.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleComment = async () => {
    if (!token || !selectedTicket || !comment.trim()) return;
    try {
      const updated = await addSupportTicketComment(token, selectedTicket.id, { comment });
      setSelectedTicket(updated);
      setComment("");
      await refreshMine();
    } catch {
      toast.error("Unable to post comment.");
    }
  };

  const handleStatus = async (status: SupportTicket["status"]) => {
    if (!token || !selectedTicket) return;
    try {
      const updated = await updateSupportTicketStatus(token, selectedTicket.id, { status });
      setSelectedTicket(updated);
      await refreshMine();
    } catch {
      toast.error("Unable to update status.");
    }
  };

  const handleAutoAssign = async () => {
    if (!token || !selectedTicket) return;
    try {
      const updated = await assignSupportTicket(token, selectedTicket.id, {});
      setSelectedTicket(updated);
      await refreshMine();
    } catch {
      toast.error("Unable to assign ticket.");
    }
  };

  if (isLoading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <Spinner />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Card className="border-0 bg-gradient-to-r from-blue-600 via-indigo-600 to-violet-600 text-white shadow-xl">
        <CardContent className="flex flex-col gap-4 p-6 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-white/70">Support Portal</p>
            <h1 className="mt-2 text-3xl font-semibold">Raise tickets, incidents, and security incidents</h1>
            <p className="mt-2 max-w-2xl text-sm text-white/80">
              ServiceNow-style support flow with auto-assignment, priority derivation, and workbench handling.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button className="bg-white text-zinc-900 hover:bg-white/90" onClick={() => setActiveView("create")}>Create</Button>
            <Button className="bg-white/15 text-white hover:bg-white/25" onClick={() => setActiveView("mine")}>My tickets</Button>
            <Button className="bg-white/15 text-white hover:bg-white/25" onClick={() => setActiveView("workbench")}>Workbench</Button>
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <Card className="border-zinc-200 shadow-md">
          <CardHeader className="border-b border-zinc-100">
            <CardTitle className="flex items-center gap-2">
              <Ticket className="h-5 w-5 text-blue-600" />
              {activeView === "create" ? "Create support ticket" : activeView === "mine" ? "My tickets" : "Support workbench"}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-5 p-5">
            {activeView === "create" ? (
              <form className="space-y-4" onSubmit={handleCreate}>
                <div className="grid gap-4 md:grid-cols-2">
                  <label className="space-y-2 text-sm">
                    <span className="font-medium">Ticket type</span>
                    <select
                      className="h-11 w-full rounded-xl border border-zinc-200 bg-white px-3"
                      value={form.ticketType}
                      onChange={(e) =>
                        setForm((current) => ({
                          ...current,
                          ticketType: e.target.value,
                          categoryCode:
                            catalog?.categories.find((item) => item.ticketType === e.target.value)?.categoryCode ?? "",
                        }))
                      }
                    >
                      <option value="SUPPORT_TICKET">Support ticket</option>
                      <option value="INCIDENT">Incident</option>
                      <option value="SECURITY_INCIDENT">Security incident</option>
                    </select>
                  </label>
                  <label className="space-y-2 text-sm">
                    <span className="font-medium">Category</span>
                    <select className="h-11 w-full rounded-xl border border-zinc-200 bg-white px-3" value={form.categoryCode} onChange={(e) => setForm({ ...form, categoryCode: e.target.value })}>
                      <option value="">Select category</option>
                      {ticketCategories.map((category) => (
                        <option key={category.categoryCode} value={category.categoryCode}>
                          {category.categoryTitle}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                  <label className="space-y-2 text-sm">
                    <span className="font-medium">Impact</span>
                    <select className="h-11 w-full rounded-xl border border-zinc-200 bg-white px-3" value={form.impactLevel} onChange={(e) => setForm({ ...form, impactLevel: e.target.value })}>
                      {(catalog?.impactLevels ?? ["Low", "Medium", "High", "Critical"]).map((value) => <option key={value}>{value}</option>)}
                    </select>
                  </label>
                  <label className="space-y-2 text-sm">
                    <span className="font-medium">Urgency</span>
                    <select className="h-11 w-full rounded-xl border border-zinc-200 bg-white px-3" value={form.urgencyLevel} onChange={(e) => setForm({ ...form, urgencyLevel: e.target.value })}>
                      {(catalog?.urgencyLevels ?? ["Low", "Medium", "High", "Critical"]).map((value) => <option key={value}>{value}</option>)}
                    </select>
                  </label>
                </div>

                <label className="space-y-2 text-sm">
                  <span className="font-medium">Subcategory</span>
                  <Input value={form.subcategoryCode} onChange={(e) => setForm({ ...form, subcategoryCode: e.target.value })} placeholder="Optional subcategory" />
                </label>

                <label className="space-y-2 text-sm">
                  <span className="font-medium">Short description</span>
                  <Input value={form.shortDescription} onChange={(e) => setForm({ ...form, shortDescription: e.target.value })} />
                </label>

                <label className="space-y-2 text-sm">
                  <span className="font-medium">Description</span>
                  <textarea
                    className="min-h-32 w-full rounded-xl border border-zinc-200 bg-white p-3 text-sm outline-none ring-0 focus:border-blue-500"
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                  />
                </label>

                <div className="flex items-center justify-between">
                  <p className="text-xs text-zinc-500">Priority and routing are derived automatically after submission.</p>
                  <Button disabled={isSubmitting} type="submit">
                    {isSubmitting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                    Submit
                  </Button>
                </div>
              </form>
            ) : activeView === "mine" ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <p className="text-sm text-zinc-500">Tickets you created.</p>
                  <Button variant="outline" onClick={refreshMine}>
                    <RefreshCw className="mr-2 h-4 w-4" />
                    Refresh
                  </Button>
                </div>
                <div className="space-y-3">
                  {myTickets.length === 0 ? (
                    <div className="rounded-2xl border border-dashed border-zinc-200 bg-zinc-50 p-6 text-sm text-zinc-500">
                      No support tickets yet.
                    </div>
                  ) : (
                    myTickets.map((ticket) => (
                      <button
                        className="w-full rounded-2xl border border-zinc-200 bg-white p-4 text-left shadow-sm transition hover:border-blue-200 hover:shadow-md"
                        key={ticket.id}
                        onClick={() => openTicket(ticket.id)}
                      >
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <p className="font-semibold text-zinc-900">{ticket.ticketNumber}</p>
                            <p className="text-sm text-zinc-500">{ticket.shortDescription}</p>
                          </div>
                          <Badge className={statusTone(ticket.status)}>{ticket.status}</Badge>
                        </div>
                        <div className="mt-3 flex flex-wrap gap-2 text-xs text-zinc-500">
                          <span>{ticket.queueTitle}</span>
                          <span>•</span>
                          <span>{ticket.priorityCode}</span>
                          <span>•</span>
                          <span>{ticket.ticketType}</span>
                        </div>
                      </button>
                    ))
                  )}
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <p className="text-sm text-zinc-500">Queues you can work on.</p>
                  <select
                    className="h-11 rounded-xl border border-zinc-200 bg-white px-3 text-sm"
                    value={workbenchQueueCode}
                    onChange={(e) => setWorkbenchQueueCode(e.target.value)}
                  >
                    {workbenchQueues.map((queue) => (
                      <option key={queue.queueCode} value={queue.queueCode}>
                        {queue.queueTitle}
                      </option>
                    ))}
                  </select>
                </div>

                {isWorkbenchLoading ? (
                  <Spinner />
                ) : (
                  <div className="space-y-3">
                    {workbenchTickets.length === 0 ? (
                      <div className="rounded-2xl border border-dashed border-zinc-200 bg-zinc-50 p-6 text-sm text-zinc-500">
                        No open tickets in this queue.
                      </div>
                    ) : (
                      workbenchTickets.map((ticket) => (
                        <button
                          className="w-full rounded-2xl border border-zinc-200 bg-white p-4 text-left shadow-sm transition hover:border-blue-200 hover:shadow-md"
                          key={ticket.id}
                          onClick={() => openTicket(ticket.id)}
                        >
                          <div className="flex items-start justify-between gap-3">
                            <div>
                              <p className="font-semibold text-zinc-900">{ticket.ticketNumber}</p>
                              <p className="text-sm text-zinc-500">{ticket.shortDescription}</p>
                            </div>
                            <Badge className={statusTone(ticket.status)}>{ticket.status}</Badge>
                          </div>
                          <div className="mt-3 flex flex-wrap gap-2 text-xs text-zinc-500">
                            <span>{ticket.queueTitle}</span>
                            <span>•</span>
                            <span>{ticket.assigneeFullName ?? "Unassigned"}</span>
                            <span>•</span>
                            <span>{ticket.priorityCode}</span>
                          </div>
                        </button>
                      ))
                    )}
                  </div>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="border-zinc-200 shadow-md">
          <CardHeader className="border-b border-zinc-100">
            <CardTitle className="flex items-center gap-2">
              <ShieldAlert className="h-5 w-5 text-violet-600" />
              Ticket details
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 p-5">
            {isTicketLoading ? (
              <Spinner />
            ) : selectedTicket ? (
              <>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="text-lg font-semibold text-zinc-900">{selectedTicket.ticketNumber}</p>
                    <p className="text-sm text-zinc-500">{selectedTicket.shortDescription}</p>
                  </div>
                  <Badge className={statusTone(selectedTicket.status)}>{selectedTicket.status}</Badge>
                </div>

                <div className="grid gap-3 text-sm sm:grid-cols-2">
                  <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-3">
                    <p className="text-xs uppercase tracking-wide text-zinc-500">Queue</p>
                    <p className="font-medium">{selectedTicket.queueTitle}</p>
                  </div>
                  <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-3">
                    <p className="text-xs uppercase tracking-wide text-zinc-500">Priority</p>
                    <p className="font-medium">{selectedTicket.priorityCode}</p>
                  </div>
                  <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-3">
                    <p className="text-xs uppercase tracking-wide text-zinc-500">Assignee</p>
                    <p className="font-medium">{selectedTicket.assigneeFullName ?? "Unassigned"}</p>
                  </div>
                  <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-3">
                    <p className="text-xs uppercase tracking-wide text-zinc-500">SLA response due</p>
                    <p className="font-medium">{selectedTicket.responseDueAt ? new Date(selectedTicket.responseDueAt).toLocaleString() : "—"}</p>
                  </div>
                </div>

                <div className="rounded-2xl border border-zinc-200 p-4">
                  <p className="text-sm font-medium text-zinc-900">Description</p>
                  <p className="mt-1 text-sm text-zinc-600 whitespace-pre-wrap">{selectedTicket.description}</p>
                </div>

                <div className="flex flex-wrap gap-2">
                  <Button variant="outline" onClick={handleAutoAssign}>
                    <Wrench className="mr-2 h-4 w-4" />
                    Auto assign
                  </Button>
                  <Button variant="outline" onClick={() => handleStatus("IN_PROGRESS")}>
                    <CheckCircle2 className="mr-2 h-4 w-4" />
                    Mark in progress
                  </Button>
                  <Button variant="outline" onClick={() => handleStatus("RESOLVED")}>
                    Resolve
                  </Button>
                  <Button variant="outline" onClick={() => handleStatus("CLOSED")}>
                    Close
                  </Button>
                </div>

                <div className="space-y-2">
                  <p className="text-sm font-medium text-zinc-900">Add comment</p>
                  <div className="space-y-2">
                    <textarea
                      className="min-h-24 w-full rounded-xl border border-zinc-200 bg-white p-3 text-sm outline-none focus:border-blue-500"
                      value={comment}
                      onChange={(e) => setComment(e.target.value)}
                    />
                    <div className="flex justify-end">
                      <Button onClick={handleComment}>Post comment</Button>
                    </div>
                  </div>
                </div>

                <div className="space-y-3">
                  <p className="text-sm font-medium text-zinc-900">Activity</p>
                  {selectedTicket.comments.length === 0 ? (
                    <div className="rounded-2xl border border-dashed border-zinc-200 bg-zinc-50 p-4 text-sm text-zinc-500">
                      No activity yet.
                    </div>
                  ) : (
                    selectedTicket.comments.map((item) => (
                      <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-3" key={item.id}>
                        <div className="flex items-start justify-between gap-3">
                          <p className="text-sm font-medium text-zinc-900">{item.actorUsername}</p>
                          <span className="text-xs text-zinc-500">{new Date(item.createdAt).toLocaleString()}</span>
                        </div>
                        <p className="text-xs uppercase tracking-wide text-zinc-500">{item.actionType}</p>
                        <p className="mt-1 text-sm text-zinc-600">{item.commentText ?? "—"}</p>
                      </div>
                    ))
                  )}
                </div>
              </>
            ) : (
              <div className="rounded-2xl border border-dashed border-zinc-200 bg-zinc-50 p-6 text-sm text-zinc-500">
                <div className="flex items-center gap-2 text-zinc-800">
                  <AlertCircle className="h-4 w-4 text-amber-500" />
                  Select a ticket to see details.
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
