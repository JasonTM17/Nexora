"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { AccessContextResponse } from "../../../../../packages/contracts/src/generated/platform-api";
import { AppShell, PageGrid } from "../../../../../packages/ui-core/src/app-shell";
import { ActionButton } from "../../../../../packages/ui-core/src/action-button";
import { StatusLabel } from "../../../../../packages/ui-core/src/status-label";
import { useI18n } from "../../../lib/i18n";

type Problem = { code: string; message: string; traceId?: string | null };
type State = "loading" | "ready" | "empty" | "denied" | "error";

interface CmsPage {
  id: string;
  title: string;
  slug: string;
  state: string;
  version: number;
  updatedAt: string;
}

async function readJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { ...init, credentials: "same-origin", headers: { Accept: "application/json", ...(init?.headers ?? {}) } });
  const payload = await response.json() as T | Problem;
  if (!response.ok) throw payload as Problem;
  return payload as T;
}

function asProblem(error: unknown): Problem {
  return error && typeof error === "object" ? error as Problem : { code: "REQUEST_FAILED", message: "We could not load pages." };
}

export default function PagesPage() {
  const { t } = useI18n();
  const [organizationId, setOrganizationId] = useState("");
  const [pages, setPages] = useState<ReadonlyArray<CmsPage>>([]);
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
      if (!selected) { setPages([]); setState("empty"); return; }
      const result = await readJson<{ items: ReadonlyArray<CmsPage> }>(`/api/bff/cms/pages?organizationId=${encodeURIComponent(selected)}`);
      setPages(result.items); setState("ready");
    } catch (error) { setFailure(error); }
  }, [organizationId, setFailure]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { heading.current?.focus(); }, [state]);

  return <AppShell><PageGrid><main className="nx-admin-page">
    <p className="nx-eyebrow">{t("admin.pages", { default: "Page management" })}</p>
    <h1 ref={heading} tabIndex={-1}>{t("admin.pages.title", { default: "Site pages" })}</h1>
    <p className="nx-lede">{t("admin.pages.lede", { default: "Create, edit, and publish pages for your site." })}</p>

    {state === "loading" && <section className="nx-access-card" role="status"><StatusLabel kind="loading" /> {t("common.loading")}</section>}
    {state === "empty" && <section className="nx-access-card"><StatusLabel kind="planned" /><h2>{t("account.selectOrganization")}</h2></section>}
    {(state === "denied" || state === "error") && <section className="nx-access-card nx-error-card" aria-live="assertive"><StatusLabel kind={state === "denied" ? "denied" : "error"} /><p>{problem?.message}</p><ActionButton tone="secondary" onClick={() => void load()}>{t("common.retry")}</ActionButton></section>}

    {state === "ready" && <section className="nx-access-card">
      <div className="nx-card-heading"><div><h2>{t("admin.pages.list", { default: "Pages" })}</h2><p className="nx-field-help">{pages.length} {t("admin.pages.count", { default: "pages" }).toLowerCase()}</p></div><StatusLabel kind="fixture" /></div>
      {pages.length === 0 ? <p className="nx-empty-copy">{t("admin.pages.empty", { default: "No pages yet. Create your first page." })}</p> :
        <ul className="nx-kb-list">{pages.map((page) => <li key={page.id}>
          <div><code>{page.title}</code><small>{page.slug} · {page.state} · v{page.version}</small></div>
          <ActionButton tone="secondary">{t("common.edit")}</ActionButton>
        </li>)}</ul>}
      <ActionButton>{t("admin.pages.create", { default: "Create page" })}</ActionButton>
    </section>}
  </main></PageGrid></AppShell>;
}
