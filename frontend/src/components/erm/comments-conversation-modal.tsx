"use client";

import { useState } from "react";
import { Send } from "lucide-react";

import { Button } from "@/components/ui/button";
import { FloatingTextareaField } from "@/components/ui/form-fields";
import type { OnboardingApprovalTrailItem } from "@/lib/api";

type CommentsConversationModalProps = {
  title: string;
  subtitle: string;
  items: OnboardingApprovalTrailItem[];
  canSendComment?: boolean;
  isSendingComment?: boolean;
  onSendComment?: (comment: string) => Promise<void>;
  onClose: () => void;
};

function decisionClasses(decision: string) {
  if (decision === "Approved") {
    return {
      pill: "bg-emerald-100 text-emerald-700",
      rail: "border-emerald-300",
      glow: "from-emerald-50 to-white",
    };
  }
  if (decision === "Rejected") {
    return {
      pill: "bg-rose-100 text-rose-700",
      rail: "border-rose-300",
      glow: "from-rose-50 to-white",
    };
  }
  if (decision === "Refer Back") {
    return {
      pill: "bg-violet-100 text-violet-700",
      rail: "border-violet-300",
      glow: "from-violet-50 to-white",
    };
  }
  return {
    pill: "bg-sky-100 text-sky-700",
    rail: "border-sky-300",
    glow: "from-sky-50 to-white",
  };
}

function formatDateTime(value: string | null) {
  if (!value) return "-";
  return new Date(value).toLocaleString("en-IN");
}

export function CommentsConversationModal({
  title,
  subtitle,
  items,
  canSendComment = false,
  isSendingComment = false,
  onSendComment,
  onClose,
}: CommentsConversationModalProps) {
  const [comment, setComment] = useState("");

  async function handleSend() {
    const trimmed = comment.trim();
    if (!trimmed || !onSendComment) {
      return;
    }
    await onSendComment(trimmed);
    setComment("");
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm">
      <div className="flex max-h-[85vh] w-full max-w-3xl flex-col overflow-hidden rounded-3xl border border-zinc-200 bg-white shadow-2xl shadow-slate-200/70">
        <div className="border-b border-zinc-200 bg-gradient-to-r from-slate-50 via-white to-blue-50 px-5 py-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h3 className="text-lg font-semibold text-zinc-900">{title}</h3>
              <p className="text-sm text-zinc-600">{subtitle}</p>
            </div>
            <Button onClick={onClose} variant="outline">
              Close
            </Button>
          </div>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto px-5 py-4">
          {items.length === 0 ? (
            <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-4 text-sm text-zinc-600">
              No comments available.
            </div>
          ) : (
            <div className="space-y-3">
              {items.map((item, index) => {
                const styles = decisionClasses(item.decision);
                return (
                  <div
                    className={`rounded-2xl border border-zinc-200 border-l-4 bg-gradient-to-r ${styles.glow} p-4 shadow-sm`}
                    key={`${item.actor}-${item.actionAt ?? "na"}-${index}`}
                  >
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                      <div className="space-y-1">
                        <p className="text-sm font-semibold text-zinc-900">{item.step}</p>
                        <p className="text-xs text-zinc-500">{item.actor}</p>
                      </div>
                      <div className="flex flex-wrap items-center gap-2 sm:justify-end">
                        <span className={`inline-flex rounded-full px-2.5 py-1 text-[11px] font-semibold ${styles.pill}`}>
                          {item.decision}
                        </span>
                        <span className="text-[11px] text-zinc-500">{formatDateTime(item.actionAt)}</span>
                      </div>
                    </div>
                    <div className="mt-3 rounded-xl bg-white/80 p-3 text-sm text-zinc-800">
                      {item.comment || "-"}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {canSendComment && onSendComment ? (
          <div className="border-t border-zinc-200 bg-white px-5 py-4">
            <div className="flex items-end gap-3">
              <FloatingTextareaField
                className="min-h-[72px] flex-1"
                label="Write a comment"
                onChange={(event) => setComment(event.target.value)}
                value={comment}
                wrapperClassName="flex-1"
              />
              <Button
                aria-label="Send comment"
                className="h-11 w-11 rounded-full bg-gradient-to-r from-blue-600 to-indigo-600 p-0 text-white hover:from-blue-500 hover:to-indigo-500"
                disabled={isSendingComment || !comment.trim()}
                onClick={() => void handleSend()}
                type="button"
              >
                {isSendingComment ? <span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" /> : <Send className="h-4 w-4" />}
              </Button>
            </div>
          </div>
        ) : null}
      </div>
    </div>
  );
}
