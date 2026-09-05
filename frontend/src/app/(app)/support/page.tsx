"use client";

import { type FormEvent, useEffect, useMemo, useState } from "react";
import { AlertCircle, CheckCircle2, Loader2, RefreshCw, ShieldAlert, Ticket, Wrench, Eye } from "lucide-react";
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
import Link from "next/link";
import TicketForm from "@/components/ui/ticket-form";

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
  const [activeView, setActiveView] = useState<"create" | "raise" | "mine" | "workbench">("create");
  const [supportTab, setSupportTab] = useState<"raise" | "mine">("raise");
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
      <Card className="border-0">
        <CardHeader className="flex min-h-[152px] flex-col gap-3 rounded-t-2xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-white/70">Support Portal</p>
            <h1 className="mt-2 text-3xl font-semibold">Support</h1>
            <p className="mt-2 max-w-2xl text-sm text-white/80">Raise a ticket or view your tickets.</p>
          </div>
          <div className="hidden sm:block">
            {/* placeholder for right-side actions if needed */}
          </div>
        </CardHeader>
      </Card>

      <Card className="border-zinc-200 shadow-md">
        <CardHeader className="border-b border-zinc-100">
          <CardTitle className="flex items-center gap-2">
            <Ticket className="h-5 w-5 text-blue-600" />
            Support
          </CardTitle>
        </CardHeader>
        <CardContent className="p-5">
          <div className="flex items-center gap-3 mb-4">
            <Button
              variant={supportTab === "raise" ? "default" : "ghost"}
              className={supportTab === "raise" ? "bg-gradient-to-r from-blue-600 to-indigo-600 text-white hover:from-blue-500 hover:to-indigo-500" : ""}
              onClick={() => setSupportTab("raise")}
            >
              Raise a Ticket
            </Button>

            <Button
              variant={supportTab === "mine" ? "default" : "ghost"}
              className={supportTab === "mine" ? "bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white hover:from-violet-500 hover:to-fuchsia-500" : ""}
              onClick={() => setSupportTab("mine")}
            >
              My Tickets
            </Button>
          </div>

          {supportTab === "raise" ? (
            <TicketForm />
          ) : (
            <div>
              <div className="flex items-center justify-between mb-4">
                <p className="text-sm text-zinc-500">Tickets you created.</p>
                <Button variant="outline" onClick={refreshMine}><RefreshCw className="mr-2 h-4 w-4" />Refresh</Button>
              </div>

              {myTickets.length === 0 ? (
                <div className="rounded-2xl border border-dashed border-zinc-200 bg-zinc-50 p-6 text-sm text-zinc-500">No support tickets yet.</div>
              ) : (
                <div className="overflow-x-auto rounded-2xl border border-zinc-200 bg-white">
                  <table className="w-full min-w-[1024px] text-sm">
                    <thead className="bg-gradient-to-r from-indigo-50 via-violet-50 to-cyan-50 text-left text-zinc-800">
                      <tr>
                        <th className="px-4 py-3 font-medium">Ticket</th>
                        <th className="px-4 py-3 font-medium">Summary</th>
                        <th className="px-4 py-3 font-medium">Queue</th>
                        <th className="px-4 py-3 font-medium">Priority</th>
                        <th className="px-4 py-3 font-medium">Created</th>
                        <th className="px-4 py-3 font-medium">Status</th>
                        <th className="px-4 py-3 font-medium">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {myTickets.map((ticket) => (
                        <tr key={ticket.id} className="border-t border-zinc-200 hover:bg-indigo-50/30">
                          <td className="px-4 py-3">
                            <p className="font-semibold text-zinc-900">{ticket.ticketNumber}</p>
                            <p className="text-xs text-zinc-600">{ticket.ticketType}</p>
                          </td>
                          <td className="px-4 py-3">
                            <p className="text-sm text-zinc-800">{ticket.shortDescription}</p>
                            <p className="text-xs text-zinc-500 truncate max-w-[320px]">{ticket.description}</p>
                          </td>
                          <td className="px-4 py-3 text-xs text-zinc-700">{ticket.queueTitle ?? ticket.queueCode ?? "-"}</td>
                          <td className="px-4 py-3 text-xs text-zinc-700">{ticket.priorityCode}</td>
                          <td className="px-4 py-3 text-xs text-zinc-700">{new Date(ticket.createdAt).toLocaleString()}</td>
                          <td className="px-4 py-3">
                            <Badge className={statusTone(ticket.status)}>{ticket.status}</Badge>
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex gap-2">
                              <Button
                                aria-label={`View ticket ${ticket.id}`}
                                className="h-9 w-9 rounded-full border-zinc-200 bg-zinc-50 p-0 text-zinc-700 hover:bg-zinc-100"
                                onClick={() => openTicket(ticket.id)}
                                size="sm"
                                title="View"
                                variant="outline"
                              >
                                <Ticket className="h-4 w-4" />
                              </Button>

                              {ticket.assigneeUserId == null && (
                                <Button
                                  className="h-9 rounded-full border-emerald-200 bg-emerald-50 px-3 text-emerald-700 hover:bg-emerald-100"
                                                                onClick={async () => {
                                                                  if (!token) return toast.error("Not authenticated.");
                                                                  try {
                                                                    await assignSupportTicket(token, ticket.id, {});
                                                                    toast.success("Ticket assigned.");
                                                                    await refreshMine();
                                                                  } catch (e) {
                                                                    toast.error("Unable to assign ticket.");
                                                                  }
                                                                }}
                                                                size="sm"
                                                                title="Auto assign"
                                                              >
                                                                <CheckCircle2 className="h-4 w-4" />
                                                              </Button>
                                                            )}

                              <Button
                                className="h-9 rounded-full border-violet-200 bg-violet-50 px-3 text-violet-700 hover:bg-violet-100"
                                onClick={() => openTicket(ticket.id)}
                                size="sm"
                                title="Comments"
                                variant="outline"
                              >
                                <Ticket className="h-4 w-4" />
                              </Button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Ticket details modal */}
      {selectedTicket ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm">
          <div className="max-h-[90vh] w-full max-w-4xl overflow-y-auto rounded-3xl border border-zinc-200 bg-white shadow-2xl shadow-slate-300/40">
            <div className="border-b border-zinc-200 bg-gradient-to-r from-indigo-600 via-violet-600 to-fuchsia-600 px-6 py-5 text-white">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h3 className="flex items-center gap-2 text-xl font-semibold">
                    <Eye className="h-5 w-5" />
                    Support Ticket Details {selectedTicket.ticketNumber ? `#${selectedTicket.ticketNumber}` : ""}
                  </h3>
                  <p className="mt-1 text-sm text-indigo-100">{selectedTicket.shortDescription}</p>
                </div>
                <Button className="border-white/30 bg-white/10 text-white hover:bg-white/20" onClick={() => setSelectedTicket(null)} variant="outline">
                  Close
                </Button>
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                <Badge className={statusTone(selectedTicket.status)}>{selectedTicket.status}</Badge>
              </div>
            </div>

            <div className="space-y-4 p-6">
              <div className="grid gap-3 md:grid-cols-2">
                <div className="rounded-2xl border border-zinc-200 bg-zinc-50/70 p-3 text-sm text-zinc-700">
                  <p className="text-xs font-semibold uppercase tracking-wider text-zinc-500">Ticket Number</p>
                  <p className="mt-1 font-medium text-zinc-900">{selectedTicket.ticketNumber}</p>
                </div>

                <div className="rounded-2xl border border-zinc-200 bg-zinc-50/70 p-3 text-sm text-zinc-700">
                  <p className="text-xs font-semibold uppercase tracking-wider text-zinc-500">Type</p>
                  <p className="mt-1 font-medium text-zinc-900">{selectedTicket.ticketType}</p>
                </div>

                <div className="rounded-2xl border border-zinc-200 bg-zinc-50/70 p-3 text-sm text-zinc-700">
                  <p className="text-xs font-semibold uppercase tracking-wider text-zinc-500">Queue</p>
                  <p className="mt-1 font-medium text-zinc-900">{selectedTicket.queueTitle ?? selectedTicket.queueCode ?? "-"}</p>
                </div>

                <div className="rounded-2xl border border-zinc-200 bg-zinc-50/70 p-3 text-sm text-zinc-700">
                  <p className="text-xs font-semibold uppercase tracking-wider text-zinc-500">Priority</p>
                  <p className="mt-1 font-medium text-zinc-900">{selectedTicket.priorityCode}</p>
                </div>

                <div className="rounded-2xl border border-zinc-200 bg-zinc-50/70 p-3 text-sm text-zinc-700">
                  <p className="text-xs font-semibold uppercase tracking-wider text-zinc-500">Assignee</p>
                  <p className="mt-1 font-medium text-zinc-900">{selectedTicket.assigneeFullName ?? selectedTicket.assigneeUsername ?? (selectedTicket.assigneeUserId ? String(selectedTicket.assigneeUserId) : "Unassigned")}</p>
                </div>

                <div className="rounded-2xl border border-zinc-200 bg-zinc-50/70 p-3 text-sm text-zinc-700">
                  <p className="text-xs font-semibold uppercase tracking-wider text-zinc-500">Created</p>
                  <p className="mt-1 font-medium text-zinc-900">{new Date(selectedTicket.createdAt).toLocaleString()}</p>
                </div>
              </div>

              <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-4 text-sm text-zinc-700">
                <p className="font-semibold text-zinc-900">Description</p>
                <p className="mt-1 whitespace-pre-wrap">{selectedTicket.description}</p>
              </div>

              <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-4 text-sm text-zinc-700">
                <p className="font-semibold text-zinc-900">Comments</p>
                <div className="mt-2 space-y-3 max-h-48 overflow-y-auto">
                  {selectedTicket.comments.length === 0 ? (
                    <div className="text-sm text-zinc-500">No comments yet.</div>
                  ) : (
                    selectedTicket.comments.map((c) => (
                      <div key={c.id} className="rounded-md border border-zinc-100 bg-white p-3 text-sm">
                        <div className="flex items-center justify-between">
                          <div className="text-zinc-700 font-medium">{c.actorUsername}</div>
                          <div className="text-xs text-zinc-500">{new Date(c.createdAt).toLocaleString()}</div>
                        </div>
                        <div className="mt-1 text-zinc-700">{c.commentText}</div>
                      </div>
                    ))
                  )}
                </div>

                <div className="mt-3 flex gap-2">
                  <Input placeholder="Add a comment" value={comment} onChange={(e) => setComment(e.target.value)} />
                  <Button onClick={handleComment} disabled={!comment.trim()}>Post</Button>
                  {selectedTicket.assigneeUserId == null && (
                    <Button variant="outline" onClick={async () => {
                      if (!token) return toast.error("Not authenticated.");
                      try {
                        await assignSupportTicket(token, selectedTicket.id, {});
                        toast.success("Ticket assigned.");
                        const refreshed = await getSupportTicketById(token, selectedTicket.id);
                        setSelectedTicket(refreshed);
                        await refreshMine();
                      } catch (e) {
                        toast.error("Unable to assign ticket.");
                      }
                    }}>Assign</Button>
                  )}
                </div>
              </div>

              <div className="mt-4 flex justify-end gap-2">
                <Button variant="outline" onClick={() => setSelectedTicket(null)}>Close</Button>
              </div>
            </div>
          </div>
        </div>
      ) : null}

    </div>
  );
}
