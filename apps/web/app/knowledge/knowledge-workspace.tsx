"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import type { KnowledgeBaseView } from "../../../../packages/contracts/src/generated/platform-api";
import { AppShell, PageGrid } from "../../../../packages/ui-core/src/app-shell";
import { ActionButton } from "../../../../packages/ui-core/src/action-button";
import { StatusLabel } from "../../../../packages/ui-core/src/status-label";
import { useI18n } from "../../lib/i18n";

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
  const { t } = useI18n();
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

  return <AppShell><PageGrid><header className="nx-site-header"><Link className="nx-wordmark" href="/">Nexora</Link><nav className="nx-nav" aria-label={t("knowledge.title")}><Link aria-current="page" href="/knowledge">{t("nav.knowledge")}</Link><Link href="/ai">{t("nav.ai")}</Link><Link href="/account">{t("nav.account")}</Link></nav></header><main id="main-content" className="nx-admin-page">
    <p className="nx-eyebrow">{t("knowledge.eyebrow")}</p><h1 ref={heading} tabIndex={-1}>{state === "denied" ? t("status.denied") : t("knowledge.title")}</h1><p className="nx-lede" aria-live="polite">{state === "denied" ? t("knowledge.accessDenied") : t("knowledge.lede")}</p>
    {state === "loading" && <section className="nx-access-card" role="status"><StatusLabel kind="loading" /> {t("common.loading")}</section>}
    {state === "empty" && <section className="nx-access-card"><StatusLabel kind="planned" /><h2>{t("account.selectOrganization")}</h2><p>{t("knowledge.organizationRequired")}</p><Link className="nx-action-button nx-action-button--secondary" href="/account">{t("account.title")}</Link></section>}
    {(state === "denied" || state === "error") && <section className="nx-access-card nx-error-card" aria-live="assertive"><StatusLabel kind={state === "denied" ? "denied" : "error"} /><p>{problem?.message}</p>{problem?.traceId && <p className="nx-field-help">Reference: {problem.traceId}</p>}<ActionButton tone="secondary" onClick={() => void load()}>{t("common.retry")}</ActionButton></section>}
    {state === "ready" && <section className="nx-access-card" aria-labelledby="kb-title"><div className="nx-card-heading"><div><h2 id="kb-title">{t("knowledge.title")}</h2><p className="nx-field-help">Server-derived organization context · knowledge.read</p></div><StatusLabel kind="fixture" /></div>{items.length === 0 ? <p className="nx-empty-copy">{t("knowledge.noBases")}</p> : <ul className="nx-kb-list">{items.map((item) => <li key={item.id}><code>{item.name}</code><small>{item.state} · {t("knowledge.version")} {item.version}</small></li>)}</ul>}
      <form className="nx-kb-form" onSubmit={create} aria-label={t("knowledge.create")}>
        <label className="nx-admin-organization" htmlFor="kb-name">{t("knowledge.create")}<input id="kb-name" value={name} maxLength={200} required disabled={saving} onChange={(event) => setName(event.target.value)} placeholder={t("knowledge.namePlaceholder")} /></label>
        <ActionButton disabled={saving || !name.trim()}>{saving ? t("common.loading") : t("knowledge.create")}</ActionButton>
      </form></section>}
  </main></PageGrid></AppShell>;
}
