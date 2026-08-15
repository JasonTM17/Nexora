"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { AccessContextResponse } from "../../../../../packages/contracts/src/generated/platform-api";
import { AppShell, PageGrid } from "../../../../../packages/ui-core/src/app-shell";
import { ActionButton } from "../../../../../packages/ui-core/src/action-button";
import { StatusLabel } from "../../../../../packages/ui-core/src/status-label";

type Problem = { code: string; message: string; traceId?: string | null };
type State = "loading" | "ready" | "empty" | "denied" | "error";

interface FeatureFlagView {
  id: string;
  flagKey: string;
  enabled: boolean;
  rolloutPercentage: number;
  rules: Record<string, unknown>;
  description: string | null;
}

async function readJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { ...init, credentials: "same-origin", headers: { Accept: "application/json", ...(init?.headers ?? {}) } });
  const payload = await response.json() as T | Problem;
  if (!response.ok) throw payload as Problem;
  return payload as T;
}

function asProblem(error: unknown): Problem {
  return error && typeof error === "object" ? error as Problem : { code: "REQUEST_FAILED", message: "We could not load feature flags." };
}

export default function FeatureFlagsPage() {
  const [organizationId, setOrganizationId] = useState("");
  const [flags, setFlags] = useState<ReadonlyArray<FeatureFlagView>>([]);
  const [state, setState] = useState<State>("loading");
  const [problem, setProblem] = useState<Problem | null>(null);
  const [showCreate, setShowCreate] = useState(false);
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
      if (!selected) { setFlags([]); setState("empty"); return; }
      const result = await readJson<ReadonlyArray<FeatureFlagView>>(`/api/bff/feature-flags?organizationId=${encodeURIComponent(selected)}`);
      setFlags(result); setState("ready");
    } catch (error) { setFailure(error); }
  }, [organizationId, setFailure]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { heading.current?.focus(); }, [state]);

  async function toggleFlag(flag: FeatureFlagView) {
    try {
      await readJson("/api/bff/feature-flags", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          organizationId,
          flagKey: flag.flagKey,
          enabled: !flag.enabled,
          rolloutPercentage: flag.rolloutPercentage,
          description: flag.description,
        }),
      });
      await load();
    } catch (error) { setFailure(error); }
  }

  return <AppShell><PageGrid><main className="nx-admin-page">
    <p className="nx-eyebrow">Feature flags</p>
    <h1 ref={heading} tabIndex={-1}>Tenant feature flags</h1>
    <p className="nx-lede">Server-derived, deterministic per-subject evaluation.</p>

    {state === "loading" && <section className="nx-access-card" role="status"><StatusLabel kind="loading" /> Loading flags…</section>}
    {state === "empty" && <section className="nx-access-card"><StatusLabel kind="planned" /><h2>Choose an organization</h2><p>Select an organization in your account to manage its flags.</p></section>}
    {(state === "denied" || state === "error") && <section className="nx-access-card nx-error-card" aria-live="assertive"><StatusLabel kind={state === "denied" ? "denied" : "error"} /><p>{problem?.message}</p><ActionButton tone="secondary" onClick={() => void load()}>Retry</ActionButton></section>}

    {state === "ready" && <section className="nx-access-card">
      <div className="nx-card-heading"><div><h2>Flags</h2><p className="nx-field-help">{flags.length} flag{flags.length !== 1 ? "s" : ""}</p></div><StatusLabel kind="fixture" /></div>
      {flags.length === 0 ? <p className="nx-empty-copy">No feature flags yet.</p> :
        <ul className="nx-flag-list">{flags.map((flag) => <li key={flag.id}>
          <div><code>{flag.flagKey}</code>{flag.description && <small>{flag.description}</small>}</div>
          <div className="nx-flag-controls">
            <span className={`nx-badge ${flag.enabled ? "nx-badge--on" : "nx-badge--off"}`}>{flag.enabled ? "On" : "Off"}</span>
            {flag.enabled && <span className="nx-field-help">{flag.rolloutPercentage}% rollout</span>}
            <ActionButton tone="secondary" onClick={() => void toggleFlag(flag)}>{flag.enabled ? "Disable" : "Enable"}</ActionButton>
          </div>
        </li>)}</ul>}
    </section>}
  </main></PageGrid></AppShell>;
}
