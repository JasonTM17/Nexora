"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { AccessContextResponse } from "../../../../../packages/contracts/src/generated/platform-api";
import { AppShell, PageGrid } from "../../../../../packages/ui-core/src/app-shell";
import { ActionButton } from "../../../../../packages/ui-core/src/action-button";
import { StatusLabel } from "../../../../../packages/ui-core/src/status-label";
import { useI18n } from "../../lib/i18n";

type Problem = { code: string; message: string; traceId?: string | null };
type State = "loading" | "ready" | "empty" | "denied" | "error";

interface EventCount {
  eventType: string;
  count: number;
}

async function readJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { ...init, credentials: "same-origin", headers: { Accept: "application/json", ...(init?.headers ?? {}) } });
  const payload = await response.json() as T | Problem;
  if (!response.ok) throw payload as Problem;
  return payload as T;
}

function asProblem(error: unknown): Problem {
  return error && typeof error === "object" ? error as Problem : { code: "REQUEST_FAILED", message: "We could not load analytics." };
}

export default function AnalyticsPage() {
  const { t } = useI18n();
  const [organizationId, setOrganizationId] = useState("");
  const [aggregation, setAggregation] = useState<ReadonlyArray<EventCount>>([]);
  const [state, setState] = useState<State>("loading");
  const [problem, setProblem] = useState<Problem | null>(null);
  const [days, setDays] = useState(7);
  const heading = useRef<HTMLHeadingElement>(null);

  const setFailure = useCallback((error: unknown) => {
    const next = asProblem(error);
    setProblem(next);
    setState(next.code === "PERMISSION_DENIED" || next.code === "MEMBERSHIP_REQUIRED" ? "denied" : "error");
  }, []);

  const load = useCallback(async () => {
    setState("loading"); setProblem(null);
    try {
      const access = await readJson<AccessContextResponse>("/api/bff/access-context");
      const selected = organizationId || (access.tenantSelectionRequired ? "" : access.memberships[0]?.organizationId ?? "");
      setOrganizationId(selected);
      if (!selected) { setAggregation([]); setState("empty"); return; }
      const since = new Date(Date.now() - days * 24 * 60 * 60 * 1000).toISOString();
      const result = await readJson<ReadonlyArray<EventCount>>(`/api/bff/analytics?organizationId=${encodeURIComponent(selected)}&since=${encodeURIComponent(since)}`);
      setAggregation(result); setState("ready");
    } catch (error) { setFailure(error); }
  }, [organizationId, setFailure, days]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { heading.current?.focus(); }, [state]);

  const totalEvents = aggregation.reduce((sum, e) => sum + e.count, 0);

  return <AppShell><PageGrid><main className="nx-admin-page">
    <p className="nx-eyebrow">{t("analytics.eyebrow")}</p>
    <h1 ref={heading} tabIndex={-1}>{t("analytics.title")}</h1>
    <p className="nx-lede">{t("analytics.lede")}</p>

    {state === "loading" && <section className="nx-access-card" role="status"><StatusLabel kind="loading" /> {t("common.loading")}</section>}
    {state === "empty" && <section className="nx-access-card"><StatusLabel kind="planned" /><h2>{t("account.selectOrganization")}</h2></section>}
    {(state === "denied" || state === "error") && <section className="nx-access-card nx-error-card" aria-live="assertive"><StatusLabel kind={state === "denied" ? "denied" : "error"} /><p>{problem?.message}</p><ActionButton tone="secondary" onClick={() => void load()}>{t("common.retry")}</ActionButton></section>}

    {state === "ready" && <section className="nx-access-card">
      <div className="nx-card-heading">
        <div><h2>{t("analytics.eventBreakdown")}</h2><p className="nx-field-help">{t("analytics.totalEvents", { total: totalEvents, days })}</p></div>
        <StatusLabel kind="fixture" />
      </div>
      <div className="nx-analytics-controls">
        {[7, 14, 30].map((d) => (
          <ActionButton key={d} tone={days === d ? "primary" : "secondary"} onClick={() => setDays(d)}>{t("analytics.window", { days: d })}</ActionButton>
        ))}
      </div>
      {aggregation.length === 0 ? <p className="nx-empty-copy">{t("analytics.noEvents")}</p> :
        <ul className="nx-analytics-bars">{aggregation.map((event) => {
          const pct = totalEvents > 0 ? Math.round((event.count / totalEvents) * 100) : 0;
          return <li key={event.eventType}>
            <div className="nx-analytics-label"><code>{event.eventType}</code><span>{event.count}</span></div>
            <div className="nx-analytics-bar-track"><div className="nx-analytics-bar-fill" style={{ width: `${pct}%` }} /></div>
          </li>;
        })}</ul>}
    </section>}
  </main></PageGrid></AppShell>;
}
