"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { AppShell, PageGrid } from "../../../../../packages/ui-core/src/app-shell";
import { ActionButton } from "../../../../../packages/ui-core/src/action-button";
import { StatusLabel } from "../../../../../packages/ui-core/src/status-label";
import type { RagTraceItem } from "../../../../../packages/ui-ai/src/trace-card";
import { TraceInspector } from "./trace-inspector";
import { EvaluationViewer, type EvaluationReportData } from "./evaluation-viewer";
import { FeedbackModeration, type FeedbackItem } from "./feedback-moderation";

type Tab = "traces" | "evaluation" | "feedback";
type Problem = { code: string; message: string; traceId?: string | null };
type PageState = "idle" | "loading" | "ready" | "denied" | "error";

async function readJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    credentials: "same-origin",
    headers: { Accept: "application/json", ...(init?.headers ?? {}) },
  });
  const payload = (await response.json()) as T | Problem;
  if (!response.ok) throw payload as Problem;
  return payload as T;
}

function asProblem(error: unknown): Problem {
  return error && typeof error === "object"
    ? (error as Problem)
    : { code: "REQUEST_FAILED", message: "Failed to connect to the RAG quality service." };
}

export function RagAdminDashboard() {
  const [tab, setTab] = useState<Tab>("traces");
  const [organizationId, setOrganizationId] = useState("");
  const [organizations, setOrganizations] = useState<ReadonlyArray<{ id: string; name: string }>>([]);
  const [pageState, setPageState] = useState<PageState>("idle");
  const [problem, setProblem] = useState<Problem | null>(null);

  const [traces, setTraces] = useState<ReadonlyArray<RagTraceItem>>([]);
  const [tracesLoading, setTracesLoading] = useState(false);

  const [evaluation, setEvaluation] = useState<EvaluationReportData | null>(null);
  const [evalLoading, setEvalLoading] = useState(false);

  const [feedback, setFeedback] = useState<ReadonlyArray<FeedbackItem>>([]);
  const [feedbackLoading, setFeedbackLoading] = useState(false);

  const headingRef = useRef<HTMLHeadingElement>(null);

  const setFailure = useCallback((error: unknown) => {
    const p = asProblem(error);
    setProblem(p);
    setPageState(p.code === "PERMISSION_DENIED" || p.code === "MEMBERSHIP_REQUIRED" ? "denied" : "error");
  }, []);

  const loadAccessContext = useCallback(async () => {
    try {
      const access = await readJson<{
        memberships: ReadonlyArray<{ organizationId: string; role?: string }>;
        tenantSelectionRequired?: boolean;
      }>("/api/bff/access-context");

      const orgs = access.memberships.map((m) => ({
        id: m.organizationId,
        name: `Organization (${m.organizationId.slice(0, 8)})`,
      }));
      setOrganizations(orgs);

      const selected = organizationId || (access.tenantSelectionRequired ? "" : access.memberships[0]?.organizationId ?? "");
      setOrganizationId(selected);

      if (selected) {
        setPageState("ready");
      } else {
        setPageState("idle");
      }
    } catch (err) {
      setFailure(err);
    }
  }, [organizationId, setFailure]);

  const loadTraces = useCallback(async () => {
    if (!organizationId) return;
    setTracesLoading(true);
    try {
      const data = await readJson<ReadonlyArray<RagTraceItem>>(`/api/bff/admin/rag/traces?organizationId=${encodeURIComponent(organizationId)}`);
      setTraces(data);
    } catch (err) {
      setFailure(err);
    } finally {
      setTracesLoading(false);
    }
  }, [organizationId, setFailure]);

  const loadEvaluation = useCallback(async () => {
    if (!organizationId) return;
    setEvalLoading(true);
    try {
      const data = await readJson<EvaluationReportData>(`/api/bff/admin/rag/evaluations?organizationId=${encodeURIComponent(organizationId)}`);
      setEvaluation(data);
    } catch (err) {
      setFailure(err);
    } finally {
      setEvalLoading(false);
    }
  }, [organizationId, setFailure]);

  const loadFeedback = useCallback(async () => {
    if (!organizationId) return;
    setFeedbackLoading(true);
    try {
      const data = await readJson<ReadonlyArray<FeedbackItem>>(`/api/bff/admin/rag/feedback?organizationId=${encodeURIComponent(organizationId)}`);
      setFeedback(data);
    } catch (err) {
      setFailure(err);
    } finally {
      setFeedbackLoading(false);
    }
  }, [organizationId, setFailure]);

  const handleDeleteFeedback = async (feedbackId: string) => {
    if (!organizationId) return;
    try {
      await readJson(`/api/bff/admin/rag/feedback?organizationId=${encodeURIComponent(organizationId)}&feedbackId=${encodeURIComponent(feedbackId)}`, {
        method: "DELETE",
      });
      setFeedback((prev) => prev.filter((f) => f.id !== feedbackId));
    } catch (err) {
      setFailure(err);
    }
  };

  useEffect(() => {
    void loadAccessContext();
  }, [loadAccessContext]);

  useEffect(() => {
    if (pageState === "ready" && organizationId) {
      if (tab === "traces") void loadTraces();
      if (tab === "evaluation") void loadEvaluation();
      if (tab === "feedback") void loadFeedback();
    }
  }, [pageState, organizationId, tab, loadTraces, loadEvaluation, loadFeedback]);

  return (
    <AppShell>
      <PageGrid>
        <header className="nx-site-header">
          <Link className="nx-wordmark" href="/">
            Nexora
          </Link>
          <nav className="nx-nav" aria-label="Admin navigation">
            <Link href="/admin">Admin</Link>
            <Link aria-current="page" href="/admin/rag">
              RAG Quality
            </Link>
            <Link href="/knowledge">Knowledge</Link>
            <Link href="/ai">AI</Link>
          </nav>
        </header>

        <main id="main-content" className="nx-admin-page">
          <p className="nx-eyebrow">Observability &amp; Quality Engineering</p>
          <h1 ref={headingRef} tabIndex={-1}>
            {pageState === "denied" ? "Access Denied" : "RAG Quality & Observability"}
          </h1>
          <p className="nx-lede">
            Inspect retrieval traces, review quality benchmarks, and moderate user feedback under tenant isolation.
          </p>

          {organizations.length > 0 && (
            <div className="nx-admin-organization">
              <label htmlFor="org-select">Active Organization</label>
              <select
                id="org-select"
                value={organizationId}
                onChange={(e) => {
                  setOrganizationId(e.target.value);
                  setPageState("ready");
                }}
              >
                {organizations.map((org) => (
                  <option key={org.id} value={org.id}>
                    {org.name}
                  </option>
                ))}
              </select>
            </div>
          )}

          {(pageState === "denied" || pageState === "error") && (
            <section className="nx-access-card nx-error-card" aria-live="assertive">
              <StatusLabel kind={pageState === "denied" ? "denied" : "error"} />
              <p>{problem?.message}</p>
              {problem?.traceId && <p className="nx-field-help">Reference: {problem.traceId}</p>}
              <ActionButton tone="secondary" onClick={() => void loadAccessContext()}>
                Retry
              </ActionButton>
            </section>
          )}

          {pageState === "idle" && !organizationId && (
            <section className="nx-access-card">
              <StatusLabel kind="planned" />
              <h2>Select an Organization</h2>
              <p>Please select an active organization to inspect RAG traces and quality metrics.</p>
              <Link className="nx-action-button nx-action-button--secondary" href="/account">
                Account Settings
              </Link>
            </section>
          )}

          {pageState === "ready" && (
            <div className="nx-admin-content-wrap">
              <div className="nx-tab-bar" role="tablist" aria-label="RAG Quality views">
                <button
                  type="button"
                  role="tab"
                  id="tab-traces"
                  aria-controls="panel-traces"
                  aria-selected={tab === "traces"}
                  className={`nx-tab-btn ${tab === "traces" ? "nx-tab-btn--active" : ""}`}
                  onClick={() => setTab("traces")}
                >
                  Retrieval Traces ({traces.length})
                </button>
                <button
                  type="button"
                  role="tab"
                  id="tab-eval"
                  aria-controls="panel-eval"
                  aria-selected={tab === "evaluation"}
                  className={`nx-tab-btn ${tab === "evaluation" ? "nx-tab-btn--active" : ""}`}
                  onClick={() => setTab("evaluation")}
                >
                  Quality Benchmarks
                </button>
                <button
                  type="button"
                  role="tab"
                  id="tab-feedback"
                  aria-controls="panel-feedback"
                  aria-selected={tab === "feedback"}
                  className={`nx-tab-btn ${tab === "feedback" ? "nx-tab-btn--active" : ""}`}
                  onClick={() => setTab("feedback")}
                >
                  User Feedback ({feedback.length})
                </button>
              </div>

              <div className="nx-tab-content">
                {tab === "traces" && (
                  <div id="panel-traces" role="tabpanel" aria-labelledby="tab-traces">
                    <TraceInspector traces={traces} loading={tracesLoading} onRefresh={() => void loadTraces()} />
                  </div>
                )}

                {tab === "evaluation" && (
                  <div id="panel-eval" role="tabpanel" aria-labelledby="tab-eval">
                    <EvaluationViewer report={evaluation} loading={evalLoading} onRefresh={() => void loadEvaluation()} />
                  </div>
                )}

                {tab === "feedback" && (
                  <div id="panel-feedback" role="tabpanel" aria-labelledby="tab-feedback">
                    <FeedbackModeration
                      feedbackList={feedback}
                      loading={feedbackLoading}
                      onDelete={(id) => void handleDeleteFeedback(id)}
                      onRefresh={() => void loadFeedback()}
                    />
                  </div>
                )}
              </div>
            </div>
          )}
        </main>
      </PageGrid>
    </AppShell>
  );
}
