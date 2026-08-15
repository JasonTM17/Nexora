"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import type { RagAnswer } from "../../../../packages/contracts/src/generated/platform-api";
import { AppShell, PageGrid } from "../../../../packages/ui-core/src/app-shell";
import { ActionButton } from "../../../../packages/ui-core/src/action-button";
import { StatusLabel } from "../../../../packages/ui-core/src/status-label";
import { Citation, type CitationState } from "../../../../packages/ui-ai/src/citation";
import { useI18n } from "../../lib/i18n";

type Problem = { code: string; message: string; traceId?: string | null };
type State = "idle" | "loading" | "answered" | "no-answer" | "denied" | "error";

async function readJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { ...init, credentials: "same-origin", headers: { Accept: "application/json", ...(init?.headers ?? {}) } });
  const payload = await response.json() as T | Problem;
  if (!response.ok) throw payload as Problem;
  return payload as T;
}

function asProblem(error: unknown): Problem {
  return error && typeof error === "object" ? error as Problem : { code: "REQUEST_FAILED", message: "We could not reach the knowledge assistant." };
}

export function KnowledgeChat() {
  const { t } = useI18n();
  const [organizationId, setOrganizationId] = useState("");
  const [query, setQuery] = useState("");
  const [answer, setAnswer] = useState<RagAnswer | null>(null);
  const [state, setState] = useState<State>("idle");
  const [problem, setProblem] = useState<Problem | null>(null);
  const heading = useRef<HTMLHeadingElement>(null);

  const setFailure = useCallback((error: unknown) => {
    const next = asProblem(error);
    setProblem(next);
    setState(next.code === "PERMISSION_DENIED" || next.code === "MEMBERSHIP_REQUIRED" ? "denied" : "error");
  }, []);

  const loadContext = useCallback(async () => {
    try {
      const access = await readJson<{ memberships: ReadonlyArray<{ organizationId: string }>; tenantSelectionRequired?: boolean }>("/api/bff/access-context");
      const selected = organizationId || (access.tenantSelectionRequired ? "" : access.memberships[0]?.organizationId ?? "");
      setOrganizationId(selected);
      if (!selected) setState("idle");
    } catch (error) { setFailure(error); }
  }, [organizationId, setFailure]);

  useEffect(() => { void loadContext(); }, [loadContext]);
  useEffect(() => { heading.current?.focus(); }, [state]);

  async function ask(event: React.FormEvent) {
    event.preventDefault();
    if (!query.trim() || !organizationId) return;
    setState("loading"); setProblem(null); setAnswer(null);
    try {
      const result = await readJson<RagAnswer>("/api/bff/rag", {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ organizationId, query }),
      });
      setAnswer(result);
      setState(result.outcome === "ANSWERED" ? "answered" : "no-answer");
    } catch (error) { setFailure(error); }
  }

  const citationState: CitationState = answer && answer.citations && answer.citations.length > 0 ? "available" : answer ? "denied" : "no-evidence";

  return <AppShell><PageGrid><header className="nx-site-header"><Link className="nx-wordmark" href="/">Nexora</Link><nav className="nx-nav" aria-label="Knowledge assistant"><Link href="/knowledge">Knowledge</Link><Link aria-current="page" href="/ai">AI</Link><Link href="/account">Account</Link></nav></header><main id="main-content" className="nx-admin-page">
    <p className="nx-eyebrow">{t("ai.eyebrow")}</p><h1 ref={heading} tabIndex={-1}>{state === "denied" ? t("status.denied") : t("ai.title")}</h1><p className="nx-lede" aria-live="polite">{state === "denied" ? t("ai.accessDenied") : t("ai.lede")}</p>
    <form className="nx-kb-form" onSubmit={ask} aria-label={t("ai.title")}>
      <label className="nx-admin-organization" htmlFor="rag-query">{t("ai.askPlaceholder")}<textarea id="rag-query" value={query} maxLength={2000} required rows={3} disabled={state === "loading" || !organizationId} onChange={(event) => setQuery(event.target.value)} placeholder={t("ai.askPlaceholder")} /></label>
      <ActionButton disabled={state === "loading" || !query.trim() || !organizationId}>{state === "loading" ? t("common.loading") : t("ai.ask")}</ActionButton>
    </form>
    {state === "answered" && answer && <article className="nx-ai-response" aria-live="polite"><p>{answer.content}</p><Citation state={citationState} title={answer.citations?.[0] ?? "Authorized source"} /><p className="nx-field-help">Model {answer.modelId} · {answer.tokenCount} tokens</p></article>}
    {state === "no-answer" && <article className="nx-ai-response" aria-live="polite"><StatusLabel kind="empty" /><p>{t("ai.noAnswer")}</p></article>}
    {(state === "denied" || state === "error") && <section className="nx-access-card nx-error-card" aria-live="assertive"><StatusLabel kind={state === "denied" ? "denied" : "error"} /><p>{problem?.message}</p>{problem?.traceId && <p className="nx-field-help">Reference: {problem.traceId}</p>}<ActionButton tone="secondary" onClick={() => void loadContext()}>{t("common.retry")}</ActionButton></section>}
    {state === "idle" && !organizationId && <section className="nx-access-card"><StatusLabel kind="planned" /><h2>{t("account.selectOrganization")}</h2><p>{t("ai.lede")}</p><Link className="nx-action-button nx-action-button--secondary" href="/account">{t("account.title")}</Link></section>}
  </main></PageGrid></AppShell>;
}
