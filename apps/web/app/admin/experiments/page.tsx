"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { AccessContextResponse } from "../../../../../packages/contracts/src/generated/platform-api";
import { AppShell, PageGrid } from "../../../../../packages/ui-core/src/app-shell";
import { ActionButton } from "../../../../../packages/ui-core/src/action-button";
import { StatusLabel } from "../../../../../packages/ui-core/src/status-label";

type Problem = { code: string; message: string; traceId?: string | null };
type State = "loading" | "ready" | "empty" | "denied" | "error";

interface ExperimentView {
  id: string;
  experimentKey: string;
  active: boolean;
  treatmentPercentage: number;
  description: string | null;
}

async function readJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { ...init, credentials: "same-origin", headers: { Accept: "application/json", ...(init?.headers ?? {}) } });
  const payload = await response.json() as T | Problem;
  if (!response.ok) throw payload as Problem;
  return payload as T;
}

function asProblem(error: unknown): Problem {
  return error && typeof error === "object" ? error as Problem : { code: "REQUEST_FAILED", message: "We could not load experiments." };
}

export default function ExperimentsPage() {
  const [organizationId, setOrganizationId] = useState("");
  const [experiments, setExperiments] = useState<ReadonlyArray<ExperimentView>>([]);
  const [state, setState] = useState<State>("loading");
  const [problem, setProblem] = useState<Problem | null>(null);
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
      if (!selected) { setExperiments([]); setState("empty"); return; }
      const result = await readJson<ReadonlyArray<ExperimentView>>(`/api/bff/experiments?organizationId=${encodeURIComponent(selected)}`);
      setExperiments(result); setState("ready");
    } catch (error) { setFailure(error); }
  }, [organizationId, setFailure]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { heading.current?.focus(); }, [state]);

  async function toggleExperiment(exp: ExperimentView) {
    try {
      await readJson("/api/bff/experiments", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ organizationId, experimentKey: exp.experimentKey, active: !exp.active, treatmentPercentage: exp.treatmentPercentage, description: exp.description }),
      });
      await load();
    } catch (error) { setFailure(error); }
  }

  return <AppShell><PageGrid><main className="nx-admin-page">
    <p className="nx-eyebrow">Experiments</p>
    <h1 ref={heading} tabIndex={-1}>A/B experiments</h1>
    <p className="nx-lede">Deterministic per-subject variant assignment.</p>

    {state === "loading" && <section className="nx-access-card" role="status"><StatusLabel kind="loading" /> Loading experiments…</section>}
    {state === "empty" && <section className="nx-access-card"><StatusLabel kind="planned" /><h2>Choose an organization</h2><p>Select an organization to manage its experiments.</p></section>}
    {(state === "denied" || state === "error") && <section className="nx-access-card nx-error-card" aria-live="assertive"><StatusLabel kind={state === "denied" ? "denied" : "error"} /><p>{problem?.message}</p><ActionButton tone="secondary" onClick={() => void load()}>Retry</ActionButton></section>}

    {state === "ready" && <section className="nx-access-card">
      <div className="nx-card-heading"><div><h2>Experiments</h2><p className="nx-field-help">{experiments.length} experiment{experiments.length !== 1 ? "s" : ""}</p></div><StatusLabel kind="fixture" /></div>
      {experiments.length === 0 ? <p className="nx-empty-copy">No experiments yet.</p> :
        <ul className="nx-flag-list">{experiments.map((exp) => <li key={exp.id}>
          <div><code>{exp.experimentKey}</code>{exp.description && <small>{exp.description}</small>}</div>
          <div className="nx-flag-controls">
            <span className={`nx-badge ${exp.active ? "nx-badge--on" : "nx-badge--off"}`}>{exp.active ? "Active" : "Off"}</span>
            {exp.active && <span className="nx-field-help">{exp.treatmentPercentage}% treatment</span>}
            <ActionButton tone="secondary" onClick={() => void toggleExperiment(exp)}>{exp.active ? "Pause" : "Activate"}</ActionButton>
          </div>
        </li>)}</ul>}
    </section>}
  </main></PageGrid></AppShell>;
}
