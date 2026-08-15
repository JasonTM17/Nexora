"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import type { KnowledgeBaseView } from "../../../../packages/contracts/src/generated/platform-api";
import { AppShell, PageGrid } from "../../../../packages/ui-core/src/app-shell";
import { ActionButton } from "../../../../packages/ui-core/src/action-button";
import { StatusLabel } from "../../../../packages/ui-core/src/status-label";

type Problem = { code: string; message: string; traceId?: string | null };
type State = "loading" | "ready" | "empty" | "denied" | "error";

async function readJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { ...init, credentials: "same-origin", headers: { Accept: "application/json", ...(init?.headers ?? {}) } });
  const payload = await response.json() as T | Problem;
  if (!response.ok) throw payload as Problem;
  return payload as T;
}

function asProblem(error: unknown): Problem {
  return error && typeof error === "object" ? error as Problem : { code: "REQUEST_FAILED", message: "We could not reach the knowledge workspace." };
}

export function KnowledgeWorkspace() {
  const [organizationId, setOrganizationId] = useState("");
  const [items, setItems] = useState<ReadonlyArray<KnowledgeBaseView>>([]);
  const [state, setState] = useState<State>("loading");
  const [problem, setProblem] = useState<Problem | null>(null);
  const [name, setName] = useState("");
  const [saving, setSaving] = useState(false);
  const heading = useRef<HTMLHeadingElement>(null);

  const setFailure = useCallback((error: unknown) => {
    const next = asProblem(error);
    setProblem(next);
    setState(next.code === "PERMISSION_DENIED" || next.code === "MEMBERSHIP_REQUIRED" ? "denied" : "error");
  }, []);

  const load = useCallback(async () => {
    setState("loading"); setProblem(null);
    try {
      const access = await readJson<{ memberships: ReadonlyArray<{ organizationId: string }>; tenantSelectionRequired?: boolean }>("/api/bff/access-context");
      const selected = organizationId || (access.tenantSelectionRequired ? "" : access.memberships[0]?.organizationId ?? "");
      setOrganizationId(selected);
      if (!selected) { setItems([]); setState("empty"); return; }
      const payload = await readJson<{ items: ReadonlyArray<KnowledgeBaseView> }>(`/api/bff/knowledge?organizationId=${encodeURIComponent(selected)}`);
      setItems(payload.items); setState("ready");
    } catch (error) { setFailure(error); }
  }, [organizationId, setFailure]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { heading.current?.focus(); }, [state]);

  async function create(event: React.FormEvent) {
    event.preventDefault();
    if (!name.trim() || !organizationId) return;
    setSaving(true); setProblem(null);
    try {
      await readJson("/api/bff/knowledge", {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ organizationId, name }),
      });
      setName("");
      await load();
    } catch (error) { setFailure(error); }
    finally { setSaving(false); }
  }

  return <AppShell><PageGrid><header className="nx-site-header"><Link className="nx-wordmark" href="/">Nexora</Link><nav className="nx-nav" aria-label="Knowledge workspace"><Link aria-current="page" href="/knowledge">Knowledge</Link><Link href="/ai">AI</Link><Link href="/account">Account</Link></nav></header><main id="main-content" className="nx-admin-page">
    <p className="nx-eyebrow">Knowledge workspace</p><h1 ref={heading} tabIndex={-1}>{state === "denied" ? "Access denied" : "Knowledge bases"}</h1><p className="nx-lede" aria-live="polite">{state === "denied" ? "You do not have current permission to manage knowledge in this organization." : "Documents are ingested into tenant-scoped knowledge bases with durable job progress."}</p>
    {state === "loading" && <section className="nx-access-card" role="status"><StatusLabel kind="loading" /> Loading the server-authorized knowledge workspace…</section>}
    {state === "empty" && <section className="nx-access-card"><StatusLabel kind="planned" /><h2>Choose an organization</h2><p>Choose an active organization in your account before accessing its knowledge bases.</p><Link className="nx-action-button nx-action-button--secondary" href="/account">Go to account</Link></section>}
    {(state === "denied" || state === "error") && <section className="nx-access-card nx-error-card" aria-live="assertive"><StatusLabel kind={state === "denied" ? "denied" : "error"} /><p>{problem?.message}</p>{problem?.traceId && <p className="nx-field-help">Reference: {problem.traceId}</p>}<ActionButton tone="secondary" onClick={() => void load()}>Retry</ActionButton></section>}
    {state === "ready" && <section className="nx-access-card" aria-labelledby="kb-title"><div className="nx-card-heading"><div><h2 id="kb-title">Tenant knowledge bases</h2><p className="nx-field-help">Server-derived organization context · knowledge.read</p></div><StatusLabel kind="fixture" /></div>{items.length === 0 ? <p className="nx-empty-copy">No knowledge bases yet. Create one to begin ingesting documents.</p> : <ul className="nx-kb-list">{items.map((item) => <li key={item.id}><code>{item.name}</code><small>{item.state} · version {item.version}</small></li>)}</ul>}
      <form className="nx-kb-form" onSubmit={create} aria-label="Create knowledge base">
        <label className="nx-admin-organization" htmlFor="kb-name">Knowledge base name<input id="kb-name" value={name} maxLength={200} required disabled={saving} onChange={(event) => setName(event.target.value)} placeholder="e.g. Product docs" /></label>
        <ActionButton disabled={saving || !name.trim()}>{saving ? "Creating…" : "Create knowledge base"}</ActionButton>
      </form></section>}
  </main></PageGrid></AppShell>;
}
